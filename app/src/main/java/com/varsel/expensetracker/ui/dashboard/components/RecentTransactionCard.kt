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

private data class TransactionIconStyle(
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
    val description: String
)

@Composable
fun RecentTransactionCard(
    transaction: TransactionUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val expenseColor = if (isDark) Color(0xFFFF5252) else Color(0xFFC62828)
    val transferColor = if (isDark) Color(0xFFD1C4E9) else Color(0xFF5E35B1)
    val eventColor = if (isDark) Color(0xFF80DEEA) else Color(0xFF00838F)

    // Dynamic icon, tint & background based on Transfer, Event Linked, or Category
    val iconStyle = when {
        transaction.isTransfer -> {
            val tint = if (isDark) Color(0xFFEDE7F6) else Color(0xFF5E35B1)
            val bg = if (isDark) Color(0xFF7E57C2).copy(alpha = 0.35f) else Color(0xFF5E35B1).copy(alpha = 0.16f)
            TransactionIconStyle(Icons.Outlined.SwapHoriz, tint, bg, "Transfer")
        }
        transaction.isEventLinked -> {
            TransactionIconStyle(Icons.Outlined.Event, eventColor, eventColor.copy(alpha = 0.14f), "Linked to Event")
        }
        else -> {
            val catColor = CategoryPalette.colorFor(transaction.category)
            TransactionIconStyle(
                CategoryIconCatalog.iconFor(transaction.category),
                catColor,
                catColor.copy(alpha = 0.14f),
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
            color = iconStyle.bg,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = iconStyle.icon,
                    contentDescription = iconStyle.description,
                    tint = iconStyle.tint,
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

