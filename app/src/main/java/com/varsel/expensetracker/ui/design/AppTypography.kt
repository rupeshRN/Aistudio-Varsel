package com.varsel.expensetracker.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

object AppTypography {

    @Composable
    fun HeroAmount(): TextStyle =
        MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold
        )

    @Composable
    fun SectionTitle(): TextStyle =
        MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold
        )

    @Composable
    fun CardTitle(): TextStyle =
        MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
        )

    @Composable
    fun MetricValue(): TextStyle =
        MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold
        )
}
