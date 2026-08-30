package com.varsel.expensetracker.domain.model.loan

data class LoanSummary(
    val loan: LoanAccount,
    val currentOutstandingBalance: Double,
    val totalPrincipalPaid: Double,
    val totalInterestPaid: Double,
    val totalProjectedInterest: Double,
    val totalRemainingInterest: Double,
    val completedTenureMonths: Int,
    val remainingTenureMonths: Int,
    val nextEmiDueDateTimestamp: Long?,
    val nextEmiAmount: Double,
    val progressPercentage: Float,
    val paymentsCount: Int,
    val prepaymentsTotal: Double
)

enum class PrepaymentReductionType {
    REDUCE_TENURE,
    REDUCE_EMI
}

data class PrepaymentSimulationResult(
    val extraLumpSum: Double,
    val extraMonthly: Double,
    val originalTenureMonths: Int,
    val newTenureMonths: Int,
    val monthsSaved: Int,
    val originalTotalInterest: Double,
    val newTotalInterest: Double,
    val interestSaved: Double,
    val newEmiAmount: Double
)
