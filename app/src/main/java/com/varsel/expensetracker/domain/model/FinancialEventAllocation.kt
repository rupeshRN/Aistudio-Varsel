package com.varsel.expensetracker.domain.model

/**
 * Domain representation of a Financial Event allocation.
 */
data class FinancialEventAllocation(

    val id: Long = 0L,

    val transactionId: Long,

    val transactionLinkId: String,

    val allocatedAmount: Double,

    val createdAt: Long
)
