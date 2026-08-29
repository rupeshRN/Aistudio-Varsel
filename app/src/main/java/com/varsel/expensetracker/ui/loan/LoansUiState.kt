package com.varsel.expensetracker.ui.loan

import com.varsel.expensetracker.domain.model.loan.LoanSummary

data class LoansUiState(
    val loans: List<LoanSummary> = emptyList(),
    val totalOutstandingDebt: Double = 0.0,
    val totalMonthlyEmi: Double = 0.0,
    val totalPrincipalRepaid: Double = 0.0,
    val totalInterestPaid: Double = 0.0,
    val activeLoansCount: Int = 0,
    val closedLoansCount: Int = 0,
    val isLoading: Boolean = true
)
