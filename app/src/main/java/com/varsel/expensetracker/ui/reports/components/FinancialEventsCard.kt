package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.reports.ReportsFinancialEvent
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FinancialEventsCard(
    financialEvents: List<ReportsFinancialEvent>,
    onFinancialEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    val totalEffectiveCost =
        financialEvents.sumOf {
            it.effectiveCost
        }

    Surface(
        modifier =
            modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        color =
            MaterialTheme.colorScheme
                .surface
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            Text(
                text =
                    "Financial Events",

                style =
                    MaterialTheme.typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Events",

                        style =
                            MaterialTheme.typography
                                .labelMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text =
                            financialEvents.size
                                .toString(),

                        style =
                            MaterialTheme.typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Effective Cost",

                        style =
                            MaterialTheme.typography
                                .labelMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text =
                            formatter.format(
                                totalEffectiveCost
                            ),

                        style =
                            MaterialTheme.typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            if (totalEffectiveCost < 0.0) {
                                AppColors.Income
                            } else {
                                AppColors.Expense
                            }
                    )
                }
            }

            if (financialEvents.isEmpty()) {

                Text(
                    text =
                        "No Financial Events for this period.",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 12.dp
                            ),

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

            } else {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    financialEvents.forEach { event ->

                        FinancialEventRow(
                            event =
                                event,

                            formatter =
                                formatter,

                            onClick = {
                                onFinancialEventClick(
                                    event.transactionLinkId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialEventRow(
    event: ReportsFinancialEvent,
    formatter: NumberFormat,
    onClick: () -> Unit
) {
    val periodAmountPresentation =
        rememberFinancialEventPeriodAmount(
            event = event,
            formatter = formatter
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    vertical = 10.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                6.dp
            )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        event.groupName,

                    style =
                        MaterialTheme.typography
                            .bodyLarge,

                    fontWeight =
                        FontWeight.Medium
                )

                if (
                    event.category.isNotBlank()
                ) {

                    Text(
                        text =
                            event.category,

                        style =
                            MaterialTheme.typography
                                .labelMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                /*
                 * Show a small indicator when the Financial Event
                 * has linked transactions in more than one month.
                 *
                 * Example:
                 *
                 * Spans Jun–Jul 2026
                 */
                if (
                    event.coveredMonths.size > 1
                ) {

                    Text(
                        text =
                            formatEventPeriod(
                                event.coveredMonths
                            ),

                        style =
                            MaterialTheme.typography
                                .labelSmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,

                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            /*
             * IMPORTANT:
             *
             * This amount represents the Financial Event's
             * activity within the CURRENTLY SELECTED REPORT
             * PERIOD.
             *
             * It must not blindly use effectiveCost because:
             *
             * June:
             *   reimbursement only
             *   effectiveCost = -₹25,474.09
             *
             * The user should see:
             *
             *   ₹25,474.09
             *
             * in GREEN, not:
             *
             *   -₹25,474.09
             *
             * in RED.
             */
            Text(
                text =
                    periodAmountPresentation.amountText,

                style =
                    MaterialTheme.typography
                        .bodyLarge,

                fontWeight =
                    FontWeight.Bold,

                color =
                    periodAmountPresentation.color
            )

            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled
                        .ArrowForward,

                contentDescription =
                    "Open Financial Event",

                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        /*
         * Detailed period activity.
         *
         * Expense is always RED.
         * Reimbursement is always GREEN.
         */
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            if (
                event.expenseAmount > 0.0
            ) {

                Text(
                    text =
                        "${formatter.format(
                            event.expenseAmount
                        )} expense",

                    style =
                        MaterialTheme.typography
                            .labelMedium,

                    color =
                        AppColors.Expense,

                    fontWeight =
                        FontWeight.Medium
                )
            }

            if (
                event.reimbursedAmount > 0.0
            ) {

                Text(
                    text =
                        "${formatter.format(
                            event.reimbursedAmount
                        )} reimbursed",

                    style =
                        MaterialTheme.typography
                            .labelMedium,

                    color =
                        AppColors.Income,

                    fontWeight =
                        FontWeight.Medium
                )
            }
        }

        HorizontalDivider(
            modifier =
                Modifier.padding(
                    top = 6.dp
                )
        )
    }
}

/**
 * Determines how the prominent amount on a Financial Event
 * should be displayed for the CURRENTLY SELECTED REPORT PERIOD.
 *
 * Rules:
 *
 * 1. Reimbursement only
 *
 *    expense = ₹0
 *    reimbursement = ₹25,474.09
 *
 *    -> ₹25,474.09 GREEN
 *
 * 2. Expense only
 *
 *    expense = ₹26,950.45
 *    reimbursement = ₹0
 *
 *    -> ₹26,950.45 RED
 *
 * 3. Both expense and reimbursement
 *
 *    expense = ₹26,950.45
 *    reimbursement = ₹25,474.09
 *
 *    effective cost = ₹1,476.36
 *
 *    -> ₹1,476.36 RED
 *
 * 4. Reimbursement exceeds expense
 *
 *    expense = ₹1,000
 *    reimbursement = ₹1,500
 *
 *    effective cost = -₹500
 *
 *    -> ₹500 GREEN
 *
 * 5. Fully reimbursed
 *
 *    expense = ₹1,000
 *    reimbursement = ₹1,000
 *
 *    -> ₹0
 *
 * This function deliberately separates:
 *
 * - transaction direction during the selected period
 * - the Financial Event's overall business meaning
 */
@Composable
private fun rememberFinancialEventPeriodAmount(
    event: ReportsFinancialEvent,
    formatter: NumberFormat
): FinancialEventPeriodAmount {

    val expense =
        event.expenseAmount

    val reimbursement =
        event.reimbursedAmount

    /*
     * --------------------------------------------------------
     * REIMBURSEMENT ONLY
     * --------------------------------------------------------
     *
     * This is the important June case for Train.
     */
    if (
        expense <= 0.0 &&
        reimbursement > 0.0
    ) {

        return FinancialEventPeriodAmount(
            amountText =
                formatter.format(
                    reimbursement
                ),

            color =
                AppColors.Income
        )
    }

    /*
     * --------------------------------------------------------
     * EXPENSE ONLY
     * --------------------------------------------------------
     */
    if (
        expense > 0.0 &&
        reimbursement <= 0.0
    ) {

        return FinancialEventPeriodAmount(
            amountText =
                formatter.format(
                    expense
                ),

            color =
                AppColors.Expense
        )
    }

    /*
     * --------------------------------------------------------
     * BOTH EXPENSE + REIMBURSEMENT
     * --------------------------------------------------------
     *
     * Use the period's net direction.
     */
    val effectiveCost =
        expense -
            reimbursement

    if (
        effectiveCost < 0.0
    ) {

        return FinancialEventPeriodAmount(
            amountText =
                formatter.format(
                    kotlin.math.abs(
                        effectiveCost
                    )
                ),

            color =
                AppColors.Income
        )
    }

    if (
        effectiveCost > 0.0
    ) {

        return FinancialEventPeriodAmount(
            amountText =
                formatter.format(
                    effectiveCost
                ),

            color =
                AppColors.Expense
        )
    }

    /*
     * --------------------------------------------------------
     * FULLY OFFSET
     * --------------------------------------------------------
     */
    return FinancialEventPeriodAmount(
        amountText =
            formatter.format(
                0.0
            ),

        color =
            MaterialTheme.colorScheme
                .onSurfaceVariant
    )
}

private data class FinancialEventPeriodAmount(
    val amountText: String,
    val color: androidx.compose.ui.graphics.Color
)

/**
 * Creates a concise user-facing description of the months
 * covered by a Financial Event.
 *
 * Examples:
 *
 * Jun 2026 + Jul 2026
 *     -> Spans Jun–Jul 2026
 *
 * Jun 2026 + Jul 2026 + Aug 2026
 *     -> Spans Jun–Aug 2026
 *
 * Dec 2025 + Jan 2026
 *     -> Spans Dec 2025–Jan 2026
 */
private fun formatEventPeriod(
    months: List<YearMonth>
): String {

    if (
        months.size < 2
    ) {
        return ""
    }

    val sortedMonths =
        months
            .distinct()
            .sorted()

    val first =
        sortedMonths.first()

    val last =
        sortedMonths.last()

    val monthFormatter =
        DateTimeFormatter.ofPattern(
            "MMM"
        )

    val monthYearFormatter =
        DateTimeFormatter.ofPattern(
            "MMM yyyy"
        )

    return if (
        first.year == last.year
    ) {

        "Spans ${
            first.format(
                monthFormatter
            )
        }–${
            last.format(
                monthFormatter
            )
        } ${last.year}"

    } else {

        "Spans ${
            first.format(
                monthYearFormatter
            )
        }–${
            last.format(
                monthYearFormatter
            )
        }"
    }
}
