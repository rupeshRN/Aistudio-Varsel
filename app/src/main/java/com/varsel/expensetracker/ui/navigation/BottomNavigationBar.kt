package com.varsel.expensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(
    currentDestination: AppDestination,
    destinations: List<AppDestination> = AppDestination.bottomBarItems,
    showLabels: Boolean = true,
    isFloating: Boolean = false,
    onDestinationSelected: (AppDestination) -> Unit
) {
    if (isFloating) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .shadow(10.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp)
        ) {
            NavigationBar(
                tonalElevation = 0.dp
            ) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination.route == destination.route,
                        onClick = { onDestinationSelected(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = if (showLabels) {
                            { Text(destination.title) }
                        } else null,
                        alwaysShowLabel = showLabels
                    )
                }
            }
        }
    } else {
        NavigationBar {
            destinations.forEach { destination ->
                NavigationBarItem(
                    selected = currentDestination.route == destination.route,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title
                        )
                    },
                    label = if (showLabels) {
                        { Text(destination.title) }
                    } else null,
                    alwaysShowLabel = showLabels
                )
            }
        }
    }
}
