package com.varsel.expensetracker.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class IconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object CategoryIconCatalog {

    const val FOOD = "ic_restaurant"
    const val TRAVEL = "ic_car"
    const val SHOPPING = "ic_bag"
    const val GROCERIES = "ic_cart"
    const val FUEL = "ic_gas"
    const val MOBILE = "ic_phone"
    const val UTILITIES = "ic_lightning"
    const val HEALTHCARE = "ic_hospital"
    const val ENTERTAINMENT = "ic_movies"
    const val SALARY = "ic_salary"
    const val TRANSFER = "ic_swap"
    const val INVESTMENT = "ic_trending_up"
    const val EDUCATION = "ic_school"
    const val BILLS = "ic_receipt"
    const val HOME = "ic_home"
    const val SUBSCRIPTIONS = "ic_subscriptions"
    const val FITNESS = "ic_fitness"
    const val COFFEE = "ic_coffee"
    const val GIFT = "ic_gift"
    const val INCOME = "ic_paid"
    const val CATEGORY = "ic_help"

    val availableIcons: List<IconOption> = listOf(
        IconOption("ic_salary", "Salary", Icons.Filled.Work),
        IconOption("ic_trending_up", "Investment", Icons.Filled.TrendingUp),
        IconOption("ic_paid", "Cash / Income", Icons.Filled.Paid),
        IconOption("ic_atm", "Bank / ATM", Icons.Filled.LocalAtm),
        IconOption("ic_gift", "Gift", Icons.Filled.CardGiftcard),
        IconOption("ic_swap", "Transfer / Reversal", Icons.Filled.SwapHoriz),
        IconOption("ic_home", "Home / Rent", Icons.Filled.Home),
        IconOption("ic_restaurant", "Food & Dining", Icons.Filled.Restaurant),
        IconOption("ic_fastfood", "Fast Food", Icons.Filled.Fastfood),
        IconOption("ic_cart", "Groceries", Icons.Filled.LocalGroceryStore),
        IconOption("ic_coffee", "Coffee & Cafe", Icons.Filled.LocalCafe),
        IconOption("ic_car", "Travel & Transit", Icons.Filled.FlightTakeoff),
        IconOption("ic_gas", "Fuel", Icons.Filled.LocalGasStation),
        IconOption("ic_bag", "Shopping", Icons.Filled.ShoppingBag),
        IconOption("ic_lightning", "Utilities", Icons.Filled.Paid),
        IconOption("ic_receipt", "Bills & Invoices", Icons.Filled.ReceiptLong),
        IconOption("ic_hospital", "Healthcare", Icons.Filled.LocalHospital),
        IconOption("ic_movies", "Entertainment", Icons.Filled.LocalMovies),
        IconOption("ic_school", "Education", Icons.Filled.School),
        IconOption("ic_phone", "Mobile / Net", Icons.Filled.LocalPhone),
        IconOption("ic_subscriptions", "Subscriptions", Icons.Filled.Subscriptions),
        IconOption("ic_fitness", "Fitness", Icons.Filled.FitnessCenter),
        IconOption("ic_bank", "Finance", Icons.Filled.AccountBalance),
        IconOption("ic_help", "Other", Icons.Filled.Label)
    )

    val availableColorHexes: List<String> = listOf(
        "#4CAF50", // Green
        "#2E7D32", // Dark Green
        "#00897B", // Teal
        "#00BCD4", // Cyan
        "#1565C0", // Blue
        "#2196F3", // Light Blue
        "#5E35B1", // Deep Purple
        "#9C27B0", // Purple
        "#E91E63", // Pink
        "#D32F2F", // Red
        "#FF9800", // Orange
        "#F57F17", // Amber
        "#795548", // Brown
        "#607D8B", // Blue Grey
        "#757575"  // Grey
    )

    fun iconFor(
        categoryOrIconKey: String
    ): ImageVector {
        val key = categoryOrIconKey.trim().lowercase()

        // Match by IconOption key first
        availableIcons.firstOrNull { it.key.equals(key, ignoreCase = true) }?.let {
            return it.icon
        }

        // Otherwise match by category name keywords
        return when {
            key.contains("salary") || key.contains("payroll") || key.contains("work") ->
                Icons.Filled.Work

            key.contains("invest") || key.contains("dividend") || key.contains("stock") ->
                Icons.Filled.TrendingUp

            key.contains("freelance") || key.contains("consult") ->
                Icons.Filled.Work

            key.contains("rent") || key.contains("property") ->
                Icons.Filled.Home

            key.contains("gift") || key.contains("grant") || key.contains("reward") ->
                Icons.Filled.CardGiftcard

            key.contains("refund") || key.contains("cashback") || key.contains("transfer") || key.contains("swap") ->
                Icons.Filled.SwapHoriz

            key.contains("food") || key.contains("dining") || key.contains("restaurant") ->
                Icons.Filled.Restaurant

            key.contains("cafe") || key.contains("coffee") ->
                Icons.Filled.LocalCafe

            key.contains("travel") || key.contains("flight") || key.contains("transit") ->
                Icons.Filled.FlightTakeoff

            key.contains("shopping") || key.contains("bag") || key.contains("store") ->
                Icons.Filled.ShoppingBag

            key.contains("grocer") || key.contains("cart") || key.contains("supermarket") ->
                Icons.Filled.LocalGroceryStore

            key.contains("fuel") || key.contains("gas") || key.contains("petrol") ->
                Icons.Filled.LocalGasStation

            key.contains("mobile") || key.contains("phone") ->
                Icons.Filled.LocalPhone

            key.contains("utility") || key.contains("electric") || key.contains("water") ->
                Icons.Filled.Paid

            key.contains("bill") || key.contains("receipt") || key.contains("invoice") ->
                Icons.Filled.ReceiptLong

            key.contains("health") || key.contains("medic") || key.contains("hospital") ->
                Icons.Filled.LocalHospital

            key.contains("entertain") || key.contains("movie") || key.contains("cinema") ->
                Icons.Filled.LocalMovies

            key.contains("educat") || key.contains("school") ->
                Icons.Filled.School

            key.contains("subscript") ->
                Icons.Filled.Subscriptions

            key.contains("fit") || key.contains("gym") ->
                Icons.Filled.FitnessCenter

            key.contains("bank") ->
                Icons.Filled.AccountBalance

            else ->
                Icons.Filled.Label
        }
    }
}

