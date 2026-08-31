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
    const val WORK = "ic_work"

    val availableIcons: List<IconOption> = listOf(
        IconOption("ic_salary", "Salary", Icons.Filled.Work),
        IconOption("ic_trending_up", "Investment", Icons.Filled.TrendingUp),
        IconOption("ic_paid", "Cash / Income", Icons.Filled.Paid),
        IconOption("ic_atm", "Bank / ATM", Icons.Filled.LocalAtm),
        IconOption("ic_gift", "Gift", Icons.Filled.CardGiftcard),
        IconOption("ic_swap", "Transfer / Reversal", Icons.Filled.SwapHoriz),
        IconOption("ic_home", "Home / Rent", Icons.Filled.Home),
        IconOption("ic_work", "Work / Freelance", Icons.Filled.Work),
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

    private val categoryMap = java.util.concurrent.ConcurrentHashMap<String, com.varsel.expensetracker.data.local.entity.CategoryEntity>()

    init {
        // Pre-seed with default categories matching Category Management
        val defaults = listOf(
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Salary", iconName = "ic_salary", colorHex = "#4CAF50", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Investments", iconName = "ic_trending_up", colorHex = "#1565C0", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Freelance & Side Hustle", iconName = "ic_work", colorHex = "#00897B", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Refunds & Cashback", iconName = "ic_swap", colorHex = "#00BCD4", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Rental & Property", iconName = "ic_home", colorHex = "#795548", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Gifts & Grants", iconName = "ic_gift", colorHex = "#E91E63", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Other Income", iconName = "ic_paid", colorHex = "#8BC34A", type = "INCOME"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Dining & Food", iconName = "ic_restaurant", colorHex = "#FF9800", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Groceries", iconName = "ic_cart", colorHex = "#4CAF50", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Fuel & Transport", iconName = "ic_car", colorHex = "#9C27B0", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Utilities", iconName = "ic_lightning", colorHex = "#2196F3", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Healthcare", iconName = "ic_hospital", colorHex = "#F44336", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Shopping", iconName = "ic_bag", colorHex = "#E91E63", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Entertainment", iconName = "ic_movies", colorHex = "#673AB7", type = "EXPENSE"),
            com.varsel.expensetracker.data.local.entity.CategoryEntity(name = "Uncategorized", iconName = "ic_help", colorHex = "#9E9E9E", type = "BOTH")
        )
        updateCategories(defaults)
    }

    fun updateCategories(categories: List<com.varsel.expensetracker.data.local.entity.CategoryEntity>) {
        categories.forEach { category ->
            categoryMap[category.name.trim().lowercase()] = category
        }
    }

    fun getCategory(name: String): com.varsel.expensetracker.data.local.entity.CategoryEntity? {
        val trimmed = name.trim().lowercase()
        return categoryMap[trimmed] ?: categoryMap.entries.firstOrNull { 
            it.key.contains(trimmed) || trimmed.contains(it.key) 
        }?.value
    }

    fun iconKeyForCategory(categoryName: String, isIncome: Boolean = false): String {
        val key = categoryName.trim().lowercase()
        categoryMap[key]?.let { return it.iconName }

        return when {
            key.contains("salary") || key.contains("payroll") -> SALARY
            key.contains("invest") || key.contains("stock") || key.contains("dividend") -> INVESTMENT
            key.contains("freelance") || key.contains("work") || key.contains("consult") -> WORK
            key.contains("rent") || key.contains("property") -> HOME
            key.contains("gift") || key.contains("reward") || key.contains("bonus") -> GIFT
            key.contains("refund") || key.contains("cashback") || key.contains("transfer") || key.contains("swap") -> TRANSFER
            key.contains("other income") || key == "income" -> INCOME
            key.contains("dining") || key.contains("food") || key.contains("restaurant") || key.contains("eat") -> FOOD
            key.contains("fastfood") || key.contains("burger") || key.contains("pizza") -> "ic_fastfood"
            key.contains("cafe") || key.contains("coffee") || key.contains("tea") -> COFFEE
            key.contains("fuel") || key.contains("transport") || key.contains("travel") || key.contains("transit") || key.contains("flight") || key.contains("train") -> TRAVEL
            key.contains("grocer") || key.contains("cart") || key.contains("supermarket") || key.contains("mart") -> GROCERIES
            key.contains("shop") || key.contains("mall") || key.contains("cloth") || key.contains("store") -> SHOPPING
            key.contains("mobile") || key.contains("phone") || key.contains("recharge") -> MOBILE
            key.contains("utility") || key.contains("electric") || key.contains("water") || key.contains("power") -> UTILITIES
            key.contains("bill") || key.contains("receipt") || key.contains("invoice") -> BILLS
            key.contains("health") || key.contains("medic") || key.contains("hospital") || key.contains("doctor") || key.contains("pharm") -> HEALTHCARE
            key.contains("entertain") || key.contains("movie") || key.contains("cinema") || key.contains("game") -> ENTERTAINMENT
            key.contains("educat") || key.contains("school") || key.contains("college") || key.contains("course") -> EDUCATION
            key.contains("subscript") || key.contains("ott") || key.contains("stream") -> SUBSCRIPTIONS
            key.contains("fit") || key.contains("gym") || key.contains("sport") || key.contains("workout") -> FITNESS
            key.contains("bank") || key.contains("atm") -> "ic_atm"
            isIncome -> INCOME
            else -> CATEGORY
        }
    }

    fun iconFor(
        categoryOrIconKey: String
    ): ImageVector {
        val key = categoryOrIconKey.trim().lowercase()

        // 1. Check if directly matches an IconOption key (e.g. "ic_car", "ic_lightning", "ic_paid", etc.)
        availableIcons.firstOrNull { it.key.equals(key, ignoreCase = true) }?.let {
            return it.icon
        }

        // 2. Check dynamic cache synced from Category Management
        getCategory(categoryOrIconKey)?.let { entity ->
            val iconEntity = availableIcons.firstOrNull { it.key.equals(entity.iconName, ignoreCase = true) }
            if (iconEntity != null) return iconEntity.icon
        }

        // 3. Fallback matching to Category Management default categories
        return when {
            key.contains("salary") || key.contains("payroll") ->
                Icons.Filled.Work

            key.contains("invest") || key.contains("dividend") || key.contains("stock") ->
                Icons.Filled.TrendingUp

            key.contains("freelance") || key.contains("side hustle") || key.contains("consult") || key.contains("work") ->
                Icons.Filled.Work

            key.contains("rent") || key.contains("property") || key.contains("lease") ->
                Icons.Filled.Home

            key.contains("gift") || key.contains("grant") || key.contains("reward") || key.contains("bonus") ->
                Icons.Filled.CardGiftcard

            key.contains("refund") || key.contains("cashback") || key.contains("transfer") || key.contains("swap") || key.contains("reversal") ->
                Icons.Filled.SwapHoriz

            key.contains("other income") || key == "income" || key.contains("cash") ->
                Icons.Filled.Paid

            key.contains("dining") || key.contains("food") || key.contains("restaurant") ->
                Icons.Filled.Restaurant

            key.contains("cafe") || key.contains("coffee") || key.contains("tea") ->
                Icons.Filled.LocalCafe

            key.contains("fastfood") || key.contains("burger") || key.contains("pizza") ->
                Icons.Filled.Fastfood

            key.contains("grocer") || key.contains("cart") || key.contains("supermarket") ->
                Icons.Filled.LocalGroceryStore

            // Fuel & Transport -> FlightTakeoff (matches Category Management ic_car)
            key.contains("fuel") || key.contains("transport") || key.contains("travel") || key.contains("transit") || key.contains("flight") || key.contains("train") || key.contains("cab") || key.contains("uber") || key.contains("ola") ->
                Icons.Filled.FlightTakeoff

            // Utilities -> Paid / Dollar (matches Category Management ic_lightning)
            key.contains("utility") || key.contains("electric") || key.contains("water") || key.contains("power") ->
                Icons.Filled.Paid

            key.contains("shopping") || key.contains("shop") || key.contains("bag") || key.contains("cloth") || key.contains("mall") || key.contains("store") ->
                Icons.Filled.ShoppingBag

            key.contains("bill") || key.contains("receipt") || key.contains("invoice") ->
                Icons.Filled.ReceiptLong

            key.contains("health") || key.contains("medic") || key.contains("hospital") || key.contains("doctor") || key.contains("pharm") ->
                Icons.Filled.LocalHospital

            key.contains("entertain") || key.contains("movie") || key.contains("cinema") || key.contains("theatre") || key.contains("game") ->
                Icons.Filled.LocalMovies

            key.contains("educat") || key.contains("school") || key.contains("college") || key.contains("course") ->
                Icons.Filled.School

            key.contains("mobile") || key.contains("phone") || key.contains("recharge") ->
                Icons.Filled.LocalPhone

            key.contains("subscript") || key.contains("ott") || key.contains("stream") ->
                Icons.Filled.Subscriptions

            key.contains("fit") || key.contains("gym") || key.contains("sport") || key.contains("workout") ->
                Icons.Filled.FitnessCenter

            key.contains("bank") || key.contains("atm") ->
                Icons.Filled.LocalAtm

            else ->
                Icons.Filled.Label
        }
    }
}

