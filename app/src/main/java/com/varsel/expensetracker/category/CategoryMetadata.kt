package com.varsel.expensetracker.category

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.design.CategoryPalette

data class CategoryUi(
    val id: String,
    val iconKey: String = "",
    val isIncome: Boolean = false
) {
    val icon: ImageVector
        get() {
            CategoryIconCatalog.getCategory(id)?.let { entity ->
                val matchingIcon = CategoryIconCatalog.availableIcons.firstOrNull { it.key.equals(entity.iconName, ignoreCase = true) }
                if (matchingIcon != null) return matchingIcon.icon
            }
            return CategoryIconCatalog.iconFor(id.ifBlank { iconKey })
        }

    val color: Color
        get() = CategoryPalette.colorFor(id)
}

object CategoryMetadata {

    val expenseCategories = listOf(
        CategoryUi(Category.FOOD, CategoryIconCatalog.FOOD),
        CategoryUi(Category.GROCERIES, CategoryIconCatalog.GROCERIES),
        CategoryUi(Category.FUEL_AND_TRANSPORT, CategoryIconCatalog.TRAVEL),
        CategoryUi(Category.UTILITIES, CategoryIconCatalog.UTILITIES),
        CategoryUi(Category.HEALTHCARE, CategoryIconCatalog.HEALTHCARE),
        CategoryUi(Category.SHOPPING, CategoryIconCatalog.SHOPPING),
        CategoryUi(Category.ENTERTAINMENT, CategoryIconCatalog.ENTERTAINMENT),
        CategoryUi(Category.UNCATEGORIZED, CategoryIconCatalog.CATEGORY)
    )

    val incomeCategories = listOf(
        CategoryUi(Category.SALARY, CategoryIconCatalog.SALARY, isIncome = true),
        CategoryUi(Category.INVESTMENTS, CategoryIconCatalog.INVESTMENT, isIncome = true),
        CategoryUi(Category.FREELANCE, CategoryIconCatalog.SALARY, isIncome = true),
        CategoryUi(Category.REFUNDS, CategoryIconCatalog.TRANSFER, isIncome = true),
        CategoryUi(Category.RENTAL, CategoryIconCatalog.HOME, isIncome = true),
        CategoryUi(Category.GIFTS, CategoryIconCatalog.GIFT, isIncome = true),
        CategoryUi(Category.OTHER_INCOME, CategoryIconCatalog.INCOME, isIncome = true),
        CategoryUi(Category.UNCATEGORIZED, CategoryIconCatalog.CATEGORY, isIncome = true)
    )

    val all: List<CategoryUi> = (expenseCategories + incomeCategories).distinctBy { it.id }

    fun emojiForCategory(categoryName: String, isIncome: Boolean = false): String {
        val trimmed = categoryName.trim()
        return when {
            trimmed.contains("Dining", ignoreCase = true) || trimmed.contains("Food", ignoreCase = true) || trimmed.contains("Restaurant", ignoreCase = true) || trimmed.contains("Cafe", ignoreCase = true) -> "🍔"
            trimmed.contains("Grocer", ignoreCase = true) || trimmed.contains("Supermarket", ignoreCase = true) -> "🛒"
            trimmed.contains("Fuel", ignoreCase = true) || trimmed.contains("Transport", ignoreCase = true) || trimmed.contains("Travel", ignoreCase = true) || trimmed.contains("Transit", ignoreCase = true) || trimmed.contains("Cab", ignoreCase = true) || trimmed.contains("Auto", ignoreCase = true) || trimmed.contains("Train", ignoreCase = true) || trimmed.contains("Flight", ignoreCase = true) -> "🚗"
            trimmed.contains("Util", ignoreCase = true) || trimmed.contains("Bill", ignoreCase = true) || trimmed.contains("Power", ignoreCase = true) || trimmed.contains("Mobile", ignoreCase = true) || trimmed.contains("Internet", ignoreCase = true) || trimmed.contains("Recharge", ignoreCase = true) || trimmed.contains("Electric", ignoreCase = true) || trimmed.contains("Water", ignoreCase = true) -> "💡"
            trimmed.contains("Health", ignoreCase = true) || trimmed.contains("Medic", ignoreCase = true) || trimmed.contains("Doctor", ignoreCase = true) || trimmed.contains("Pharmacy", ignoreCase = true) || trimmed.contains("Hospital", ignoreCase = true) -> "💊"
            trimmed.contains("Shop", ignoreCase = true) || trimmed.contains("Cloth", ignoreCase = true) || trimmed.contains("Mall", ignoreCase = true) -> "🛍️"
            trimmed.contains("Entertain", ignoreCase = true) || trimmed.contains("Movie", ignoreCase = true) || trimmed.contains("Cinema", ignoreCase = true) || trimmed.contains("Game", ignoreCase = true) -> "🎬"
            trimmed.contains("Educat", ignoreCase = true) || trimmed.contains("School", ignoreCase = true) -> "🎓"
            trimmed.contains("Salary", ignoreCase = true) || trimmed.contains("Payroll", ignoreCase = true) -> "💰"
            trimmed.contains("Invest", ignoreCase = true) || trimmed.contains("Stock", ignoreCase = true) || trimmed.contains("Mutual", ignoreCase = true) -> "📈"
            trimmed.contains("Freelance", ignoreCase = true) || trimmed.contains("Side Hustle", ignoreCase = true) || trimmed.contains("Consult", ignoreCase = true) -> "💼"
            trimmed.contains("Refund", ignoreCase = true) || trimmed.contains("Cashback", ignoreCase = true) || trimmed.contains("Reversal", ignoreCase = true) -> "🔄"
            trimmed.contains("Rent", ignoreCase = true) || trimmed.contains("Property", ignoreCase = true) || trimmed.contains("Lease", ignoreCase = true) -> "🏠"
            trimmed.contains("Gift", ignoreCase = true) || trimmed.contains("Grant", ignoreCase = true) || trimmed.contains("Bonus", ignoreCase = true) -> "🎁"
            trimmed.contains("Other Income", ignoreCase = true) -> "💵"
            trimmed.contains("Income", ignoreCase = true) -> "💵"
            trimmed.contains("Uncategorized", ignoreCase = true) -> "🏷️"
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

