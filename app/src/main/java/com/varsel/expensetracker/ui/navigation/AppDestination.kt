package com.varsel.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(

    val route: String,
    val title: String,
    val icon: ImageVector

) {

    data object Home : AppDestination(
        "home",
        "Home",
        Icons.Outlined.Home
    )

    data object Transactions : AppDestination(
        "transactions",
        "Transactions",
        Icons.Outlined.ListAlt
    )

    data object Budgets : AppDestination(
        "budgets",
        "Budgets",
        Icons.Outlined.PieChart
    )

    data object TransactionDetail : AppDestination(
        "transaction_detail/{transactionId}",
        "Transaction Detail",
        Icons.Outlined.Description
    )

    data object Reports : AppDestination(
        "reports",
        "Reports",
        Icons.Outlined.Assessment
    )

    data object More : AppDestination(
        "more",
        "More",
        Icons.Outlined.MoreHoriz
    )

    companion object {

        val bottomBarItems = listOf(
            Home,
            Transactions,
            Reports,
            More
        )
    }
}
