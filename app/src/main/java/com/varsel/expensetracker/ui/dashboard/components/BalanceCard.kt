package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.varsel.expensetracker.ui.design.AppDimensions
import com.varsel.expensetracker.ui.design.AppShapes
import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
import kotlin.math.abs

@Composable
fun BalanceCard(
    summary: BalanceSummaryUiModel,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimensions.ScreenPadding
            ),

        shape = AppShapes.HeroCard,

        elevation = CardDefaults.cardElevation(
            defaultElevation = AppDimensions.CardElevation
        )
    ) {

        Column(
            modifier = Modifier.padding(
                AppDimensions.CardPadding
            )
        ) {

            //--------------------------------------------------
            // Total Balance
            //--------------------------------------------------

            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(
                    AppDimensions.SmallSpacing
                )
            )

            Text(
                text = "₹%,.2f".format(
                    summary.totalBalance
                ),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            //--------------------------------------------------
            // Account-wise balance
            //--------------------------------------------------

            if (summary.accounts.isNotEmpty()) {

                Spacer(
                    modifier = Modifier.height(
                        AppDimensions.LargeSpacing
                    )
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(
                        AppDimensions.MediumSpacing
                    )
                )

                Text(
                    text = "Account Wise Balance",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(
                    modifier = Modifier.height(
                        AppDimensions.SmallSpacing
                    )
                )

                summary.accounts.forEach { account ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "${account.bankName} " +
                                account.accountDisplayName,

                            style =
                                MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text =
                                "₹%,.2f".format(
                                    account.balance
                                ),

                            style =
                                MaterialTheme.typography.bodyMedium,

                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(
                            AppDimensions.SmallSpacing
                        )
                    )
                }
            }

            //--------------------------------------------------
            // Current month income / expense
            //--------------------------------------------------

            Spacer(
                modifier = Modifier.height(
                    AppDimensions.LargeSpacing
                )
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(
                    AppDimensions.LargeSpacing
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {

                MonthlyMetricItem(
                    modifier =
                        Modifier.fillMaxWidth(0.5f),

                    title = "Income",

                    amount =
                        summary.totalIncome,

                    previousAmount =
                        summary.previousMonthIncome,

                    changePercent =
                        summary.incomeChangePercent,

                    positiveColor =
                        MaterialTheme.colorScheme.primary
                )

                MonthlyMetricItem(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start =
                                    AppDimensions.SmallSpacing
                            ),

                    title = "Expense",

                    amount =
                        summary.totalExpense,

                    previousAmount =
                        summary.previousMonthExpense,

                    changePercent =
                        summary.expenseChangePercent,

                    positiveColor =
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MonthlyMetricItem(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    previousAmount: Double,
    changePercent: Double?,
    positiveColor: Color
) {

    Column(
        modifier = modifier
    ) {

        //--------------------------------------------------
        // Title
        //--------------------------------------------------

        Text(
            text = title,
            style =
                MaterialTheme.typography.labelMedium,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(
                AppDimensions.ExtraSmallSpacing
            )
        )

        //--------------------------------------------------
        // Amount + percentage
        //--------------------------------------------------

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "₹%,.2f".format(amount),

                style =
                    MaterialTheme.typography.titleMedium,

                fontWeight =
                    androidx.compose.ui.text.font.FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.width(
                    AppDimensions.ExtraSmallSpacing
                )
            )

            val percent =
                changePercent ?: 0.0

            val isIncrease =
                percent > 0.0

            val isDecrease =
                percent < 0.0

            //--------------------------------------------------
            // Keep the original visual language:
            //
            // ↑ increase
            // ↓ decrease
            // → no change
            //--------------------------------------------------

            val arrow =
                when {
                    isIncrease -> "↑"
                    isDecrease -> "↓"
                    else -> "↑"
                }

            val changeColor =
                when {

                    // Income:
                    // Increase = good
                    title == "Income" &&
                        isIncrease ->
                        positiveColor

                    // Income:
                    // Decrease = bad
                    title == "Income" &&
                        isDecrease ->
                        MaterialTheme.colorScheme.error

                    // Expense:
                    // Increase = bad
                    title == "Expense" &&
                        isIncrease ->
                        MaterialTheme.colorScheme.error

                    // Expense:
                    // Decrease = good
                    title == "Expense" &&
                        isDecrease ->
                        positiveColor

                    else ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                }

            Text(
                text =
                    "$arrow${abs(percent).toInt()}%",

                style =
                    MaterialTheme.typography.titleMedium,

                color =
                    changeColor
            )
        }

        Spacer(
            modifier = Modifier.height(
                AppDimensions.ExtraSmallSpacing
            )
        )

        //--------------------------------------------------
        // Previous month
        //--------------------------------------------------

        Text(
            text =
                "Compared to ₹%,.2f last\nmonth"
                    .format(previousAmount),

            style =
                MaterialTheme.typography.bodyMedium,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
