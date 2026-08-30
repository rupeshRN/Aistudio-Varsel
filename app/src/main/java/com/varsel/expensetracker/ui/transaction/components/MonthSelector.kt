package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppDimensions
import com.varsel.expensetracker.ui.transaction.TransactionMonth

@Composable
fun MonthSelector(

    months: List<TransactionMonth>,

    selectedMonth: TransactionMonth?,

    onMonthSelected: (TransactionMonth) -> Unit,

    modifier: Modifier = Modifier

) {

    Row(

        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppDimensions.ScreenPadding),

        horizontalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        months.forEach { month ->

            FilterChip(

                selected = month == selectedMonth,

                onClick = {

                    onMonthSelected(month)

                },

                label = {

                    Text(month.displayName)

                },

                colors = FilterChipDefaults.filterChipColors(

                    selectedContainerColor =
                        MaterialTheme.colorScheme.primary,

                    selectedLabelColor =
                        MaterialTheme.colorScheme.onPrimary

                )

            )

        }

    }

}
