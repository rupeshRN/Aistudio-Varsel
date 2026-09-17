package com.varsel.expensetracker.domain.usecase

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
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val scheduleEngine: RecurringScheduleEngine
) {

    /**
     * Records a transaction for the current occurrence of a recurring item,
     * updates the recurring item's occurrence timestamp, and advances to the next period.
     * Prevents duplicate generation if already generated for this occurrence.
     */
    suspend fun processOccurrence(
        item: RecurringItem,
        recordDateTimestamp: Long = item.nextOccurrenceTimestamp
    ): Result<Transaction> {
        val occurrenceTimestamp = item.nextOccurrenceTimestamp

        // Prevent duplicate generation for same occurrence
        if (item.lastGeneratedTimestamp != null && item.lastGeneratedTimestamp == occurrenceTimestamp) {
            return Result.failure(IllegalStateException("Transaction for this occurrence has already been generated."))
        }

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
            amount = item.amount,
            type = txType,
            description = txDescription,
            category = item.category.ifBlank { "Other" },
            dateTimestamp = recordDateTimestamp,
            referenceNumber = "REC-${item.id}-${occurrenceTimestamp}",
            accountId = item.accountId,
            accountLast4 = item.accountLast4,
            recurringItemId = item.id,
            bankName = item.bankName,
            role = TransactionRole.NORMAL
        )

        return try {
            transactionRepository.insertTransaction(transaction)
            val nextTimestamp = scheduleEngine.calculateNextOccurrence(
                currentOccurrenceTimestamp = occurrenceTimestamp,
                frequency = item.frequency,
                startDateTimestamp = item.startDateTimestamp
            )

            // If an end date is set and next occurrence exceeds it, deactivate item
            val willBeActive = if (item.endDateTimestamp != null && nextTimestamp > item.endDateTimestamp) {
                false
            } else {
                item.isActive
            }

            val updatedItem = item.copy(
                nextOccurrenceTimestamp = nextTimestamp,
                lastGeneratedTimestamp = occurrenceTimestamp,
                isActive = willBeActive,
                updatedAt = System.currentTimeMillis()
            )
            recurringRepository.updateRecurringItem(updatedItem)

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
}
