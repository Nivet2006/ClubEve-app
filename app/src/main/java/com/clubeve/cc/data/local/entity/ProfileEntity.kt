package com.clubeve.cc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val usn: String,
    val department: String,
    val semester: Int,
    val year: Int,
    val role: String
)
