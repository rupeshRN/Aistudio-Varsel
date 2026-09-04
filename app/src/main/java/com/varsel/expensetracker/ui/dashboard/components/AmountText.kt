package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AmountText(
    amount: String,
    isIncome: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val color = if (isIncome) {
        if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    } else {
        if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)
    }

    Text(
        text = if (isIncome) "+$amount" else "-$amount",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}
