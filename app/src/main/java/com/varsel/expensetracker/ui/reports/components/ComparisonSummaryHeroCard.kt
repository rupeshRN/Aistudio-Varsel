package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.reports.ComparisonOverviewSummary
import com.varsel.expensetracker.ui.reports.ReportsFlow
import com.varsel.expensetracker.ui.theme.isDark
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun ComparisonSummaryHeroCard(
    summary: ComparisonOverviewSummary,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark

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

    val cardBgGradient = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                themeColor.copy(alpha = 0.14f),
                Color(0xFF0F172A).copy(alpha = 0.95f),
                Color(0xFF0B0F19)
            ),
            radius = 900f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                themeColor.copy(alpha = 0.08f),
                Color(0xFFF8FAFC),
                Color(0xFFFFFFFF)
            ),
            radius = 900f
        )
    }

    val cardBorder = if (isDark) {
        themeColor.copy(alpha = 0.28f)
    } else {
        Color(0xFFE2E8F0)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorder),
        tonalElevation = 2.dp,
        shadowElevation = if (isDark) 0.dp else 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgGradient)
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "TREND & COMPARISON",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Text(
                            text = if (summary.flow == ReportsFlow.EXPENSES) "Spending Trajectory" else "Income Trajectory",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        if (summary.months.isNotEmpty()) {
                            val startLabel = summary.months.first().format(monthShortFormatter)
                            val endLabel = summary.months.last().format(monthShortFormatter)
                            Text(
                                text = "$startLabel → $endLabel",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Month-over-Month Delta badge with explicit "vs. Month"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Current Month ($targetMonthName)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Text(
                            text = currencyFormatter.format(summary.totalTargetAmount),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Net change vs. $baselineMonthName",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        val changeStr = currencyFormatter.format(abs(summary.totalChangeAmount))
                        val prefix = if (summary.totalChangeAmount > 0) "+ " else "- "
                        Text(
                            text = "$prefix$changeStr",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isPositiveOutcome) AppColors.Success else AppColors.Expense
                        )
                    }
                }

                // Macro Sparkline Chart in sleek Bento container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFFFFFFF).copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
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
                    HorizontalDivider(
                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
    val isDark = MaterialTheme.colorScheme.isDark

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.14f),
                modifier = Modifier.size(30.dp)
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

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    maxLines = 1
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
            }
        }
    }
}
