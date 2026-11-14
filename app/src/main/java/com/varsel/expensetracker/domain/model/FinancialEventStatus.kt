package com.varsel.expensetracker.domain.model

/**
 * Settlement lifecycle status for a Financial Event (shared expense & reimbursement tracking).
 */
enum class FinancialEventStatus(val label: String) {
    PENDING("Pending"),
    PARTIAL("Partial"),
    SETTLED("Settled"),
    OVERPAID("Overpaid"),
    ADVANCE_RECEIVED("Advance"),
    NO_ACTIVITY("Empty")
}

/**
 * Reports filter options for Financial Events.
 */
enum class FinancialEventStatusFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    PARTIAL("Partial"),
    SETTLED("Settled"),
    OVERPAID("Overpaid"),
    ADVANCE("Advance");

    fun matches(status: FinancialEventStatus): Boolean = when (this) {
        ALL -> true
        PENDING -> status == FinancialEventStatus.PENDING
        PARTIAL -> status == FinancialEventStatus.PARTIAL
        SETTLED -> status == FinancialEventStatus.SETTLED
        OVERPAID -> status == FinancialEventStatus.OVERPAID
        ADVANCE -> status == FinancialEventStatus.ADVANCE_RECEIVED
    }
}

/**
 * Computed settlement status details for a Financial Event.
 */
data class FinancialEventStatusSummary(
    val status: FinancialEventStatus = FinancialEventStatus.NO_ACTIVITY,
    val badgeLabel: String = "Pending",
    val statusMessage: String = "",
    val targetReimbursement: Double = 0.0,
    val remainingDue: Double = 0.0,
    val surplusAmount: Double = 0.0,
    val personalShare: Double? = null,
    val recoveryRatio: Float = 0f,
    val recoveryPercent: Int = 0,
    val isManuallySettled: Boolean = false,
    val hasExpectedReimbursement: Boolean = false
)
