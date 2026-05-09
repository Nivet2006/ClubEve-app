package com.clubeve.cc.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.*

@Composable
fun SyncStatusBar(isOnline: Boolean, pendingCount: Int) {
    AnimatedVisibility(
        visible = !isOnline || pendingCount > 0,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isOnline) StatusWarning.copy(0.15f) else StatusError.copy(0.15f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Default.Sync else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isOnline) StatusWarning else StatusError,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (!isOnline) "Offline — scans saved locally"
                       else "$pendingCount scan(s) syncing…",
                color = if (isOnline) StatusWarning else StatusError,
                fontSize = 12.sp
            )
        }
    }
}
