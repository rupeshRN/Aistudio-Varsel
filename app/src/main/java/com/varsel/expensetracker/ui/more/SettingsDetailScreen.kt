package com.varsel.expensetracker.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDetailScreen(

    title: String,

    description: String,

    icon: ImageVector = Icons.Outlined.Construction

) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {

        Icon(

            imageVector = icon,

            contentDescription = null,

            tint = MaterialTheme.colorScheme.primary,

            modifier = Modifier.height(64.dp)

        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(

            text = title,

            style = MaterialTheme.typography.headlineSmall,

            fontWeight = FontWeight.Bold

        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(

            text = description,

            style = MaterialTheme.typography.bodyMedium,

            color = MaterialTheme.colorScheme.onSurfaceVariant

        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(

            text = "Coming in a future update.",

            style = MaterialTheme.typography.labelLarge,

            color = MaterialTheme.colorScheme.primary

        )
    }
}
