package com.varsel.expensetracker.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BottomNavigationBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {

    NavigationBar {

        AppDestination.bottomBarItems.forEach { destination ->

            NavigationBarItem(

                selected =
                    currentDestination.route ==
                            destination.route,

                onClick = {
                    onDestinationSelected(destination)
                },

                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.title
                    )
                },

                label = {
                    Text(destination.title)
                }
            )
        }
    }
}
