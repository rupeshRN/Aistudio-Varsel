package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.theme.isDark

@Composable
fun DescriptionSection(
    description: String,
    onDescriptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isManual: Boolean = false,
    transactionType: TransactionType = TransactionType.EXPENSE
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val isExpense = transactionType == TransactionType.EXPENSE || transactionType == TransactionType.DEBIT
    val themeColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isManual) {
            Text(
                text = "DESCRIPTION / NOTE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        } else {
            Text(
                text = "Description / Merchant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_transaction_description_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            leadingIcon = if (isManual) {
                {
                    Icon(
                        Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            trailingIcon = if (description.isNotEmpty()) {
                {
                    IconButton(onClick = { onDescriptionChanged("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null,
            singleLine = true,
            placeholder = {
                Text(
                    if (isManual) {
                        if (isExpense) "e.g. Grocery shopping, Starbucks coffee, Uber ride"
                        else "e.g. Monthly salary, Freelance design, Dividend"
                    } else {
                        "Enter transaction narration or merchant name"
                    }
                )
            }
        )

        // Quick tags for manual entries
        if (isManual) {
            val quickTags = if (isExpense) {
                listOf("Groceries", "Food & Dining", "Uber / Cab", "Coffee", "Shopping", "Medical", "Bills", "Fuel")
            } else {
                listOf("Salary", "Freelance", "Bonus", "Dividend", "Gift", "Cashback", "Investment Return")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickTags.forEach { tag ->
                    val isCurrent = description.equals(tag, ignoreCase = true)
                    Surface(
                        onClick = { onDescriptionChanged(tag) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCurrent) {
                            themeColor.copy(alpha = 0.16f)
                        } else {
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) themeColor
                            else (if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0))
                        )
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isCurrent) themeColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
