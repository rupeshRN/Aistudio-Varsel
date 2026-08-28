package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
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

    saveEnabled: Boolean = true

) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        OutlinedButton(

            modifier = Modifier.weight(1f),

            onClick = onDeleteClick

        ) {

            Icon(

                imageVector = Icons.Outlined.Delete,

                contentDescription = null

            )

Text("Delete")

        }

        Button(

            modifier = Modifier.weight(1f),

            enabled = saveEnabled,

            onClick = onSaveClick

        ) {

            Icon(

                imageVector = Icons.Outlined.Save,

                contentDescription = null

            )

Text("Save Changes")

        }

    }

}
