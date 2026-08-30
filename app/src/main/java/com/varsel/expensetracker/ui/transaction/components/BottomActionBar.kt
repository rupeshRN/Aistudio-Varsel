package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionBar(
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveEnabled: Boolean = true,
    isImported: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
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
            Text(if (isImported) "Locked" else "Delete")
        }

        Button(
            modifier = Modifier.weight(1.3f),
            shape = RoundedCornerShape(14.dp),
            enabled = saveEnabled,
            onClick = onSaveClick
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Changes")
        }
    }
}

