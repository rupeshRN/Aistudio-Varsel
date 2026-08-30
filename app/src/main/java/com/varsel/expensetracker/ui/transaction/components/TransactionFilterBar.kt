package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.varsel.expensetracker.ui.transaction.TransactionFilter

@Composable
fun TransactionFilterBar(

    filters: Iterable<TransactionFilter>,

    selectedFilter: TransactionFilter,

    onFilterSelected: (TransactionFilter) -> Unit

) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            ),

        horizontalArrangement = Arrangement.spacedBy(8.dp)

    ) {

        filters.forEach { filter ->

            FilterChip(

                selected = filter == selectedFilter,

                onClick = {

                    onFilterSelected(filter)

                },

                label = {

                    Text(

                        text = filter.name,

                        style = MaterialTheme.typography.labelLarge

                    )

                },

                colors = FilterChipDefaults.filterChipColors()

            )

        }

    }

}
