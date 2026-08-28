package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import com.varsel.expensetracker.ui.design.AppDimensions
import com.varsel.expensetracker.ui.model.TransactionUiModel

@Composable
fun DashboardRecentSection(

    transactions: List<TransactionUiModel>,

    onViewAll: () -> Unit,

    onTransactionClick: (TransactionUiModel) -> Unit

) {

    Column(

        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.ScreenPadding)

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text(

                text = "Recent Transactions",

                style = MaterialTheme.typography.titleLarge

            )

            TextButton(

                onClick = onViewAll

            ) {

                Text("View All")

            }
        }

        Spacer(

            modifier = Modifier.height(
                AppDimensions.SmallSpacing
            )

        )

        transactions.forEach {

            RecentTransactionCard(

                transaction = it,

                modifier = Modifier.padding(
                    bottom = AppDimensions.SmallSpacing
                ),

                onClick = {

                    onTransactionClick(it)

                }
            )
        }
    }
}
