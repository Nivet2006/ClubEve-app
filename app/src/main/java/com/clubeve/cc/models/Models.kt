package com.clubeve.cc.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val usn: String = "",
    val email: String = "",
    val department: String = "",
    val semester: Int = 0,
    val year: Int = 0,
    val role: String = ""
)

@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("club_name") val clubName: String = "",
    val location: String? = null,
    @SerialName("event_date") val eventDate: String = "",
    val status: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("max_capacity") val maxCapacity: Int? = null,
    var registrationCount: Long = 0,
    var attendanceCount: Long = 0
)

@Serializable
data class Registration(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null
)

@Serializable
data class PrAssignment(
    @SerialName("event_id") val eventId: String
)

@Serializable
data class Attendee(
    val id: String,
    val fullName: String,
    val usn: String,
    val department: String,
    val semester: Int,
    val year: Int,
    val checkedIn: Boolean,
    val checkedInAt: String?,
    val registeredAt: String?
)

@Serializable
data class IdOnly(val id: String)

sealed class ScanResult {
    data class Success(
        val registrationId: String,
        val eventId: String,
        val alreadyCheckedIn: Boolean,
        val checkedInAt: String?,
        val studentName: String,
        val studentUsn: String,
        val studentDepartment: String,
        val studentSemester: Int,
        val eventTitle: String,
        val eventLocation: String?
    ) : ScanResult()

    data class Error(val message: String) : ScanResult()
}
