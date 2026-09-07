package com.varsel.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.varsel.expensetracker.category.CategoryIconCatalog
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.preference.AppearanceConfig
import com.varsel.expensetracker.data.preference.AppearanceRepository
import com.varsel.expensetracker.data.preference.GeneralConfig
import com.varsel.expensetracker.data.preference.GeneralPreferencesRepository
import com.varsel.expensetracker.security.BiometricAuthManager
import com.varsel.expensetracker.security.BiometricLockOverlay
import com.varsel.expensetracker.ui.navigation.AppDestination
import com.varsel.expensetracker.ui.navigation.AppShell
import com.varsel.expensetracker.ui.navigation.NavGraph
import com.varsel.expensetracker.ui.theme.VarselExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var categoryDao: CategoryDao

    @Inject
    lateinit var appearanceRepository: AppearanceRepository

    @Inject
    lateinit var generalPreferencesRepository: GeneralPreferencesRepository

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    private var currentTimeout = com.varsel.expensetracker.data.preference.BiometricTimeout.OFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read fast synchronous preference on launch
        currentTimeout = generalPreferencesRepository.getBiometricTimeoutSync()
        if (currentTimeout == com.varsel.expensetracker.data.preference.BiometricTimeout.OFF) {
            biometricAuthManager.unlockManually()
        } else {
            biometricAuthManager.lockOnAppLaunch(currentTimeout)
        }

        lifecycleScope.launch {
            categoryDao.getAllCategories().collect { categories ->
                CategoryIconCatalog.updateCategories(categories)
            }
        }

        lifecycleScope.launch {
            generalPreferencesRepository.generalConfig.collect { config ->
                currentTimeout = config.biometricTimeout
                if (config.biometricTimeout == com.varsel.expensetracker.data.preference.BiometricTimeout.OFF) {
                    biometricAuthManager.unlockManually()
                }
            }
        }

        enableEdgeToEdge()

        setContent {
            val appearanceConfig by appearanceRepository.appearanceConfig.collectAsState(
                initial = AppearanceConfig()
            )
            val generalConfig by generalPreferencesRepository.generalConfig.collectAsState(
                initial = GeneralConfig()
            )
            val isLocked by biometricAuthManager.isLocked.collectAsState()
            var authErrorMessage by remember { mutableStateOf<String?>(null) }

            // Auto-trigger biometric / credentials prompt when app is locked
            LaunchedEffect(isLocked) {
                if (isLocked && currentTimeout != com.varsel.expensetracker.data.preference.BiometricTimeout.OFF) {
                    authErrorMessage = null
                    biometricAuthManager.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { authErrorMessage = null },
                        onError = { msg -> authErrorMessage = msg }
                    )
                }
            }

            VarselExpenseTrackerTheme(
                themeMode = appearanceConfig.themeMode,
                dynamicColor = appearanceConfig.dynamicColor,
                accentScheme = appearanceConfig.accentScheme,
                amoledDark = appearanceConfig.amoledDark
            ) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                // Map 4 configured tab routes to AppDestinations
                val currentNavDestinations = remember(generalConfig.navigationTabs) {
                    generalConfig.navigationTabs.take(4).map { route ->
                        AppDestination.fromRoute(route)
                    }
                }

                val showBottomBar = currentNavDestinations.any { it.route == currentRoute }

                val currentDestination = currentNavDestinations.firstOrNull {
                    it.route == currentRoute
                } ?: AppDestination.fromRoute(currentRoute)

                Box(modifier = Modifier.fillMaxSize()) {
                    AppShell(
                        currentDestination = currentDestination,
                        showBottomBar = showBottomBar,
                        destinations = currentNavDestinations,
                        showNavLabels = generalConfig.showNavLabels,
                        isFloatingNavBar = generalConfig.floatingNavBar,
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

                    // Biometric Lock overlay
                    BiometricLockOverlay(
                        isLocked = isLocked,
                        errorMessage = authErrorMessage,
                        onUnlockRequest = {
                            authErrorMessage = null
                            biometricAuthManager.authenticate(
                                activity = this@MainActivity,
                                onSuccess = { authErrorMessage = null },
                                onError = { msg -> authErrorMessage = msg }
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        biometricAuthManager.checkLockOnResume(currentTimeout)
    }

    override fun onPause() {
        super.onPause()
        biometricAuthManager.onAppBackgrounded(currentTimeout)
    }

    override fun onDestroy() {
        super.onDestroy()
        biometricAuthManager.onAppClosed(currentTimeout)
    }
}
