package com.varsel.expensetracker.ui.loan.detail

import com.varsel.expensetracker.domain.model.loan.AmortizationScheduleItem
import com.varsel.expensetracker.domain.model.loan.LoanPayment
import com.varsel.expensetracker.domain.model.loan.LoanSummary
import com.varsel.expensetracker.domain.model.loan.PrepaymentSimulationResult

data class LoanDetailUiState(
    val loanSummary: LoanSummary? = null,
    val payments: List<LoanPayment> = emptyList(),
    val amortizationSchedule: List<AmortizationScheduleItem> = emptyList(),
    val simulationResult: PrepaymentSimulationResult? = null,
    val selectedTab: LoanDetailTab = LoanDetailTab.OVERVIEW,
    val isLoading: Boolean = true
)

enum class LoanDetailTab(val title: String) {
    OVERVIEW("Overview"),
    SCHEDULE("Schedule"),
    PAYMENTS("Payments"),
    PREPAY_CALC("Prepay Calculator")
}
