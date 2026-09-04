package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    fun getAllTransactions(): Flow<List<Transaction>>

    suspend fun insertTransactions(
        transactions: List<Transaction>
    )

    suspend fun insertTransaction(
        transaction: Transaction
    )

    suspend fun updateTransaction(
        transaction: Transaction
    )

    suspend fun deleteTransaction(
        transaction: Transaction
    )

    suspend fun getTransactionById(
        id: Long
    ): Transaction?

    suspend fun findExistingFingerprints(
        fingerprints: List<String>
    ): Set<String>

    //--------------------------------------------------
    // Financial Event
    //--------------------------------------------------

    /**
     * Assigns the same Financial Event link ID to
     * multiple transactions.
     *
     * Used for:
     * - LENT expenses
     * - REIMBURSEMENT incomes
     */
    suspend fun linkTransactions(
        transactionIds: List<Long>,
        transactionLinkId: String
    )

    /**
     * Removes a transaction from its Financial Event.
     *
     * The transaction itself is not deleted.
     */
    suspend fun unlinkTransaction(
        transactionId: Long
    )

    //--------------------------------------------------
    // Transfer
    //--------------------------------------------------

    /**
     * Attempts to link exactly two transactions as
     * one account transfer.
     *
     * A valid transfer must contain:
     *
     *     TRANSFER_OUT + TRANSFER_IN
     *
     * and both transactions must have exactly the
     * same amount.
     *
     * The implementation must validate the pair
     * before persisting the transfer relationship.
     */
    suspend fun linkTransfer(
        transferOutTransactionId: Long,
        transferInTransactionId: Long
    ): TransferLinkResult

    /**
     * Removes a transaction from its transfer.
     *
     * The transaction itself is not deleted.
     */
    suspend fun unlinkTransfer(
        transactionId: Long
    )

    /**
     * Returns all transactions belonging to the same
     * transfer relationship.
     */
    suspend fun getLinkedTransferTransactions(
        transferLinkId: String
    ): List<Transaction>

    suspend fun updateTransactions(
        transactions: List<Transaction>
    )

    suspend fun findSimilarTransactions(
        excludeId: Long,
        pattern: String,
        isIncome: Boolean,
        sinceTimestamp: Long
    ): List<Transaction>
}

/**
 * Result of attempting to create a transfer relationship.
 */
sealed interface TransferLinkResult {

    /**
     * Transfer was successfully created.
     */
    data object Success : TransferLinkResult

    /**
     * The selected transactions do not form a valid
     * Transfer Out + Transfer In pair.
     */
    data object InvalidTransactionPair : TransferLinkResult

    /**
     * The Transfer Out and Transfer In amounts differ.
     */
    data class AmountMismatch(
        val transferOutAmount: Double,
        val transferInAmount: Double
    ) : TransferLinkResult

    /**
     * One or both transactions could not be found.
     */
    data object TransactionNotFound : TransferLinkResult

    /**
     * One or both transactions already belong to
     * another transfer.
     */
    data object AlreadyLinked : TransferLinkResult
}
