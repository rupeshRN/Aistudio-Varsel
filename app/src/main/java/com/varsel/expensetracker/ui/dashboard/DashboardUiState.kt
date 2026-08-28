package com.varsel.expensetracker.ui.dashboard

import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
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

    val isLoading: Boolean = true
)
