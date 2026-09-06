package com.varsel.expensetracker.ui.budget.components

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.budget.model.BudgetTrendPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun BudgetSplineChart(
    trendPoints: List<BudgetTrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    animated: Boolean = true
) {
    if (trendPoints.isEmpty()) return

    val totalPoints = trendPoints.size
    val minSpan = 2f
    val maxSpan = totalPoints.toFloat().coerceAtLeast(minSpan)
    val defaultSpan = 4f.coerceIn(minSpan, maxSpan)

    // visibleSpan: number of months displayed across the chart width
    // - Zoom out / spreading fingers: adds months
    // - Zoom in / pinching fingers: reduces months
    var visibleSpan by remember(totalPoints) {
        mutableFloatStateOf(defaultSpan)
    }

    // windowOffset: start index in trendPoints (0 .. totalPoints - visibleSpan)
    // - Swiping horizontally shifts the window while keeping the exact span
    var windowOffset by remember(totalPoints) {
        mutableFloatStateOf((totalPoints - defaultSpan).coerceAtLeast(0f))
    }

    // Interactive point selection
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val animProgress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    // Dynamic maxAmount for visible slice with smooth transition
    val clampedStart = windowOffset.toInt().coerceIn(0, totalPoints - 1)
    val clampedEnd = (windowOffset + visibleSpan).toInt().coerceIn(0, totalPoints - 1)
    val visibleSlice = trendPoints.subList(clampedStart, (clampedEnd + 1).coerceAtMost(totalPoints))
    val currentMaxAmount = (visibleSlice.maxOfOrNull { it.amount } ?: 100.0).coerceAtLeast(100.0)

    val animatedMaxAmount by animateFloatAsState(
        targetValue = currentMaxAmount.toFloat(),
        animationSpec = tween(300),
        label = "animatedMaxAmount"
    )

    val yStep = (animatedMaxAmount / 4.0).coerceAtLeast(25.0)
    val yMax = yStep * 4.0

    val density = LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val selectedLabelColor = lineColor.toArgb()

    val textPaint = remember(density, labelColor) {
        Paint().apply {
            color = labelColor
            textSize = with(density) { 10.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    val selectedTextPaint = remember(density, selectedLabelColor) {
        Paint().apply {
            color = selectedLabelColor
            textSize = with(density) { 11.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    val selectedPointColor = MaterialTheme.colorScheme.secondary
    val tooltipSurfaceColor = MaterialTheme.colorScheme.surfaceVariant

    val startMonthName = trendPoints.getOrNull(clampedStart)?.monthLabel ?: ""
    val endMonthName = trendPoints.getOrNull(clampedEnd)?.monthLabel ?: ""
    val spanMonthsCount = visibleSpan.roundToInt().coerceIn(2, totalPoints)

    Column(modifier = modifier.fillMaxWidth()) {
        // Top Toolbar: Range info, Quick span presets & Month shifting chevrons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Span & Date range badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$startMonthName – $endMonthName",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(${spanMonthsCount}M)",
                        style = MaterialTheme.typography.labelSmall,
                        color = lineColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Right: Navigation arrows & Quick span buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Step back one month (earlier)
                IconButton(
                    onClick = {
                        windowOffset = (windowOffset - 1f).coerceIn(0f, (totalPoints - visibleSpan).coerceAtLeast(0f))
                    },
                    enabled = windowOffset > 0.05f,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Earlier Month",
                        modifier = Modifier.size(18.dp),
                        tint = if (windowOffset > 0.05f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Step forward one month (later)
                IconButton(
                    onClick = {
                        val maxOffset = (totalPoints - visibleSpan).coerceAtLeast(0f)
                        windowOffset = (windowOffset + 1f).coerceIn(0f, maxOffset)
                    },
                    enabled = windowOffset < (totalPoints - visibleSpan - 0.05f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Later Month",
                        modifier = Modifier.size(18.dp),
                        tint = if (windowOffset < (totalPoints - visibleSpan - 0.05f)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Quick Span Presets: 3M, 6M, 1Y
                listOf(3 to "3M", 6 to "6M", 12 to "1Y").forEach { (months, label) ->
                    val isSelected = spanMonthsCount == months
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) lineColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val targetSpan = months.toFloat().coerceIn(minSpan, maxSpan)
                                val maxOffset = (totalPoints - targetSpan).coerceAtLeast(0f)
                                visibleSpan = targetSpan
                                windowOffset = maxOffset // snap to latest for the chosen span
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Reset Button
                IconButton(
                    onClick = {
                        visibleSpan = defaultSpan
                        windowOffset = (totalPoints - defaultSpan).coerceAtLeast(0f)
                        selectedIndex = null
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset view",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Main Interactive Chart Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                // Gesture 1: Tap to select point, double tap to reset
                .pointerInput(totalPoints) {
                    detectTapGestures(
                        onDoubleTap = {
                            visibleSpan = defaultSpan
                            windowOffset = (totalPoints - defaultSpan).coerceAtLeast(0f)
                            selectedIndex = null
                        },
                        onTap = { offset ->
                            val width = size.width
                            val leftPadding = 50.dp.toPx()
                            val chartWidth = width - leftPadding - 16.dp.toPx()
                            if (chartWidth > 0 && offset.x >= leftPadding) {
                                val fraction = ((offset.x - leftPadding) / chartWidth).coerceIn(0f, 1f)
                                val targetIndex = (windowOffset + fraction * (visibleSpan - 1f)).roundToInt().coerceIn(0, totalPoints - 1)
                                selectedIndex = if (selectedIndex == targetIndex) null else targetIndex
                            }
                        }
                    )
                }
                // Gesture 2: Horizontal swipe to move months in exact span & pinch zoom to add/remove months
                .pointerInput(totalPoints) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val width = size.width
                        val leftPadding = 50.dp.toPx()
                        val chartWidth = width - leftPadding - 16.dp.toPx()

                        // 1. Pinch Zoom:
                        // - zoom > 1 (spreading fingers / pan out) -> increase span (add months)
                        // - zoom < 1 (pinching fingers / pan in) -> decrease span (reduce months)
                        if (zoom != 1f && chartWidth > 0) {
                            val newSpan = (visibleSpan * zoom).coerceIn(minSpan, maxSpan)
                            val centroidFraction = ((centroid.x - leftPadding) / chartWidth).coerceIn(0f, 1f)
                            val oldCenterMonth = windowOffset + centroidFraction * (visibleSpan - 1f)
                            val newOffset = (oldCenterMonth - centroidFraction * (newSpan - 1f)).coerceIn(
                                0f,
                                (totalPoints - newSpan).coerceAtLeast(0f)
                            )
                            visibleSpan = newSpan
                            windowOffset = newOffset
                        }

                        // 2. Horizontal Pan / Swipe: moves month in the exact span
                        if (pan.x != 0f && chartWidth > 0) {
                            val monthStepPixels = chartWidth / (visibleSpan - 1f).coerceAtLeast(1f)
                            val deltaMonths = -pan.x / monthStepPixels
                            val maxOffset = (totalPoints - visibleSpan).coerceAtLeast(0f)
                            windowOffset = (windowOffset + deltaMonths).coerceIn(0f, maxOffset)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val leftPadding = 50.dp.toPx()
                val bottomPadding = 28.dp.toPx()
                val topPadding = 16.dp.toPx()
                val chartWidth = width - leftPadding - 16.dp.toPx()
                val chartHeight = height - bottomPadding - topPadding
                val progress = animProgress.value

                // 1. Draw horizontal grid lines across chart width
                for (i in 0..4) {
                    val y = topPadding + chartHeight - (i.toFloat() / 4f * chartHeight)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (totalPoints >= 2 && chartWidth > 0) {
                    // Compute (x, y) coordinates for ALL points in dataset
                    val points = trendPoints.mapIndexed { index, point ->
                        val x = leftPadding + ((index - windowOffset) / (visibleSpan - 1f).coerceAtLeast(1f)) * chartWidth
                        val targetYRatio = (point.amount / yMax).toFloat().coerceIn(0f, 1f)
                        val animatedYRatio = targetYRatio * progress
                        val y = topPadding + chartHeight - (animatedYRatio * chartHeight)
                        Offset(x, y)
                    }

                    // Clip the spline, gradient, and data points strictly to the chart viewport
                    clipRect(
                        left = leftPadding,
                        top = 0f,
                        right = width,
                        bottom = topPadding + chartHeight + 6.dp.toPx()
                    ) {
                        // Build smooth cubic Bézier curve connecting all data points
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

                        // Draw spline line stroke
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Selected point vertical guideline
                        selectedIndex?.let { idx ->
                            if (idx in points.indices) {
                                val sp = points[idx]
                                if (sp.x in (leftPadding - 5f)..(width + 5f)) {
                                    drawLine(
                                        color = lineColor.copy(alpha = 0.65f),
                                        start = Offset(sp.x, topPadding),
                                        end = Offset(sp.x, topPadding + chartHeight),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }
                            }
                        }

                        // Draw data points for visible points
                        points.forEachIndexed { idx, point ->
                            // Only draw circles if near or within the visible chart area
                            if (point.x in (leftPadding - 10f)..(width + 10f)) {
                                val isSelected = selectedIndex == idx
                                val pointRadius = (if (isSelected) 7.dp.toPx() else 4.5.dp.toPx()) * progress
                                if (pointRadius > 0.5f) {
                                    // White center
                                    drawCircle(
                                        color = Color.White,
                                        radius = pointRadius,
                                        center = point
                                    )
                                    // Ring
                                    drawCircle(
                                        color = if (isSelected) selectedPointColor else lineColor,
                                        radius = pointRadius,
                                        center = point,
                                        style = Stroke(width = (if (isSelected) 3.dp.toPx() else 2.dp.toPx()) * progress)
                                    )
                                }
                            }
                        }
                    }

                    // Draw month labels along X-axis at each point's X position with smooth edge fading
                    val labelY = height - 6.dp.toPx()
                    points.forEachIndexed { idx, pt ->
                        if (pt.x in (leftPadding - 24.dp.toPx())..(width + 24.dp.toPx())) {
                            val isSelected = selectedIndex == idx
                            // Edge fade alpha for smooth transitions when swiping
                            val leftDist = (pt.x - leftPadding).coerceAtLeast(0f)
                            val rightDist = (width - pt.x).coerceAtLeast(0f)
                            val fadeMargin = 20.dp.toPx()
                            val fadeAlpha = ((leftDist / fadeMargin).coerceIn(0f, 1f) * (rightDist / fadeMargin).coerceIn(0f, 1f))

                            val paintToUse = if (isSelected) selectedTextPaint else textPaint
                            val baseAlpha = if (isSelected) 255 else 180
                            paintToUse.alpha = (baseAlpha * fadeAlpha).roundToInt()

                            val labelText = trendPoints[idx].monthLabel
                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                pt.x,
                                labelY,
                                paintToUse
                            )
                        }
                    }
                }
            }

            // Left Y-Axis text labels overlay (always anchored on left)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 28.dp, top = 8.dp),
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

            // Interactive Tooltip on touch / selection
            selectedIndex?.let { idx ->
                if (idx in trendPoints.indices) {
                    val selectedPoint = trendPoints[idx]
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = tooltipSurfaceColor,
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            lineColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedPoint.monthLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BudgetCalculator.formatCurrency(selectedPoint.amount, round = true),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = lineColor
                            )
                        }
                    }
                }
            }
        }
    }
}
