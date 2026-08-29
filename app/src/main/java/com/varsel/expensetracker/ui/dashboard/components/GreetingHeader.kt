package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.varsel.expensetracker.ui.design.AppDimensions
import java.time.LocalDateTime

@Composable
fun GreetingHeader(
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val greeting = when (LocalDateTime.now().hour) {

        in 5..11 -> "Good Morning"

        in 12..16 -> "Good Afternoon"

        in 17..21 -> "Good Evening"

        else -> "Get some sleep, money can wait!"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimensions.ScreenPadding,
                vertical = AppDimensions.MediumSpacing
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Here's your financial overview",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

