package com.varsel.expensetracker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable

@Composable
fun AppShell(
    currentDestination: AppDestination,
    showBottomBar: Boolean,
    destinations: List<AppDestination> = AppDestination.bottomBarItems,
    showNavLabels: Boolean = true,
    isFloatingNavBar: Boolean = false,
    onDestinationSelected: (AppDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentDestination = currentDestination,
                    destinations = destinations,
                    showLabels = showNavLabels,
                    isFloating = isFloatingNavBar,
                    onDestinationSelected = onDestinationSelected
                )
            }
        }
    ) { padding ->
        content(padding)
    }
}
