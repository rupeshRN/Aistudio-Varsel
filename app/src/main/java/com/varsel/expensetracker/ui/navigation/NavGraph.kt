package com.varsel.expensetracker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.varsel.expensetracker.ui.category.CategoryScreen
import com.varsel.expensetracker.ui.dashboard.DashboardScreen
import com.varsel.expensetracker.ui.import_statement.ImportScreen
import com.varsel.expensetracker.ui.more.MoreScreen
import com.varsel.expensetracker.ui.transaction.TransactionScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import com.varsel.expensetracker.ui.more.SettingsDetailScreen
import com.varsel.expensetracker.ui.developer.DeveloperSettingsScreen
import com.varsel.expensetracker.ui.transaction.TransactionDetailScreen
import com.varsel.expensetracker.ui.financialevent.FinancialEventScreen
import com.varsel.expensetracker.ui.reports.ReportsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {

    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = Modifier.padding(innerPadding)
    ) {

        composable(AppDestination.Home.route) {

            DashboardScreen(
                viewModel = hiltViewModel(),
                onNavigateToAllTransactions = {
                    navController.navigate(AppDestination.Transactions.route)
                }
            )
        }

        composable(AppDestination.Transactions.route) {

TransactionScreen(
    viewModel = hiltViewModel(),

    onBackClick = {
        navController.popBackStack()
    },

    onTransactionClick = { transactionId ->

        navController.navigate(
            "transaction_detail/$transactionId"
        )

    }
)
        }

composable(
    route = AppDestination.TransactionDetail.route
) { backStackEntry ->

    val transactionId =

        backStackEntry
            .arguments
            ?.getString("transactionId")
            ?.toLongOrNull()
            ?: return@composable

TransactionDetailScreen(

    transactionId = transactionId,

    viewModel = hiltViewModel(),

    onBackClick = {

        navController.popBackStack()},

        onFinancialEventClick = { transactionLinkId ->

            navController.navigate(
                "financial_event/$transactionLinkId"
            )
        }

)

}

composable(
    route = "financial_event/{transactionLinkId}"
) { backStackEntry ->

    val transactionLinkId =
        backStackEntry
            .arguments
            ?.getString("transactionLinkId")
            ?: return@composable

    FinancialEventScreen(

        transactionLinkId =
            transactionLinkId,

        onBackClick = {
            navController.popBackStack()
        }
    )
}

composable(AppDestination.Reports.route) {

    ReportsScreen(
        viewModel = hiltViewModel(),

        onTransactionClick = { transactionId ->
            navController.navigate(
                "transaction_detail/$transactionId"
            )
        },

        onFinancialEventClick = { transactionLinkId ->

            navController.navigate(
                "financial_event/$transactionLinkId"
            )
        }
    )
}

        composable(AppDestination.More.route) {

            MoreScreen(

                onImportClick = {
                    navController.navigate("import_statement")
                },

                onCategoriesClick = {
                    navController.navigate("categories")
                },

                onLearningRulesClick = {
    navController.navigate("learning_rules")
},

onAppearanceClick = {
    navController.navigate("appearance")
},

onDeveloperClick = {
    navController.navigate("developer")
},

onAboutClick = {
    navController.navigate("about")
}
            )
        }

        composable("categories") {

            CategoryScreen(
                viewModel = hiltViewModel(),
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("import_statement") {

            ImportScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("learning_rules") {

    SettingsDetailScreen(

        title = "Learning Rules",

        description = "View and manage the rules that help Varsel automatically categorize your transactions.",

        icon = Icons.Outlined.AutoAwesome
    )
}

composable("appearance") {

    SettingsDetailScreen(

        title = "Appearance",

        description = "Customize themes, colors and other display preferences.",

        icon = Icons.Outlined.Palette
    )
}

composable("developer") {

    DeveloperSettingsScreen(

        onBackClick = {
            navController.popBackStack()
        }

    )
}

composable("about") {

    SettingsDetailScreen(

        title = "About Varsel",

        description = "Varsel is a smart personal finance application designed to automatically import, understand and organize your financial statements.",

        icon = Icons.Outlined.Info
    )
}
        
    }
}
