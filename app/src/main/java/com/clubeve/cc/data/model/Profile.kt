package com.clubeve.cc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val usn: String = "",
    val role: String = "",
    val email: String = ""
)
