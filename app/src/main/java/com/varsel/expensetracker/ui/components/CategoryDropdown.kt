package com.varsel.expensetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.varsel.expensetracker.data.local.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    label: String = "Category"
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    /*
     * Normally selectedCategory will already exist in the
     * category list.
     *
     * If an old Financial Event contains a category that was
     * subsequently deleted, keep displaying that existing value
     * instead of silently changing it.
     */
    val displayedCategory =
        selectedCategory.ifBlank {
            "Select category"
        }

    ExposedDropdownMenuBox(

        expanded = expanded,

        onExpandedChange = {
            expanded = !expanded
        }

    ) {

        OutlinedTextField(

            value = displayedCategory,

            onValueChange = {
                // Intentionally disabled.
                // Category must be selected from the dropdown.
            },

            readOnly = true,

            label = {
                Text(label)
            },

            trailingIcon = {

                ExposedDropdownMenuDefaults
                    .TrailingIcon(
                        expanded = expanded
                    )
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(),

            colors =
                ExposedDropdownMenuDefaults
                    .outlinedTextFieldColors(),

            textStyle =
                MaterialTheme
                    .typography
                    .bodyLarge
        )

        ExposedDropdownMenu(

            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }

        ) {

            categories.forEach { category ->

                DropdownMenuItem(

                    text = {

                        Text(
                            text = category.name
                        )
                    },

                    onClick = {

                        onCategorySelected(
                            category.name
                        )

                        expanded = false
                    }
                )
            }

            /*
             * If the existing Financial Event contains a category
             * that no longer exists in the category table, don't
             * discard it from the UI.
             *
             * The user can select a current category to replace it.
             */
            if (
                selectedCategory.isNotBlank() &&
                categories.none {
                    it.name == selectedCategory
                }
            ) {

                DropdownMenuItem(

                    text = {

                        Text(
                            text =
                                "$selectedCategory (current)"
                        )
                    },

                    onClick = {

                        onCategorySelected(
                            selectedCategory
                        )

                        expanded = false
                    }
                )
            }
        }
    }
}
