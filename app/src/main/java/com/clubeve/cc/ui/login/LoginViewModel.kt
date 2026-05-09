package com.clubeve.cc.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val usn: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsnChange(value: String) {
        _uiState.value = _uiState.value.copy(usn = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.usn.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please enter your USN and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            val result = repository.loginWithUsn(state.usn.trim(), state.password)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Login failed. Check your USN and password."
                    )
                }
            )
        }
    }
}
