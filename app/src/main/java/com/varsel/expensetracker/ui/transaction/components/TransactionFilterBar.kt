package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.theme.isDark
import com.varsel.expensetracker.ui.transaction.TransactionFilter

@Composable
fun TransactionFilterBar(
    filters: Iterable<TransactionFilter>,
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter

            val (label, icon, selectedBg, selectedBorder, selectedContent) = when (filter) {
                TransactionFilter.All -> FilterPillColors(
                    label = "All",
                    icon = Icons.Outlined.FormatListBulleted,
                    bg = if (isDark) Color(0xFF312E81).copy(alpha = 0.55f) else Color(0xFFEEF2FF),
                    border = if (isDark) Color(0xFF6366F1) else Color(0xFFA5B4FC),
                    content = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                )
                TransactionFilter.Expense -> FilterPillColors(
                    label = "Expenses",
                    icon = Icons.Outlined.ArrowUpward,
                    bg = if (isDark) Color(0xFF450A0A).copy(alpha = 0.65f) else Color(0xFFFEE2E2),
                    border = if (isDark) Color(0xFFEF4444) else Color(0xFFFCA5A5),
                    content = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                )
                TransactionFilter.Income -> FilterPillColors(
                    label = "Income",
                    icon = Icons.Outlined.ArrowDownward,
                    bg = if (isDark) Color(0xFF052E16).copy(alpha = 0.65f) else Color(0xFFDCFCE7),
                    border = if (isDark) Color(0xFF22C55E) else Color(0xFF86EFAC),
                    content = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                )
                TransactionFilter.Transfer -> FilterPillColors(
                    label = "Transfers",
                    icon = Icons.Outlined.SwapHoriz,
                    bg = if (isDark) Color(0xFF2E1065).copy(alpha = 0.65f) else Color(0xFFF3E8FF),
                    border = if (isDark) Color(0xFFA855F7) else Color(0xFFD8B4FE),
                    content = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6D28D9)
                )
            }

            val unselectedBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFFFFFFF)
            val unselectedBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
            val unselectedContent = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF64748B)

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) selectedBg else unselectedBg,
                animationSpec = tween(180),
                label = "filter_container"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) selectedBorder else unselectedBorder,
                animationSpec = tween(180),
                label = "filter_border"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) selectedContent else unselectedContent,
                animationSpec = tween(180),
                label = "filter_content_color"
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp
                        ),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

private data class FilterPillColors(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bg: Color,
    val border: Color,
    val content: Color
)

