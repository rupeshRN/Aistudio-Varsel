package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.reports.ComparisonOverviewSummary
import com.varsel.expensetracker.ui.reports.ReportsFlow
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun ComparisonSummaryHeroCard(
    summary: ComparisonOverviewSummary,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    val monthShortFormatter = remember {
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    }

    val themeColor = if (summary.flow == ReportsFlow.EXPENSES) {
        AppColors.Expense
    } else {
        AppColors.Income
    }

    val isExpense = summary.flow == ReportsFlow.EXPENSES
    val change = summary.totalChangeAmount
    val pct = summary.totalPercentageChange
    val isPositiveOutcome = (isExpense && change <= 0) || (!isExpense && change >= 0)
    val badgeColor = if (isPositiveOutcome) AppColors.Success else AppColors.Expense

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title + Flow
            val baselineMonthName = remember(summary.months) {
                if (summary.months.size >= 2) {
                    summary.months[summary.months.size - 2].format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                } else "Prev"
            }
            val targetMonthName = remember(summary.months) {
                if (summary.months.isNotEmpty()) {
                    summary.months.last().format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                } else "Current"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (summary.flow == ReportsFlow.EXPENSES) "Total Spending Trend" else "Total Income Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (summary.months.isNotEmpty()) {
                        val startLabel = summary.months.first().format(monthShortFormatter)
                        val endLabel = summary.months.last().format(monthShortFormatter)
                        Text(
                            text = "$startLabel → $endLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Month-over-Month Delta badge with explicit "vs. Month"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (change >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        val prefix = if (change > 0) "+" else ""
                        Text(
                            text = "vs. $baselineMonthName $prefix${String.format(Locale.ENGLISH, "%.1f", pct)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Big Numbers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Month ($targetMonthName)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormatter.format(summary.totalTargetAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "vs. $baselineMonthName (MoM)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val changeStr = currencyFormatter.format(abs(summary.totalChangeAmount))
                    val prefix = if (summary.totalChangeAmount > 0) "+ " else "- "
                    Text(
                        text = "$prefix$changeStr",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositiveOutcome) AppColors.Success else AppColors.Expense
                    )
                }
            }

            // Macro Sparkline Chart
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    SparklineCanvas(
                        points = summary.sparklinePoints,
                        lineColor = themeColor,
                        strokeWidth = 3.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Micro Insights Divider & Highlights
            if (summary.topIncreasedCategory != null || summary.topDecreasedCategory != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (summary.topDecreasedCategory != null) {
                        InsightMiniPill(
                            title = if (summary.flow == ReportsFlow.EXPENSES) "Top Saving" else "Largest Drop",
                            category = summary.topDecreasedCategory,
                            amount = "-${currencyFormatter.format(abs(summary.topDecreasedAmount))}",
                            color = if (summary.flow == ReportsFlow.EXPENSES) AppColors.Success else AppColors.Expense,
                            icon = Icons.Default.Savings,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (summary.topIncreasedCategory != null) {
                        InsightMiniPill(
                            title = if (summary.flow == ReportsFlow.EXPENSES) "Largest Increase" else "Top Gain",
                            category = summary.topIncreasedCategory,
                            amount = "+${currencyFormatter.format(abs(summary.topIncreasedAmount))}",
                            color = if (summary.flow == ReportsFlow.EXPENSES) AppColors.Expense else AppColors.Success,
                            icon = Icons.Default.WarningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightMiniPill(
    title: String,
    category: String,
    amount: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
    }
}
