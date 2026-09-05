package com.varsel.expensetracker.domain.model.loan

data class LoanAccount(
    val id: Long = 0L,
    val name: String,
    val loanType: LoanType,
    val principal: Double,
    val annualInterestRate: Double,
    val emiAmount: Double,
    val totalTenureMonths: Int,
    val startDateTimestamp: Long,
    val collateralOrNotes: String? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val linkedBankAccountId: String? = null,
    val bankAccountLast4: String? = null,
    val lenderName: String? = null,
    val loanAccountNumber: String? = null,
    val interestType: InterestRateType = InterestRateType.FIXED,
    val benchmarkRate: Double? = null,
    val spreadRate: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
