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
    availableCategories: List<String> = emptyList(),
    onCategorySelected: (String) -> Unit,
    onNewCategoryClick: () -> Unit
) {
    val isIncome = transactionType == TransactionType.INCOME || transactionType == TransactionType.CREDIT

    Text(
        text = if (isIncome) "Income Category" else "Expense Category",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val displayCategories = remember(transactionType, availableCategories, selectedCategory) {
        val staticCategories = CategoryMetadata.categoriesFor(transactionType)
        val dynamicCategoryUis = availableCategories.map { name ->
            val emoji = CategoryMetadata.emojiForCategory(name, isIncome)
            CategoryUi(id = name, icon = emoji, isIncome = isIncome)
        }

        val combined = (staticCategories + dynamicCategoryUis).distinctBy { it.id.lowercase() }.toMutableList()

        if (selectedCategory.isNotBlank() && combined.none { it.id.equals(selectedCategory, ignoreCase = true) }) {
            combined.add(CategoryUi(selectedCategory, CategoryMetadata.emojiForCategory(selectedCategory, isIncome), isIncome = isIncome))
        }
        combined
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

                repeat(3 - row.size) {
                    NewCategoryCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNewCategoryClick
                    )
                }
            }
        }

        // If categories evenly divide by 3, provide a dedicated row with NewCategoryCard
        if (displayCategories.size % 3 == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NewCategoryCard(
                    modifier = Modifier.weight(1f),
                    onClick = onNewCategoryClick
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

