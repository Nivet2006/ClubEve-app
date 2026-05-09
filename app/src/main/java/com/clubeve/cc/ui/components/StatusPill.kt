package com.clubeve.cc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.ui.theme.*

enum class PillType { SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusPill(label: String, type: PillType) {
    val (bg, fg) = when (type) {
        PillType.SUCCESS -> StatusSuccess.copy(alpha = 0.15f) to StatusSuccess
        PillType.WARNING -> StatusWarning.copy(alpha = 0.15f) to StatusWarning
        PillType.ERROR   -> StatusError.copy(alpha = 0.15f)   to StatusError
        PillType.INFO    -> AccentGlow to AccentPrimary
    }
    Text(
        text = label,
        color = fg,
        fontSize = 12.sp,
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
