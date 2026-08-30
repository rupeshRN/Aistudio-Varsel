package com.varsel.expensetracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.ui.design.CategoryPalette

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

    val displayedCategory =
        selectedCategory.ifBlank {
            "Select category"
        }

    val selectedColor = remember(selectedCategory) {
        if (selectedCategory.isNotBlank()) CategoryPalette.colorFor(selectedCategory) else null
    }

    val selectedIcon = remember(selectedCategory) {
        if (selectedCategory.isNotBlank()) CategoryIconCatalog.iconFor(selectedCategory) else null
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = displayedCategory,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(label)
            },
            leadingIcon = if (selectedIcon != null && selectedColor != null) {
                {
                    Icon(
                        imageVector = selectedIcon,
                        contentDescription = selectedCategory,
                        tint = selectedColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            categories.forEach { category ->
                val icon = CategoryIconCatalog.iconFor(category.iconName.ifBlank { category.name })
                val color = CategoryPalette.colorFor(category.name)

                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = category.name,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onCategorySelected(category.name)
                        expanded = false
                    }
                )
            }

            if (
                selectedCategory.isNotBlank() &&
                categories.none {
                    it.name == selectedCategory
                }
            ) {
                val icon = CategoryIconCatalog.iconFor(selectedCategory)
                val color = CategoryPalette.colorFor(selectedCategory)

                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = selectedCategory,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "$selectedCategory (current)"
                        )
                    },
                    onClick = {
                        onCategorySelected(selectedCategory)
                        expanded = false
                    }
                )
            }
        }
    }
}
