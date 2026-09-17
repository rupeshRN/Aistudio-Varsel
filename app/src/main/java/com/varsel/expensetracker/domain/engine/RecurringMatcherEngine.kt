package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Result of attempting to match an imported bank statement transaction to an active recurring commitment.
 */
data class RecurringMatchResult(
    val recurringItem: RecurringItem,
    val confidence: MatchConfidence,
    val reason: String
)

enum class MatchConfidence {
    HIGH,   // Exact / near amount + title/note keyword in description + close date proximity
    MEDIUM  // Title keyword in description + close date proximity (with variable amount)
}

@Singleton
class RecurringMatcherEngine @Inject constructor(
    private val scheduleEngine: RecurringScheduleEngine
) {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    /**
     * Matches a transaction to the best active recurring item candidate, if one exists.
     * Prevents false matches across income vs debit, and respects account filtering if present.
     */
    fun findBestMatch(
        transaction: Transaction,
        activeRecurringItems: List<RecurringItem>
    ): RecurringMatchResult? {
        if (activeRecurringItems.isEmpty()) return null

        val txDate = Instant.ofEpochMilli(transaction.dateTimestamp).atZone(zoneId).toLocalDate()
        val descNormalized = normalizeForMatching(
            listOfNotNull(transaction.description, transaction.rawDescription).joinToString(" ")
        )

        var bestMatch: RecurringMatchResult? = null
        var bestScore = -1

        for (item in activeRecurringItems) {
            if (!item.isActive) continue

            // 1. Transaction Type alignment
            val typesCompatible = when (transaction.type) {
                TransactionType.EXPENSE, TransactionType.DEBIT ->
                    item.type == RecurringType.EXPENSE || item.type == RecurringType.SUBSCRIPTION
                TransactionType.INCOME, TransactionType.CREDIT ->
                    item.type == RecurringType.INCOME
                else -> false
            }
            if (!typesCompatible) continue

            // 2. Account compatibility check (if both specify accounts)
            if (!item.accountId.isNullOrBlank() && !transaction.accountId.isNullOrBlank()) {
                if (item.accountId != transaction.accountId) continue
            } else if (!item.accountLast4.isNullOrBlank() && !transaction.accountLast4.isNullOrBlank()) {
                if (item.accountLast4 != transaction.accountLast4) continue
            }

            // 3. Keyword / title matching in description
            val titleTokens = extractKeyTokens(item.title)
            val noteTokens = item.notes?.let { extractKeyTokens(it) } ?: emptyList()
            val allTokens = (titleTokens + noteTokens).filter { it.length >= 3 }

            val hasTextMatch = allTokens.isNotEmpty() && allTokens.any { token ->
                descNormalized.contains(token)
            }

            // 4. Amount matching
            val amountDiff = abs(transaction.amount - item.amount)
            val isExactAmount = amountDiff < 0.01
            val isNearAmount = amountDiff <= (item.amount * 0.05).coerceAtLeast(1.0) // 5% tolerance

            // 5. Date proximity to nextOccurrenceTimestamp or current cycle
            val scheduledDate = Instant.ofEpochMilli(item.nextOccurrenceTimestamp).atZone(zoneId).toLocalDate()
            val daysDiff = abs(ChronoUnit.DAYS.between(scheduledDate, txDate))

            // Maximum allowed window based on frequency
            val maxDayTolerance = when (item.frequency) {
                RecurringFrequency.DAILY -> 2L
                RecurringFrequency.WEEKLY -> 4L
                RecurringFrequency.MONTHLY -> 10L
                RecurringFrequency.QUARTERLY -> 15L
                RecurringFrequency.SEMI_ANNUALLY -> 20L
                RecurringFrequency.YEARLY -> 30L
            }

            val isDateAcceptable = daysDiff <= maxDayTolerance

            // Scoring
            var score = 0
            if (isExactAmount) score += 40
            else if (isNearAmount) score += 25
            else if (item.isVariableAmount && hasTextMatch) score += 20 // variable utility bills, etc.

            if (hasTextMatch) score += 40

            if (isDateAcceptable) {
                // Higher score for closer dates
                val proximityBonus = ((maxDayTolerance - daysDiff).toFloat() / maxDayTolerance * 20).toInt()
                score += proximityBonus.coerceAtLeast(0)
            }

            // Must meet threshold to be considered a match
            if (hasTextMatch && (isExactAmount || isNearAmount) && isDateAcceptable) {
                if (score > bestScore) {
                    bestScore = score
                    bestMatch = RecurringMatchResult(
                        recurringItem = item,
                        confidence = MatchConfidence.HIGH,
                        reason = "Matches ${item.title} (${item.frequency.displayName}) • Due approx ${scheduledDate}"
                    )
                }
            } else if (hasTextMatch && item.isVariableAmount && isDateAcceptable) {
                if (score > bestScore && score >= 50) {
                    bestScore = score
                    bestMatch = RecurringMatchResult(
                        recurringItem = item,
                        confidence = MatchConfidence.MEDIUM,
                        reason = "Variable amount match for ${item.title}"
                    )
                }
            } else if (isExactAmount && daysDiff <= 3 && !hasTextMatch && item.frequency == RecurringFrequency.MONTHLY) {
                // Exact amount and very tight date proximity (within 3 days of expected monthly bill)
                if (score > bestScore && score >= 45) {
                    bestScore = score
                    bestMatch = RecurringMatchResult(
                        recurringItem = item,
                        confidence = MatchConfidence.MEDIUM,
                        reason = "Exact amount matches scheduled ${item.title}"
                    )
                }
            }
        }

        return bestMatch
    }

    /**
     * Helper to reconcile and advance a recurring item when a matching imported transaction is saved.
     * Advances occurrence to the next cycle and records the transaction's timestamp.
     */
    fun advanceOccurrenceAfterMatch(
        item: RecurringItem,
        matchedTxDateTimestamp: Long
    ): RecurringItem {
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

        return item.copy(
            nextOccurrenceTimestamp = nextTimestamp,
            lastGeneratedTimestamp = matchedTxDateTimestamp,
            isActive = willBeActive,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun normalizeForMatching(text: String): String {
        return text.uppercase()
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractKeyTokens(text: String): List<String> {
        val stopWords = setOf(
            "THE", "AND", "FOR", "WITH", "SUBSCRIPTION", "MONTHLY", "PAYMENT", "BILL", "FEES", "CHARGE"
        )
        return normalizeForMatching(text)
            .split(" ")
            .filter { it.length >= 3 && it !in stopWords }
    }
}
