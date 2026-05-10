package com.clubeve.cc.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.auth.CredentialStore
import com.clubeve.cc.notifications.AssignmentPollWorker
import com.clubeve.cc.notifications.SessionStore
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usn: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val error: String? = null,
    // Biometric state
    val hasSavedCredentials: Boolean = false,
    val savedUsn: String = "",          // shown on the biometric card (not the password)
    val showManualForm: Boolean = false  // user tapped "Use password instead"
)

class LoginViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsnChange(value: String) = _uiState.update { it.copy(usn = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun toggleRememberMe() = _uiState.update { it.copy(rememberMe = !it.rememberMe) }
    fun showManualForm() = _uiState.update { it.copy(showManualForm = true, error = null) }

    /** Called on screen start — checks if saved credentials exist */
    fun checkSavedCredentials(context: Context) {
        val creds = CredentialStore.load(context)
        if (creds != null) {
            _uiState.update {
                it.copy(hasSavedCredentials = true, savedUsn = creds.first, showManualForm = false)
            }
        } else {
            _uiState.update { it.copy(hasSavedCredentials = false, showManualForm = true) }
        }
    }

    /** Called after biometric success — uses stored credentials to sign in */
    fun loginWithSavedCredentials(context: Context, onSuccess: () -> Unit) {
        val creds = CredentialStore.load(context) ?: run {
            _uiState.update { it.copy(error = "No saved credentials found. Please sign in manually.") }
            return
        }
        performLogin(usn = creds.first, password = creds.second, rememberMe = true,
            context = context, onSuccess = onSuccess)
    }

    /** Called from the manual form Sign In button */
    fun login(context: Context, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.usn.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your USN and password.") }
            return
        }
        performLogin(usn = state.usn, password = state.password,
            rememberMe = state.rememberMe, context = context, onSuccess = onSuccess)
    }

    fun forgetSavedCredentials(context: Context) {
        CredentialStore.clear(context)
        _uiState.update {
            it.copy(hasSavedCredentials = false, savedUsn = "", showManualForm = true,
                usn = "", password = "", rememberMe = false)
        }
    }

    private fun performLogin(usn: String, password: String, rememberMe: Boolean,
                             context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Step 1: Resolve email from USN via RPC
                val rpcResult = client.postgrest.rpc(
                    "get_email_by_usn",
                    buildJsonObject { put("p_usn", usn.uppercase().trim()) }
                )
                val emailToUse = rpcResult.decodeAsOrNull<String>()?.trim()?.ifBlank { null }
                    ?: run {
                        _uiState.update {
                            it.copy(isLoading = false,
                                error = "No account found for USN: ${usn.uppercase()}.")
                        }
                        return@launch
                    }

                // Step 2: Sign in
                client.auth.signInWith(Email) {
                    email = emailToUse
                    this.password = password
                }

                // Step 3: Get user ID
                val userId = client.auth.currentUserOrNull()?.id
                    ?: throw Exception("Authentication failed. Please try again.")

                // Step 4: Fetch profile + verify role
                val profile = client.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<Profile>()

                if (profile.role != "pr" && profile.role != "student") {
                    client.auth.signOut()
                    SessionManager.clear()
                    _uiState.update {
                        it.copy(isLoading = false,
                            error = "Access denied. Unrecognised account role.")
                    }
                    return@launch
                }

                // Step 5: Save credentials if Remember Me is on
                if (rememberMe) {
                    CredentialStore.save(context, usn.uppercase().trim(), password)
                }

                // Step 6: Store session, save pr_id for background polling, and navigate
                SessionManager.currentUserId = userId
                SessionManager.currentProfile = profile
                // Persist pr_id so AssignmentPollWorker works even after logout/app close
                SessionStore.savePrId(context, userId)
                AssignmentPollWorker.schedule(context)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()

            } catch (e: Exception) {
                try { client.auth.signOut() } catch (_: Exception) {}
                SessionManager.clear()
                val message = when {
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                        "Incorrect password. Please try again."
                    e.message?.contains("network", ignoreCase = true) == true ||
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Network error. Check your connection."
                    else -> e.message ?: "Login failed. Please try again."
                }
                _uiState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }
}
