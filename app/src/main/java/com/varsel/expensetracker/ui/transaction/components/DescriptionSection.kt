package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DescriptionSection(

    description: String,

    onDescriptionChanged: (String) -> Unit

) {

    Column {

        Text(

            text = "Description",

            style = MaterialTheme.typography.titleMedium,

            modifier = Modifier.padding(bottom = 8.dp)

        )

        OutlinedTextField(

            value = description,

            onValueChange = onDescriptionChanged,

            modifier = Modifier.fillMaxWidth(),

            singleLine = false,

            minLines = 2

        )

    }

}
