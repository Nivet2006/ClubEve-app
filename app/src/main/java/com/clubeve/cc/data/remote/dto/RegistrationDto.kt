package com.clubeve.cc.data.remote.dto

import com.clubeve.cc.data.local.entity.RegistrationEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationDto(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("student_name") val studentName: String = "",
    val usn: String = "",
    val email: String = "",
    @SerialName("is_present") val isPresent: Boolean = false,
    @SerialName("marked_at") val markedAt: String? = null
) {
    fun toEntity() = RegistrationEntity(
        id = id,
        eventId = eventId,
        studentName = studentName,
        usn = usn.uppercase(),
        email = email,
        isPresent = isPresent,
        markedAt = markedAt?.let {
            runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
        },
        isSynced = true
    )
}
