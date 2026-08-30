package com.varsel.expensetracker.ui.model

enum class InsightType {
    POSITIVE,
    ATTENTION,
    NEUTRAL
}

data class FinancialInsight(
    val emoji: String,
    val title: String,
    val description: String,
    val type: InsightType = InsightType.NEUTRAL
)
