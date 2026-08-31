package com.varsel.expensetracker.ui.financialevent

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup

data class FinancialEventItemUiModel(
    val allocationId: Long,
    val transaction: Transaction,
    val allocatedAmount: Double,
    val totalTransactionAmount: Double,
    val isPartialAllocation: Boolean = (totalTransactionAmount - allocatedAmount) > 0.01
) {
    val isPartial: Boolean get() = isPartialAllocation
    val percent: Int
        get() = if (totalTransactionAmount > 0.0) {
            ((allocatedAmount / totalTransactionAmount) * 100).toInt()
        } else 100
}

data class AvailableTransactionUiModel(
    val transaction: Transaction,
    val remainingAmount: Double,
    val totalAmount: Double,
    val isPartiallyAllocated: Boolean = (totalAmount - remainingAmount) > 0.01
)

sealed interface FinancialEventUiState {

    data object Loading : FinancialEventUiState

    data class Loaded(

        val group: TransactionLinkGroup,

        val allocatedExpenses: List<FinancialEventItemUiModel> = emptyList(),

        val allocatedReimbursements: List<FinancialEventItemUiModel> = emptyList(),

        /**
         * Backward compatibility: list of transactions
         */
        val expenses: List<Transaction> = emptyList(),

        val reimbursements: List<Transaction> = emptyList(),

        /**
         * Expenses with remaining unallocated amount.
         */
        val availableExpenses: List<AvailableTransactionUiModel> = emptyList(),

        /**
         * Reimbursements with remaining unallocated amount.
         */
        val availableReimbursements: List<AvailableTransactionUiModel> = emptyList(),

        val totalExpenses: Double = 0.0,

        val totalReimbursements: Double = 0.0,

        val isUpdating: Boolean = false,

        val isEditingGroup: Boolean = false,

        val editingItem: FinancialEventItemUiModel? = null,

        val categories: List<String> = emptyList()

    ) : FinancialEventUiState {

        val actualExpense: Double
            get() =
                totalExpenses - totalReimbursements
    }

    data class Error(

        val message: String

    ) : FinancialEventUiState

    data object EventDeleted : FinancialEventUiState
}

