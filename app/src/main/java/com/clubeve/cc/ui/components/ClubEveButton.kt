package com.clubeve.cc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clubeve.cc.ui.theme.AccentPrimary
import com.clubeve.cc.ui.theme.TextPrimary

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPrimary,
            disabledContainerColor = AccentPrimary.copy(alpha = 0.4f)
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}

@Composable
fun OutlinedAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPrimary),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentPrimary)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
