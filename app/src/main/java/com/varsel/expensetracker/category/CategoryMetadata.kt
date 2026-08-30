package com.varsel.expensetracker.category

import com.varsel.expensetracker.domain.model.TransactionType

data class CategoryUi(
    val id: String,
    val icon: String,
    val isIncome: Boolean = false
)

object CategoryMetadata {

    val expenseCategories = listOf(
        CategoryUi(Category.FOOD, "🍔"),
        CategoryUi(Category.GROCERIES, "🛒"),
        CategoryUi("Fuel & Transport", "🚗"),
        CategoryUi(Category.TRAVEL, "🚗"),
        CategoryUi(Category.FUEL, "⛽"),
        CategoryUi(Category.SHOPPING, "🛍️"),
        CategoryUi(Category.UTILITIES, "💡"),
        CategoryUi(Category.HEALTHCARE, "💊"),
        CategoryUi(Category.ENTERTAINMENT, "🎬"),
        CategoryUi(Category.EDUCATION, "🎓"),
        CategoryUi(Category.MOBILE, "📱"),
        CategoryUi(Category.UNCATEGORIZED, "🏷️")
    )

    val incomeCategories = listOf(
        CategoryUi(Category.SALARY, "💰", isIncome = true),
        CategoryUi(Category.INVESTMENTS, "📈", isIncome = true),
        CategoryUi(Category.FREELANCE, "💼", isIncome = true),
        CategoryUi(Category.REFUNDS, "🔄", isIncome = true),
        CategoryUi(Category.RENTAL, "🏠", isIncome = true),
        CategoryUi(Category.GIFTS, "🎁", isIncome = true),
        CategoryUi(Category.OTHER_INCOME, "💵", isIncome = true),
        CategoryUi(Category.UNCATEGORIZED, "🏷️", isIncome = true)
    )

    val all: List<CategoryUi> = (expenseCategories + incomeCategories).distinctBy { it.id }

    fun emojiForCategory(categoryName: String, isIncome: Boolean = false): String {
        return when {
            categoryName.contains("Food", ignoreCase = true) || categoryName.contains("Dining", ignoreCase = true) -> "🍔"
            categoryName.contains("Grocer", ignoreCase = true) -> "🛒"
            categoryName.contains("Fuel", ignoreCase = true) || categoryName.contains("Transport", ignoreCase = true) || categoryName.contains("Travel", ignoreCase = true) -> "🚗"
            categoryName.contains("Shop", ignoreCase = true) -> "🛍️"
            categoryName.contains("Util", ignoreCase = true) || categoryName.contains("Bill", ignoreCase = true) || categoryName.contains("Power", ignoreCase = true) -> "💡"
            categoryName.contains("Health", ignoreCase = true) || categoryName.contains("Medical", ignoreCase = true) -> "💊"
            categoryName.contains("Entertain", ignoreCase = true) || categoryName.contains("Movie", ignoreCase = true) -> "🎬"
            categoryName.contains("Educat", ignoreCase = true) -> "🎓"
            categoryName.contains("Mobile", ignoreCase = true) || categoryName.contains("Internet", ignoreCase = true) -> "📱"
            categoryName.contains("Salary", ignoreCase = true) -> "💰"
            categoryName.contains("Invest", ignoreCase = true) -> "📈"
            categoryName.contains("Freelance", ignoreCase = true) || categoryName.contains("Work", ignoreCase = true) -> "💼"
            categoryName.contains("Refund", ignoreCase = true) || categoryName.contains("Cashback", ignoreCase = true) -> "🔄"
            categoryName.contains("Rent", ignoreCase = true) || categoryName.contains("Property", ignoreCase = true) -> "🏠"
            categoryName.contains("Gift", ignoreCase = true) -> "🎁"
            categoryName.contains("Income", ignoreCase = true) -> "💵"
            isIncome -> "💵"
            else -> "🏷️"
        }
    }

    fun categoriesFor(type: TransactionType): List<CategoryUi> {
        return when (type) {
            TransactionType.INCOME, TransactionType.CREDIT -> incomeCategories
            TransactionType.EXPENSE, TransactionType.DEBIT -> expenseCategories
        }
    }

    fun categoriesFor(isIncome: Boolean): List<CategoryUi> {
        return if (isIncome) incomeCategories else expenseCategories
    }
}
