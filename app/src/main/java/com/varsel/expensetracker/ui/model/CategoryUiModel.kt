package com.varsel.expensetracker.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryUiModel(
    val name: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val textColor: Color,
    val emoji: String = ""
)

