package com.varsel.expensetracker.ui.heatmap.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.heatmap.CalendarHeatmapUiState
import com.varsel.expensetracker.ui.heatmap.DayHeatmapEntry
import com.varsel.expensetracker.ui.heatmap.HeatmapMetric
import com.varsel.expensetracker.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HeatmapCalendarView(
    uiState: CalendarHeatmapUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthTitle = uiState.selectedYearMonth.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.testTag("heatmap_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onTodayClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.testTag("heatmap_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday Headers (Mon - Sun)
            val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdays.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (index >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days Grid (7 columns)
            val rows = uiState.monthDays.chunked(7)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        week.forEach { dayEntry ->
                            CalendarDayCell(
                                entry = dayEntry,
                                isSelected = dayEntry.date == uiState.selectedDate,
                                metric = uiState.selectedMetric,
                                onDateClick = { onDateSelected(dayEntry.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap Legend
            HeatmapLegend(
                metric = uiState.selectedMetric,
                maxThreshold = uiState.legendMaxThreshold
            )
        }
    }
}

private val Color.isDarkThemeSurface: Boolean
    get() = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

@Composable
private fun CalendarDayCell(
    entry: DayHeatmapEntry,
    isSelected: Boolean,
    metric: HeatmapMetric,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.isDarkThemeSurface
    val hasExpense = entry.totalExpense > 0.0
    val hasIncome = entry.totalIncome > 0.0
    val netSum = entry.totalIncome - entry.totalExpense

    // Cell Background:
    // Unselected: Neutral card background (theme surface)
    // Selected: Full color filled (Green if net positive/income, Red if net negative/expense)
    val cellColor = if (isSelected) {
        when {
            netSum > 0.0 -> if (isDark) Color(0xFF2E7D32) else Color(0xFF388E3C)
            netSum < 0.0 -> if (isDark) Color(0xFFC62828) else Color(0xFFD32F2F)
            hasIncome && !hasExpense -> if (isDark) Color(0xFF2E7D32) else Color(0xFF388E3C)
            hasExpense && !hasIncome -> if (isDark) Color(0xFFC62828) else Color(0xFFD32F2F)
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        if (!entry.isCurrentMonth) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    val dayNumColor = when {
        isSelected -> Color.White
        !entry.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        entry.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val cumulativeTextColor = when {
        isSelected -> Color.White.copy(alpha = 0.95f)
        !entry.isCurrentMonth -> Color.Transparent
        netSum > 0.0 -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
        netSum < 0.0 -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
        hasIncome || hasExpense -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    val cumulativeAmountText = when {
        !entry.isCurrentMonth -> ""
        hasIncome && hasExpense -> {
            if (netSum > 0.0) "+${formatCompactAmount(netSum)}"
            else if (netSum < 0.0) "-${formatCompactAmount(-netSum)}"
            else "₹0"
        }
        hasIncome -> "+${formatCompactAmount(entry.totalIncome)}"
        hasExpense -> "-${formatCompactAmount(entry.totalExpense)}"
        else -> "—"
    }

    // Small Dots in the predefined color scheme
    val incomeDotColor = if (isSelected) {
        Color.White.copy(alpha = 0.92f)
    } else {
        getHeatmapCellColor(
            intensity = entry.incomeIntensity.coerceIn(1, 4),
            metric = HeatmapMetric.INCOME,
            isCurrentMonth = true,
            isDark = isDark
        )
    }

    val expenseDotColor = if (isSelected) {
        Color.White.copy(alpha = 0.92f)
    } else {
        getHeatmapCellColor(
            intensity = entry.expenseIntensity.coerceIn(1, 4),
            metric = HeatmapMetric.EXPENSE,
            isCurrentMonth = true,
            isDark = isDark
        )
    }

    Surface(
        modifier = modifier
            .aspectRatio(0.88f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = entry.isCurrentMonth, onClick = onDateClick)
            .testTag("calendar_day_${entry.date}"),
        shape = RoundedCornerShape(10.dp),
        color = cellColor,
        border = when {
            isSelected -> BorderStroke(2.dp, if (isDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onPrimaryContainer)
            entry.isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            entry.isCurrentMonth -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Day Number
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (entry.isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = dayNumColor
                )
            }

            // Middle: Small Dot(s) (Shows both Income & Expense if present)
            if (entry.isCurrentMonth && (hasIncome || hasExpense)) {
                Row(
                    modifier = Modifier.padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasIncome) {
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(incomeDotColor)
                        )
                    }
                    if (hasExpense) {
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(expenseDotColor)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(5.5.dp))
            }

            // Bottom: Cumulative Amount in the specific date
            if (entry.isCurrentMonth) {
                Text(
                    text = cumulativeAmountText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (hasIncome || hasExpense) FontWeight.Bold else FontWeight.Normal,
                    color = cumulativeTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegend(
    metric: HeatmapMetric,
    maxThreshold: Double,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.isDarkThemeSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dots legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF43A047) else Color(0xFF2E7D32))
                    )
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFFE53935) else Color(0xFFC62828))
                    )
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selection guide
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Tap to select",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDark) Color(0xFF2E7D32) else Color(0xFF388E3C),
                    modifier = Modifier.size(12.dp)
                ) {}
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDark) Color(0xFFC62828) else Color(0xFFD32F2F),
                    modifier = Modifier.size(12.dp)
                ) {}
            }
        }
    }
}

private fun getHeatmapCellColor(
    intensity: Int,
    metric: HeatmapMetric,
    isCurrentMonth: Boolean,
    isDark: Boolean
): Color {
    if (!isCurrentMonth) {
        return if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.25f) else Color(0xFFF5F5F5).copy(alpha = 0.4f)
    }

    return when (metric) {
        HeatmapMetric.BOTH,
        HeatmapMetric.INCOME -> {
            when (intensity) {
                0 -> if (isDark) Color(0xFF25282A) else Color(0xFFF1F3F4)
                1 -> if (isDark) Color(0xFF2E6930) else Color(0xFF66BB6A)
                2 -> if (isDark) Color(0xFF388E3C) else Color(0xFF43A047)
                3 -> if (isDark) Color(0xFF43A047) else Color(0xFF2E7D32)
                4 -> if (isDark) Color(0xFF66BB6A) else Color(0xFF1B5E20)
                else -> Color.Transparent
            }
        }
        HeatmapMetric.EXPENSE -> {
            when (intensity) {
                0 -> if (isDark) Color(0xFF25282A) else Color(0xFFF1F3F4)
                1 -> if (isDark) Color(0xFF8D3B1B) else Color(0xFFFF7043)
                2 -> if (isDark) Color(0xFFB03A2E) else Color(0xFFF4511E)
                3 -> if (isDark) Color(0xFFE53935) else Color(0xFFE53935)
                4 -> if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)
                else -> Color.Transparent
            }
        }
        HeatmapMetric.NET -> {
            when (intensity) {
                0 -> if (isDark) Color(0xFF25282A) else Color(0xFFF1F3F4)
                1 -> if (isDark) Color(0xFF243B53) else Color(0xFFD0E1FD)
                2 -> if (isDark) Color(0xFF334E68) else Color(0xFF88B3F5)
                3 -> if (isDark) Color(0xFF486581) else Color(0xFF4B83E8)
                4 -> if (isDark) Color(0xFF1E88E5) else Color(0xFF1976D2)
                else -> Color.Transparent
            }
        }
        HeatmapMetric.ACTIVITY -> {
            when (intensity) {
                0 -> if (isDark) Color(0xFF25282A) else Color(0xFFF1F3F4)
                1 -> if (isDark) Color(0xFF3B2E58) else Color(0xFFE1D5E7)
                2 -> if (isDark) Color(0xFF5A4482) else Color(0xFFC4A8DE)
                3 -> if (isDark) Color(0xFF7958A6) else Color(0xFF9E77C6)
                4 -> if (isDark) Color(0xFF8E44AD) else Color(0xFF7E38B7)
                else -> Color.Transparent
            }
        }
    }
}

private fun formatCompactAmount(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    if (absAmount <= 0.0) return "0"
    return when {
        absAmount >= 100000 -> "₹%.0fL".format(absAmount / 100000.0)
        absAmount >= 1000 -> {
            val k = absAmount / 1000.0
            if (k >= 10) "₹%.0fk".format(k) else "₹%.1fk".format(k).replace(".0k", "k")
        }
        else -> "₹%.0f".format(absAmount)
    }
}
