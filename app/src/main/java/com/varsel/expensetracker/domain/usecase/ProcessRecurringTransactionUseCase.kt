package com.varsel.expensetracker.domain.usecase

import androidx.room.withTransaction
import com.varsel.expensetracker.data.local.AppDatabase
import com.varsel.expensetracker.domain.engine.RecurringScheduleEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import com.varsel.expensetracker.domain.repository.RecurringRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessRecurringTransactionUseCase @Inject constructor(
    private val appDatabase: AppDatabase,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val scheduleEngine: RecurringScheduleEngine
) {

    /**
     * Records a transaction for an occurrence of a recurring item atomically.
     * Prevents duplicate generation by:
     * 1) Checking item.lastGeneratedTimestamp against target occurrence timestamp
     * 2) Checking if a transaction with reference number REC-{id}-{occurrenceTimestamp} already exists
     * 3) Running the insert and the recurring item update inside a single Room database transaction.
     *
     * [actualAmount]: When provided (e.g. for variable bills), overrides item.amount for this transaction.
     * [updateBaselineAmount]: If true, updates item.amount to [actualAmount] for future occurrences.
     */
    suspend fun processOccurrence(
        item: RecurringItem,
        occurrenceTimestamp: Long = item.nextOccurrenceTimestamp,
        recordDateTimestamp: Long = occurrenceTimestamp,
        actualAmount: Double? = null,
        updateBaselineAmount: Boolean = false
    ): Result<Transaction> {
        val referenceNumber = "REC-${item.id}-${occurrenceTimestamp}"

        // Fast in-memory guard
        if (item.lastGeneratedTimestamp != null && item.lastGeneratedTimestamp == occurrenceTimestamp) {
            return Result.failure(IllegalStateException("Transaction for this occurrence has already been recorded."))
        }

        // Database-level guard against duplicate occurrence reference
        if (transactionRepository.hasTransactionWithReference(referenceNumber)) {
            return Result.failure(IllegalStateException("A transaction with reference $referenceNumber already exists."))
        }

        val txAmount = actualAmount ?: item.amount
        val txType = when (item.type) {
            RecurringType.EXPENSE, RecurringType.SUBSCRIPTION -> TransactionType.DEBIT
            RecurringType.INCOME -> TransactionType.CREDIT
        }

        val txDescription = if (!item.notes.isNullOrBlank()) {
            "${item.title} (${item.notes})"
        } else {
            item.title
        }

        val transaction = Transaction(
            amount = txAmount,
            type = txType,
            description = txDescription,
            category = item.category.ifBlank { "Other" },
            dateTimestamp = recordDateTimestamp,
            referenceNumber = referenceNumber,
            accountId = item.accountId,
            accountLast4 = item.accountLast4,
            recurringItemId = item.id,
            bankName = item.bankName,
            role = TransactionRole.NORMAL
        )

        return try {
            appDatabase.withTransaction {
                transactionRepository.insertTransaction(transaction)

                // Advance occurrence
                val nextTimestamp = scheduleEngine.calculateNextOccurrence(
                    currentOccurrenceTimestamp = occurrenceTimestamp,
                    frequency = item.frequency,
                    startDateTimestamp = item.startDateTimestamp
                )

                val willBeActive = if (item.endDateTimestamp != null && nextTimestamp > item.endDateTimestamp) {
                    false
                } else {
                    item.isActive
                }

                val newAmount = if (updateBaselineAmount && actualAmount != null) actualAmount else item.amount

                val updatedItem = item.copy(
                    amount = newAmount,
                    nextOccurrenceTimestamp = nextTimestamp,
                    lastGeneratedTimestamp = occurrenceTimestamp,
                    isActive = willBeActive,
                    updatedAt = System.currentTimeMillis()
                )
                recurringRepository.updateRecurringItem(updatedItem)
            }

            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Skips the current occurrence and moves to the next occurrence without recording a transaction.
     */
    suspend fun skipOccurrence(item: RecurringItem): Result<Long> {
        val nextTimestamp = scheduleEngine.calculateNextOccurrence(
            currentOccurrenceTimestamp = item.nextOccurrenceTimestamp,
            frequency = item.frequency,
            startDateTimestamp = item.startDateTimestamp
        )

        val willBeActive = if (item.endDateTimestamp != null && nextTimestamp > item.endDateTimestamp) {
            false
        } else {
            item.isActive
        }

        val updatedItem = item.copy(
            nextOccurrenceTimestamp = nextTimestamp,
            isActive = willBeActive,
            updatedAt = System.currentTimeMillis()
        )

        return try {
            recurringRepository.updateRecurringItem(updatedItem)
            Result.success(nextTimestamp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fast-forwards the schedule to the current/next future occurrence without recording back-dated transactions.
     * (e.g. When user was away for 3 months and wants to ignore missed cycles or marked paid externally).
     */
    suspend fun fastForwardToFuture(item: RecurringItem): Result<Long> {
        val nextFutureTimestamp = scheduleEngine.getNextFutureOccurrence(item)
        val willBeActive = if (item.endDateTimestamp != null && nextFutureTimestamp > item.endDateTimestamp) {
            false
        } else {
            item.isActive
        }

        val updatedItem = item.copy(
            nextOccurrenceTimestamp = nextFutureTimestamp,
            isActive = willBeActive,
            updatedAt = System.currentTimeMillis()
        )

        return try {
            recurringRepository.updateRecurringItem(updatedItem)
            Result.success(nextFutureTimestamp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
