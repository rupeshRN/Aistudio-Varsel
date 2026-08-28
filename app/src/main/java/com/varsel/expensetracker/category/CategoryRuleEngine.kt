package com.varsel.expensetracker.category

import javax.inject.Inject

/**
 * Primary category decision engine.
 *
 * Responsibilities
 * ----------------------------------------------------
 * Determines the category for a transaction description
 * using a layered decision strategy.
 *
 * Decision Order
 * ----------------------------------------------------
 *
 * 1. User-learned knowledge (highest priority)
 *      • Applied through CustomRuleEngine.
 *      • Represents explicit user corrections.
 *
 * 2. Built-in keyword rules
 *      • Used only when no learned rule exists.
 *      • Confidence-based matching.
 *
 * 3. Uncategorized
 *      • Returned when no rule matches.
 *
 * This class never:
 * • reads Room directly
 * • writes learned rules
 * • performs statement parsing
 * • updates UI
 *
 * It is intentionally a pure decision engine.
 */
class CategoryRuleEngine @Inject constructor(

    private val customRuleEngine: CustomRuleEngine

) {

    //--------------------------------------------------
    // Built-in keyword rules.
    //
    // These act as fallback heuristics whenever the
    // learning engine has no knowledge for a merchant.
    //--------------------------------------------------

    private val rules = listOf(

        // Food
        KeywordRule("tea", Category.FOOD, 95),
        KeywordRule("coffee", Category.FOOD, 95),
        KeywordRule("breakfast", Category.FOOD, 95),
        KeywordRule("lunch", Category.FOOD, 95),
        KeywordRule("dinner", Category.FOOD, 95),
        KeywordRule("restaurant", Category.FOOD, 90),
        KeywordRule("egg", Category.FOOD, 95),
        KeywordRule("eggs", Category.FOOD, 95),
        KeywordRule("roti", Category.FOOD, 95),
        KeywordRule("rotti", Category.FOOD, 95),
        KeywordRule("chapati", Category.FOOD, 95),
        KeywordRule("parotta", Category.FOOD, 95),
        KeywordRule("meal", Category.FOOD, 95),
        KeywordRule("meals", Category.FOOD, 95),
        KeywordRule("snack", Category.FOOD, 95),
        KeywordRule("snacks", Category.FOOD, 95),
        KeywordRule("juice", Category.FOOD, 95),
        KeywordRule("pizza", Category.FOOD, 95),
        KeywordRule("burger", Category.FOOD, 95),
        KeywordRule("chai", Category.FOOD, 95),
        KeywordRule("samosa", Category.FOOD, 95),
        KeywordRule("biryani", Category.FOOD, 95),
        KeywordRule("dosa", Category.FOOD, 95),
        KeywordRule("idli", Category.FOOD, 95),
        KeywordRule("Appe", Category.FOOD, 95),
        KeywordRule("Poha", Category.FOOD, 95),
        KeywordRule("maggie", Category.FOOD, 95),

        // Groceries
        KeywordRule("grocery", Category.GROCERIES, 95),
        KeywordRule("vegetable", Category.GROCERIES, 90),
        KeywordRule("milk", Category.GROCERIES, 90),
        KeywordRule("rice", Category.GROCERIES, 95),
        KeywordRule("atta", Category.GROCERIES, 95),
        KeywordRule("flour", Category.GROCERIES, 95),
        KeywordRule("fruit", Category.GROCERIES, 95),
        KeywordRule("fruits", Category.GROCERIES, 95),
        KeywordRule("vegetables", Category.GROCERIES, 95),
        KeywordRule("grocery", Category.GROCERIES, 95),
        KeywordRule("groceries", Category.GROCERIES, 95),
        KeywordRule("veggies", Category.GROCERIES, 95),
        KeywordRule("veggie", Category.GROCERIES, 95),

        // Travel
        KeywordRule("train", Category.TRAVEL, 98),
        KeywordRule("ticket", Category.TRAVEL, 90),
        KeywordRule("metro", Category.TRAVEL, 95),
        KeywordRule("bus", Category.TRAVEL, 95),
        KeywordRule("cab", Category.TRAVEL, 95),
        KeywordRule("railway", Category.TRAVEL, 98),
        KeywordRule("railways", Category.TRAVEL, 98),
        KeywordRule("flight", Category.TRAVEL, 98),
        KeywordRule("airport", Category.TRAVEL, 98),
        KeywordRule("taxi", Category.TRAVEL, 95),
        KeywordRule("auto", Category.TRAVEL, 95),
        KeywordRule("ola", Category.TRAVEL, 95),
        KeywordRule("uber", Category.TRAVEL, 95),
        KeywordRule("rapido", Category.TRAVEL, 95),

        // Fuel
        KeywordRule("petrol", Category.FUEL, 100),
        KeywordRule("diesel", Category.FUEL, 100),
        KeywordRule("fuel", Category.FUEL, 100),

        // Mobile
        KeywordRule("recharge", Category.MOBILE, 100),

        // Bills
        KeywordRule("electricity", Category.BILLS, 95),
        KeywordRule("water", Category.BILLS, 95),
        KeywordRule("gas", Category.BILLS, 95),
        KeywordRule("eb", Category.BILLS, 95),

        // Healthcare
        KeywordRule("medical", Category.HEALTHCARE, 95),
        KeywordRule("hospital", Category.HEALTHCARE, 100),
        KeywordRule("pharmacy", Category.HEALTHCARE, 100),
        KeywordRule("medicine", Category.HEALTHCARE, 100),
        KeywordRule("medicines", Category.HEALTHCARE, 100),
        KeywordRule("clinic", Category.HEALTHCARE, 100),
        KeywordRule("doctor", Category.HEALTHCARE, 100),
        KeywordRule("tablet", Category.HEALTHCARE, 95),
        KeywordRule("tablets", Category.HEALTHCARE, 95)
    )

    /**
     * Determines the most appropriate category for the
     * supplied transaction description.
     */
    fun categorize(

        description: String

    ): CategoryResult {

        //--------------------------------------------------
        // Stage 1
        //
        // User-learned knowledge always has priority over
        // built-in keyword rules.
        //--------------------------------------------------

        customRuleEngine
            .findKnowledge(description)
            ?.let { knowledge ->

                return CategoryResult(

                    category = knowledge.categoryName,

                    confidence = 100

                )

            }

        //--------------------------------------------------
        // Stage 2
        //
        // Tokenize description and evaluate keyword rules.
        //--------------------------------------------------

        val words =
            description
                .lowercase()
                .replace(Regex("[^a-z0-9 ]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

        val match =
            rules
                .filter { rule ->

                    words.any { word ->
                        word == rule.keyword
                    }

                }
                .maxByOrNull { it.confidence }

        //--------------------------------------------------
        // Stage 3
        //
        // Return best keyword match or fall back to
        // Uncategorized.
        //--------------------------------------------------

        return if (match != null) {

            CategoryResult(
                category = match.category,
                confidence = match.confidence
            )

        } else {

            CategoryResult(
                category = Category.UNCATEGORIZED,
                confidence = 0
            )
        }
    }
}
