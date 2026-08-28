package com.varsel.expensetracker.ui.transaction

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.model.TransactionRole

data class TransactionEventAllocationUiModel(
    val allocationId: Long,
    val transactionLinkId: String,
    val groupName: String,
    val category: String,
    val allocatedAmount: Double,
    val totalTransactionAmount: Double,
    val percent: Int = if (totalTransactionAmount > 0.0) ((allocatedAmount / totalTransactionAmount) * 100).toInt().coerceIn(0, 100) else 100
)

sealed interface TransactionDetailUiState {

    //--------------------------------------------------
    // Loading
    //--------------------------------------------------

    object Loading : TransactionDetailUiState

    //--------------------------------------------------
    // Loaded
    //--------------------------------------------------

    data class Loaded(

        //--------------------------------------------------
        // Current transaction
        //--------------------------------------------------

        val transaction: Transaction,

        val editableDescription: String,

        val selectedCategory: String,

        val selectedRole: TransactionRole,

        val hasChanges: Boolean,

        val isSaving: Boolean = false,

        //--------------------------------------------------
        // Application categories
        //--------------------------------------------------

        val categories: List<String> =
            emptyList(),

        //--------------------------------------------------
        // Financial Event Allocations (Multi-Event)
        //--------------------------------------------------

        val allocations: List<TransactionEventAllocationUiModel> =
            emptyList(),

        val totalAllocatedAmount: Double = 0.0,

        val remainingUnallocatedAmount: Double = 0.0,

        val allAvailableEventGroups: List<TransactionLinkGroup> =
            emptyList(),

        val showCreateGroupPrompt: Boolean = false,

        val showAllocateExistingPrompt: Boolean = false,

        val editingAllocation: TransactionEventAllocationUiModel? = null,

        val allocationErrorMessage: String? = null,

        //--------------------------------------------------
        // Financial Event / Legacy transaction grouping
        //--------------------------------------------------

        /**
         * All transactions currently belonging to the
         * same Financial Event.
         */
        val linkedTransactions: List<Transaction> =
            emptyList(),

        /**
         * Transactions that can potentially be added
         * to the current Financial Event.
         *
         * This is retained for Financial Event handling.
         */
        val linkableTransactions: List<Transaction> =
            emptyList(),

        /**
         * True while a Financial Event link/unlink
         * operation is being persisted.
         */
        val isLinking: Boolean = false,

        val transactionLinkGroup:
            TransactionLinkGroup? = null,

        val isSavingGroup: Boolean = false,

        //--------------------------------------------------
        // Transfer In / Transfer Out
        //--------------------------------------------------

        /**
         * The opposite-side transaction currently linked
         * to this transfer.
         *
         * For TRANSFER_OUT:
         *     this is the TRANSFER_IN transaction.
         *
         * For TRANSFER_IN:
         *     this is the TRANSFER_OUT transaction.
         */
        val linkedTransfer:
            Transaction? = null,

        /**
         * Transactions that may be selected as the
         * opposite side of this transfer.
         *
         * The ViewModel is responsible for determining
         * whether a candidate is valid.
         */
        val transferCandidates:
            List<Transaction> =
                emptyList(),

        /**
         * True while a transfer link/unlink operation
         * is being persisted.
         */
        val isTransferLinking:
            Boolean = false,

        /**
         * User-facing validation error from a transfer
         * linking attempt.
         *
         * Example:
         * the selected Transfer In and Transfer Out
         * amounts do not match.
         */
        val transferErrorMessage:
            String? = null

    ) : TransactionDetailUiState

    //--------------------------------------------------
    // Error
    //--------------------------------------------------

    data class Error(

        val message: String

    ) : TransactionDetailUiState
}
