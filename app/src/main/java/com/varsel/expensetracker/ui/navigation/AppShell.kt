package com.varsel.expensetracker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@Composable
fun AppShell(

    currentDestination: AppDestination,

    showBottomBar: Boolean,

    onDestinationSelected: (AppDestination) -> Unit,

    content: @Composable (
        PaddingValues
    ) -> Unit
) {

Scaffold(

    bottomBar = {

        if (showBottomBar) {

            BottomNavigationBar(

                currentDestination = currentDestination,

                onDestinationSelected =
                    onDestinationSelected

            )
        }
    }

) { padding ->

        content(padding)
    }
}
