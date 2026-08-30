package com.varsel.expensetracker.ui.reports.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.reports.ReportsFlow
import com.varsel.expensetracker.ui.reports.ReportsTab

/**
 * Consolidated Sticky Control Row:
 * Houses both the Mode switcher (Overview vs Compare) and the Flow switcher (Expenses vs Income).
 * Remains pinned during scroll to prevent context loss.
 */
@Composable
fun ReportsStickyControls(
    selectedTab: ReportsTab,
    onTabSelected: (ReportsTab) -> Unit,
    selectedFlow: ReportsFlow,
    onFlowSelected: (ReportsFlow) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode Switcher (Overview vs Compare)
        Surface(
            modifier = Modifier.weight(1.15f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TabPill(
                    label = "Overview",
                    icon = Icons.Default.DonutSmall,
                    selected = selectedTab == ReportsTab.OVERVIEW,
                    onClick = { onTabSelected(ReportsTab.OVERVIEW) },
                    modifier = Modifier.weight(1f)
                )

                TabPill(
                    label = "Compare",
                    icon = Icons.Default.ShowChart,
                    selected = selectedTab == ReportsTab.COMPARE,
                    onClick = { onTabSelected(ReportsTab.COMPARE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Flow Switcher (Expenses vs Income)
        Surface(
            modifier = Modifier.weight(0.95f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isExpense = selectedFlow == ReportsFlow.EXPENSES
                val isIncome = selectedFlow == ReportsFlow.INCOME

                FlowPill(
                    label = "Expenses",
                    selected = isExpense,
                    activeColor = AppColors.Expense,
                    onClick = { onFlowSelected(ReportsFlow.EXPENSES) },
                    modifier = Modifier.weight(1f)
                )

                FlowPill(
                    label = "Income",
                    selected = isIncome,
                    activeColor = AppColors.Income,
                    onClick = { onFlowSelected(ReportsFlow.INCOME) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Backward-compatible single tab selector if needed.
 */
@Composable
fun ReportsTabSelector(
    selectedTab: ReportsTab,
    onTabSelected: (ReportsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .height(42.dp)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            TabPill(
                label = "Overview",
                icon = Icons.Default.DonutSmall,
                selected = selectedTab == ReportsTab.OVERVIEW,
                onClick = { onTabSelected(ReportsTab.OVERVIEW) },
                modifier = Modifier.weight(1f)
            )

            TabPill(
                label = "Compare & Trends",
                icon = Icons.Default.ShowChart,
                selected = selectedTab == ReportsTab.COMPARE,
                onClick = { onTabSelected(ReportsTab.COMPARE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "tab_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "tab_fg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = backgroundColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FlowPill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "flow_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "flow_fg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = backgroundColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
