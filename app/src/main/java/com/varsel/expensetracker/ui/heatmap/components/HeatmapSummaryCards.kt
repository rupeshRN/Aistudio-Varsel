package com.varsel.expensetracker.ui.heatmap.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.heatmap.CalendarHeatmapUiState
import com.varsel.expensetracker.ui.heatmap.HeatmapMetric
import com.varsel.expensetracker.ui.heatmap.HeatmapViewMode
import com.varsel.expensetracker.util.CurrencyFormatter
import java.time.format.DateTimeFormatter

@Composable
fun HeatmapSummaryCards(
    uiState: CalendarHeatmapUiState,
    modifier: Modifier = Modifier
) {
    val isYearMode = uiState.viewMode == HeatmapViewMode.YEAR
    val totalAmount = when (uiState.selectedMetric) {
        HeatmapMetric.BOTH -> if (isYearMode) uiState.yearTotalIncome - uiState.yearTotalExpense else uiState.monthNetSavings
        HeatmapMetric.EXPENSE -> if (isYearMode) uiState.yearTotalExpense else uiState.monthTotalExpense
        HeatmapMetric.INCOME -> if (isYearMode) uiState.yearTotalIncome else uiState.monthTotalIncome
        HeatmapMetric.NET -> if (isYearMode) uiState.yearTotalIncome - uiState.yearTotalExpense else uiState.monthNetSavings
        HeatmapMetric.ACTIVITY -> if (isYearMode) uiState.yearMonths.sumOf { it.totalTransactions }.toDouble() else uiState.monthDays.filter { it.isCurrentMonth }.sumOf { it.transactionCount }.toDouble()
    }

    val zeroSpendDays = if (isYearMode) uiState.yearZeroSpendDaysCount else uiState.monthZeroSpendDaysCount
    val activeDays = if (isYearMode) uiState.yearActiveDaysCount else uiState.monthActiveDaysCount

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("heatmap_hero_card"),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (uiState.selectedMetric) {
                            HeatmapMetric.BOTH -> if (isYearMode) "Net Cash Flow (${uiState.selectedYear})" else "Net Cash Flow"
                            HeatmapMetric.EXPENSE -> if (isYearMode) "Total ${uiState.selectedYear} Expenses" else "Total Monthly Spend"
                            HeatmapMetric.INCOME -> if (isYearMode) "Total ${uiState.selectedYear} Income" else "Total Monthly Income"
                            HeatmapMetric.NET -> if (isYearMode) "Net Cash Flow (${uiState.selectedYear})" else "Net Monthly Savings"
                            HeatmapMetric.ACTIVITY -> "Total Transactions"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (uiState.selectedMetric == HeatmapMetric.ACTIVITY) {
                            "${totalAmount.toInt()} Entries"
                        } else {
                            CurrencyFormatter.format(totalAmount, includeDecimals = false)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (uiState.selectedMetric) {
                            HeatmapMetric.BOTH -> if (totalAmount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            HeatmapMetric.EXPENSE -> MaterialTheme.colorScheme.error
                            HeatmapMetric.INCOME -> MaterialTheme.colorScheme.primary
                            HeatmapMetric.NET -> if (totalAmount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            HeatmapMetric.ACTIVITY -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (uiState.selectedMetric == HeatmapMetric.BOTH) {
                        val exp = if (isYearMode) uiState.yearTotalExpense else uiState.monthTotalExpense
                        val inc = if (isYearMode) uiState.yearTotalIncome else uiState.monthTotalIncome
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spend: ${CurrencyFormatter.format(exp, false)}  •  Income: ${CurrencyFormatter.format(inc, false)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!isYearMode && uiState.selectedMetric == HeatmapMetric.EXPENSE && uiState.monthDailyAverage > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily Avg: ${CurrencyFormatter.format(uiState.monthDailyAverage, includeDecimals = false)}/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Metric Icon Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when (uiState.selectedMetric) {
                        HeatmapMetric.BOTH -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        HeatmapMetric.EXPENSE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        HeatmapMetric.INCOME -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        HeatmapMetric.NET -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        HeatmapMetric.ACTIVITY -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (uiState.selectedMetric) {
                                HeatmapMetric.BOTH -> Icons.Outlined.SwapVert
                                HeatmapMetric.EXPENSE -> Icons.Outlined.TrendingDown
                                HeatmapMetric.INCOME -> Icons.Outlined.TrendingUp
                                HeatmapMetric.NET -> Icons.Outlined.Savings
                                HeatmapMetric.ACTIVITY -> Icons.Outlined.Analytics
                            },
                            contentDescription = null,
                            tint = when (uiState.selectedMetric) {
                                HeatmapMetric.BOTH -> MaterialTheme.colorScheme.primary
                                HeatmapMetric.EXPENSE -> MaterialTheme.colorScheme.error
                                HeatmapMetric.INCOME -> MaterialTheme.colorScheme.primary
                                HeatmapMetric.NET -> MaterialTheme.colorScheme.tertiary
                                HeatmapMetric.ACTIVITY -> MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Secondary stats row: Zero-spend days & Peak day / Streaks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zero-spend badge
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Zero-Spend Days",
                value = "$zeroSpendDays days",
                subtext = if (uiState.longestZeroSpendStreak > 1) "Best streak: ${uiState.longestZeroSpendStreak}d" else "$activeDays active days",
                icon = Icons.Outlined.Shield,
                accentColor = MaterialTheme.colorScheme.primary
            )

            // Peak Spend or Activity Day
            StatCard(
                modifier = Modifier.weight(1f),
                title = if (uiState.selectedMetric == HeatmapMetric.EXPENSE) "Peak Spend Day" else "Active Days",
                value = if (uiState.selectedMetric == HeatmapMetric.EXPENSE && uiState.monthPeakDay != null) {
                    uiState.monthPeakDay.format(DateTimeFormatter.ofPattern("d MMM"))
                } else {
                    "$activeDays days"
                },
                subtext = if (uiState.selectedMetric == HeatmapMetric.EXPENSE && uiState.monthPeakAmount > 0) {
                    CurrencyFormatter.format(uiState.monthPeakAmount, includeDecimals = false)
                } else {
                    "Activity recorded"
                },
                icon = if (uiState.selectedMetric == HeatmapMetric.EXPENSE) Icons.Outlined.LocalFireDepartment else Icons.Outlined.EventAvailable,
                accentColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
