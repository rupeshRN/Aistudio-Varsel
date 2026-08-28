package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reports page header.
 *
 * Responsibilities:
 * - Display Reports title.
 * - Display the currently selected reporting period.
 * - Navigate between months.
 * - Display the account-filter button.
 *
 * Account selection itself is deliberately kept outside this component.
 */
@Composable
fun ReportsHeader(
    selectedMonth: YearMonth,
    accountFilterLabel: String,
    hasActiveAccountFilter: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormatter =
        DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            Locale.getDefault()
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * Period selector.
             *
             * At this stage it represents Month.
             *
             * Later this area can become:
             *
             * Week
             * Month
             * Quarter
             * Year
             * Custom
             *
             * without changing the filter button.
             */
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = onPreviousMonth
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription =
                                "Previous month"
                        )
                    }

                    Text(
                        text = monthFormatter.format(
                            selectedMonth
                        ),
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = onNextMonth
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription =
                                "Next month"
                        )
                    }
                }
            }

            /*
             * Filter button.
             *
             * A small indicator appears when an account
             * filter is active.
             */
            Box {

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            onClick = onFilterClick
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color =
                        if (hasActiveAccountFilter) {
                            MaterialTheme.colorScheme
                                .primaryContainer
                        } else {
                            MaterialTheme.colorScheme
                                .surfaceVariant
                        }
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.FilterList,
                            contentDescription =
                                "Report filters",
                            tint =
                                if (hasActiveAccountFilter) {
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                }
                        )
                    }
                }

                /*
                 * Small active-filter indicator.
                 */
                if (hasActiveAccountFilter) {

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color =
                                    MaterialTheme.colorScheme
                                        .primary,
                                shape =
                                    RoundedCornerShape(50)
                            )
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }

        /*
         * Only show the account description when a
         * specific account selection is active.
         *
         * All Accounts remains the implicit default.
         */
        if (hasActiveAccountFilter) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = accountFilterLabel,
                    style =
                        MaterialTheme.typography.labelLarge,
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.size(4.dp)
                )

                Text(
                    text = "filtered",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
