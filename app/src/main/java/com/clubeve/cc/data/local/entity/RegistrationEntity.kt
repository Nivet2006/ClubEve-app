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
    indices = [Index("eventId"), Index("usn"), Index("qrToken"), Index("studentId")]
)
data class RegistrationEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val studentId: String = "",
    val studentName: String = "",
    val usn: String = "",
    val email: String = "",
    val qrToken: String? = null,
    val isPresent: Boolean = false,
    val markedAt: Long? = null,
    val checkedInAt: String? = null,
    val registeredAt: String? = null,
    val isSynced: Boolean = true,
    // pendingSync = true means a local offline check-in needs to be pushed to Supabase
    val pendingSync: Boolean = false
)
