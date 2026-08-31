package com.varsel.expensetracker.ui.design

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object CategoryPalette {

    // Standard Expense Colors
    val Food = Color(0xFFFF9800)          // Vibrant Orange
    val Travel = Color(0xFF5E35B1)        // Deep Purple
    val Shopping = Color(0xFFE91E63)      // Pink
    val Bills = Color(0xFF6D4C41)         // Warm Brown
    val Utilities = Color(0xFF0288D1)     // Deep Sky Blue
    val Fuel = Color(0xFF00897B)          // Teal
    val Medical = Color(0xFFD32F2F)       // Crimson Red
    val Entertainment = Color(0xFF8E24AA) // Vivid Orchid Purple
    val Education = Color(0xFF3949AB)     // Indigo
    val Groceries = Color(0xFF4CAF50)     // Fresh Green
    val Fitness = Color(0xFFFF5722)       // Bright Orange-Red
    val Subscriptions = Color(0xFF00ACC1) // Aqua Teal
    val Home = Color(0xFF795548)          // Brown

    // Standard Income Colors
    val Salary = Color(0xFF2E7D32)        // Rich Dark Green
    val Investment = Color(0xFF1565C0)    // Royal Blue
    val Freelance = Color(0xFF00897B)     // Vibrant Teal
    val Refunds = Color(0xFF00BCD4)       // Bright Cyan
    val Rental = Color(0xFFF57F17)        // Rich Amber / Gold
    val Gifts = Color(0xFFC2185B)         // Magenta Rose
    val OtherIncome = Color(0xFF43A047)   // Emerald Green

    // Universal / System
    val Transfer = Color(0xFF7B1FA2)      // Purple
    val Uncategorized = Color(0xFF78909C) // Slate Grey

    // Curated high-contrast 24-color wheel for custom/dynamic categories
    private val dynamicColorPalette = listOf(
        Color(0xFFFF9800), // Orange
        Color(0xFF2E7D32), // Dark Green
        Color(0xFF1E88E5), // Blue
        Color(0xFFE91E63), // Pink
        Color(0xFF8E24AA), // Purple
        Color(0xFF00897B), // Teal
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF3949AB), // Indigo
        Color(0xFF43A047), // Green
        Color(0xFF00ACC1), // Cyan
        Color(0xFFF57C00), // Amber-Orange
        Color(0xFFD81B60), // Magenta
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF0097A7), // Dark Cyan
        Color(0xFF689F38), // Light Olive Green
        Color(0xFFC2185B), // Deep Pink
        Color(0xFF5C6BC0), // Soft Indigo
        Color(0xFF26A69A), // Mint Teal
        Color(0xFFFFA000), // Amber
        Color(0xFF7E57C2), // Medium Purple
        Color(0xFF039BE5), // Light Blue
        Color(0xFF8D6E63), // Cocoa Brown
        Color(0xFF4DB6AC), // Soft Teal
        Color(0xFFFF7043)  // Coral
    )

    /**
     * Resolves an intelligent, consistent color for any category name.
     */
    fun colorFor(categoryName: String): Color {
        val key = categoryName.trim().lowercase()

        if (key.isBlank() || key == "uncategorized") {
            return Uncategorized
        }

        // 0. Check dynamic cache synced from Category Management
        com.varsel.expensetracker.category.CategoryIconCatalog.getCategory(categoryName)?.let { entity ->
            try {
                if (entity.colorHex.isNotBlank()) {
                    return Color(android.graphics.Color.parseColor(entity.colorHex.trim()))
                }
            } catch (e: Exception) {
                // Fall back to predefined palette
            }
        }

        // 1. Income matching
        if (key.contains("salary") || key.contains("payroll") || key.contains("wages") || key.contains("stipend")) {
            return Salary
        }
        if (key.contains("invest") || key.contains("dividend") || key.contains("stock") || key.contains("mutual") || key.contains("trading")) {
            return Investment
        }
        if (key.contains("freelance") || key.contains("consult") || key.contains("gig") || key.contains("contract")) {
            return Freelance
        }
        if (key.contains("refund") || key.contains("cashback") || key.contains("reversal")) {
            return Refunds
        }
        if (key.contains("rental") || key.contains("tenant") || key.contains("lease")) {
            return Rental
        }
        if (key.contains("gift") || key.contains("grant") || key.contains("reward") || key.contains("bonus") || key.contains("prize")) {
            return Gifts
        }
        if (key.contains("other income") || key.contains("misc income")) {
            return OtherIncome
        }

        // 2. Expense matching
        if (key.contains("food") || key.contains("dining") || key.contains("restaurant") || key.contains("eat") || key.contains("meal")) {
            return Food
        }
        if (key.contains("cafe") || key.contains("coffee") || key.contains("tea") || key.contains("bakery") || key.contains("snack")) {
            return Color(0xFFD84315) // Deep Warm Amber
        }
        if (key.contains("grocer") || key.contains("supermarket") || key.contains("market") || key.contains("veggie") || key.contains("provisions")) {
            return Groceries
        }
        if (key.contains("travel") || key.contains("flight") || key.contains("airline") || key.contains("transit") || key.contains("train") || key.contains("bus") || key.contains("cab") || key.contains("taxi") || key.contains("uber") || key.contains("ola")) {
            return Travel
        }
        if (key.contains("fuel") || key.contains("gas") || key.contains("petrol") || key.contains("diesel")) {
            return Fuel
        }
        if (key.contains("shop") || key.contains("cloth") || key.contains("apparel") || key.contains("mall") || key.contains("fashion") || key.contains("wear")) {
            return Shopping
        }
        if (key.contains("bill") || key.contains("invoice") || key.contains("receipt") || key.contains("recharge") || key.contains("mobile") || key.contains("phone") || key.contains("internet") || key.contains("broadband") || key.contains("wifi")) {
            return Bills
        }
        if (key.contains("utilit") || key.contains("electric") || key.contains("water") || key.contains("power")) {
            return Utilities
        }
        if (key.contains("health") || key.contains("medic") || key.contains("hospital") || key.contains("clinic") || key.contains("pharmacy") || key.contains("doctor") || key.contains("dental")) {
            return Medical
        }
        if (key.contains("entertain") || key.contains("movie") || key.contains("cinema") || key.contains("theatre") || key.contains("game") || key.contains("concert") || key.contains("show")) {
            return Entertainment
        }
        if (key.contains("subscript") || key.contains("ott") || key.contains("streaming") || key.contains("netflix") || key.contains("spotify") || key.contains("prime")) {
            return Subscriptions
        }
        if (key.contains("educat") || key.contains("school") || key.contains("college") || key.contains("course") || key.contains("tuition") || key.contains("book") || key.contains("class")) {
            return Education
        }
        if (key.contains("fit") || key.contains("gym") || key.contains("sport") || key.contains("workout") || key.contains("yoga")) {
            return Fitness
        }
        if (key.contains("home") || key.contains("rent") || key.contains("house") || key.contains("furnish") || key.contains("maintenance")) {
            return Home
        }
        if (key.contains("transfer") || key.contains("swap") || key.contains("send") || key.contains("p2p")) {
            return Transfer
        }

        // 3. Fallback to deterministic hash-based vibrant color
        val hash = abs(key.hashCode())
        return dynamicColorPalette[hash % dynamicColorPalette.size]
    }
}
