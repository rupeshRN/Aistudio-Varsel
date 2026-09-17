package com.varsel.expensetracker.ui.reports.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.reports.PeriodFilter
import com.varsel.expensetracker.ui.reports.ReportsAccount
import com.varsel.expensetracker.ui.theme.isDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Modern Bento-styled filter sheet for the Reports page.
 *
 * Provides tactile period controls, interactive custom date range selection,
 * and clear multi-account scoping with immediate visual feedback.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportFilterSheet(
    accounts: List<ReportsAccount>,
    selectedAccountIds: Set<String>,
    selectedPeriod: PeriodFilter,
    customStartDate: LocalDate,
    customEndDate: LocalDate,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onCustomDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    var temporarySelectedAccounts by remember(selectedAccountIds) {
        mutableStateOf(selectedAccountIds)
    }

    val zoneId = ZoneId.systemDefault()
    val isDark = MaterialTheme.colorScheme.isDark
    var showCustomDateRangeDialog by remember { mutableStateOf(false) }

    val customPickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = customStartDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis = customEndDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title + Reset Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Report Filters",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scope period and account visibility",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = {
                        temporarySelectedAccounts = emptySet()
                        onPeriodSelected(PeriodFilter.THIS_MONTH)
                    }
                ) {
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Section 1: Period Selection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TIME PERIOD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val periods = listOf(
                        PeriodFilter.THIS_MONTH to "This Month",
                        PeriodFilter.LAST_3_MONTHS to "Last 3M",
                        PeriodFilter.LAST_6_MONTHS to "Last 6M",
                        PeriodFilter.YEAR_TO_DATE to "Year to Date",
                        PeriodFilter.CUSTOM to "Custom Range"
                    )

                    periods.forEach { (period, label) ->
                        val isSelected = selectedPeriod == period
                        PeriodBentoChip(
                            label = label,
                            selected = isSelected,
                            onClick = {
                                onPeriodSelected(period)
                                if (period == PeriodFilter.CUSTOM) {
                                    showCustomDateRangeDialog = true
                                }
                            }
                        )
                    }
                }

                // Interactive Custom Range Preview Card
                if (selectedPeriod == PeriodFilter.CUSTOM) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showCustomDateRangeDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.EditCalendar,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "Custom Date Range",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatSelectedDateRange(customPickerState, zoneId),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Section 2: Account Selection
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNTS SCOPE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    val activeCount = if (temporarySelectedAccounts.isEmpty()) {
                        "All Accounts"
                    } else {
                        "${temporarySelectedAccounts.size} of ${accounts.size}"
                    }

                    Text(
                        text = activeCount,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // All Accounts Master Toggle
                AccountBentoRow(
                    title = "All Accounts",
                    subtitle = "Show unified financial flow from every account",
                    isCash = false,
                    selected = temporarySelectedAccounts.isEmpty(),
                    onClick = { temporarySelectedAccounts = emptySet() }
                )

                // Individual Accounts List
                if (accounts.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = accounts, key = { it.accountId }) { account ->
                            val isSelected = account.accountId in temporarySelectedAccounts
                            AccountBentoRow(
                                title = account.displayName,
                                subtitle = account.bankName ?: "Liquid Funds",
                                isCash = account.accountId == ReportsAccount.CASH_ID,
                                selected = isSelected,
                                onClick = {
                                    temporarySelectedAccounts = if (isSelected) {
                                        temporarySelectedAccounts - account.accountId
                                    } else {
                                        temporarySelectedAccounts + account.accountId
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = {
                        if (selectedPeriod == PeriodFilter.CUSTOM) {
                            val startMillis = customPickerState.selectedStartDateMillis
                            val endMillis = customPickerState.selectedEndDateMillis

                            if (startMillis != null && endMillis != null) {
                                val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
                                val endDate = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate()
                                onCustomDateRangeSelected(startDate, endDate)
                                onApply(temporarySelectedAccounts)
                            }
                        } else {
                            onApply(temporarySelectedAccounts)
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply Filters",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Custom Date Range Modal Dialog
    if (showCustomDateRangeDialog) {
        DatePickerDialog(
            onDismissRequest = { showCustomDateRangeDialog = false },
            confirmButton = {
                Button(
                    onClick = { showCustomDateRangeDialog = false },
                    enabled = customPickerState.selectedEndDateMillis != null,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Select Range")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateRangeDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = customPickerState,
                title = {
                    Text(
                        text = "Select custom date range",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                headline = {
                    Text(
                        text = formatSelectedDateRange(customPickerState, zoneId),
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun PeriodBentoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val bg by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
        },
        animationSpec = tween(150),
        label = "chip_bg"
    )

    val border by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
        },
        animationSpec = tween(150),
        label = "chip_border"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(150),
        label = "chip_text"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AccountBentoRow(
    title: String,
    subtitle: String,
    isCash: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.08f)
    } else {
        if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f) else Color(0xFFF8FAFC)
    }

    val cardBorder = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        if (isDark) Color(0xFF334155).copy(alpha = 0.35f) else Color(0xFFE2E8F0)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isCash) {
                    Color(0xFF10B981).copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCash) Icons.Outlined.Payments else Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = if (isCash) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun formatSelectedDateRange(
    state: DateRangePickerState,
    zoneId: ZoneId
): String {
    val startMillis = state.selectedStartDateMillis
    val endMillis = state.selectedEndDateMillis

    if (startMillis == null) {
        return "Select start date"
    }

    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()

    if (endMillis == null) {
        return "${startDate.format(formatter)} → Select end"
    }

    val endDate = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate()
    return "${startDate.format(formatter)} – ${endDate.format(formatter)}"
}
