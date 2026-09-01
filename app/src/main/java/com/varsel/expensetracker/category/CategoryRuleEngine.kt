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

        // Fuel & Transport
        KeywordRule("train", Category.FUEL_AND_TRANSPORT, 98),
        KeywordRule("ticket", Category.FUEL_AND_TRANSPORT, 90),
        KeywordRule("metro", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("bus", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("cab", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("railway", Category.FUEL_AND_TRANSPORT, 98),
        KeywordRule("railways", Category.FUEL_AND_TRANSPORT, 98),
        KeywordRule("flight", Category.FUEL_AND_TRANSPORT, 98),
        KeywordRule("airport", Category.FUEL_AND_TRANSPORT, 98),
        KeywordRule("taxi", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("auto", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("ola", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("uber", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("rapido", Category.FUEL_AND_TRANSPORT, 95),
        KeywordRule("petrol", Category.FUEL_AND_TRANSPORT, 100),
        KeywordRule("diesel", Category.FUEL_AND_TRANSPORT, 100),
        KeywordRule("fuel", Category.FUEL_AND_TRANSPORT, 100),

        // Utilities
        KeywordRule("recharge", Category.UTILITIES, 100),
        KeywordRule("airtel", Category.UTILITIES, 95),
        KeywordRule("jio", Category.UTILITIES, 95),
        KeywordRule("electricity", Category.UTILITIES, 95),
        KeywordRule("water", Category.UTILITIES, 95),
        KeywordRule("gas", Category.UTILITIES, 95),
        KeywordRule("eb", Category.UTILITIES, 95),
        KeywordRule("tneb", Category.UTILITIES, 95),
        KeywordRule("bescom", Category.UTILITIES, 95),
        KeywordRule("power", Category.UTILITIES, 95),

        // Healthcare & Fitness
        KeywordRule("medical", Category.HEALTHCARE, 95),
        KeywordRule("hospital", Category.HEALTHCARE, 100),
        KeywordRule("pharmacy", Category.HEALTHCARE, 100),
        KeywordRule("medicine", Category.HEALTHCARE, 100),
        KeywordRule("medicines", Category.HEALTHCARE, 100),
        KeywordRule("clinic", Category.HEALTHCARE, 100),
        KeywordRule("doctor", Category.HEALTHCARE, 100),
        KeywordRule("tablet", Category.HEALTHCARE, 95),
        KeywordRule("tablets", Category.HEALTHCARE, 95),
        KeywordRule("gym", Category.PERSONAL_CARE, 95),
        KeywordRule("fitness", Category.PERSONAL_CARE, 95),
        KeywordRule("workout", Category.PERSONAL_CARE, 95),

        // Income & Salary
        KeywordRule("salary", Category.SALARY, 100),
        KeywordRule("payroll", Category.SALARY, 100),
        KeywordRule("stipend", Category.SALARY, 95),
        KeywordRule("interest", Category.INVESTMENTS, 90),
        KeywordRule("dividend", Category.INVESTMENTS, 95),
        KeywordRule("cashback", Category.REFUNDS, 95),
        KeywordRule("refund", Category.REFUNDS, 95),

        // Shopping
        KeywordRule("amazon", Category.SHOPPING, 95),
        KeywordRule("flipkart", Category.SHOPPING, 95),
        KeywordRule("myntra", Category.SHOPPING, 95),
        KeywordRule("ajio", Category.SHOPPING, 95),
        KeywordRule("zudio", Category.SHOPPING, 95),
        KeywordRule("decathlon", Category.SHOPPING, 95)
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
