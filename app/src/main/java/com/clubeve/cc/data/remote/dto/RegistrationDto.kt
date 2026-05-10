package com.clubeve.cc.data.remote.dto

import com.clubeve.cc.data.local.entity.RegistrationEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationDto(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("student_name") val studentName: String = "",
    val usn: String = "",
    val email: String = "",
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("is_present") val isPresent: Boolean = false,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("marked_at") val markedAt: String? = null,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null
) {
    fun toEntity() = RegistrationEntity(
        id = id,
        eventId = eventId,
        studentId = studentId,
        studentName = studentName,
        usn = usn.uppercase(),
        email = email,
        qrToken = qrToken,
        isPresent = isPresent || checkedIn,
        markedAt = (markedAt ?: checkedInAt)?.let {
            runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
        },
        checkedInAt = checkedInAt ?: markedAt,
        registeredAt = registeredAt,
        isSynced = true,
        pendingSync = false
    )
}
