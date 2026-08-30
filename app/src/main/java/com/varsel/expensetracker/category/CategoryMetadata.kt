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
