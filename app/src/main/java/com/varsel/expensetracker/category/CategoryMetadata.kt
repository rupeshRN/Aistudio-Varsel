package com.varsel.expensetracker.category

data class CategoryUi(

    val id: String,

    val icon: String

)

object CategoryMetadata {

    val all = listOf(

        CategoryUi(Category.FOOD, "🍔"),

        CategoryUi(Category.TRAVEL, "🚗"),

        CategoryUi(Category.FUEL, "⛽"),

        CategoryUi(Category.GROCERIES, "🛒"),

        CategoryUi(Category.BILLS, "💡"),

        CategoryUi(Category.HEALTHCARE, "💊"),

        CategoryUi(Category.ENTERTAINMENT, "🎬"),

        CategoryUi(Category.MOBILE, "📱"),

        CategoryUi(Category.SHOPPING, "🛍️")

    )

}
