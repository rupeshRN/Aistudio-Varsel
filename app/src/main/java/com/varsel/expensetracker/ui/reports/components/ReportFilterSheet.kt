package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.reports.PeriodFilter
import com.varsel.expensetracker.ui.reports.ReportsAccount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Account/report filter sheet.
 *
 * The sheet owns only temporary UI selection.
 *
 * Changes are not applied to the report until the user
 * presses Apply.
 */
@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ReportFilterSheet(
    accounts: List<ReportsAccount>,
    selectedAccountIds: Set<String>,
    selectedPeriod: PeriodFilter,
    customStartDate: LocalDate,
    customEndDate: LocalDate,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onCustomDateRangeSelected: (
        LocalDate,
        LocalDate
    ) -> Unit,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    var temporarySelectedAccounts by remember(
        selectedAccountIds
    ) {
        mutableStateOf(selectedAccountIds)
    }

    val zoneId = ZoneId.systemDefault()

    var showCustomDateRangeDialog by remember {
        mutableStateOf(false)
    }

    /*
     * Keep the date picker state at the ReportFilterSheet level.
     *
     * This is important because the Apply button below also needs
     * access to the selected start/end dates.
     */
    val customPickerState =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis =
                customStartDate
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli(),

            initialSelectedEndDateMillis =
                customEndDate
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Report Filters",
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall
            )

            /*
             * ---------------------------------------------------------
             * PERIOD
             * ---------------------------------------------------------
             */

            Text(
                text = "Period",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            FlowRow(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                PeriodFilterChip(
                    label = "This Month",

                    selected =
                        selectedPeriod ==
                            PeriodFilter.THIS_MONTH,

                    onClick = {
                        onPeriodSelected(
                            PeriodFilter.THIS_MONTH
                        )
                    }
                )

                PeriodFilterChip(
                    label = "Last 3M",

                    selected =
                        selectedPeriod ==
                            PeriodFilter.LAST_3_MONTHS,

                    onClick = {
                        onPeriodSelected(
                            PeriodFilter.LAST_3_MONTHS
                        )
                    }
                )

                PeriodFilterChip(
                    label = "Last 6M",

                    selected =
                        selectedPeriod ==
                            PeriodFilter.LAST_6_MONTHS,

                    onClick = {
                        onPeriodSelected(
                            PeriodFilter.LAST_6_MONTHS
                        )
                    }
                )

                PeriodFilterChip(
                    label = "Year to Date",

                    selected =
                        selectedPeriod ==
                            PeriodFilter.YEAR_TO_DATE,

                    onClick = {
                        onPeriodSelected(
                            PeriodFilter.YEAR_TO_DATE
                        )
                    }
                )

                PeriodFilterChip(
                    label = "Custom Range",

                    selected =
                        selectedPeriod ==
                            PeriodFilter.CUSTOM,

                    onClick = {
                        onPeriodSelected(
                            PeriodFilter.CUSTOM
                        )
                        showCustomDateRangeDialog = true
                    }
                )
            }

            /*
             * ---------------------------------------------------------
             * CUSTOM DATE RANGE (POPUP STYLE)
             * ---------------------------------------------------------
             *
             * The custom date range is displayed as a clean clickable card
             * matching the loan start date picker style. Clicking it opens
             * a DatePickerDialog with the DateRangePicker.
             */

            if (
                selectedPeriod ==
                    PeriodFilter.CUSTOM
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomDateRangeDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Custom Date Range",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatSelectedDateRange(
                                    state = customPickerState,
                                    zoneId = zoneId
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Select Custom Date Range",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (showCustomDateRangeDialog) {
                DatePickerDialog(
                    onDismissRequest = { showCustomDateRangeDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = { showCustomDateRangeDialog = false },
                            enabled = customPickerState.selectedEndDateMillis != null
                        ) {
                            Text("OK")
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
                                text = "Select date range",
                                modifier = Modifier.padding(
                                    start = 24.dp,
                                    top = 16.dp,
                                    end = 24.dp
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        headline = {
                            Text(
                                text = formatSelectedDateRange(
                                    state = customPickerState,
                                    zoneId = zoneId
                                ),
                                modifier = Modifier.padding(
                                    start = 24.dp,
                                    end = 24.dp,
                                    bottom = 12.dp
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        showModeToggle = false
                    )
                }
            }

            /*
             * ---------------------------------------------------------
             * ACCOUNTS
             * ---------------------------------------------------------
             */

            Text(
                text = "Accounts",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            /*
             * All Accounts
             *
             * Empty selection represents All Accounts.
             */
            FilterAccountRow(
                label = "All Accounts",

                selected =
                    temporarySelectedAccounts
                        .isEmpty(),

                onClick = {
                    temporarySelectedAccounts =
                        emptySet()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            if (accounts.isEmpty()) {

                Text(
                    text =
                        "No accounts available",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.padding(
                            vertical = 4.dp
                        )
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(
                                1f,
                                fill = false
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(0.dp)
                ) {

                    items(
                        items = accounts,

                        key = {
                            it.accountId
                        }
                    ) { account ->

                        FilterAccountRow(
                            label =
                                account.displayName,

                            selected =
                                account.accountId in
                                    temporarySelectedAccounts,

                            onClick = {

                                temporarySelectedAccounts =
                                    if (
                                        account.accountId in
                                            temporarySelectedAccounts
                                    ) {

                                        temporarySelectedAccounts -
                                            account.accountId

                                    } else {

                                        temporarySelectedAccounts +
                                            account.accountId
                                    }
                            }
                        )
                    }
                }
            }

            /*
             * ---------------------------------------------------------
             * ACTIONS
             * ---------------------------------------------------------
             */

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Button(
                    onClick = {

                        /*
                         * For Custom Range, read the dates only when
                         * Apply is pressed.
                         */
                        if (
                            selectedPeriod ==
                                PeriodFilter.CUSTOM
                        ) {

                            val startMillis =
                                customPickerState
                                    .selectedStartDateMillis

                            val endMillis =
                                customPickerState
                                    .selectedEndDateMillis

                            /*
                             * Do not apply an incomplete range.
                             */
                            if (
                                startMillis != null &&
                                endMillis != null
                            ) {

                                val startDate =
                                    Instant
                                        .ofEpochMilli(
                                            startMillis
                                        )
                                        .atZone(
                                            zoneId
                                        )
                                        .toLocalDate()

                                val endDate =
                                    Instant
                                        .ofEpochMilli(
                                            endMillis
                                        )
                                        .atZone(
                                            zoneId
                                        )
                                        .toLocalDate()

                                onCustomDateRangeSelected(
                                    startDate,
                                    endDate
                                )

                                onApply(
                                    temporarySelectedAccounts
                                )
                            }

                        } else {

                            /*
                             * Non-custom periods work exactly as
                             * before.
                             */
                            onApply(
                                temporarySelectedAccounts
                            )
                        }
                    }
                ) {
                    Text("Apply")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }
}

/**
 * Individual account filter row.
 */
@Composable
private fun FilterAccountRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    onClick = onClick
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Checkbox(
            checked = selected,

            onCheckedChange = {
                onClick()
            },

            modifier =
                Modifier.offset(x = (-8).dp)
        )

        Text(
            text = label,

            style =
                MaterialTheme
                    .typography
                    .bodyLarge,

            fontWeight =
                if (selected) FontWeight.Medium else FontWeight.Normal,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface,

            modifier =
                Modifier.offset(x = (-4).dp)
        )
    }
}

/**
 * Small horizontal period card.
 *
 * FlowRow in ReportFilterSheet automatically wraps these cards
 * onto another line on narrower phone screens.
 */
@Composable
private fun PeriodFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .clickable(
                    onClick = onClick
                ),

        shape =
            RoundedCornerShape(12.dp),

        color =
            if (selected) {
                MaterialTheme
                    .colorScheme
                    .primaryContainer
            } else {
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
            },

        tonalElevation =
            if (selected) {
                2.dp
            } else {
                0.dp
            },

        border =
            if (selected) {
                BorderStroke(
                    1.dp,
                    MaterialTheme
                        .colorScheme
                        .primary
                )
            } else {
                null
            }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = label,

                style =
                    MaterialTheme
                        .typography
                        .labelLarge,

                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },

                color =
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )

            if (selected) {

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    text = "✓",

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}

/**
 * Formats the current selection shown in the DateRangePicker headline.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun formatSelectedDateRange(
    state: DateRangePickerState,
    zoneId: ZoneId
): String {

    val startMillis =
        state.selectedStartDateMillis

    val endMillis =
        state.selectedEndDateMillis

    if (startMillis == null) {
        return "Select start date"
    }

    val startDate =
        Instant
            .ofEpochMilli(startMillis)
            .atZone(zoneId)
            .toLocalDate()

    if (endMillis == null) {
        return "$startDate → Select end date"
    }

    val endDate =
        Instant
            .ofEpochMilli(endMillis)
            .atZone(zoneId)
            .toLocalDate()

    return "$startDate → $endDate"
}
