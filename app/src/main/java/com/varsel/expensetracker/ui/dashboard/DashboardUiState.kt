package com.varsel.expensetracker.ui.dashboard

import com.varsel.expensetracker.domain.model.loan.LoanSummary
import com.varsel.expensetracker.ui.budget.model.BudgetUiModel
import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
import com.varsel.expensetracker.ui.model.FinancialInsight
import com.varsel.expensetracker.ui.model.TransactionUiModel

data class DashboardUiState(
    val balanceSummary: BalanceSummaryUiModel =
        BalanceSummaryUiModel(
            totalBalance = 0.0,
            totalIncome = 0.0,
            totalExpense = 0.0,
            savings = 0.0,
            accounts = emptyList()
        ),

    val recentTransactions: List<TransactionUiModel> = emptyList(),

    val loans: List<LoanSummary> = emptyList(),

    val insights: List<FinancialInsight> = emptyList(),

    val allBudgets: List<BudgetUiModel> = emptyList(),

    val allGoals: List<BudgetUiModel> = emptyList(),

    val visibleBudgets: List<BudgetUiModel> = emptyList(),

    val visibleGoals: List<BudgetUiModel> = emptyList(),

    val homeBudgetsSelection: String = "ALL",

    val homeGoalsSelection: String = "ALL",

    val totalBudgetLimit: Double = 0.0,

    val totalBudgetSpent: Double = 0.0,

    val totalGoalTarget: Double = 0.0,

    val totalGoalSaved: Double = 0.0,

    val isBalanceHidden: Boolean = false,

    val showNetWorthBreakdown: Boolean = true,

    val isLoading: Boolean = true
)
