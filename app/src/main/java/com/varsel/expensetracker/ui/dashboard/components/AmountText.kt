package com.varsel.expensetracker.ui.dashboard.components

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

    Text(

        text = if (isIncome) "+$amount" else "-$amount",

        style = MaterialTheme.typography.titleMedium,

        fontWeight = FontWeight.Bold,

        color = if (isIncome)
            Color(0xFF2E7D32)
        else
            Color(0xFFC62828)
    )
}
