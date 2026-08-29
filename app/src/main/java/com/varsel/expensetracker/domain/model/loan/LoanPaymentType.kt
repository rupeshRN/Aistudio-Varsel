package com.varsel.expensetracker.domain.model.loan

enum class LoanPaymentType(
    val displayName: String
) {
    REGULAR_EMI("Regular EMI"),
    PRE_PAYMENT("Pre-payment / Lump Sum"),
    CLOSURE("Loan Closure")
}
