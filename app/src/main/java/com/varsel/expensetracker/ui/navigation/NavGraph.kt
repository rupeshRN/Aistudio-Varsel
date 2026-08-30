package com.varsel.expensetracker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import com.varsel.expensetracker.ui.category.CategoryScreen
import com.varsel.expensetracker.ui.dashboard.DashboardScreen
import com.varsel.expensetracker.ui.developer.DeveloperSettingsScreen
import com.varsel.expensetracker.ui.financialevent.FinancialEventScreen
import com.varsel.expensetracker.ui.import_statement.ImportScreen
import com.varsel.expensetracker.ui.loan.LoansScreen
import com.varsel.expensetracker.ui.loan.add_edit.AddEditLoanScreen
import com.varsel.expensetracker.ui.loan.detail.LoanDetailScreen
import com.varsel.expensetracker.ui.more.MoreScreen
import com.varsel.expensetracker.ui.more.SettingsDetailScreen
import com.varsel.expensetracker.ui.reports.ReportsScreen
import com.varsel.expensetracker.ui.settings.SettingsScreen
import com.varsel.expensetracker.ui.transaction.TransactionDetailScreen
import com.varsel.expensetracker.ui.transaction.TransactionScreen

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
                },
                onNavigateToImport = {
                    navController.navigate("import_statement")
                },
                onNavigateToAnalytics = {
                    navController.navigate(AppDestination.Reports.route)
                },
                onNavigateToTransactionDetail = { transactionId ->
                    navController.navigate("transaction_detail/$transactionId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToLoans = {
                    navController.navigate("loans")
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
                onLoansClick = {
                    navController.navigate("loans")
                },
                onImportClick = {
                    navController.navigate("import_statement")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {

            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
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

        composable("loans") {

            LoansScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onAddLoanClick = {
                    navController.navigate("add_edit_loan?loanId=0")
                },
                onLoanClick = { loanId ->
                    navController.navigate("loan_detail/$loanId")
                }
            )
        }

        composable(
            route = "loan_detail/{loanId}",
            arguments = listOf(
                navArgument("loanId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L

            LoanDetailScreen(
                loanId = loanId,
                onBackClick = {
                    navController.popBackStack()
                },
                onEditLoanClick = { editId ->
                    navController.navigate("add_edit_loan?loanId=$editId")
                }
            )
        }

        composable(
            route = "add_edit_loan?loanId={loanId}",
            arguments = listOf(
                navArgument("loanId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: 0L

            AddEditLoanScreen(
                loanId = loanId,
                onBackClick = {
                    navController.popBackStack()
                },
                onLoanSaved = {
                    navController.popBackStack()
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
