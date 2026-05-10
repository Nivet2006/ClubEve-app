package com.clubeve.cc.ui.login

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.auth.BiometricHelper
import com.clubeve.cc.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity

    // Check for saved credentials on first composition
    LaunchedEffect(Unit) {
        viewModel.checkSavedCredentials(context)
    }

    // Auto-trigger biometric prompt if saved credentials exist
    LaunchedEffect(uiState.hasSavedCredentials) {
        if (uiState.hasSavedCredentials && !uiState.showManualForm) {
            val canBio = BiometricManager.from(context)
                .canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) ==
                    BiometricManager.BIOMETRIC_SUCCESS
            if (canBio) {
                BiometricHelper.prompt(
                    activity = activity,
                    title = "Welcome back, ${uiState.savedUsn}",
                    subtitle = "Verify to sign in",
                    onSuccess = { viewModel.loginWithSavedCredentials(context, onLoginSuccess) },
                    onFailed = { /* stay on screen */ },
                    onError = { msg -> /* stay on screen, user can tap button again */ }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("CLUB-EVE", fontFamily = Mono, fontWeight = FontWeight.Black,
                fontSize = 22.sp, letterSpacing = 2.sp, color = Black)

            Spacer(Modifier.height(40.dp))

            // ── Biometric card (shown when saved credentials exist) ──────────
            if (uiState.hasSavedCredentials && !uiState.showManualForm) {
                BiometricCard(
                    usn = uiState.savedUsn,
                    isLoading = uiState.isLoading,
                    onBiometricTap = {
                        BiometricHelper.prompt(
                            activity = activity,
                            title = "Welcome back, ${uiState.savedUsn}",
                            subtitle = "Verify to sign in",
                            onSuccess = { viewModel.loginWithSavedCredentials(context, onLoginSuccess) },
                            onFailed = {},
                            onError = {}
                        )
                    },
                    onUsePassword = { viewModel.showManualForm() },
                    onForget = { viewModel.forgetSavedCredentials(context) }
                )
            } else {
                // ── Manual login form ────────────────────────────────────────
                ManualLoginForm(
                    uiState = uiState,
                    onUsnChange = viewModel::onUsnChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onTogglePassword = viewModel::togglePasswordVisibility,
                    onToggleRememberMe = viewModel::toggleRememberMe,
                    onSignIn = { viewModel.login(context, onLoginSuccess) }
                )
            }

            // Error card
            AnimatedVisibility(visible = uiState.error != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .border(1.dp, StatusError.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .background(StatusError.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(uiState.error ?: "", fontFamily = Mono, fontSize = 12.sp, color = StatusError)
                }
            }
        }
    }
}

// ── Biometric card ────────────────────────────────────────────────────────────

@Composable
private fun BiometricCard(
    usn: String,
    isLoading: Boolean,
    onBiometricTap: () -> Unit,
    onUsePassword: () -> Unit,
    onForget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDefault, RoundedCornerShape(12.dp))
            .background(White, RoundedCornerShape(12.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SAVED ACCOUNT", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, letterSpacing = 2.sp, color = MidGray)

        Text(usn, fontFamily = Mono, fontWeight = FontWeight.Black,
            fontSize = 20.sp, color = Black)

        HorizontalDivider(color = BorderSubtle)

        // Big fingerprint / face button
        if (isLoading) {
            CircularProgressIndicator(color = Black, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        } else {
            IconButton(
                onClick = onBiometricTap,
                modifier = Modifier
                    .size(72.dp)
                    .border(1.dp, Black, RoundedCornerShape(36.dp))
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric login",
                    tint = Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text("Tap to sign in with biometrics", fontFamily = Mono,
                fontSize = 11.sp, color = MidGray)
        }

        HorizontalDivider(color = BorderSubtle)

        // Secondary actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onUsePassword) {
                Text("Use password", fontFamily = Mono, fontSize = 11.sp, color = DarkGray)
            }
            TextButton(onClick = onForget) {
                Text("Forget account", fontFamily = Mono, fontSize = 11.sp, color = StatusError)
            }
        }
    }
}

// ── Manual login form ─────────────────────────────────────────────────────────

@Composable
private fun ManualLoginForm(
    uiState: LoginUiState,
    onUsnChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleRememberMe: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDefault, RoundedCornerShape(12.dp))
            .background(White, RoundedCornerShape(12.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MinimalTextField(
            value = uiState.usn,
            onValueChange = onUsnChange,
            label = "USN",
            placeholder = "1GD24CS001",
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        MinimalTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "PASSWORD",
            placeholder = "••••••",
            isPassword = true,
            isPasswordVisible = uiState.isPasswordVisible,
            onTogglePassword = onTogglePassword,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        // Remember Me toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = uiState.rememberMe,
                onCheckedChange = { onToggleRememberMe() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Black,
                    uncheckedColor = BorderDefault,
                    checkmarkColor = White
                )
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Remember me", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, color = Black)
                Text("Use biometrics next time", fontFamily = Mono,
                    fontSize = 10.sp, color = MidGray)
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onSignIn,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Black,
                disabledContainerColor = LightGray
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp),
                    color = White, strokeWidth = 2.dp)
            } else {
                Text("Sign In", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, letterSpacing = 1.sp, color = White)
            }
        }
    }
}

// ── Shared text field ─────────────────────────────────────────────────────────

@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 10.sp,
            letterSpacing = 1.5.sp, color = MidGray, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontFamily = Mono, fontSize = 13.sp, color = LightGray) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            visualTransformation = if (isPassword && !isPasswordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = MidGray, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = BorderDefault,
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                cursorColor = Black
            )
        )
    }
}
