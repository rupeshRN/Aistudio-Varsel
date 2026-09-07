package com.varsel.expensetracker.domain.model.loan

enum class LoanRepaymentType(
    val displayName: String,
    val description: String
) {
    MONTHLY_EMI(
        displayName = "Monthly EMI",
        description = "Regular monthly payments covering principal and interest"
    ),
    BULLET_YEARLY(
        displayName = "Yearly / Bullet Repayment",
        description = "Principal repaid at end of tenure, with simple annual interest"
    )
}
