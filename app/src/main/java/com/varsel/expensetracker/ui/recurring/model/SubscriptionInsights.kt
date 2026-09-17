package com.varsel.expensetracker.ui.recurring.model

import com.varsel.expensetracker.domain.model.recurring.RecurringItem

/**
 * Detailed insights into subscriptions and recurring commitments.
 */
data class SubscriptionInsights(
    val totalMonthlySubscriptionCost: Double = 0.0,
    val totalYearlySubscriptionCost: Double = 0.0,
    val activeSubscriptionsCount: Int = 0,
    val mostExpensiveSubscription: RecurringItem? = null,
    val renewingInNext30Days: List<RenewingSubscriptionItem> = emptyList(),
    val spendingByCategory: List<CategorySpendBreakdown> = emptyList(),
    val priceChangesDetected: List<PriceChangeAlert> = emptyList()
)

data class RenewingSubscriptionItem(
    val item: RecurringItem,
    val daysUntilRenewal: Long,
    val renewalDateFormatted: String,
    val formattedAmount: String
)

data class CategorySpendBreakdown(
    val categoryName: String,
    val monthlyAmount: Double,
    val percentage: Float, // 0.0 to 1.0
    val colorHex: String = "#3B82F6"
)

data class PriceChangeAlert(
    val item: RecurringItem,
    val configuredAmount: Double,
    val latestRecordedAmount: Double,
    val difference: Double,
    val isIncrease: Boolean
)

/**
 * Items requiring review in the "Review Subscriptions" workflow.
 */
sealed class SubscriptionReviewItem {
    data class OverdueMissedCycles(
        val item: RecurringItem,
        val missedCount: Int,
        val missedTimestamps: List<Long>,
        val oldestMissedDateFormatted: String
    ) : SubscriptionReviewItem()

    data class PriceHike(
        val item: RecurringItem,
        val oldAmount: Double,
        val newAmount: Double,
        val diff: Double
    ) : SubscriptionReviewItem()

    data class NoRecentConfirmation(
        val item: RecurringItem,
        val daysSinceLastConfirmation: Long?,
        val message: String
    ) : SubscriptionReviewItem()

    data class MissingAccountLink(
        val item: RecurringItem,
        val message: String
    ) : SubscriptionReviewItem()

    data class InactiveItem(
        val item: RecurringItem
    ) : SubscriptionReviewItem()
}
