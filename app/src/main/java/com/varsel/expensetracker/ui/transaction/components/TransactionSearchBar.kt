package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TransactionSearchBar(

    query: String,

    onQueryChange: (String) -> Unit,

    modifier: Modifier = Modifier

) {

    OutlinedTextField(

        value = query,

        onValueChange = onQueryChange,

        modifier = modifier.fillMaxWidth(),

        leadingIcon = {

            Icon(

                imageVector = Icons.Default.Search,

                contentDescription = "Search"

            )

        },

        placeholder = {

            Text("Search transactions")

        },

        singleLine = true

    )

}
