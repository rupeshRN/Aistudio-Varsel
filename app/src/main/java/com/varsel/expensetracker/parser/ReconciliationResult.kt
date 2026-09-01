package com.varsel.expensetracker.parser

data class ReconciliationResult(

    val calculatedCredits: Double,

    val calculatedDebits: Double,

    val creditDifference: Double,

    val debitDifference: Double,

    val isBalanced: Boolean,

    val hasSummaryTotals: Boolean = true
)
