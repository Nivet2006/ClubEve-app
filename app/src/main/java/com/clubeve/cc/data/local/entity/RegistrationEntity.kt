package com.clubeve.cc.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registrations",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId"), Index("usn")]
)
data class RegistrationEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val studentName: String,
    val usn: String,
    val email: String,
    val isPresent: Boolean = false,
    val markedAt: Long? = null,
    val isSynced: Boolean = true
)
