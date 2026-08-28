package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.category.CategoryUi

@Composable
fun CategoryCard(

    modifier: Modifier = Modifier,

    category: CategoryUi,

    selected: Boolean,

    onClick: () -> Unit

) {

    Card(

        modifier = modifier
            .aspectRatio(1.25f)
            .clickable(onClick = onClick),

        colors = CardDefaults.cardColors(

            containerColor =

                if (selected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface

        ),

        border =

            if (selected)

                BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                )

            else

                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )

    ) {

        Column(

            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center

        ) {

            Text(

                text = category.icon,

                style = MaterialTheme.typography.headlineSmall

            )

            Text(

                text = category.id,

                style = MaterialTheme.typography.bodySmall,

                textAlign = TextAlign.Center,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis

            )

        }

    }

}
