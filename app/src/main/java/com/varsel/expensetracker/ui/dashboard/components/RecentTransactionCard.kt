package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.varsel.expensetracker.ui.components.CategoryChip
import com.varsel.expensetracker.ui.dashboard.components.AmountText
import com.varsel.expensetracker.ui.design.AppDimensions
import com.varsel.expensetracker.ui.design.AppShapes
import com.varsel.expensetracker.ui.model.TransactionUiModel

@Composable
fun RecentTransactionCard(

    transaction: TransactionUiModel,

    modifier: Modifier = Modifier,

    onClick: (() -> Unit)? = null

) {

    Card(

        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },

        shape = AppShapes.Card,

        elevation = CardDefaults.cardElevation(
            defaultElevation = AppDimensions.CardElevation
        )

    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.CardPadding),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(

                    text = transaction.title,

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.SemiBold
                )

                transaction.subtitle?.let {

                    Spacer(
                        modifier = Modifier.height(
                            AppDimensions.ExtraSmallSpacing
                        )
                    )

                    Text(

                        text = it,

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(
                    modifier = Modifier.height(
                        AppDimensions.SmallSpacing
                    )
                )

                CategoryChip(

                    category = transaction.category
                )

                Spacer(
                    modifier = Modifier.height(
                        AppDimensions.SmallSpacing
                    )
                )

                Text(

                    text = transaction.dateText,

                    style = MaterialTheme.typography.labelMedium,

                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.width(
                    AppDimensions.MediumSpacing
                )
            )

            AmountText(

                amount = transaction.amountText,

                isIncome = transaction.isIncome
            )
        }
    }
}
