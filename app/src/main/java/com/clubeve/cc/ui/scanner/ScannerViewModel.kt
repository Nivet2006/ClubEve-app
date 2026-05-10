package com.clubeve.cc.ui.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.IdOnly
import com.clubeve.cc.models.Profile
import com.clubeve.cc.models.Registration
import com.clubeve.cc.models.ScanResult
import com.clubeve.cc.models.Event
import com.clubeve.cc.utils.NetworkMonitor
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Shown briefly in the QR tab after each auto-confirmed scan. Null = nothing to show. */
data class ScanFlash(
    val studentName: String,
    val studentUsn: String,
    val isAlreadyCheckedIn: Boolean,
    val isOffline: Boolean,
    val isError: Boolean,
    val errorMessage: String = ""
)

data class ScannerUiState(
    val selectedTab: Int = 0,           // 0 = QR, 1 = Manual USN
    val scanResult: ScanResult? = null, // used by Manual USN tab only
    val isProcessing: Boolean = false,
    val manualUsn: String = "",
    val snackbarMessage: String? = null,
    val cameraActive: Boolean = true,
    val isOfflineCheckIn: Boolean = false,
    val scanFlash: ScanFlash? = null,   // transient overlay for continuous QR mode
    val batchCount: Int = 0             // running total of check-ins this session
)

class ScannerViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(ScannerUiState())
    val state: StateFlow<ScannerUiState> = _state.asStateFlow()

    /**
     * Tokens scanned recently — prevents the same QR from being processed twice
     * while the camera is still pointing at it. Entries expire after COOLDOWN_MS.
     */
    private val recentTokens = mutableMapOf<String, Long>()
    private val COOLDOWN_MS = 5_000L

    fun selectTab(tab: Int) = _state.update {
        it.copy(selectedTab = tab, scanResult = null, scanFlash = null, cameraActive = true)
    }
    fun onManualUsnChange(value: String) = _state.update { it.copy(manualUsn = value) }
    fun clearSnackbar() = _state.update { it.copy(snackbarMessage = null) }
    fun clearFlash() = _state.update { it.copy(scanFlash = null) }

    fun resetScanner() {
        _state.update {
            it.copy(
                scanResult = null,
                scanFlash = null,
                cameraActive = true,
                isProcessing = false,
                isOfflineCheckIn = false
            )
        }
    }

    // ─── Continuous QR mode ─────────────────────────────────────────────────

    /**
     * Called by the camera analyzer on every detected QR code.
     * Auto-confirms the check-in immediately — no user tap required.
     * Duplicate scans within COOLDOWN_MS are silently ignored.
     */
    fun onQrScanned(rawValue: String, eventId: String, context: Context) {
        val token = extractToken(rawValue)

        // Cooldown dedup — same token within 5 s is ignored
        val now = System.currentTimeMillis()
        val lastSeen = recentTokens[token]
        if (lastSeen != null && now - lastSeen < COOLDOWN_MS) return
        recentTokens[token] = now

        // Prune stale entries so the map doesn't grow unbounded
        recentTokens.entries.removeIf { now - it.value >= COOLDOWN_MS }

        viewModelScope.launch {
            val isOnline = NetworkMonitor(context).isOnline()
            val lookupResult = if (isOnline) {
                lookupQRTokenOnline(token, eventId)
            } else {
                lookupQRTokenOffline(token, eventId, context)
            }

            when (lookupResult) {
                is ScanResult.Success -> {
                    if (lookupResult.alreadyCheckedIn) {
                        // Already in — flash a warning, no write needed
                        _state.update {
                            it.copy(
                                scanFlash = ScanFlash(
                                    studentName = lookupResult.studentName,
                                    studentUsn = lookupResult.studentUsn,
                                    isAlreadyCheckedIn = true,
                                    isOffline = false,
                                    isError = false
                                )
                            )
                        }
                    } else {
                        // Auto-confirm: write the check-in immediately
                        performCheckIn(
                            registrationId = lookupResult.registrationId,
                            studentName = lookupResult.studentName,
                            studentUsn = lookupResult.studentUsn,
                            isOnline = isOnline,
                            context = context
                        )
                    }
                }
                is ScanResult.Error -> {
                    _state.update {
                        it.copy(
                            scanFlash = ScanFlash(
                                studentName = "",
                                studentUsn = "",
                                isAlreadyCheckedIn = false,
                                isOffline = false,
                                isError = true,
                                errorMessage = lookupResult.message
                            )
                        )
                    }
                }
            }
        }
    }

    /** Writes the check-in and emits a flash + haptic signal. */
    private suspend fun performCheckIn(
        registrationId: String,
        studentName: String,
        studentUsn: String,
        isOnline: Boolean,
        context: Context
    ) {
        val now = Instant.now().toString()
        val db = AppDatabase.getInstance(context)

        if (isOnline) {
            try {
                client.from("registrations").update({
                    set("checked_in", true)
                    set("checked_in_at", now)
                }) {
                    filter { eq("id", registrationId) }
                }
                db.registrationDao().markCheckedInOffline(registrationId, now)
                db.registrationDao().markSynced(registrationId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        scanFlash = ScanFlash(
                            studentName = studentName,
                            studentUsn = studentUsn,
                            isAlreadyCheckedIn = false,
                            isOffline = false,
                            isError = true,
                            errorMessage = "Check-in failed: ${e.message}"
                        )
                    )
                }
                return
            }
        } else {
            db.registrationDao().markCheckedInOffline(registrationId, now)
        }

        _state.update {
            it.copy(
                scanFlash = ScanFlash(
                    studentName = studentName,
                    studentUsn = studentUsn,
                    isAlreadyCheckedIn = false,
                    isOffline = !isOnline,
                    isError = false
                ),
                batchCount = it.batchCount + 1
            )
        }
    }

    // ─── Manual USN tab (still uses ConfirmCard + explicit confirm) ──────────

    fun findByManualUsn(eventId: String, context: Context) {
        val usn = _state.value.manualUsn.trim()
        if (usn.isBlank()) return
        _state.update { it.copy(isProcessing = true, scanResult = null) }
        viewModelScope.launch {
            val isOnline = NetworkMonitor(context).isOnline()
            val result = if (isOnline) {
                manualCheckInByUSNOnline(usn, eventId)
            } else {
                manualCheckInByUSNOffline(usn, eventId, context)
            }
            _state.update { it.copy(scanResult = result, isProcessing = false, isOfflineCheckIn = !isOnline) }
        }
    }

    fun confirmCheckIn(registrationId: String, studentName: String, context: Context) {
        viewModelScope.launch {
            val isOnline = NetworkMonitor(context).isOnline()
            val now = Instant.now().toString()
            val db = AppDatabase.getInstance(context)

            if (isOnline) {
                try {
                    client.from("registrations").update({
                        set("checked_in", true)
                        set("checked_in_at", now)
                    }) {
                        filter { eq("id", registrationId) }
                    }
                    db.registrationDao().markCheckedInOffline(registrationId, now)
                    db.registrationDao().markSynced(registrationId)
                    _state.update {
                        it.copy(
                            snackbarMessage = "✓ $studentName checked in",
                            scanResult = null,
                            cameraActive = true,
                            isProcessing = false,
                            isOfflineCheckIn = false
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
            } else {
                db.registrationDao().markCheckedInOffline(registrationId, now)
                _state.update {
                    it.copy(
                        snackbarMessage = "☁ $studentName checked in offline — will sync when online",
                        scanResult = null,
                        cameraActive = true,
                        isProcessing = false,
                        isOfflineCheckIn = false
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

    private suspend fun lookupQRTokenOnline(token: String, eventId: String): ScanResult {
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

    private suspend fun lookupQRTokenOffline(token: String, eventId: String, context: Context): ScanResult {
        val db = AppDatabase.getInstance(context)
        val reg = db.registrationDao().findByQrToken(token)
            ?: return ScanResult.Error("QR not found in offline cache. Connect to internet and refresh.")

        if (reg.eventId != eventId) {
            return ScanResult.Error("This QR code belongs to a different event.")
        }

        val student = if (reg.studentId.isNotBlank()) {
            db.profileDao().getById(reg.studentId)
        } else {
            db.profileDao().getByUsn(reg.usn)
        }
        val event = db.eventDao().getById(eventId)

        return ScanResult.Success(
            registrationId = reg.id,
            eventId = reg.eventId,
            alreadyCheckedIn = reg.isPresent,
            checkedInAt = reg.checkedInAt,
            studentName = student?.fullName ?: reg.studentName.ifBlank { "Unknown" },
            studentUsn = student?.usn ?: reg.usn.ifBlank { "Unknown" },
            studentDepartment = student?.department ?: "Unknown",
            studentSemester = student?.semester ?: 0,
            eventTitle = event?.title ?: "Unknown Event",
            eventLocation = event?.location
        )
    }

    private suspend fun manualCheckInByUSNOnline(usn: String, eventId: String): ScanResult {
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

    private suspend fun manualCheckInByUSNOffline(usn: String, eventId: String, context: Context): ScanResult {
        val db = AppDatabase.getInstance(context)
        val student = db.profileDao().getByUsn(usn.uppercase().trim())
            ?: return ScanResult.Error("USN $usn not found in offline cache. Connect to internet and refresh.")

        val reg = db.registrationDao().findByStudentAndEvent(student.id, eventId)
            ?: db.registrationDao().findByUsn(usn.uppercase().trim(), eventId)
            ?: return ScanResult.Error("${student.fullName} ($usn) is not registered for this event (offline cache).")

        return ScanResult.Success(
            registrationId = reg.id,
            eventId = eventId,
            alreadyCheckedIn = reg.isPresent,
            checkedInAt = reg.checkedInAt,
            studentName = student.fullName,
            studentUsn = student.usn,
            studentDepartment = student.department,
            studentSemester = student.semester,
            eventTitle = "",
            eventLocation = null
        )
    }
}
