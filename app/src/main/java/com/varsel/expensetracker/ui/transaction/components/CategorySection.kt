package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.category.CategoryUi
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.design.CategoryPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySection(
    selectedCategory: String,
    transactionType: TransactionType = TransactionType.EXPENSE,
    availableCategories: List<String> = emptyList(),
    onCategorySelected: (String) -> Unit,
    onNewCategoryClick: () -> Unit
) {
    val isIncome = transactionType == TransactionType.INCOME || transactionType == TransactionType.CREDIT
    var isExpanded by remember { mutableStateOf(true) }

    val displayCategories = remember(transactionType, availableCategories, selectedCategory) {
        val dynamicCategoryUis = availableCategories.map { name ->
            CategoryUi(id = name, isIncome = isIncome)
        }
        val staticCategories = CategoryMetadata.categoriesFor(transactionType).map {
            CategoryUi(id = it.id, isIncome = isIncome)
        }

        val combined = (dynamicCategoryUis + staticCategories).distinctBy { it.id.lowercase() }.toMutableList()

        if (selectedCategory.isNotBlank() && combined.none { it.id.equals(selectedCategory, ignoreCase = true) }) {
            combined.add(CategoryUi(selectedCategory, isIncome = isIncome))
        }
        combined
    }

    val selectedIcon = remember(selectedCategory) {
        CategoryIconCatalog.iconFor(selectedCategory)
    }
    val selectedColor = remember(selectedCategory) {
        CategoryPalette.colorFor(selectedCategory)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collapsible Header - Standardized title without extraneous leading icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isIncome) "Income Category" else "Expense Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedCategory.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = selectedIcon,
                            contentDescription = null,
                            tint = selectedColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = selectedColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse Categories" else "Expand Categories",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        // Compact Category Pills with Vector Icons and Semantic Palette
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayCategories.forEach { category ->
                    val isSelected = category.id.equals(selectedCategory, ignoreCase = true)
                    val catColor = category.color
                    val catIcon = category.icon

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onCategorySelected(category.id) },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected)
                            catColor.copy(alpha = 0.18f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = if (isSelected)
                            BorderStroke(1.5.dp, catColor)
                        else
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = category.id,
                                tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = category.id,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected)
                                    catColor
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Add Category Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onNewCategoryClick() },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


