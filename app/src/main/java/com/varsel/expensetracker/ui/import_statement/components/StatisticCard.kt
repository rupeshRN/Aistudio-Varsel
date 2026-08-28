package com.varsel.expensetracker.ui.import_statement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Small reusable statistics card.
 *
 * Used by:
 * • Statement Summary
 * • Dashboard (future)
 * • Analytics (future)
 */
@Composable
fun StatisticCard(

    title: String,

    value: String,

    modifier: Modifier = Modifier

) {

    Card(

        modifier = modifier,

        elevation = CardDefaults.cardElevation(

            defaultElevation = 4.dp

        ),

        shape = MaterialTheme.shapes.large

    ) {

        Column(

            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center

        ) {

            Text(

                text = title,

                style = MaterialTheme.typography.labelMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant,

                textAlign = TextAlign.Center

            )

            Text(

                text = value,

                style = MaterialTheme.typography.headlineMedium,

                fontWeight = FontWeight.Bold,

                textAlign = TextAlign.Center,

                modifier = Modifier.padding(top = 8.dp)

            )

        }

    }

}
