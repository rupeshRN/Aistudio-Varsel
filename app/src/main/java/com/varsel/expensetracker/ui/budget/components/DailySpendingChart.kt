package com.varsel.expensetracker.ui.budget.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.budget.model.DailySpendingPoint

@Composable
fun DailySpendingChart(
    dailyPoints: List<DailySpendingPoint>,
    targetDailyAllowance: Double,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
) {
    val maxSpent = (dailyPoints.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(targetDailyAllowance * 1.5).coerceAtLeast(50.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daily Spending",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Target: ${BudgetCalculator.formatCurrency(targetDailyAllowance, round = true)}/day",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val overTargetColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val bottomPadding = 24.dp.toPx()
                val chartHeight = height - bottomPadding

                // Target allowance dashed horizontal line
                if (targetDailyAllowance > 0) {
                    val targetY = chartHeight - ((targetDailyAllowance / maxSpent).toFloat() * chartHeight).coerceIn(0f, chartHeight)
                    drawLine(
                        color = Color(0xFFFFA000), // Amber
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Baseline line
                drawLine(
                    color = gridColor,
                    start = Offset(0f, chartHeight),
                    end = Offset(width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                if (dailyPoints.isNotEmpty()) {
                    val stepX = width / dailyPoints.size.coerceAtLeast(1)
                    val barWidth = (stepX * 0.55f).coerceIn(4.dp.toPx(), 22.dp.toPx())

                    dailyPoints.forEachIndexed { index, point ->
                        val centerX = index * stepX + (stepX / 2)
                        val barHeight = if (maxSpent > 0 && point.amount > 0) {
                            ((point.amount / maxSpent).toFloat() * (chartHeight - 10f)).coerceAtLeast(4.dp.toPx())
                        } else {
                            0f
                        }

                        if (barHeight > 0) {
                            drawRoundRect(
                                color = if (point.amount > targetDailyAllowance) overTargetColor else barColor,
                                topLeft = Offset(centerX - (barWidth / 2), chartHeight - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        } else {
                            // Empty day indicator dot on baseline
                            drawCircle(
                                color = gridColor,
                                radius = 2.dp.toPx(),
                                center = Offset(centerX, chartHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}
