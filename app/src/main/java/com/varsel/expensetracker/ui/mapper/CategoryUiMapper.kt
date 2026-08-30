package com.varsel.expensetracker.ui.mapper

import androidx.compose.ui.graphics.Color
import com.varsel.expensetracker.ui.model.CategoryUiModel
import javax.inject.Inject

class CategoryUiMapper @Inject constructor() {

    fun map(category: String): CategoryUiModel {

        val normalized = category.trim().lowercase()

        return when {
            // Income categories
            normalized.contains("salary") || normalized.contains("payroll") || normalized.contains("stipend") ->
                CategoryUiModel(
                    name = category,
                    emoji = "💰",
                    backgroundColor = Color(0xFFE8F5E9),
                    textColor = Color(0xFF1B5E20)
                )

            normalized.contains("invest") || normalized.contains("dividend") || normalized.contains("stock") ->
                CategoryUiModel(
                    name = category,
                    emoji = "📈",
                    backgroundColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF0D47A1)
                )

            normalized.contains("freelance") || normalized.contains("consult") || normalized.contains("side hustle") ->
                CategoryUiModel(
                    name = category,
                    emoji = "💼",
                    backgroundColor = Color(0xFFE0F2F1),
                    textColor = Color(0xFF004D40)
                )

            normalized.contains("refund") || normalized.contains("cashback") || normalized.contains("reversal") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🔄",
                    backgroundColor = Color(0xFFE0F7FA),
                    textColor = Color(0xFF006064)
                )

            normalized.contains("rent") || normalized.contains("property") || normalized.contains("lease") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🏠",
                    backgroundColor = Color(0xFFEFEBE9),
                    textColor = Color(0xFF3E2723)
                )

            normalized.contains("gift") || normalized.contains("grant") || normalized.contains("reward") || normalized.contains("bonus") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🎁",
                    backgroundColor = Color(0xFFFCE4EC),
                    textColor = Color(0xFF880E4F)
                )

            normalized.contains("income") ->
                CategoryUiModel(
                    name = category,
                    emoji = "💵",
                    backgroundColor = Color(0xFFF1F8E9),
                    textColor = Color(0xFF33691E)
                )

            // Expense categories
            normalized.contains("food") || normalized.contains("dining") || normalized.contains("restaurant") || normalized.contains("cafe") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🍔",
                    backgroundColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100)
                )

            normalized.contains("grocer") || normalized.contains("supermarket") || normalized.contains("produce") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🛒",
                    backgroundColor = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32)
                )

            normalized.contains("travel") || normalized.contains("transport") || normalized.contains("flight") || normalized.contains("taxi") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🚗",
                    backgroundColor = Color(0xFFEDE7F6),
                    textColor = Color(0xFF4527A0)
                )

            normalized.contains("fuel") || normalized.contains("petrol") || normalized.contains("gas") ->
                CategoryUiModel(
                    name = category,
                    emoji = "⛽",
                    backgroundColor = Color(0xFFE0F2F1),
                    textColor = Color(0xFF00695C)
                )

            normalized.contains("shop") || normalized.contains("mall") || normalized.contains("cloth") || normalized.contains("store") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🛍️",
                    backgroundColor = Color(0xFFFCE4EC),
                    textColor = Color(0xFFAD1457)
                )

            normalized.contains("utility") || normalized.contains("bill") || normalized.contains("electric") || normalized.contains("water") ->
                CategoryUiModel(
                    name = category,
                    emoji = "💡",
                    backgroundColor = Color(0xFFFFF8E1),
                    textColor = Color(0xFFF57F17)
                )

            normalized.contains("health") || normalized.contains("medic") || normalized.contains("pharma") || normalized.contains("doctor") ->
                CategoryUiModel(
                    name = category,
                    emoji = "💊",
                    backgroundColor = Color(0xFFFFEBEE),
                    textColor = Color(0xFFC62828)
                )

            normalized.contains("entertain") || normalized.contains("movie") || normalized.contains("cinema") || normalized.contains("game") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🎬",
                    backgroundColor = Color(0xFFF3E5F5),
                    textColor = Color(0xFF6A1B9A)
                )

            normalized.contains("educat") || normalized.contains("course") || normalized.contains("school") || normalized.contains("tuition") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🎓",
                    backgroundColor = Color(0xFFE8EAF6),
                    textColor = Color(0xFF283593)
                )

            normalized.contains("mobile") || normalized.contains("phone") || normalized.contains("recharge") || normalized.contains("broadband") ->
                CategoryUiModel(
                    name = category,
                    emoji = "📱",
                    backgroundColor = Color(0xFFE1F5FE),
                    textColor = Color(0xFF0277BD)
                )

            normalized.contains("transfer") ->
                CategoryUiModel(
                    name = category,
                    emoji = "🔁",
                    backgroundColor = Color(0xFFF3E5F5),
                    textColor = Color(0xFF4A148C)
                )

            else ->
                CategoryUiModel(
                    name = category.ifBlank { "Uncategorized" },
                    emoji = "🏷️",
                    backgroundColor = Color(0xFFEEEEEE),
                    textColor = Color(0xFF616161)
                )
        }
    }
}

