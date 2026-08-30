package com.varsel.expensetracker.domain.model.loan

data class LoanPayment(
    val id: Long = 0L,
    val loanId: Long,
    val paymentDateTimestamp: Long,
    val amount: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val paymentType: LoanPaymentType = LoanPaymentType.REGULAR_EMI,
    val linkedTransactionId: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
