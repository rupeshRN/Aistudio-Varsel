package com.varsel.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.varsel.expensetracker.ui.navigation.AppDestination
import com.varsel.expensetracker.ui.navigation.AppShell
import com.varsel.expensetracker.ui.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            MaterialTheme {

                val navController = rememberNavController()

                val backStackEntry by navController.currentBackStackEntryAsState()

                val currentRoute =
                    backStackEntry?.destination?.route

                val showBottomBar =
                        AppDestination.bottomBarItems.any {
                    
                            it.route == currentRoute
                    
                        }

                val currentDestination =
                    AppDestination.bottomBarItems.firstOrNull {
                        it.route == currentRoute
                    } ?: AppDestination.Home

                AppShell(

                    currentDestination = currentDestination,

                    showBottomBar = showBottomBar,

                    onDestinationSelected = { destination ->
                        if (destination.route == AppDestination.Home.route) {
                            navController.popBackStack(AppDestination.Home.route, inclusive = false)
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(AppDestination.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                ) { padding ->

                    NavGraph(

                        navController = navController,

                        innerPadding = padding
                    )
                }
            }
        }
    }
}
