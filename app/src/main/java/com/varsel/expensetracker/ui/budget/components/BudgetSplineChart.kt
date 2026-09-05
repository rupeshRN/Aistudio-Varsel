package com.varsel.expensetracker.ui.budget.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.budget.model.BudgetTrendPoint

@Composable
fun BudgetSplineChart(
    trendPoints: List<BudgetTrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    animated: Boolean = true
) {
    if (trendPoints.isEmpty()) return

    val animProgress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    val maxAmount = (trendPoints.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(100.0)
    // Round maxAmount to neat interval
    val yStep = (maxAmount / 4.0).coerceAtLeast(20.0)
    val yMax = yStep * 4.0

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val leftPadding = 56.dp.toPx()
                val bottomPadding = 24.dp.toPx()
                val topPadding = 12.dp.toPx()
                val chartWidth = width - leftPadding - 16.dp.toPx()
                val chartHeight = height - bottomPadding - topPadding
                val progress = animProgress.value

                // Draw Y-axis grid lines
                for (i in 0..4) {
                    val y = topPadding + chartHeight - (i.toFloat() / 4f * chartHeight)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (trendPoints.size >= 2) {
                    val stepX = chartWidth / (trendPoints.size - 1)
                    val points = trendPoints.mapIndexed { index, point ->
                        val x = leftPadding + (index * stepX)
                        val targetYRatio = (point.amount / yMax).toFloat().coerceIn(0f, 1f)
                        val animatedYRatio = targetYRatio * progress
                        val y = topPadding + chartHeight - (animatedYRatio * chartHeight)
                        Offset(x, y)
                    }

                    // Cubic spline path
                    val path = Path()
                    path.moveTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX1 = p0.x + (p1.x - p0.x) / 2
                        val controlY1 = p0.y
                        val controlX2 = p0.x + (p1.x - p0.x) / 2
                        val controlY2 = p1.y

                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    }

                    // Fill gradient under spline
                    val fillPath = Path()
                    fillPath.addPath(path)
                    fillPath.lineTo(points.last().x, topPadding + chartHeight)
                    fillPath.lineTo(points.first().x, topPadding + chartHeight)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.35f * progress),
                                lineColor.copy(alpha = 0.03f * progress)
                            ),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )

                    // Draw the spline stroke
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw data points
                    points.forEach { point ->
                        val pointRadius = 4.5.dp.toPx() * progress
                        if (pointRadius > 0.5f) {
                            // White inner circle
                            drawCircle(
                                color = Color.White,
                                radius = pointRadius,
                                center = point
                            )
                            // Line color outer ring
                            drawCircle(
                                color = lineColor,
                                radius = pointRadius,
                                center = point,
                                style = Stroke(width = 2.dp.toPx() * progress)
                            )
                        }
                    }
                }
            }

            // Overlay Y axis labels on the left
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 24.dp, top = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 4 downTo 0) {
                    val value = yStep * i
                    val formatted = when {
                        value >= 1000 -> "₹${(value / 1000).toInt()}K"
                        else -> "₹${value.toInt()}"
                    }
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Month labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trendPoints.forEach { point ->
                Text(
                    text = point.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
