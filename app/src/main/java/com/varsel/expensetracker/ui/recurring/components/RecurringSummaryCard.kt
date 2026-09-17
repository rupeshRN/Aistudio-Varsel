package com.varsel.expensetracker.ui.recurring.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.recurring.model.RecurringPeriod
import com.varsel.expensetracker.ui.recurring.model.RecurringSummaryModel
import com.varsel.expensetracker.ui.theme.isDark
import com.varsel.expensetracker.util.CurrencyFormatter

@Composable
fun RecurringSummaryCard(
    summary: RecurringSummaryModel,
    selectedPeriod: RecurringPeriod,
    onPeriodSelected: (RecurringPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val cardBackground = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                Color(0xFF0F172A),
                Color(0xFF020617)
            ),
            center = Offset(200f, 0f),
            radius = 950f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f),
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC)
            ),
            center = Offset(200f, 0f),
            radius = 850f
        )
    }

    val cardBorderColor = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.55f)
    } else {
        Color(0xFFE2E8F0)
    }

    // Determine values according to selected period
    val mainTitle = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> "Estimated monthly commitment"
        RecurringPeriod.YEARLY -> "Annual Recurring (${summary.currentYear})"
        RecurringPeriod.YTD -> "Year-to-Date So Far (${summary.currentYear})"
    }

    val mainAmount = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> CurrencyFormatter.formatWhole(
            if (summary.estimatedMonthlyCommitment > 0) summary.estimatedMonthlyCommitment else summary.totalActiveMonthlyExpense
        )
        RecurringPeriod.YEARLY -> CurrencyFormatter.formatWhole(summary.totalActiveYearlyExpense)
        RecurringPeriod.YTD -> CurrencyFormatter.formatWhole(summary.totalYtdExpense)
    }

    val mainSubtitle = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> if (summary.occurrencesInMonthCount > 0) {
            "${summary.occurrencesInMonthCount} payment events in ${summary.currentMonthName}"
        } else {
            "Active bills & subscriptions / month"
        }
        RecurringPeriod.YEARLY -> "12-month projected annual commitment"
        RecurringPeriod.YTD -> "Jan – ${summary.currentMonthName} (${summary.elapsedMonthsCount} of 12 months)"
    }

    val subAmount = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> CurrencyFormatter.formatWhole(summary.totalActiveMonthlySubscriptions)
        RecurringPeriod.YEARLY -> CurrencyFormatter.formatWhole(summary.totalActiveYearlySubscriptions)
        RecurringPeriod.YTD -> CurrencyFormatter.formatWhole(summary.totalYtdSubscriptions)
    }

    val subSubtext = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> "${summary.activeSubscriptionsCount} active / mo"
        RecurringPeriod.YEARLY -> "${summary.activeSubscriptionsCount} active / yr"
        RecurringPeriod.YTD -> "${summary.activeSubscriptionsCount} active YTD"
    }

    val incAmount = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> CurrencyFormatter.formatWhole(summary.totalActiveMonthlyIncome)
        RecurringPeriod.YEARLY -> CurrencyFormatter.formatWhole(summary.totalActiveYearlyIncome)
        RecurringPeriod.YTD -> CurrencyFormatter.formatWhole(summary.totalYtdIncome)
    }

    val incSubtext = when (selectedPeriod) {
        RecurringPeriod.MONTHLY -> "per month"
        RecurringPeriod.YEARLY -> "per year"
        RecurringPeriod.YTD -> "received YTD"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = if (isDark) 0.dp else 6.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top row: Category tag & Period switcher (Monthly | Yearly | YTD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COMMITMENTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Period Switcher pills (Monthly | Yearly | YTD)
                    PeriodSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = onPeriodSelected
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero metric with animated transitions
                AnimatedContent(
                    targetState = Triple(mainTitle, mainAmount, mainSubtitle),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "hero_amount_transition"
                ) { (title, amount, subtitle) ->
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = amount,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryStatPill(
                        icon = Icons.Outlined.Subscriptions,
                        label = "Subscriptions",
                        value = subAmount,
                        subtext = subSubtext,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    SummaryStatPill(
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        label = "Income",
                        value = incAmount,
                        subtext = incSubtext,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedPeriod == RecurringPeriod.MONTHLY && (summary.expectedBillsMonthly > 0 || summary.expectedVariableMonthly > 0)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Bills: ${CurrencyFormatter.formatWhole(summary.expectedBillsMonthly)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (summary.expectedVariableMonthly > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "Variable: ${CurrencyFormatter.formatWhole(summary.expectedVariableMonthly)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (summary.dueItemsCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${summary.dueItemsCount} recurring item${if (summary.dueItemsCount > 1) "s are" else " is"} due today or overdue",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: RecurringPeriod,
    onPeriodSelected: (RecurringPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecurringPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                val animBgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "period_bg"
                )
                val animTextColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "period_text"
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(animBgColor)
                        .clickable { onPeriodSelected(period) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = animTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

