package com.varsel.expensetracker.category

data class KeywordRule(

    val keyword: String,

    val category: String,

    val confidence: Int,

    val isIncome: Boolean? = null
)
