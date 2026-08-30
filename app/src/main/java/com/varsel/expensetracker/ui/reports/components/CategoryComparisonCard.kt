package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
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
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.reports.CategoryComparisonItem
import com.varsel.expensetracker.ui.reports.ReportsFlow
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun CategoryComparisonCard(
    item: CategoryComparisonItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = remember(item.category) {
        CategoryPalette.colorFor(item.category)
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    val monthShortFormatter = remember {
        DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    }

    val maxAmountInItem = remember(item.monthlyTotals) {
        (item.monthlyTotals.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon + Title + Transition Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReportCategoryIcon(
                    category = item.category,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    // Month A -> Month B comparison values with explicit month labels
                    val baselineMonthLabel = remember(item.baselineMonth) {
                        item.baselineMonth.format(monthShortFormatter)
                    }
                    val targetMonthLabel = remember(item.targetMonth) {
                        item.targetMonth.format(monthShortFormatter)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$baselineMonthLabel: ${currencyFormatter.format(item.baselineAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$targetMonthLabel: ${currencyFormatter.format(item.targetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val baselineMonthLabel = remember(item.baselineMonth) {
                    item.baselineMonth.format(monthShortFormatter)
                }

                // Delta Badge
                DeltaBadge(
                    changeAmount = item.changeAmount,
                    percentageChange = item.percentageChange,
                    isNew = item.isNew,
                    isEliminated = item.isEliminated,
                    flow = item.flow,
                    baselineMonthLabel = baselineMonthLabel,
                    currencyFormatter = currencyFormatter
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View category transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Sparkline Visualizer Row
            if (item.monthlyTotals.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                        SparklineCanvas(
                            points = item.sparklinePoints,
                            lineColor = categoryColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Micro Bar & Month Label Breakdown Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.monthlyTotals.forEachIndexed { index, monthTotal ->
                        val isTargetMonth = index == item.monthlyTotals.size - 1
                        val heightFraction = (monthTotal.amount / maxAmountInItem).toFloat().coerceIn(0.08f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currencyFormatter.format(monthTotal.amount),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = if (isTargetMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTargetMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )

                            // Proportional Micro-bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {}
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth(heightFraction)
                                        .height(4.dp),
                                    color = if (isTargetMonth) categoryColor else categoryColor.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(2.dp)
                                ) {}
                            }

                            Text(
                                text = monthTotal.month.format(monthShortFormatter),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = if (isTargetMonth) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTargetMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeltaBadge(
    changeAmount: Double,
    percentageChange: Double,
    isNew: Boolean,
    isEliminated: Boolean,
    flow: ReportsFlow,
    baselineMonthLabel: String,
    currencyFormatter: NumberFormat
) {
    val (badgeColor, textColor, text, icon) = when {
        isNew -> {
            Quadruple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                "New",
                null
            )
        }
        isEliminated -> {
            Quadruple(
                AppColors.Success.copy(alpha = 0.15f),
                AppColors.Success,
                "vs. $baselineMonthLabel -100%",
                Icons.AutoMirrored.Filled.TrendingDown
            )
        }
        flow == ReportsFlow.EXPENSES -> {
            if (changeAmount < 0) {
                // Spending decreased -> Good (Success)
                Quadruple(
                    AppColors.Success.copy(alpha = 0.15f),
                    AppColors.Success,
                    "vs. $baselineMonthLabel -${String.format(Locale.ENGLISH, "%.1f", abs(percentageChange))}%",
                    Icons.AutoMirrored.Filled.TrendingDown
                )
            } else if (changeAmount > 0) {
                // Spending increased -> Warning/Expense
                Quadruple(
                    AppColors.Expense.copy(alpha = 0.12f),
                    AppColors.Expense,
                    "vs. $baselineMonthLabel +${String.format(Locale.ENGLISH, "%.1f", percentageChange)}%",
                    Icons.AutoMirrored.Filled.TrendingUp
                )
            } else {
                // No change
                Quadruple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    "vs. $baselineMonthLabel 0%",
                    null
                )
            }
        }
        else -> {
            // Income
            if (changeAmount > 0) {
                // Income increased -> Good (Success)
                Quadruple(
                    AppColors.Success.copy(alpha = 0.15f),
                    AppColors.Success,
                    "vs. $baselineMonthLabel +${String.format(Locale.ENGLISH, "%.1f", percentageChange)}%",
                    Icons.AutoMirrored.Filled.TrendingUp
                )
            } else if (changeAmount < 0) {
                // Income decreased -> Warning
                Quadruple(
                    AppColors.Expense.copy(alpha = 0.12f),
                    AppColors.Expense,
                    "vs. $baselineMonthLabel -${String.format(Locale.ENGLISH, "%.1f", abs(percentageChange))}%",
                    Icons.AutoMirrored.Filled.TrendingDown
                )
            } else {
                Quadruple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    "vs. $baselineMonthLabel 0%",
                    null
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
