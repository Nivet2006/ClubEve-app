package com.clubeve.cc.data.remote.dto

import com.clubeve.cc.data.local.entity.EventEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val description: String = "",
    val date: String,
    val venue: String = "",
    val status: String = "approved",
    @SerialName("max_capacity") val maxCapacity: Int = 0
) {
    fun toEntity() = EventEntity(
        id = id,
        title = title,
        description = description,
        date = runCatching { java.time.Instant.parse(date).toEpochMilli() }.getOrDefault(0L),
        venue = venue,
        status = status,
        maxCapacity = maxCapacity
    )
}
