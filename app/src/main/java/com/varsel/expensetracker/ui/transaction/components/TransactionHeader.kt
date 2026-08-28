package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.varsel.expensetracker.ui.design.AppDimensions

@Composable
fun TransactionHeader(

    transactionCount: Int,

    modifier: Modifier = Modifier

) {

    Column(

        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimensions.ScreenPadding
            )

    ) {

        Text(

            text = "Transactions",

            style = MaterialTheme.typography.headlineMedium,

            color = MaterialTheme.colorScheme.onBackground

        )

        Text(

            text = "$transactionCount transactions",

            style = MaterialTheme.typography.bodyMedium,

            color = MaterialTheme.colorScheme.onSurfaceVariant

        )
    }
}
