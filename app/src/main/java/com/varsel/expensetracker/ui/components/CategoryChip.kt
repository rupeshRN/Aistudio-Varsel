package com.varsel.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.mapper.CategoryUiMapper

@Composable
fun CategoryChip(
    category: String,
    modifier: Modifier = Modifier
) {
    val uiCategory = remember(category) {
        CategoryUiMapper().map(category)
    }

    Row(
        modifier = modifier
            .background(
                color = uiCategory.backgroundColor,
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = uiCategory.icon,
            contentDescription = uiCategory.name,
            tint = uiCategory.textColor,
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = uiCategory.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = uiCategory.textColor
        )
    }
}
