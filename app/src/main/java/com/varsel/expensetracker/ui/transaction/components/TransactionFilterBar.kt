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

    val incomeColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val expenseColor = if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)
    val transferColor = if (isDark) Color(0xFFD1C4E9) else Color(0xFF5E35B1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter

            val (label, icon, activeColor) = when (filter) {
                TransactionFilter.All -> Triple(
                    "All",
                    Icons.Outlined.FormatListBulleted,
                    MaterialTheme.colorScheme.primary
                )
                TransactionFilter.Expense -> Triple(
                    "Expenses",
                    Icons.Outlined.ArrowUpward,
                    expenseColor
                )
                TransactionFilter.Income -> Triple(
                    "Income",
                    Icons.Outlined.ArrowDownward,
                    incomeColor
                )
                TransactionFilter.Transfer -> Triple(
                    "Transfers",
                    Icons.Outlined.SwapHoriz,
                    transferColor
                )
            }

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    activeColor.copy(alpha = if (isDark) 0.22f else 0.14f)
                } else {
                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFFFFFFF)
                },
                animationSpec = tween(150),
                label = "filter_container"
            )

            val borderColor = if (isSelected) {
                activeColor.copy(alpha = 0.65f)
            } else {
                if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0)
            }

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) activeColor else activeColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(150),
                label = "filter_content_color"
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = if (isSelected && !isDark) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                            fontSize = 12.sp
                        ),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

