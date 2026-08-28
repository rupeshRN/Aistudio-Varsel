package com.varsel.expensetracker.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.varsel.expensetracker.ui.design.AppDimensions
import java.time.LocalDateTime

@Composable
fun GreetingHeader(
    modifier: Modifier = Modifier
) {

    val greeting = when (LocalDateTime.now().hour) {

        in 5..11 -> "Good Morning"

        in 12..16 -> "Good Afternoon"

        in 17..21 -> "Good Evening"

        else -> "Get some sleep, money can wait!"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppDimensions.ScreenPadding,
                vertical = AppDimensions.MediumSpacing
            )
    ) {

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
}
