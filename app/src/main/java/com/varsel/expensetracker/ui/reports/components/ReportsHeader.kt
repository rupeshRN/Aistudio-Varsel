package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.FilterAlt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.reports.ComparisonWindow
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
 * - Display the 3M/6M comparison window selector when in comparison mode.
 * - Display the account-filter button.
 *
 * Account selection itself is deliberately kept outside this component.
 */
@Composable
fun ReportsHeader(
    periodLabel: String,
    accountFilterLabel: String,
    hasActiveAccountFilter: Boolean,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onFilterClick: () -> Unit,
    isPreviousEnabled: Boolean = true,
    isNextEnabled: Boolean = true,
    showComparisonWindowSelector: Boolean = false,
    selectedComparisonWindow: ComparisonWindow = ComparisonWindow.THREE_MONTHS,
    onComparisonWindowSelected: (ComparisonWindow) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * Period selector.
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
                        onClick = onPreviousPeriod,
                        enabled = isPreviousEnabled
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription =
                                "Previous period",
                            tint =
                                if (isPreviousEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                }
                        )
                    }

                    Text(
                        text = periodLabel,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = onNextPeriod,
                        enabled = isNextEnabled
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription =
                                "Next period",
                            tint =
                                if (isNextEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                }
                        )
                    }
                }
            }

            /*
             * Consolidated 3M / 6M Comparison Window Selector
             * Reclaims vertical space by living right alongside the Date Header.
             */
            if (showComparisonWindowSelector) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ComparisonWindow.values().forEach { window ->
                            val isSelected = window == selectedComparisonWindow
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onComparisonWindowSelected(window) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Text(
                                    text = when (window) {
                                        ComparisonWindow.THREE_MONTHS -> "3M"
                                        ComparisonWindow.SIX_MONTHS -> "6M"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                                )
                            }
                        }
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
                                Icons.Default.FilterAlt,
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
