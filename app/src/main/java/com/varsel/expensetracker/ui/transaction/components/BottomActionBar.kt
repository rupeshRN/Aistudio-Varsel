package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BottomActionBar(
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveEnabled: Boolean = true,
    isImported: Boolean = false,
    amount: Double? = null,
    isExpense: Boolean = true
) {
    val themeColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            modifier = Modifier
                .weight(0.9f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            onClick = onDeleteClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isImported) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.error
                }
            ),
            border = BorderStroke(
                1.dp,
                if (isImported) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                }
            )
        ) {
            Icon(
                imageVector = if (isImported) Icons.Outlined.Lock else Icons.Outlined.Delete,
                contentDescription = if (isImported) "Locked" else "Delete",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isImported) "Locked" else "Delete",
                fontWeight = FontWeight.SemiBold
            )
        }

        val buttonText = if (!isImported && amount != null && amount > 0.0) {
            val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
                maximumFractionDigits = 2
            }
            "Save ₹${formatter.format(amount)}"
        } else {
            "Save Changes"
        }

        Button(
            modifier = Modifier
                .weight(1.4f)
                .height(52.dp)
                .testTag("save_manual_transaction_button"),
            shape = RoundedCornerShape(16.dp),
            enabled = saveEnabled,
            onClick = onSaveClick,
            colors = if (!isImported) {
                ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = Color.White,
                    disabledContainerColor = themeColor.copy(alpha = 0.38f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
