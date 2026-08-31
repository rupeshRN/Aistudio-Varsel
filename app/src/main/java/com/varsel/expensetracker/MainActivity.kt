package com.varsel.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.preference.AppearanceConfig
import com.varsel.expensetracker.data.preference.AppearanceRepository
import com.varsel.expensetracker.ui.navigation.AppDestination
import com.varsel.expensetracker.ui.navigation.AppShell
import com.varsel.expensetracker.ui.navigation.NavGraph
import com.varsel.expensetracker.ui.theme.VarselExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var categoryDao: CategoryDao

    @Inject
    lateinit var appearanceRepository: AppearanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            categoryDao.getAllCategories().collect { categories ->
                CategoryIconCatalog.updateCategories(categories)
            }
        }

        enableEdgeToEdge()

        setContent {
            val appearanceConfig by appearanceRepository.appearanceConfig.collectAsState(
                initial = AppearanceConfig()
            )

            VarselExpenseTrackerTheme(
                themeMode = appearanceConfig.themeMode,
                dynamicColor = appearanceConfig.dynamicColor,
                accentScheme = appearanceConfig.accentScheme,
                amoledDark = appearanceConfig.amoledDark
            ) {

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
