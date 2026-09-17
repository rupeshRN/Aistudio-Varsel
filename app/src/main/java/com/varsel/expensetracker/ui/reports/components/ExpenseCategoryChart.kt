package com.varsel.expensetracker.ui.reports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.reports.ReportsExpenseCategory
import com.varsel.expensetracker.ui.theme.isDark
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun ExpenseCategoryChart(
    categories: List<ReportsExpenseCategory>,
    selectedCategory: String?,
    onCategoryClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Text(
            text = "No expense data for this period.",
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val isDark = MaterialTheme.colorScheme.isDark

    val validCategories = categories.filter {
        it.totalAmount > 0.0
    }

    val total = validCategories.sumOf {
        max(it.totalAmount, 0.0)
    }

    if (total <= 0.0) {
        return
    }

    // Animated entrance sweep
    val animSweep = remember { Animatable(0f) }
    LaunchedEffect(validCategories, total) {
        animSweep.snapTo(0f)
        animSweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val selectedModel = selectedCategory?.let { selected ->
        validCategories.firstOrNull { it.category == selected }
    }

    val centerLabel = selectedModel?.category ?: "Total Expenses"
    val centerAmount = selectedModel?.totalAmount ?: total
    val selectedPercentage = if (total > 0.0 && selectedModel != null) {
        (selectedModel.totalAmount / total) * 100.0
    } else null

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .height(220.dp)
                    .fillMaxWidth()
                    .pointerInput(validCategories, total) {
                        if (onCategoryClick != null) {
                            detectTapGestures { offset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val dist = sqrt(dx * dx + dy * dy)

                                val defaultStrokeWidth = 30.dp.toPx()
                                val selectedStrokeWidth = 40.dp.toPx()
                                val diameter = minOf(size.width, size.height) - selectedStrokeWidth
                                val outerRadius = diameter / 2f + selectedStrokeWidth / 2f
                                val innerRadius = diameter / 2f - selectedStrokeWidth / 2f

                                if (dist in innerRadius..outerRadius) {
                                    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    var normalizedAngle = (angle + 90f)
                                    if (normalizedAngle < 0f) {
                                        normalizedAngle += 360f
                                    }

                                    var currentAngle = 0f
                                    for (cat in validCategories) {
                                        val sweep = (cat.totalAmount / total).toFloat() * 360f
                                        if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                            onCategoryClick(cat.category)
                                            break
                                        }
                                        currentAngle += sweep
                                    }
                                }
                            }
                        }
                    }
            ) {
                val defaultStrokeWidth = 30.dp.toPx()
                val selectedStrokeWidth = 40.dp.toPx()

                val diameter = minOf(size.width, size.height) - selectedStrokeWidth
                val left = (size.width - diameter) / 2f
                val top = (size.height - diameter) / 2f

                var startAngle = -90f
                val currentProgress = animSweep.value
                val hasMultiple = validCategories.size > 1

                validCategories.forEach { category ->
                    val fullSweep = (category.totalAmount / total).toFloat() * 360f
                    val sweep = fullSweep * currentProgress

                    val isSelected = selectedCategory == category.category
                    val hasSelection = selectedCategory != null

                    val alpha = if (hasSelection && !isSelected) {
                        0.25f
                    } else {
                        1f
                    }

                    val sliceGap = if (hasMultiple && currentProgress > 0.5f) 2.5f else 0f
                    val effectiveSweep = max(0.1f, sweep - sliceGap)

                    drawArc(
                        color = categoryColor(category.category).copy(alpha = alpha),
                        startAngle = startAngle + (sliceGap / 2f),
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = Size(diameter, diameter),
                        style = Stroke(
                            width = if (isSelected) selectedStrokeWidth else defaultStrokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )

                    startAngle += fullSweep
                }
            }

            // Central Elevated Interactive Hub
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF0F172A).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.95f),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.size(134.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (selectedModel != null) {
                            categoryColor(selectedModel.category)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )

                    Text(
                        text = formatter.format(centerAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    if (selectedPercentage != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = categoryColor(selectedModel!!.category).copy(alpha = 0.16f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "%.1f%% of total".format(Locale.ENGLISH, selectedPercentage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = categoryColor(selectedModel.category),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Tap slice to inspect",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Tactile Clear Selection Chip
        AnimatedVisibility(
            visible = selectedCategory != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCategoryClick?.invoke(selectedCategory!!) },
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Reset Category Filter",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun categoryColor(
    category: String
): Color = CategoryPalette.colorFor(category)
