package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.reports.ReportsIncomeCategory
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun IncomeCategoryChart(
    categories: List<ReportsIncomeCategory>,
    selectedCategory: String?,
    onCategoryClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Text(
            text =
                "No income data for this period.",

            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 20.dp
                    ),

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        return
    }

    val validCategories =
        categories.filter {
            it.totalAmount > 0.0
        }

    val total =
        validCategories.sumOf {
            max(
                it.totalAmount,
                0.0
            )
        }

    if (total <= 0.0) {
        return
    }

    val formatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    val selectedModel =
        selectedCategory?.let { selected ->
            validCategories.firstOrNull {
                it.category == selected
            }
        }

    val centerLabel =
        selectedModel?.category
            ?: "Overall"

    val centerAmount =
        selectedModel?.totalAmount
            ?: total

    Column(
        modifier =
            modifier.fillMaxWidth(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp),

            contentAlignment =
                Alignment.Center
        ) {

            Canvas(
                modifier =
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .pointerInput(validCategories, total) {
                            if (onCategoryClick != null) {
                                detectTapGestures { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    val dist = sqrt(dx * dx + dy * dy)

                                    val defaultStrokeWidth = 32.dp.toPx()
                                    val selectedStrokeWidth = 42.dp.toPx()
                                    val diameter = minOf(size.width, size.height) - selectedStrokeWidth
                                    val outerRadius = diameter / 2f + selectedStrokeWidth / 2f
                                    val innerRadius = diameter / 2f - selectedStrokeWidth / 2f

                                    if (dist in innerRadius..outerRadius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
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

                val defaultStrokeWidth =
                    32.dp.toPx()

                val selectedStrokeWidth =
                    42.dp.toPx()

                val diameter =
                    minOf(
                        size.width,
                        size.height
                    ) -
                        selectedStrokeWidth

                val left =
                    (
                        size.width -
                            diameter
                        ) / 2f

                val top =
                    (
                        size.height -
                            diameter
                        ) / 2f

                var startAngle =
                    -90f

                validCategories.forEach { category ->

                    val sweep =
                        (
                            category.totalAmount /
                                total
                            ).toFloat() *
                            360f

                    val isSelected =
                        selectedCategory ==
                            category.category

                    val hasSelection =
                        selectedCategory != null

                    val alpha =
                        if (
                            hasSelection &&
                            !isSelected
                        ) {
                            0.28f
                        } else {
                            1f
                        }

                    drawArc(
                        color =
                            incomeCategoryColor(
                                category.category
                            ).copy(
                                alpha = alpha
                            ),

                        startAngle =
                            startAngle,

                        sweepAngle =
                            sweep,

                        useCenter =
                            false,

                        topLeft =
                            androidx.compose.ui.geometry
                                .Offset(
                                    left,
                                    top
                                ),

                        size =
                            androidx.compose.ui.geometry
                                .Size(
                                    diameter,
                                    diameter
                                ),

                        style =
                            Stroke(
                                width =
                                    if (isSelected) {
                                        selectedStrokeWidth
                                    } else {
                                        defaultStrokeWidth
                                    },

                                cap =
                                    StrokeCap.Butt
                            )
                    )

                    startAngle += sweep
                }
            }

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        centerLabel,

                    style =
                        MaterialTheme.typography
                            .labelMedium,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        formatter.format(
                            centerAmount
                        ),

                    style =
                        MaterialTheme.typography
                            .titleLarge
                )
            }
        }
    }
}

private fun incomeCategoryColor(
    category: String
): androidx.compose.ui.graphics.Color = CategoryPalette.colorFor(category)
