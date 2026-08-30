package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppColors
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.reports.ReportsExpenseCategory
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
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 20.dp
                    ),
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        return
    }

    val formatter =
        NumberFormat.getCurrencyInstance(
            Locale("en", "IN")
        )

    val total =
        categories.sumOf {
            it.totalAmount
        }

    Column(
        modifier =
            modifier.fillMaxWidth(),

        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        categories.forEach { category ->

            val percentage =
                if (total > 0.0) {
                    (
                        category.totalAmount /
                            total
                        ) * 100.0
                } else {
                    0.0
                }

            val isSelected =
                selectedCategory ==
                    category.category

            val itemColor = CategoryPalette.colorFor(category.category)

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCategorySelected(
                                if (isSelected) {
                                    null
                                } else {
                                    category.category
                                }
                            )
                        },
                shape =
                    RoundedCornerShape(
                        14.dp
                    ),
                color =
                    if (isSelected) {
                        itemColor.copy(
                            alpha = 0.12f
                        )
                    } else {
                        MaterialTheme.colorScheme
                            .surface
                    }
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 10.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    ReportCategoryIcon(
                        category =
                            category.category
                    )

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Text(
                        text =
                            category.category,
                        modifier =
                            Modifier.weight(1f),
                        style =
                            MaterialTheme.typography
                                .bodyLarge,
                        fontWeight =
                            if (isSelected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                        color =
                            if (isSelected) {
                                itemColor
                            } else {
                                MaterialTheme.colorScheme
                                    .onSurface
                            }
                    )

                    Text(
                        text =
                            "${percentage.toInt()}%",
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )

                    Text(
                        text =
                            formatter.format(
                                category.totalAmount
                            ),
                        style =
                            MaterialTheme.typography
                                .bodyLarge,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            if (isSelected) {
                                itemColor
                            } else {
                                MaterialTheme.colorScheme
                                    .onSurface
                            }
                    )
                }
            }
        }
    }
}
