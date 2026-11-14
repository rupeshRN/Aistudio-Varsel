package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.domain.model.FinancialEventStatus
import com.varsel.expensetracker.domain.model.FinancialEventStatusSummary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure domain calculator that resolves the settlement status of a Financial Event
 * across real-world roommate and shared-expense scenarios.
 */
object FinancialEventStatusCalculator {

    private const val SETTLEMENT_TOLERANCE = 1.0
    private const val EPSILON = 0.01

    fun calculate(
        totalExpense: Double,
        totalReimbursement: Double,
        expectedReimbursement: Double? = null,
        isManuallySettled: Boolean = false
    ): FinancialEventStatusSummary {
        val expense = totalExpense.coerceAtLeast(0.0)
        val reimbursed = totalReimbursement.coerceAtLeast(0.0)
        val sanitizedExpected = expectedReimbursement?.takeIf { !it.isNaN() && !it.isInfinite() && it >= 0.0 }
        val hasExpected = sanitizedExpected != null
        val target = sanitizedExpected ?: expense
        val personalShare = if (hasExpected && expense > target + EPSILON) {
            expense - target
        } else {
            null
        }

        // 1. Manual settlement override takes top precedence so user-approved settled events stay Settled
        if (isManuallySettled) {
            val surplus = (reimbursed - target).coerceAtLeast(0.0)
            val ratio = if (target > EPSILON) (reimbursed / target).toFloat().coerceIn(0f, 1f) else 1f
            val percent = if (target > EPSILON) ((reimbursed / target) * 100.0).roundToInt().coerceIn(0, 100) else 100

            val message = when {
                expense <= EPSILON && reimbursed <= EPSILON -> "Marked settled"
                surplus > SETTLEMENT_TOLERANCE -> "Settled • +${formatCompactInr(surplus)} extra"
                personalShare != null && personalShare > EPSILON ->
                    "Settled • My share ${formatCompactInr(personalShare)}"
                reimbursed <= EPSILON -> "Marked settled • Paid on my own"
                expense - reimbursed > SETTLEMENT_TOLERANCE ->
                    "Marked settled • ${formatCompactInr(expense - reimbursed)} self/waived"
                else -> "Settled • ${formatCompactInr(reimbursed)} recovered"
            }

            return FinancialEventStatusSummary(
                status = FinancialEventStatus.SETTLED,
                badgeLabel = "Settled",
                statusMessage = message,
                targetReimbursement = target,
                remainingDue = 0.0,
                surplusAmount = surplus,
                personalShare = personalShare,
                recoveryRatio = ratio,
                recoveryPercent = percent,
                isManuallySettled = true,
                hasExpectedReimbursement = hasExpected
            )
        }

        // 2. No expenses recorded yet
        if (expense <= EPSILON) {
            return if (reimbursed > EPSILON) {
                val remainingBeforeExpense = if (hasExpected) (target - reimbursed).coerceAtLeast(0.0) else 0.0
                val ratio = if (hasExpected && target > EPSILON) {
                    (reimbursed / target).toFloat().coerceIn(0f, 1f)
                } else {
                    1f
                }
                val percent = if (hasExpected && target > EPSILON) {
                    ((reimbursed / target) * 100.0).roundToInt().coerceAtLeast(1)
                } else {
                    100
                }
                val message = if (hasExpected && target > EPSILON) {
                    "Advance ${formatCompactInr(reimbursed)} of ${formatCompactInr(target)}"
                } else {
                    "Advance ${formatCompactInr(reimbursed)} received"
                }

                FinancialEventStatusSummary(
                    status = FinancialEventStatus.ADVANCE_RECEIVED,
                    badgeLabel = "Advance",
                    statusMessage = message,
                    targetReimbursement = target,
                    remainingDue = remainingBeforeExpense,
                    surplusAmount = reimbursed,
                    personalShare = null,
                    recoveryRatio = ratio,
                    recoveryPercent = percent,
                    isManuallySettled = false,
                    hasExpectedReimbursement = hasExpected
                )
            } else {
                FinancialEventStatusSummary(
                    status = FinancialEventStatus.NO_ACTIVITY,
                    badgeLabel = "Empty",
                    statusMessage = "No transactions linked yet",
                    targetReimbursement = target,
                    remainingDue = 0.0,
                    surplusAmount = 0.0,
                    personalShare = null,
                    recoveryRatio = 0f,
                    recoveryPercent = 0,
                    isManuallySettled = false,
                    hasExpectedReimbursement = hasExpected
                )
            }
        }

        // 3. Expected reimbursement explicitly set to zero (100% personal expense)
        if (target <= EPSILON) {
            return if (reimbursed > SETTLEMENT_TOLERANCE) {
                FinancialEventStatusSummary(
                    status = FinancialEventStatus.OVERPAID,
                    badgeLabel = "Overpaid",
                    statusMessage = "Overpaid by ${formatCompactInr(reimbursed)}",
                    targetReimbursement = 0.0,
                    remainingDue = 0.0,
                    surplusAmount = reimbursed,
                    personalShare = expense,
                    recoveryRatio = 1f,
                    recoveryPercent = 100,
                    isManuallySettled = false,
                    hasExpectedReimbursement = hasExpected
                )
            } else {
                FinancialEventStatusSummary(
                    status = FinancialEventStatus.SETTLED,
                    badgeLabel = "Settled",
                    statusMessage = "Settled • My share ${formatCompactInr(expense)}",
                    targetReimbursement = 0.0,
                    remainingDue = 0.0,
                    surplusAmount = 0.0,
                    personalShare = expense,
                    recoveryRatio = 1f,
                    recoveryPercent = 100,
                    isManuallySettled = false,
                    hasExpectedReimbursement = hasExpected
                )
            }
        }

        // 4. Reimbursements exceeded target beyond tolerance
        if (reimbursed > target + SETTLEMENT_TOLERANCE) {
            val surplus = reimbursed - target
            val percent = ((reimbursed / target) * 100.0).roundToInt()
            return FinancialEventStatusSummary(
                status = FinancialEventStatus.OVERPAID,
                badgeLabel = "Overpaid",
                statusMessage = "Overpaid by ${formatCompactInr(surplus)}",
                targetReimbursement = target,
                remainingDue = 0.0,
                surplusAmount = surplus,
                personalShare = personalShare,
                recoveryRatio = 1f,
                recoveryPercent = percent,
                isManuallySettled = false,
                hasExpectedReimbursement = hasExpected
            )
        }

        // 5. Reimbursements met target within rounding tolerance
        if (reimbursed >= target - SETTLEMENT_TOLERANCE) {
            val message = if (personalShare != null) {
                "Settled • My share ${formatCompactInr(personalShare)}"
            } else {
                "Settled • 100% recovered"
            }
            return FinancialEventStatusSummary(
                status = FinancialEventStatus.SETTLED,
                badgeLabel = "Settled",
                statusMessage = message,
                targetReimbursement = target,
                remainingDue = 0.0,
                surplusAmount = 0.0,
                personalShare = personalShare,
                recoveryRatio = 1f,
                recoveryPercent = 100,
                isManuallySettled = false,
                hasExpectedReimbursement = hasExpected
            )
        }

        // 6. Partial reimbursement received
        if (reimbursed > EPSILON) {
            val remaining = (target - reimbursed).coerceAtLeast(0.0)
            val ratio = (reimbursed / target).toFloat().coerceIn(0f, 1f)
            val percent = ((reimbursed / target) * 100.0).roundToInt().coerceIn(1, 99)
            val message = if (personalShare != null) {
                "$percent% • ${formatCompactInr(remaining)} left (share ${formatCompactInr(personalShare)})"
            } else {
                "$percent% recovered • ${formatCompactInr(remaining)} left"
            }

            return FinancialEventStatusSummary(
                status = FinancialEventStatus.PARTIAL,
                badgeLabel = "Partial",
                statusMessage = message,
                targetReimbursement = target,
                remainingDue = remaining,
                surplusAmount = 0.0,
                personalShare = personalShare,
                recoveryRatio = ratio,
                recoveryPercent = percent,
                isManuallySettled = false,
                hasExpectedReimbursement = hasExpected
            )
        }

        // 7. Zero reimbursements received yet
        val message = if (personalShare != null) {
            "${formatCompactInr(target)} due from roommates"
        } else {
            "${formatCompactInr(target)} pending"
        }
        return FinancialEventStatusSummary(
            status = FinancialEventStatus.PENDING,
            badgeLabel = "Pending",
            statusMessage = message,
            targetReimbursement = target,
            remainingDue = target,
            surplusAmount = 0.0,
            personalShare = personalShare,
            recoveryRatio = 0f,
            recoveryPercent = 0,
            isManuallySettled = false,
            hasExpectedReimbursement = hasExpected
        )
    }

    internal fun formatCompactInr(amount: Double): String {
        val clean = abs(amount)
        val hasFraction = abs(clean - clean.roundToInt()) >= 0.01
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = if (hasFraction) 2 else 0
            maximumFractionDigits = if (hasFraction) 2 else 0
        }
        return formatter.format(clean)
    }
}
