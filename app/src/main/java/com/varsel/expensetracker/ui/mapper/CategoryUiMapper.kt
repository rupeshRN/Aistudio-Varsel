package com.varsel.expensetracker.ui.mapper

import androidx.compose.ui.graphics.Color
import com.varsel.expensetracker.ui.model.CategoryUiModel
import javax.inject.Inject

class CategoryUiMapper @Inject constructor() {

    fun map(category: String): CategoryUiModel {

        return when (category.trim().lowercase()) {

            "food",
            "food & drink" -> CategoryUiModel(
                name = category,
                emoji = "🍔",
                backgroundColor = Color(0xFFFFF3E0),
                textColor = Color(0xFFE65100)
            )

            "groceries" -> CategoryUiModel(
                name = category,
                emoji = "🛒",
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32)
            )

            "travel",
            "transport" -> CategoryUiModel(
                name = category,
                emoji = "🚕",
                backgroundColor = Color(0xFFE3F2FD),
                textColor = Color(0xFF1565C0)
            )

            "shopping" -> CategoryUiModel(
                name = category,
                emoji = "🛍️",
                backgroundColor = Color(0xFFF3E5F5),
                textColor = Color(0xFF7B1FA2)
            )

            "salary",
            "income" -> CategoryUiModel(
                name = category,
                emoji = "💰",
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF1B5E20)
            )

            "subscription",
            "subscriptions" -> CategoryUiModel(
                name = category,
                emoji = "📱",
                backgroundColor = Color(0xFFEDE7F6),
                textColor = Color(0xFF512DA8)
            )

            "utilities" -> CategoryUiModel(
                name = category,
                emoji = "💡",
                backgroundColor = Color(0xFFFFF8E1),
                textColor = Color(0xFFF9A825)
            )

            else -> CategoryUiModel(
                name = category.ifBlank { "Uncategorized" },
                emoji = "🏷️",
                backgroundColor = Color(0xFFEEEEEE),
                textColor = Color(0xFF616161)
            )
        }
    }
}
