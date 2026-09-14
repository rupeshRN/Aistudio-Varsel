package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.ui.dashboard.components.RecentTransactionCard
import com.varsel.expensetracker.ui.model.TransactionUiModel
import com.varsel.expensetracker.ui.theme.isDark

fun LazyListScope.transactionList(
    transactions: List<TransactionUiModel>,
    onTransactionClick: (TransactionUiModel) -> Unit = {},
    onResetFilter: (() -> Unit)? = null
) {
    if (transactions.isEmpty()) {
        item(key = "empty_transactions") {
            TransactionEmptyState(onResetFilter = onResetFilter)
        }
    } else {
        // Group transactions by date for unified, scannable ledger hierarchy
        val groupedByDate = transactions.groupBy { it.dateText }

        groupedByDate.forEach { (dateText, itemsForDate) ->
            item(key = "header_$dateText") {
                DateGroupHeader(
                    dateText = dateText,
                    itemsCount = itemsForDate.size
                )
            }

            item(key = "group_$dateText") {
                DateGroupCard(
                    transactions = itemsForDate,
                    onTransactionClick = onTransactionClick
                )
            }
        }
    }
}

@Composable
private fun DateGroupHeader(
    dateText: String,
    itemsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = if (itemsCount == 1) "1 item" else "$itemsCount items",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DateGroupCard(
    transactions: List<TransactionUiModel>,
    onTransactionClick: (TransactionUiModel) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val cardBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.45f)
    } else {
        Color(0xFFFFFFFF)
    }

    val cardBorder = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.45f)
    } else {
        Color(0xFFE2E8F0)
    }

    val dividerColor = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.35f)
    } else {
        Color(0xFFF1F5F9)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            transactions.forEachIndexed { index, transaction ->
                RecentTransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )

                if (index < transactions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.6.dp,
                        color = dividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionEmptyState(
    onResetFilter: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val cardBg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = 0.45f)
    } else {
        Color(0xFFFFFFFF)
    }

    val cardBorder = if (isDark) {
        Color(0xFF334155).copy(alpha = 0.45f)
    } else {
        Color(0xFFE2E8F0)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Transactions Found",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Try adjusting your search query, selecting another month, or resetting filters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (onResetFilter != null) {
                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onResetFilter,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterAltOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Reset All Filters",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

