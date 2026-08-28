package com.varsel.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// Fallback Material 3 Palette (Used for Android 11 & older or fixed branding)
// ==============================================================================

// Dark Theme Fallback Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// Light Theme Fallback Colors
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ==============================================================================
// Custom Financial Status Colors (Consistent regardless of Material You state)
// ==============================================================================

/** High-contrast green indicator for positive income transactions */
val IncomeGreen = Color(0xFF10B981)
val IncomeGreenContainer = Color(0xFFD1FAE5)

/** High-contrast red indicator for expense transactions */
val ExpenseRed = Color(0xFFEF4444)
val ExpenseRedContainer = Color(0xFFFEE2E2)

/** Neutral status chip color for transfer/other types */
val NeutralBlue = Color(0xFF3B82F6)
