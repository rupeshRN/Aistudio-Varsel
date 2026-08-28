package com.varsel.expensetracker.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Central category icon catalog for Varsel.
 *
 * Screens should obtain category icons through this object
 * instead of maintaining their own category -> icon mappings.
 *
 * This is the foundation for the future user-selectable
 * category icon system.
 */
object CategoryIconCatalog {

    const val FOOD = "food"
    const val TRAVEL = "travel"
    const val SHOPPING = "shopping"
    const val GROCERIES = "groceries"
    const val FUEL = "fuel"
    const val MOBILE = "mobile"
    const val UTILITIES = "utilities"
    const val HEALTHCARE = "healthcare"
    const val ENTERTAINMENT = "entertainment"
    const val SALARY = "salary"
    const val TRANSFER = "transfer"
    const val INVESTMENT = "investment"
    const val EDUCATION = "education"
    const val BILLS = "bills"
    const val HOME = "home"
    const val SUBSCRIPTIONS = "subscriptions"
    const val FITNESS = "fitness"
    const val COFFEE = "coffee"
    const val GIFT = "gift"
    const val INCOME = "income"
    const val CATEGORY = "category"

    fun iconFor(
        category: String
    ): ImageVector {

        return when {

            category.equals(
                Category.FOOD,
                ignoreCase = true
            ) ->
                Icons.Filled.Restaurant

            category.equals(
                Category.TRAVEL,
                ignoreCase = true
            ) ->
                Icons.Filled.FlightTakeoff

            category.equals(
                Category.SHOPPING,
                ignoreCase = true
            ) ->
                Icons.Filled.ShoppingBag

            category.equals(
                Category.GROCERIES,
                ignoreCase = true
            ) ->
                Icons.Filled.LocalGroceryStore

            category.equals(
                Category.FUEL,
                ignoreCase = true
            ) ->
                Icons.Filled.LocalGasStation

            category.equals(
                Category.MOBILE,
                ignoreCase = true
            ) ->
                Icons.Filled.LocalPhone

            category.equals(
                Category.UTILITIES,
                ignoreCase = true
            ) ->
                Icons.Filled.Paid

            category.equals(
                Category.HEALTHCARE,
                ignoreCase = true
            ) ->
                Icons.Filled.LocalHospital

            category.equals(
                Category.ENTERTAINMENT,
                ignoreCase = true
            ) ->
                Icons.Filled.LocalMovies

            category.equals(
                Category.SALARY,
                ignoreCase = true
            ) ->
                Icons.Filled.Work

            category.equals(
                Category.TRANSFER,
                ignoreCase = true
            ) ->
                Icons.Filled.SwapHoriz

            category.equals(
                Category.INVESTMENT,
                ignoreCase = true
            ) ->
                Icons.Filled.TrendingUp

            category.equals(
                Category.EDUCATION,
                ignoreCase = true
            ) ->
                Icons.Filled.School

            category.equals(
                Category.BILLS,
                ignoreCase = true
            ) ->
                Icons.Filled.ReceiptLong

            category.equals(
                Category.UNCATEGORIZED,
                ignoreCase = true
            ) ->
                Icons.Filled.Label

            else ->
                Icons.Filled.Label
        }
    }
}
