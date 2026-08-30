package com.varsel.expensetracker.ui.mapper

import androidx.compose.ui.graphics.Color
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.model.CategoryUiModel
import javax.inject.Inject

class CategoryUiMapper @Inject constructor() {

    fun map(category: String): CategoryUiModel {
        val color = CategoryPalette.colorFor(category)
        val icon = CategoryIconCatalog.iconFor(category)

        return CategoryUiModel(
            name = category.ifBlank { "Uncategorized" },
            icon = icon,
            backgroundColor = color.copy(alpha = 0.14f),
            textColor = color
        )
    }
}
