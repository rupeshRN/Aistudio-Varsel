package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.model.BalanceSummaryUiModel
import com.varsel.expensetracker.ui.theme.isDark
import kotlin.math.abs

@Composable
fun BalanceCard(
    summary: BalanceSummaryUiModel,
    isBalanceHidden: Boolean = false,
    showBreakdown: Boolean = true,
    onToggleVisibility: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark

    // Elevated Modern Fintech Card Canvas: Deep obsidian/slate with subtle ambient glow
    val cardBackground = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                Color(0xFF0F172A),
                Color(0xFF020617)
            ),
            center = Offset(200f, 0f),
            radius = 900f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC)
            ),
            center = Offset(200f, 0f),
            radius = 800f
        )
    }

    val cardBorderColor = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.55f)
    } else {
        Color(0xFFE2E8F0)
    }

    val netSavings = summary.savings
    val isPositiveSavings = netSavings >= 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = if (isDark) 0.dp else 6.dp,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
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
                // Top Header Row: Label, Net Delta Badge & Privacy Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL LIQUID ASSETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )

                    // Tactile Privacy Toggle Button
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.7f) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
                        )
                    ) {
                        IconButton(
                            onClick = onToggleVisibility,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (isBalanceHidden) "Show balance" else "Hide balance",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Main Balance Display with Mini Trajectory Sparkline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isBalanceHidden) "₹ ••••••••" else "₹%,.2f".format(summary.totalBalance),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                lineHeight = 42.sp,
                                letterSpacing = (-1.2).sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                    }

                    // Mini Sparkline preview representing balance trajectory
                    if (!isBalanceHidden) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(40.dp)
                                .padding(bottom = 4.dp)
                        ) {
                            FintechSparkline(
                                isPositive = isPositiveSavings,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (showBreakdown) {
                    HorizontalDivider(
                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                        thickness = 1.dp
                    )

                    // Cash flow section header: aligned with Period and Contextual Cash Flow Summary Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${summary.periodLabel.uppercase()} CASH FLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )

                        // Contextual Cash Flow Summary Pill (Percentage only, safe from format exceptions)
                        if (!isBalanceHidden && summary.totalIncome > 0) {
                            val isPositive = netSavings >= 0
                            val pillBg = if (isPositive) {
                                if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                            } else {
                                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEE2E2)
                            }
                            val pillTextColor = if (isPositive) {
                                if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                            } else {
                                if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                            }
                            val pillText = if (isPositive) {
                                val savingsRate = ((netSavings / summary.totalIncome) * 100).coerceIn(0.0, 100.0).toInt()
                                "$savingsRate% Saved"
                            } else {
                                val deficitRate = (((summary.totalExpense - summary.totalIncome) / summary.totalIncome) * 100).coerceIn(0.0, 999.0).toInt()
                                "$deficitRate% Deficit"
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = pillText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = pillTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                                )
                            }
                        }
                    }

                    // Bento Grid: Income & Expense Twin Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FintechCashflowTile(
                            modifier = Modifier.weight(1f),
                            title = "Income",
                            amount = summary.totalIncome,
                            isIncome = true,
                            isBalanceHidden = isBalanceHidden,
                            changePercent = summary.incomeChangePercent
                        )

                        FintechCashflowTile(
                            modifier = Modifier.weight(1f),
                            title = "Expenses",
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
}

@Composable
private fun FintechSparkline(
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val strokeColor = if (isPositive) {
        if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    } else {
        if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            strokeColor.copy(alpha = 0.35f),
            strokeColor.copy(alpha = 0.0f)
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Simulated smooth spline curve
        val points = if (isPositive) {
            listOf(
                Offset(0f, height * 0.85f),
                Offset(width * 0.2f, height * 0.70f),
                Offset(width * 0.4f, height * 0.75f),
                Offset(width * 0.65f, height * 0.40f),
                Offset(width * 0.85f, height * 0.45f),
                Offset(width, height * 0.15f)
            )
        } else {
            listOf(
                Offset(0f, height * 0.20f),
                Offset(width * 0.2f, height * 0.35f),
                Offset(width * 0.45f, height * 0.30f),
                Offset(width * 0.7f, height * 0.65f),
                Offset(width * 0.85f, height * 0.55f),
                Offset(width, height * 0.85f)
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }
        }

        // Fill path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(fillPath, brush = gradientBrush)
        drawPath(path, color = strokeColor, style = Stroke(width = 2.dp.toPx()))

        // Draw last point pulse dot
        drawCircle(
            color = strokeColor,
            radius = 3.dp.toPx(),
            center = points.last()
        )
    }
}

@Composable
private fun FintechCashflowTile(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    isIncome: Boolean,
    isBalanceHidden: Boolean,
    changePercent: Double?
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val accentColor = if (isIncome) {
        if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
    } else {
        if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    }

    val tileBg = if (isDark) {
        if (isIncome) Color(0xFF064E3B).copy(alpha = 0.18f) else Color(0xFF7F1D1D).copy(alpha = 0.16f)
    } else {
        if (isIncome) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
    }

    val tileBorder = if (isDark) {
        if (isIncome) Color(0xFF059669).copy(alpha = 0.35f) else Color(0xFFDC2626).copy(alpha = 0.30f)
    } else {
        if (isIncome) Color(0xFFBBF7D0) else Color(0xFFFECACA)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = tileBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, tileBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = if (isDark) 0.25f else 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    )
                }

                changePercent?.let { pct ->
                    val arrow = if (pct > 0) "↑" else "↓"
                    Text(
                        text = "$arrow${abs(pct).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = accentColor
                    )
                }
            }

            Text(
                text = if (isBalanceHidden) "₹ ••••" else "₹%,.2f".format(amount),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = accentColor
            )
        }
    }
}

