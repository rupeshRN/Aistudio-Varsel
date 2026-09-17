package com.varsel.expensetracker.ui.reports.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.reports.ReportsExpenseCategory
import com.varsel.expensetracker.ui.theme.isDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenseCategoryList(
    categories: List<ReportsExpenseCategory>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Text(
            text = "No expenses for this period.",
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val isDark = MaterialTheme.colorScheme.isDark
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val total = categories.sumOf { it.totalAmount }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val percentage = if (total > 0.0) {
                (category.totalAmount / total) * 100.0
            } else 0.0

            val isSelected = selectedCategory == category.category
            val itemColor = CategoryPalette.colorFor(category.category)

            val unselectedBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.45f) else Color(0xFFF8FAFC)
            val unselectedBorder = if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)

            val targetBg = if (isSelected) itemColor.copy(alpha = 0.14f) else unselectedBg
            val targetBorder = if (isSelected) itemColor.copy(alpha = 0.45f) else unselectedBorder

            val backgroundColor by animateColorAsState(
                targetValue = targetBg,
                animationSpec = tween(180),
                label = "cat_item_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = targetBorder,
                animationSpec = tween(180),
                label = "cat_item_border"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onCategorySelected(if (isSelected) null else category.category)
                    },
                shape = RoundedCornerShape(16.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReportCategoryIcon(category = category.category)

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = category.category,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isSelected) itemColor else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Proportional progress track
                        val progressFraction = if (total > 0.0) {
                            (category.totalAmount / total).toFloat().coerceIn(0.01f, 1f)
                        } else 0.01f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(itemColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = formatter.format(category.totalAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = if (isSelected) itemColor else MaterialTheme.colorScheme.onSurface
                        )

                        // Percentage badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) itemColor.copy(alpha = 0.18f) else if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "%.1f%%".format(Locale.ENGLISH, percentage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) itemColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
