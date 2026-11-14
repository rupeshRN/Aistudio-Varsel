package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.varsel.expensetracker.domain.model.FinancialEventStatus
import com.varsel.expensetracker.domain.model.FinancialEventStatusFilter
import com.varsel.expensetracker.ui.components.FinancialEventStatusBadge
import com.varsel.expensetracker.ui.components.rememberFinancialEventStatusPalette
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
    modifier: Modifier = Modifier,
    selectedStatusFilter: FinancialEventStatusFilter = FinancialEventStatusFilter.ALL,
    onStatusFilterSelected: (FinancialEventStatusFilter) -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val totalEffectiveCost = financialEvents.sumOf { it.effectiveCost }
    val totalPendingDues = financialEvents.sumOf { it.statusSummary.remainingDue }

    val filteredEvents = financialEvents.filter { event ->
        selectedStatusFilter.matches(event.statusSummary.status)
    }

    val advanceCount = financialEvents.count {
        it.statusSummary.status == FinancialEventStatus.ADVANCE_RECEIVED
    }
    val visibleFilters = buildList {
        add(FinancialEventStatusFilter.ALL)
        add(FinancialEventStatusFilter.PENDING)
        add(FinancialEventStatusFilter.PARTIAL)
        add(FinancialEventStatusFilter.SETTLED)
        add(FinancialEventStatusFilter.OVERPAID)
        if (advanceCount > 0) {
            add(FinancialEventStatusFilter.ADVANCE)
        }
    }

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
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                    )
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
                            color = if (totalEffectiveCost < 0.0) AppColors.Income else AppColors.Expense,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Pending Dues Tile (shown whenever events exist)
                if (financialEvents.isNotEmpty()) {
                    val hasPending = totalPendingDues > 0.5
                    val pendingAccent = if (hasPending) {
                        if (isDark) Color(0xFFFFB74D) else Color(0xFFD97706)
                    } else {
                        if (isDark) Color(0xFF4ADE80) else AppColors.Income
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Pending Dues",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Text(
                                text = if (hasPending) formatter.format(totalPendingDues) else "All Clear",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = pendingAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            if (financialEvents.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleFilters.forEach { filter ->
                        val count = financialEvents.count { filter.matches(it.statusSummary.status) }
                        val isSelected = selectedStatusFilter == filter

                        StatusFilterPill(
                            label = if (filter == FinancialEventStatusFilter.ALL) {
                                "${filter.label} (${financialEvents.size})"
                            } else {
                                "${filter.label} ($count)"
                            },
                            selected = isSelected,
                            onClick = { onStatusFilterSelected(filter) },
                            modifier = Modifier.testTag("event_status_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (financialEvents.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0)
                    )
                ) {
                    Text(
                        text = "No Financial Events linked to this period.",
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredEvents.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No ${selectedStatusFilter.label.lowercase()} events in this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onStatusFilterSelected(FinancialEventStatusFilter.ALL) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("reset_event_status_filter_button")
                        ) {
                            Text("Show All")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredEvents.forEach { event ->
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
private fun StatusFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.6f)
    } else {
        Color(0xFFF1F5F9)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else if (isDark) {
        Color(0xFFCBD5E1)
    } else {
        Color(0xFF475569)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else if (isDark) {
        Color(0xFF334155)
    } else {
        Color(0xFFE2E8F0)
    }

    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor,
                maxLines = 1
            )
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
    val statusSummary = event.statusSummary
    val statusPalette = rememberFinancialEventStatusPalette(statusSummary.status)

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
            .clickable(onClick = onClick)
            .testTag("financial_event_card_${event.transactionLinkId}"),
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = event.groupName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialEventStatusBadge(
                            summary = statusSummary,
                            compact = true,
                            modifier = Modifier.testTag("financial_event_status_badge_${event.transactionLinkId}")
                        )

                        if (event.category.isNotBlank()) {
                            Text(
                                text = event.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (statusSummary.statusMessage.isNotBlank()) {
                        Text(
                            text = statusSummary.statusMessage,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = statusPalette.contentColor
                        )
                    }

                    if (event.coveredMonths.size > 1) {
                        val periodText = formatEventPeriod(event.coveredMonths)
                        val periodSuffix = if (event.isFinalMonth) " • Final Month" else " • Ongoing"
                        Text(
                            text = periodText + periodSuffix,
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

            if (statusSummary.status == FinancialEventStatus.PARTIAL && statusSummary.targetReimbursement > 0.0) {
                LinearProgressIndicator(
                    progress = { statusSummary.recoveryRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = statusPalette.contentColor,
                    trackColor = statusPalette.contentColor.copy(alpha = 0.16f)
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
