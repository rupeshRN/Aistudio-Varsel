package com.varsel.expensetracker.domain.model.loan

data class AmortizationScheduleItem(
    val monthIndex: Int,
    val dueDateTimestamp: Long,
    val openingBalance: Double,
    val emiAmount: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val closingBalance: Double,
    val isPaid: Boolean = false
)
