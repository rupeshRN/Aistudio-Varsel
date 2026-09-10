package com.varsel.expensetracker.domain.usecase

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/**
 * UseCase for recording manual transactions and account-to-account transfers.
 *
 * Ensures all business rules are satisfied:
 * - Amount must be strictly greater than zero.
 * - Non-empty description and category.
 * - Imports remain unaffected because manual transactions do not have
 *   a transactionFingerprint or rawDescription.
 * - Transfers create paired TRANSFER_OUT and TRANSFER_IN records sharing
 *   a unique transferLinkId.
 */
class AddManualTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend fun addTransaction(
        amount: Double,
        type: TransactionType,
        description: String,
        category: String,
        dateTimestamp: Long,
        referenceNumber: String? = null,
        accountId: String? = null,
        accountLast4: String? = null,
        bankName: String? = null
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }
        val validAmount = amount

        val cleanCategory = category.trim().ifBlank { "Other" }
        val cleanDesc = description.trim().ifBlank { cleanCategory }
        val cleanRef = referenceNumber?.trim()?.takeIf { it.isNotBlank() }

        val transaction = Transaction(
            amount = validAmount,
            type = type,
            description = cleanDesc,
            category = cleanCategory,
            dateTimestamp = dateTimestamp,
            referenceNumber = cleanRef,
            accountId = accountId,
            accountLast4 = accountLast4,
            bankName = bankName,
            role = TransactionRole.NORMAL
        )

        return try {
            transactionRepository.insertTransaction(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTransfer(
        amount: Double,
        description: String,
        dateTimestamp: Long,
        fromAccountId: String?,
        fromAccountLast4: String?,
        fromBankName: String?,
        toAccountId: String?,
        toAccountLast4: String?,
        toBankName: String?,
        referenceNumber: String? = null
    ): Result<Unit> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }
        val validAmount = amount

        val transferLinkId = UUID.randomUUID().toString()
        val baseDesc = description.trim().ifBlank { "Transfer" }
        val cleanRef = referenceNumber?.trim()?.takeIf { it.isNotBlank() }

        val fromLabel = fromBankName?.let { if (fromAccountLast4 != null) "$it •••• $fromAccountLast4" else it } ?: "Cash"
        val toLabel = toBankName?.let { if (toAccountLast4 != null) "$it •••• $toAccountLast4" else it } ?: "Cash"

        val outTransaction = Transaction(
            amount = validAmount,
            type = TransactionType.EXPENSE,
            description = "$baseDesc to $toLabel",
            category = "Transfer",
            dateTimestamp = dateTimestamp,
            referenceNumber = cleanRef,
            accountId = fromAccountId,
            accountLast4 = fromAccountLast4,
            bankName = fromBankName,
            role = TransactionRole.TRANSFER_OUT,
            transferLinkId = transferLinkId
        )

        val inTransaction = Transaction(
            amount = validAmount,
            type = TransactionType.INCOME,
            description = "$baseDesc from $fromLabel",
            category = "Transfer",
            dateTimestamp = dateTimestamp,
            referenceNumber = cleanRef,
            accountId = toAccountId,
            accountLast4 = toAccountLast4,
            bankName = toBankName,
            role = TransactionRole.TRANSFER_IN,
            transferLinkId = transferLinkId
        )

        return try {
            transactionRepository.insertTransactions(listOf(outTransaction, inTransaction))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
