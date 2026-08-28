package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.category.CategoryUi
import com.varsel.expensetracker.domain.model.TransactionType

@Composable
fun CategorySection(
    selectedCategory: String,
    transactionType: TransactionType = TransactionType.EXPENSE,
    onCategorySelected: (String) -> Unit,
    onNewCategoryClick: (() -> Unit)? = null
) {
    val isIncome = transactionType == TransactionType.INCOME || transactionType == TransactionType.CREDIT

    Text(
        text = if (isIncome) "Income Category" else "Expense Category",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val baseCategories = remember(transactionType) {
        CategoryMetadata.categoriesFor(transactionType)
    }

    // Ensure currently selected category is visible even if custom or non-standard
    val displayCategories = remember(baseCategories, selectedCategory) {
        if (selectedCategory.isNotBlank() && baseCategories.none { it.id.equals(selectedCategory, ignoreCase = true) }) {
            baseCategories + CategoryUi(selectedCategory, "🏷️", isIncome = isIncome)
        } else {
            baseCategories
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayCategories.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    CategoryCard(
                        modifier = Modifier.weight(1f),
                        category = category,
                        selected = category.id.equals(selectedCategory, ignoreCase = true),
                        onClick = {
                            onCategorySelected(category.id)
                        }
                    )
                }

                repeat(3 - row.size) { index ->
                    if (index == 0 && onNewCategoryClick != null) {
                        NewCategoryCard(
                            modifier = Modifier.weight(1f),
                            onClick = onNewCategoryClick
                        )
                    } else {
                        NewCategoryCard(
                            modifier = Modifier.weight(1f),
                            onClick = onNewCategoryClick ?: {}
                        )
                    }
                }
            }
        }
    }
}

