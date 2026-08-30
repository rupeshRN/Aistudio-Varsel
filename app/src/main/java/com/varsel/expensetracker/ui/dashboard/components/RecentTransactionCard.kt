package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.ui.design.CategoryPalette
import com.varsel.expensetracker.ui.model.TransactionUiModel

@Composable
fun RecentTransactionCard(
    transaction: TransactionUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
    val expenseColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)

    val categoryColor = CategoryPalette.colorFor(transaction.category)
    val categoryIcon = CategoryIconCatalog.iconFor(transaction.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category Icon Avatar with standardized vector icon and semantic palette tint
        Surface(
            shape = CircleShape,
            color = categoryColor.copy(alpha = 0.14f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Description & Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = transaction.dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Amount Display with semantic green / red colors
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (transaction.isIncome) "+${transaction.amountText}" else "-${transaction.amountText}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) incomeColor else expenseColor
            )
        }
    }
}
