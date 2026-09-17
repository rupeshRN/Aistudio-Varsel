package com.varsel.expensetracker.ui.reports.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.reports.ReportsFlow
import com.varsel.expensetracker.ui.theme.isDark

@Composable
fun MoneyFlowCard(
    selectedFlow: ReportsFlow,
    onFlowSelected: (ReportsFlow) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (selectedFlow == ReportsFlow.EXPENSES) "Expenses Breakdown" else "Income Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Compact Flow Selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isExpense = selectedFlow == ReportsFlow.EXPENSES

                        // Expense Pill
                        val expBg by animateColorAsState(
                            targetValue = if (isExpense) {
                                if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
                            } else Color.Transparent,
                            animationSpec = tween(150),
                            label = "exp_bg"
                        )
                        val expColor by animateColorAsState(
                            targetValue = if (isExpense) {
                                if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(150),
                            label = "exp_fg"
                        )

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onFlowSelected(ReportsFlow.EXPENSES) },
                            shape = RoundedCornerShape(9.dp),
                            color = expBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    tint = expColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    color = expColor
                                )
                            }
                        }

                        // Income Pill
                        val incBg by animateColorAsState(
                            targetValue = if (!isExpense) {
                                if (isDark) Color(0xFF052E16) else Color(0xFFDCFCE7)
                            } else Color.Transparent,
                            animationSpec = tween(150),
                            label = "inc_bg"
                        )
                        val incColor by animateColorAsState(
                            targetValue = if (!isExpense) {
                                if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                            } else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(150),
                            label = "inc_fg"
                        )

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onFlowSelected(ReportsFlow.INCOME) },
                            shape = RoundedCornerShape(9.dp),
                            color = incBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = null,
                                    tint = incColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    color = incColor
                                )
                            }
                        }
                    }
                }
            }

            content()
        }
    }
}
