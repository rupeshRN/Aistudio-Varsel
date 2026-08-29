package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance smooth sparkline canvas with cubic Bézier curve,
 * glowing gradient fill, and end-point indicator.
 */
@Composable
fun SparklineCanvas(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    fillGradient: Boolean = true,
    strokeWidth: Dp = 2.5.dp,
    showDots: Boolean = true,
    highlightLastPoint: Boolean = true
) {
    if (points.isEmpty()) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0) return@Canvas

        val topPadding = 6.dp.toPx()
        val bottomPadding = 6.dp.toPx()
        val usableHeight = (height - topPadding - bottomPadding).coerceAtLeast(1f)

        // Map points to canvas coordinates
        val coords = if (points.size == 1) {
            listOf(Offset(width / 2f, topPadding + usableHeight * (1f - points[0])))
        } else {
            val stepX = width / (points.size - 1).toFloat()
            points.mapIndexed { index, normValue ->
                val x = index * stepX
                val y = topPadding + usableHeight * (1f - normValue.coerceIn(0f, 1f))
                Offset(x, y)
            }
        }

        if (coords.size == 1) {
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = coords[0]
            )
            return@Canvas
        }

        // Build smooth cubic bezier curve
        val strokePath = Path().apply {
            moveTo(coords[0].x, coords[0].y)
            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }
        }

        // Draw gradient area underneath if requested
        if (fillGradient) {
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(coords.last().x, height)
                lineTo(coords.first().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.22f),
                        lineColor.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = height
                ),
                style = Fill
            )
        }

        // Draw the smooth stroke
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw point dots
        if (showDots) {
            coords.forEachIndexed { index, offset ->
                val isLast = index == coords.size - 1
                val radius = if (isLast && highlightLastPoint) 3.5.dp.toPx() else 2.5.dp.toPx()

                // Outer halo for active/last dot
                if (isLast && highlightLastPoint) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.35f),
                        radius = 6.dp.toPx(),
                        center = offset
                    )
                }

                drawCircle(
                    color = if (isLast && highlightLastPoint) lineColor else lineColor.copy(alpha = 0.7f),
                    radius = radius,
                    center = offset
                )
            }
        }
    }
}
