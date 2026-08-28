package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryMetadata

@Composable
fun CategorySection(

    selectedCategory: String,

    onCategorySelected: (String) -> Unit

) {

    Text(

        text = "Category",

        style = MaterialTheme.typography.titleMedium,

        modifier = Modifier.padding(bottom = 8.dp)

    )

    val categories = CategoryMetadata.all

    Column(

        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {

        categories.chunked(3).forEach { row ->

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                row.forEach { category ->

                    CategoryCard(

                        modifier = Modifier.weight(1f),

                        category = category,

                        selected =

                            category.id == selectedCategory,

                        onClick = {

                            onCategorySelected(

                                category.id

                            )

                        }

                    )

                }

                repeat(3 - row.size) {

                    NewCategoryCard(

                        modifier = Modifier.weight(1f)

                    )

                }

            }

        }

    }

}
