package com.clubeve.cc.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clubeve.cc.ui.theme.BackgroundElevated
import com.clubeve.cc.ui.theme.BorderSubtle

@Composable
fun ClubEveCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundElevated),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        content()
    }
}
