package com.varsel.expensetracker.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NewCategoryCard(

    modifier: Modifier = Modifier,

    onClick: () -> Unit = {}

) {

    Card(

        modifier = modifier
            .aspectRatio(1.25f)
            .clickable(onClick = onClick),

        colors = CardDefaults.cardColors(

            containerColor = MaterialTheme.colorScheme.surface

        ),

        border = BorderStroke(

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

            Icon(

                imageVector = Icons.Outlined.Add,

                contentDescription = "New Category"

            )

            Text(

                text = "New",

                style = MaterialTheme.typography.bodySmall,

                textAlign = TextAlign.Center

            )

        }

    }

}
