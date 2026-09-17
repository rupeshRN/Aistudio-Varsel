package com.varsel.expensetracker.ui.reports.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.reports.ReportsFlow
import com.varsel.expensetracker.ui.theme.isDark
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun NetCashFlowCard(
    actualIncome: Double,
    effectiveExpense: Double,
    netCashFlow: Double,
    selectedFlow: ReportsFlow? = null,
    onFlowSelected: ((ReportsFlow) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val incomeColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val expenseColor = if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)

    val cardBackground = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                Color(0xFF0F172A),
                Color(0xFF020617)
            ),
            center = Offset(220f, 0f),
            radius = 950f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC)
            ),
            center = Offset(220f, 0f),
            radius = 850f
        )
    }

    val cardBorderColor = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.55f)
    } else {
        Color(0xFFE2E8F0)
    }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val isPositive = netCashFlow >= 0.0
    val netColor = if (isPositive) incomeColor else expenseColor

    // Savings rate percentage calculation
    val savingsRate = if (actualIncome > 0.0) {
        ((actualIncome - effectiveExpense) / actualIncome * 100.0).coerceIn(-100.0, 100.0)
    } else if (effectiveExpense > 0.0) {
        -100.0
    } else {
        0.0
    }

    val totalVolume = (actualIncome + effectiveExpense).coerceAtLeast(1.0)
    val incomeProportion = (actualIncome / totalVolume).toFloat()
    val animatedIncomeProportion by animateFloatAsState(
        targetValue = incomeProportion,
        animationSpec = tween(durationMillis = 600),
        label = "income_ratio_anim"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = if (isDark) 0.dp else 4.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row: Micro-Label & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NET CASH FLOW SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    // Tactical Surplus/Deficit Pill with Savings Rate
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = netColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, netColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                                contentDescription = null,
                                tint = netColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isPositive) {
                                    "Surplus • %.0f%% Saved".format(Locale.ENGLISH, abs(savingsRate))
                                } else {
                                    "Deficit • %.0f%%".format(Locale.ENGLISH, abs(savingsRate))
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = netColor
                            )
                        }
                    }
                }

                // Main Net Cash Flow Headline Display
                Text(
                    text = (if (isPositive) "+ " else "- ") + currencyFormatter.format(abs(netCashFlow)),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-1.0).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = if (isPositive) {
                        if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    } else {
                        expenseColor
                    }
                )

                // Dual-Ratio Cash Flow Track (Visual proportion of Income vs Expense)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (animatedIncomeProportion > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(animatedIncomeProportion.coerceAtLeast(0.01f))
                                        .background(incomeColor)
                                )
                            }
                            if (1f - animatedIncomeProportion > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight((1f - animatedIncomeProportion).coerceAtLeast(0.01f))
                                        .background(expenseColor)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Income: %.0f%%".format(Locale.ENGLISH, animatedIncomeProportion * 100f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = incomeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Expenses: %.0f%%".format(Locale.ENGLISH, (1f - animatedIncomeProportion) * 100f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = expenseColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Twin Bento Breakdown Tiles: Total Income & Total Expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isIncomeActive = selectedFlow == ReportsFlow.INCOME
                    val isExpenseActive = selectedFlow == ReportsFlow.EXPENSES

                    // Inflow Tile
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = onFlowSelected != null) {
                                onFlowSelected?.invoke(ReportsFlow.INCOME)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isIncomeActive) {
                            incomeColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                        } else {
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isIncomeActive) incomeColor else if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = incomeColor.copy(alpha = 0.18f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDownward,
                                        contentDescription = "Total Income",
                                        tint = incomeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Total Income",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormatter.format(actualIncome),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = incomeColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Outflow Tile
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = onFlowSelected != null) {
                                onFlowSelected?.invoke(ReportsFlow.EXPENSES)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isExpenseActive) {
                            expenseColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                        } else {
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isExpenseActive) expenseColor else if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = expenseColor.copy(alpha = 0.18f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowUpward,
                                        contentDescription = "Total Expenses",
                                        tint = expenseColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Total Expenses",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormatter.format(effectiveExpense),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = expenseColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
