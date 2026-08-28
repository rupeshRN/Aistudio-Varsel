package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransactionInfoSection(

    amount: String,

    date: String,

    type: String

) {

    Card(

        colors = CardDefaults.cardColors(

            containerColor =
                MaterialTheme.colorScheme.surfaceVariant

        )

    ) {

        Column(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp)

        ) {

            InfoRow(

                title = "Amount",

                value = amount

            )

            InfoRow(

                title = "Date",

                value = date

            )

            InfoRow(

                title = "Type",

                value = type

            )

        }

    }

}

@Composable
private fun InfoRow(

    title: String,

    value: String

) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween

    ) {

        Text(

            text = title,

            style = MaterialTheme.typography.bodyMedium

        )

        Text(

            text = value,

            style = MaterialTheme.typography.bodyMedium

        )

    }

}
