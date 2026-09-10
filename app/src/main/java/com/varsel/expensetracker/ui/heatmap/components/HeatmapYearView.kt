package com.varsel.expensetracker.ui.heatmap.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.heatmap.CalendarHeatmapUiState
import com.varsel.expensetracker.ui.heatmap.HeatmapMetric
import com.varsel.expensetracker.ui.heatmap.MonthOverview
import com.varsel.expensetracker.util.CurrencyFormatter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Color.isDarkThemeSurface: Boolean
    get() = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

@Composable
fun HeatmapYearView(
    uiState: CalendarHeatmapUiState,
    onMonthSelected: (YearMonth) -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Year Navigation Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousYear,
                    modifier = Modifier.testTag("heatmap_prev_year")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                        contentDescription = "Previous Year",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${uiState.selectedYear} Annual Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onNextYear,
                    modifier = Modifier.testTag("heatmap_next_year")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Next Year",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 12 Months Grid (2 columns)
        val monthPairs = uiState.yearMonths.chunked(2)
        monthPairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { monthOverview ->
                    MiniMonthCard(
                        monthOverview = monthOverview,
                        metric = uiState.selectedMetric,
                        isSelected = monthOverview.yearMonth == uiState.selectedYearMonth,
                        onClick = { onMonthSelected(monthOverview.yearMonth) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // If odd number of months in the final row
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniMonthCard(
    monthOverview: MonthOverview,
    metric: HeatmapMetric,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = monthOverview.yearMonth.format(
        DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    )

    val amount = when (metric) {
        HeatmapMetric.BOTH,
        HeatmapMetric.NET -> monthOverview.totalIncome - monthOverview.totalExpense
        HeatmapMetric.EXPENSE -> monthOverview.totalExpense
        HeatmapMetric.INCOME -> monthOverview.totalIncome
        HeatmapMetric.ACTIVITY -> monthOverview.totalTransactions.toDouble()
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("mini_month_${monthOverview.yearMonth}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Month name & total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (metric == HeatmapMetric.ACTIVITY) "${amount.toInt()}tx" else CurrencyFormatter.format(amount, includeDecimals = false),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = when (metric) {
                        HeatmapMetric.BOTH -> if (amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        HeatmapMetric.EXPENSE -> MaterialTheme.colorScheme.error
                        HeatmapMetric.INCOME -> MaterialTheme.colorScheme.primary
                        HeatmapMetric.NET -> if (amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        HeatmapMetric.ACTIVITY -> MaterialTheme.colorScheme.secondary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini Calendar Matrix (7 columns)
            // Leading blank cells for first day of month (Monday start)
            val firstDayOffset = monthOverview.yearMonth.atDay(1).dayOfWeek.value - 1
            val totalMiniCells = firstDayOffset + monthOverview.days.size

            val rows = (0 until totalMiniCells).chunked(7)
            val isDark = MaterialTheme.colorScheme.surface.isDarkThemeSurface

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rows.forEach { rowIndices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = rowIndices.getOrNull(col)
                            if (cellIndex != null && cellIndex >= firstDayOffset) {
                                val dayIndex = cellIndex - firstDayOffset
                                val dayEntry = monthOverview.days.getOrNull(dayIndex)
                                val intensity = dayEntry?.intensityLevel ?: 0
                                val miniColor = getMiniCellColor(
                                    intensity = intensity,
                                    metric = metric,
                                    isDark = isDark
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(miniColor)
                                        .then(
                                            if (intensity == 0 && !isDark) {
                                                Modifier.border(
                                                    0.5.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                            } else Modifier
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Zero spend days count
            Text(
                text = "${monthOverview.zeroSpendDays} zero-spend days",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getMiniCellColor(
    intensity: Int,
    metric: HeatmapMetric,
    isDark: Boolean
): Color {
    return when (metric) {
        HeatmapMetric.EXPENSE -> when (intensity) {
            0 -> if (isDark) Color(0xFF282A2C) else Color(0xFFE8EAED)
            1 -> if (isDark) Color(0xFF5D2E14) else Color(0xFFFFE0B2)
            2 -> if (isDark) Color(0xFF8D3B1B) else Color(0xFFFFB74D)
            3 -> if (isDark) Color(0xFFC0392B) else Color(0xFFFF7043)
            4 -> if (isDark) Color(0xFFE53935) else Color(0xFFE53935)
            else -> Color.Transparent
        }
        HeatmapMetric.INCOME -> when (intensity) {
            0 -> if (isDark) Color(0xFF282A2C) else Color(0xFFE8EAED)
            1 -> if (isDark) Color(0xFF1B3B22) else Color(0xFFC8E6C9)
            2 -> if (isDark) Color(0xFF2E6930) else Color(0xFF81C784)
            3 -> if (isDark) Color(0xFF388E3C) else Color(0xFF4CAF50)
            4 -> if (isDark) Color(0xFF43A047) else Color(0xFF2E7D32)
            else -> Color.Transparent
        }
        HeatmapMetric.BOTH,
        HeatmapMetric.NET -> when (intensity) {
            0 -> if (isDark) Color(0xFF282A2C) else Color(0xFFE8EAED)
            1 -> if (isDark) Color(0xFF243B53) else Color(0xFFD0E1FD)
            2 -> if (isDark) Color(0xFF334E68) else Color(0xFF88B3F5)
            3 -> if (isDark) Color(0xFF486581) else Color(0xFF4B83E8)
            4 -> if (isDark) Color(0xFF1E88E5) else Color(0xFF1976D2)
            else -> Color.Transparent
        }
        HeatmapMetric.ACTIVITY -> when (intensity) {
            0 -> if (isDark) Color(0xFF282A2C) else Color(0xFFE8EAED)
            1 -> if (isDark) Color(0xFF3B2E58) else Color(0xFFE1D5E7)
            2 -> if (isDark) Color(0xFF5A4482) else Color(0xFFC4A8DE)
            3 -> if (isDark) Color(0xFF7958A6) else Color(0xFF9E77C6)
            4 -> if (isDark) Color(0xFF8E44AD) else Color(0xFF7E38B7)
            else -> Color.Transparent
        }
    }
}
