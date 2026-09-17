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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.reports.ReportsFlow
import com.varsel.expensetracker.ui.reports.ReportsTab
import com.varsel.expensetracker.ui.theme.isDark

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
    val isDark = MaterialTheme.colorScheme.isDark
    val containerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
    val containerBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode Switcher (Overview vs Compare)
        Surface(
            modifier = Modifier.weight(0.95f),
            shape = RoundedCornerShape(16.dp),
            color = containerBg,
            border = BorderStroke(1.dp, containerBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
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
            modifier = Modifier.weight(1.05f),
            shape = RoundedCornerShape(16.dp),
            color = containerBg,
            border = BorderStroke(1.dp, containerBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val isExpense = selectedFlow == ReportsFlow.EXPENSES
                val isIncome = selectedFlow == ReportsFlow.INCOME

                FlowPill(
                    label = "Expenses",
                    icon = Icons.Outlined.ArrowUpward,
                    selected = isExpense,
                    activeBg = if (isDark) Color(0xFF450A0A).copy(alpha = 0.7f) else Color(0xFFFEE2E2),
                    activeBorder = if (isDark) Color(0xFFEF4444) else Color(0xFFFCA5A5),
                    activeColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                    onClick = { onFlowSelected(ReportsFlow.EXPENSES) },
                    modifier = Modifier.weight(1f)
                )

                FlowPill(
                    label = "Income",
                    icon = Icons.Outlined.ArrowDownward,
                    selected = isIncome,
                    activeBg = if (isDark) Color(0xFF052E16).copy(alpha = 0.7f) else Color(0xFFDCFCE7),
                    activeBorder = if (isDark) Color(0xFF22C55E) else Color(0xFF86EFAC),
                    activeColor = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                    onClick = { onFlowSelected(ReportsFlow.INCOME) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Single tab selector for standalone view.
 */
@Composable
fun ReportsTabSelector(
    selectedTab: ReportsTab,
    onTabSelected: (ReportsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val containerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
    val containerBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        border = BorderStroke(1.dp, containerBorder)
    ) {
        Row(
            modifier = Modifier
                .height(44.dp)
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
    val isDark = MaterialTheme.colorScheme.isDark

    val selectedBg = if (isDark) Color(0xFF312E81).copy(alpha = 0.65f) else Color(0xFFEEF2FF)
    val selectedBorder = if (isDark) Color(0xFF6366F1) else Color(0xFFA5B4FC)
    val selectedContent = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)

    val unselectedBg = Color.Transparent
    val unselectedBorder = Color.Transparent
    val unselectedContent = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) selectedBg else unselectedBg,
        animationSpec = tween(durationMillis = 180),
        label = "tab_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedBorder else unselectedBorder,
        animationSpec = tween(durationMillis = 180),
        label = "tab_border"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContent else unselectedContent,
        animationSpec = tween(durationMillis = 180),
        label = "tab_fg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp
                ),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlowPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeBg: Color,
    activeBorder: Color,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val unselectedBg = Color.Transparent
    val unselectedBorder = Color.Transparent
    val unselectedContent = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) activeBg else unselectedBg,
        animationSpec = tween(durationMillis = 180),
        label = "flow_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) activeBorder else unselectedBorder,
        animationSpec = tween(durationMillis = 180),
        label = "flow_border"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) activeColor else unselectedContent,
        animationSpec = tween(durationMillis = 180),
        label = "flow_fg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp
                ),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
