package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.reports.ReportsFinancialEvent
import com.varsel.expensetracker.ui.theme.isDark
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun FinancialEventsCard(
    financialEvents: List<ReportsFinancialEvent>,
    onFinancialEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalEffectiveCost = financialEvents.sumOf { it.effectiveCost }

    val cardBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        tonalElevation = 1.dp,
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "LINKED ACTIVITY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Text(
                        text = "Financial Events",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${financialEvents.size} Events",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Metric Summary Bento Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Effective Cost Tile
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Net Effective Cost",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Text(
                            text = formatter.format(totalEffectiveCost),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (totalEffectiveCost < 0.0) AppColors.Income else AppColors.Expense
                        )
                    }
                }
            }

            if (financialEvents.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = "No Financial Events linked to this period.",
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    financialEvents.forEach { event ->
                        FinancialEventItem(
                            event = event,
                            formatter = formatter,
                            onClick = { onFinancialEventClick(event.transactionLinkId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialEventItem(
    event: ReportsFinancialEvent,
    formatter: NumberFormat,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val (amountText, amountColor) = when {
        event.effectiveCost > 0.0 -> {
            formatter.format(event.effectiveCost) to AppColors.Expense
        }
        event.effectiveCost < 0.0 -> {
            formatter.format(abs(event.effectiveCost)) to AppColors.Income
        }
        else -> {
            formatter.format(0.0) to (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.groupName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )

                    if (event.category.isNotBlank()) {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }

                    if (event.coveredMonths.size > 1) {
                        val periodText = formatEventPeriod(event.coveredMonths)
                        val statusText = if (event.isFinalMonth) " • Final Month" else " • Ongoing"
                        Text(
                            text = periodText + statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = amountColor
                )

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Financial Event",
                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Period Breakdown Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.expenseAmount > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppColors.Expense.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${formatter.format(event.expenseAmount)} expense",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = AppColors.Expense,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (event.reimbursedAmount > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppColors.Income.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${formatter.format(event.reimbursedAmount)} reimbursed",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = AppColors.Income,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (event.expenseAmount == 0.0 && event.reimbursedAmount == 0.0) {
                    Text(
                        text = "No transactions this month",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                }
            }

            // Multi-month cumulative totals
            if (event.coveredMonths.size > 1) {
                Text(
                    text = "Total: ${formatter.format(event.totalEventExpense)} exp · ${formatter.format(event.totalEventReimbursement)} reimb",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

private fun formatEventPeriod(
    months: List<YearMonth>
): String {
    if (months.size < 2) {
        return ""
    }

    val sortedMonths = months.distinct().sorted()
    val first = sortedMonths.first()
    val last = sortedMonths.last()

    val monthFormatter = DateTimeFormatter.ofPattern("MMM")
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    return if (first.year == last.year) {
        "Spans ${first.format(monthFormatter)}–${last.format(monthFormatter)} ${last.year}"
    } else {
        "Spans ${first.format(monthYearFormatter)}–${last.format(monthYearFormatter)}"
    }
}
