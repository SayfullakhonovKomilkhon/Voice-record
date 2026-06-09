package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun UnifiedCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = BorderStroke(
        width = 1.dp,
        color = if (isDark) Color(0xFF334155) else Color(0xFFE2EAFD)
    ),
    colors: CardColors? = null,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = colors ?: CardDefaults.cardColors(
        containerColor = if (isDark) Color(0xFF1E2430) else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = cardColors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = cardColors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}
