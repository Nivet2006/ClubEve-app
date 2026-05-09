package com.clubeve.cc.data.repository

import com.clubeve.cc.data.model.Profile
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val client = SupabaseClientProvider.client

    /**
     * Login with USN + password.
     * Flow:
     *  1. Look up the profile row by USN to get the registered email.
     *  2. Sign in to Supabase Auth with that email + password.
     *  3. Check role — only club_coordinator, admin, pr_team allowed.
     */
    suspend fun loginWithUsn(usn: String, password: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            // Step 1: find the email for this USN
            val profiles = client.from("profiles")
                .select {
                    filter { eq("usn", usn.uppercase().trim()) }
                }
                .decodeList<Profile>()

            if (profiles.isEmpty()) {
                return@withContext Result.failure(Exception("No account found for USN: ${usn.uppercase()}"))
            }

            val profile = profiles.first()
            val emailToUse = profile.email.ifBlank {
                return@withContext Result.failure(Exception("Account has no email linked. Contact admin."))
            }

            // Step 2: sign in with email + password
            client.auth.signInWith(Email) {
                this.email = emailToUse
                this.password = password
            }

            val user = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Login failed — please try again."))

            // Step 3: re-fetch profile by auth user id to get fresh data
            val freshProfile = client.from("profiles")
                .select { filter { eq("id", user.id) } }
                .decodeSingle<Profile>()

            val allowedRoles = listOf("club_coordinator", "admin", "pr_team")
            if (freshProfile.role !in allowedRoles) {
                client.auth.signOut()
                return@withContext Result.failure(
                    Exception("Access Denied: role '${freshProfile.role}' is not allowed.\nOnly Club Coordinators, PR Team and Admins can use this app.")
                )
            }

            Result.success(freshProfile)
        } catch (e: Exception) {
            // Sign out on any failure to avoid stale session
            try { client.auth.signOut() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try { client.auth.signOut() } catch (_: Exception) {}
    }

    fun getCurrentUser() = client.auth.currentUserOrNull()
}
