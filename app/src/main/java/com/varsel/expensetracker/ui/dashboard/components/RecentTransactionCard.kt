package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val transferColor = if (isDark) Color(0xFFB39DDB) else Color(0xFF5E35B1)
    val eventColor = if (isDark) Color(0xFF80DEEA) else Color(0xFF00838F)

    // Dynamic icon & tint based on Transfer, Event Linked, or Category
    val (icon: ImageVector, iconColor: Color, iconDescription: String) = when {
        transaction.isTransfer -> {
            Triple(Icons.Outlined.SwapHoriz, transferColor, "Transfer")
        }
        transaction.isEventLinked -> {
            Triple(Icons.Outlined.Event, eventColor, "Linked to Event")
        }
        else -> {
            Triple(
                CategoryIconCatalog.iconFor(transaction.category),
                CategoryPalette.colorFor(transaction.category),
                transaction.category
            )
        }
    }

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
        // Icon Avatar (Differentiated for Transfer, Event-Linked, or Category)
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.14f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Description & Account + Date
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
                // Bank short name + masked number format instead of category name
                val accountDisplay = transaction.accountInfoText ?: transaction.category
                Text(
                    text = accountDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
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

        // Amount Display with semantic green / red / neutral colors
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            val amountColor = when {
                transaction.isTransfer -> transferColor
                transaction.isIncome -> incomeColor
                else -> expenseColor
            }
            val prefix = when {
                transaction.isIncome -> "+"
                else -> "-"
            }
            Text(
                text = "$prefix${transaction.amountText}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

