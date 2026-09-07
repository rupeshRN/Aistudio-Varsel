package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
import kotlin.math.abs

@Composable
fun BalanceCard(
    summary: BalanceSummaryUiModel,
    isBalanceHidden: Boolean = false,
    onToggleVisibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Hero Balance Card with Rich Tonal Depth Gradient
    val heroGradient = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            )
        }
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = if (isDark) 2.dp else 4.dp,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header with Privacy Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Net Liquid Balance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isBalanceHidden) "Show balance" else "Hide balance",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Dominant Hero Balance Amount Display
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isBalanceHidden) "₹ ••••••••" else "₹%,.2f".format(summary.totalBalance),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 38.sp,
                            lineHeight = 44.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = (-1).sp
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                // Monthly Income and Expense Pills with Strong Semantic Styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IncomeExpensePill(
                        modifier = Modifier.weight(1f),
                        title = "Income",
                        amount = summary.totalIncome,
                        isIncome = true,
                        isBalanceHidden = isBalanceHidden,
                        changePercent = summary.incomeChangePercent
                    )

                    IncomeExpensePill(
                        modifier = Modifier.weight(1f),
                        title = "Expense",
                        amount = summary.totalExpense,
                        isIncome = false,
                        isBalanceHidden = isBalanceHidden,
                        changePercent = summary.expenseChangePercent
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeExpensePill(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    isIncome: Boolean,
    isBalanceHidden: Boolean,
    changePercent: Double?
) {
    val isDark = isSystemInDarkTheme()

    // High Contrast Semantic Green & Red Palettes matching Transaction Detail
    val primaryColor = if (isIncome) {
        if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    } else {
        if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)
    }

    val pillBackground = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.92f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = pillBackground,
        shadowElevation = if (isDark) 0.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                changePercent?.let { pct ->
                    val arrow = if (pct > 0) "↑" else "↓"
                    Text(
                        text = "$arrow${abs(pct).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                }
            }

            Text(
                text = if (isBalanceHidden) "₹ ••••" else "₹%,.2f".format(amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = primaryColor
            )
        }
    }
}
