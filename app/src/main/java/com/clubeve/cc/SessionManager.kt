package com.clubeve.cc

import com.clubeve.cc.models.Profile

/**
 * Singleton session store. Populated after successful login + role verification.
 * Cleared on logout.
 */
object SessionManager {
    var currentUserId: String = ""
    var currentProfile: Profile? = null

    fun clear() {
        currentUserId = ""
        currentProfile = null
    }
}
