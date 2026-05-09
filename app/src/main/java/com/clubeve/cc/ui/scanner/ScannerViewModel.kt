package com.clubeve.cc.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.IdOnly
import com.clubeve.cc.models.Profile
import com.clubeve.cc.models.Registration
import com.clubeve.cc.models.ScanResult
import com.clubeve.cc.models.Event
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ScannerUiState(
    val selectedTab: Int = 0,           // 0 = QR, 1 = Manual USN
    val scanResult: ScanResult? = null,
    val isProcessing: Boolean = false,
    val manualUsn: String = "",
    val snackbarMessage: String? = null,
    val cameraActive: Boolean = true    // paused after scan until reset
)

class ScannerViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    fun selectTab(tab: Int) = _state.update { it.copy(selectedTab = tab, scanResult = null, cameraActive = true) }
    fun onManualUsnChange(value: String) = _state.update { it.copy(manualUsn = value) }
    fun clearSnackbar() = _state.update { it.copy(snackbarMessage = null) }

    fun resetScanner() {
        _state.update { it.copy(scanResult = null, cameraActive = true, isProcessing = false) }
    }

    fun onQrScanned(rawValue: String, eventId: String) {
        if (_state.value.isProcessing || !_state.value.cameraActive) return
        _state.update { it.copy(isProcessing = true, cameraActive = false) }
        viewModelScope.launch {
            val token = extractToken(rawValue)
            val result = lookupQRToken(token, eventId)
            _state.update { it.copy(scanResult = result, isProcessing = false) }
        }
    }

    fun findByManualUsn(eventId: String) {
        val usn = _state.value.manualUsn.trim()
        if (usn.isBlank()) return
        _state.update { it.copy(isProcessing = true, scanResult = null) }
        viewModelScope.launch {
            val result = manualCheckInByUSN(usn, eventId)
            _state.update { it.copy(scanResult = result, isProcessing = false) }
        }
    }

    fun confirmCheckIn(registrationId: String, studentName: String) {
        viewModelScope.launch {
            try {
                client.from("registrations").update({
                    set("checked_in", true)
                    set("checked_in_at", Instant.now().toString())
                }) {
                    filter { eq("id", registrationId) }
                }
                _state.update {
                    it.copy(
                        snackbarMessage = "✓ $studentName checked in successfully",
                        scanResult = null,
                        cameraActive = true,
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        scanResult = ScanResult.Error("Check-in failed: ${e.message}"),
                        isProcessing = false
                    )
                }
            }
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private fun extractToken(scannedText: String): String {
        val regex = Regex("token=([a-zA-Z0-9\\-]+)")
        val match = regex.find(scannedText)
        return match?.groupValues?.get(1) ?: scannedText
    }

    private suspend fun validatePRAssignment(prUserId: String, eventId: String): Boolean {
        return try {
            val result = client.from("pr_event_assignments")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("pr_id", prUserId)
                        eq("event_id", eventId)
                    }
                }
                .decodeList<IdOnly>()
            result.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun lookupQRToken(token: String, eventId: String): ScanResult {
        val userId = SessionManager.currentUserId
        if (!validatePRAssignment(userId, eventId)) {
            return ScanResult.Error("Access denied: you are not assigned to this event.")
        }

        return try {
            val registration = client.from("registrations")
                .select(columns = Columns.list("id", "checked_in", "checked_in_at", "student_id", "event_id")) {
                    filter { eq("qr_token", token) }
                }
                .decodeSingleOrNull<Registration>()
                ?: return ScanResult.Error("Invalid QR code. No registration found.")

            if (registration.eventId != eventId) {
                return ScanResult.Error("This QR code belongs to a different event.")
            }

            val student = client.from("profiles")
                .select(columns = Columns.list("id", "full_name", "usn", "department", "semester", "year")) {
                    filter { eq("id", registration.studentId) }
                }
                .decodeSingleOrNull<Profile>()

            val event = client.from("events")
                .select(columns = Columns.list("id", "title", "location")) {
                    filter { eq("id", registration.eventId) }
                }
                .decodeSingleOrNull<Event>()

            ScanResult.Success(
                registrationId = registration.id,
                eventId = registration.eventId,
                alreadyCheckedIn = registration.checkedIn,
                checkedInAt = registration.checkedInAt,
                studentName = student?.fullName ?: "Unknown",
                studentUsn = student?.usn ?: "Unknown",
                studentDepartment = student?.department ?: "Unknown",
                studentSemester = student?.semester ?: 0,
                eventTitle = event?.title ?: "Unknown Event",
                eventLocation = event?.location
            )
        } catch (e: Exception) {
            ScanResult.Error(e.message ?: "An error occurred.")
        }
    }

    private suspend fun manualCheckInByUSN(usn: String, eventId: String): ScanResult {
        val userId = SessionManager.currentUserId
        if (!validatePRAssignment(userId, eventId)) {
            return ScanResult.Error("Access denied: you are not assigned to this event.")
        }

        return try {
            val student = client.from("profiles")
                .select(columns = Columns.list("id", "full_name", "usn", "department", "semester", "year")) {
                    filter { eq("usn", usn.uppercase().trim()) }
                }
                .decodeSingleOrNull<Profile>()
                ?: return ScanResult.Error("No student found with USN: $usn")

            val registration = client.from("registrations")
                .select(columns = Columns.list("id", "checked_in", "checked_in_at", "event_id", "student_id")) {
                    filter {
                        eq("student_id", student.id)
                        eq("event_id", eventId)
                    }
                }
                .decodeSingleOrNull<Registration>()
                ?: return ScanResult.Error("${student.fullName} ($usn) is not registered for this event.")

            ScanResult.Success(
                registrationId = registration.id,
                eventId = eventId,
                alreadyCheckedIn = registration.checkedIn,
                checkedInAt = registration.checkedInAt,
                studentName = student.fullName,
                studentUsn = student.usn,
                studentDepartment = student.department,
                studentSemester = student.semester,
                eventTitle = "",
                eventLocation = null
            )
        } catch (e: Exception) {
            ScanResult.Error(e.message ?: "An error occurred.")
        }
    }
}
