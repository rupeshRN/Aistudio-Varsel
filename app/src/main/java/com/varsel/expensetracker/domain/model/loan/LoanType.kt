package com.varsel.expensetracker.domain.model.loan

enum class LoanType(
    val displayName: String
) {
    HOME_LOAN("Home Loan"),
    CAR_LOAN("Car / Auto Loan"),
    PERSONAL_LOAN("Personal Loan"),
    GOLD_LOAN("Gold Loan"),
    EDUCATION_LOAN("Education Loan"),
    BUSINESS_LOAN("Business Loan"),
    OTHER("Other Loan")
}
