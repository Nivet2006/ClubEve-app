package com.clubeve.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val date: Long,
    val venue: String,
    val status: String,
    val maxCapacity: Int,
    val registeredCount: Int = 0
)
