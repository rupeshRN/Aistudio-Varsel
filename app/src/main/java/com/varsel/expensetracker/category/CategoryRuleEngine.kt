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

        // Housing & Rent (Expense)
        KeywordRule("rent", Category.HOUSING, 95, isIncome = false),
        KeywordRule("maintenance", Category.HOUSING, 90, isIncome = false),
        KeywordRule("society", Category.HOUSING, 90, isIncome = false),
        KeywordRule("lease", Category.HOUSING, 90, isIncome = false),
        KeywordRule("tenancy", Category.HOUSING, 95, isIncome = false),

        // Food & Dining (Expense)
        KeywordRule("tea", Category.FOOD, 95, isIncome = false),
        KeywordRule("coffee", Category.FOOD, 95, isIncome = false),
        KeywordRule("breakfast", Category.FOOD, 95, isIncome = false),
        KeywordRule("lunch", Category.FOOD, 95, isIncome = false),
        KeywordRule("dinner", Category.FOOD, 95, isIncome = false),
        KeywordRule("restaurant", Category.FOOD, 90, isIncome = false),
        KeywordRule("egg", Category.FOOD, 95, isIncome = false),
        KeywordRule("eggs", Category.FOOD, 95, isIncome = false),
        KeywordRule("roti", Category.FOOD, 95, isIncome = false),
        KeywordRule("rotti", Category.FOOD, 95, isIncome = false),
        KeywordRule("chapati", Category.FOOD, 95, isIncome = false),
        KeywordRule("parotta", Category.FOOD, 95, isIncome = false),
        KeywordRule("meal", Category.FOOD, 95, isIncome = false),
        KeywordRule("meals", Category.FOOD, 95, isIncome = false),
        KeywordRule("snack", Category.FOOD, 95, isIncome = false),
        KeywordRule("snacks", Category.FOOD, 95, isIncome = false),
        KeywordRule("juice", Category.FOOD, 95, isIncome = false),
        KeywordRule("pizza", Category.FOOD, 95, isIncome = false),
        KeywordRule("burger", Category.FOOD, 95, isIncome = false),
        KeywordRule("chai", Category.FOOD, 95, isIncome = false),
        KeywordRule("samosa", Category.FOOD, 95, isIncome = false),
        KeywordRule("biryani", Category.FOOD, 95, isIncome = false),
        KeywordRule("dosa", Category.FOOD, 95, isIncome = false),
        KeywordRule("idli", Category.FOOD, 95, isIncome = false),
        KeywordRule("Appe", Category.FOOD, 95, isIncome = false),
        KeywordRule("Poha", Category.FOOD, 95, isIncome = false),
        KeywordRule("maggie", Category.FOOD, 95, isIncome = false),
        KeywordRule("swiggy", Category.FOOD, 98, isIncome = false),
        KeywordRule("zomato", Category.FOOD, 98, isIncome = false),
        KeywordRule("starbucks", Category.FOOD, 98, isIncome = false),
        KeywordRule("mcdonald", Category.FOOD, 98, isIncome = false),

        // Groceries (Expense)
        KeywordRule("grocery", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("vegetable", Category.GROCERIES, 90, isIncome = false),
        KeywordRule("milk", Category.GROCERIES, 90, isIncome = false),
        KeywordRule("rice", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("atta", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("flour", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("fruit", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("fruits", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("vegetables", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("groceries", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("veggies", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("veggie", Category.GROCERIES, 95, isIncome = false),
        KeywordRule("dmart", Category.GROCERIES, 98, isIncome = false),
        KeywordRule("bigbasket", Category.GROCERIES, 98, isIncome = false),
        KeywordRule("zepto", Category.GROCERIES, 98, isIncome = false),
        KeywordRule("blinkit", Category.GROCERIES, 98, isIncome = false),
        KeywordRule("instamart", Category.GROCERIES, 98, isIncome = false),

        // Fuel & Transport (Expense)
        KeywordRule("train", Category.FUEL_AND_TRANSPORT, 98, isIncome = false),
        KeywordRule("ticket", Category.FUEL_AND_TRANSPORT, 90, isIncome = false),
        KeywordRule("metro", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("bus", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("cab", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("railway", Category.FUEL_AND_TRANSPORT, 98, isIncome = false),
        KeywordRule("railways", Category.FUEL_AND_TRANSPORT, 98, isIncome = false),
        KeywordRule("flight", Category.FUEL_AND_TRANSPORT, 98, isIncome = false),
        KeywordRule("airport", Category.FUEL_AND_TRANSPORT, 98, isIncome = false),
        KeywordRule("taxi", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("auto", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("ola", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("uber", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("rapido", Category.FUEL_AND_TRANSPORT, 95, isIncome = false),
        KeywordRule("petrol", Category.FUEL_AND_TRANSPORT, 100, isIncome = false),
        KeywordRule("diesel", Category.FUEL_AND_TRANSPORT, 100, isIncome = false),
        KeywordRule("fuel", Category.FUEL_AND_TRANSPORT, 100, isIncome = false),

        // Utilities (Expense)
        KeywordRule("recharge", Category.UTILITIES, 100, isIncome = false),
        KeywordRule("airtel", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("jio", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("electricity", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("water", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("gas", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("eb", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("tneb", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("bescom", Category.UTILITIES, 95, isIncome = false),
        KeywordRule("power", Category.UTILITIES, 95, isIncome = false),

        // Healthcare & Fitness (Expense)
        KeywordRule("medical", Category.HEALTHCARE, 95, isIncome = false),
        KeywordRule("hospital", Category.HEALTHCARE, 100, isIncome = false),
        KeywordRule("pharmacy", Category.HEALTHCARE, 100, isIncome = false),
        KeywordRule("medicine", Category.HEALTHCARE, 100, isIncome = false),
        KeywordRule("medicines", Category.HEALTHCARE, 100, isIncome = false),
        KeywordRule("clinic", Category.HEALTHCARE, 100, isIncome = false),
        KeywordRule("doctor", Category.HEALTHCARE, 100, isIncome = false),
        KeywordRule("tablet", Category.HEALTHCARE, 95, isIncome = false),
        KeywordRule("tablets", Category.HEALTHCARE, 95, isIncome = false),
        KeywordRule("gym", Category.PERSONAL_CARE, 95, isIncome = false),
        KeywordRule("fitness", Category.PERSONAL_CARE, 95, isIncome = false),
        KeywordRule("workout", Category.PERSONAL_CARE, 95, isIncome = false),

        // Income Categories (Income)
        KeywordRule("salary", Category.SALARY, 100, isIncome = true),
        KeywordRule("payroll", Category.SALARY, 100, isIncome = true),
        KeywordRule("stipend", Category.SALARY, 95, isIncome = true),
        KeywordRule("interest", Category.INVESTMENTS, 90, isIncome = true),
        KeywordRule("dividend", Category.INVESTMENTS, 95, isIncome = true),
        KeywordRule("cashback", Category.REFUNDS, 95, isIncome = true),
        KeywordRule("refund", Category.REFUNDS, 95, isIncome = true),
        KeywordRule("freelance", Category.FREELANCE, 95, isIncome = true),
        KeywordRule("upwork", Category.FREELANCE, 98, isIncome = true),
        KeywordRule("fiverr", Category.FREELANCE, 98, isIncome = true),
        KeywordRule("bonus", Category.GIFTS, 95, isIncome = true),
        KeywordRule("grant", Category.GIFTS, 95, isIncome = true),
        KeywordRule("gift", Category.GIFTS, 95, isIncome = true),

        // Shopping (Expense)
        KeywordRule("amazon", Category.SHOPPING, 95, isIncome = false),
        KeywordRule("flipkart", Category.SHOPPING, 95, isIncome = false),
        KeywordRule("myntra", Category.SHOPPING, 95, isIncome = false),
        KeywordRule("ajio", Category.SHOPPING, 95, isIncome = false),
        KeywordRule("zudio", Category.SHOPPING, 95, isIncome = false),
        KeywordRule("decathlon", Category.SHOPPING, 95, isIncome = false)
    )

    /**
     * Determines the most appropriate category for the
     * supplied transaction description and type.
     */
    fun categorize(
        description: String,
        isIncome: Boolean = false
    ): CategoryResult {

        //--------------------------------------------------
        // Stage 1
        //
        // User-learned knowledge always has priority over
        // built-in keyword rules.
        //--------------------------------------------------

        customRuleEngine
            .findKnowledge(description, isIncome)
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
                    (rule.isIncome == null || rule.isIncome == isIncome) &&
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
