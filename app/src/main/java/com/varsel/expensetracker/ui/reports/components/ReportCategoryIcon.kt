package com.varsel.expensetracker.ui.reports.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.ui.design.CategoryPalette

@Composable
fun ReportCategoryIcon(
    category: String,
    modifier: Modifier = Modifier
) {
    val icon = remember(category) {
        CategoryIconCatalog.iconFor(category)
    }
    val categoryColor = remember(category) {
        CategoryPalette.colorFor(category)
    }

    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = categoryColor.copy(alpha = 0.15f)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category,
                modifier = Modifier.size(20.dp),
                tint = categoryColor
            )
        }
    }
}
