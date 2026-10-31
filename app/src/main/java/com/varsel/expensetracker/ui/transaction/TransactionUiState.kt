package com.varsel.expensetracker.ui.transaction

import com.varsel.expensetracker.ui.model.TransactionUiModel
import com.varsel.expensetracker.util.AppError

data class TransactionUiState(

    val transactions: List<TransactionUiModel> = emptyList(),

    val availableMonths: List<TransactionMonth> = emptyList(),

    val selectedMonth: TransactionMonth? = null,

    val selectedFilter: TransactionFilter = TransactionFilter.All,

    val searchQuery: String = "",

    val isLoading: Boolean = false,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val error: AppError? = null
)

enum class TransactionFilter {
    All,
    Expense,
    Income,
    Transfer
}

private fun currentMonth(): String {

    return java.time.LocalDate.now()
        .month
        .name
        .lowercase()
        .replaceFirstChar { it.uppercase() }

}
