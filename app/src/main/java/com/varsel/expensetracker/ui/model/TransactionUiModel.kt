package com.varsel.expensetracker.ui.model

data class TransactionUiModel(

    val id: Long,

    val title: String,

    val subtitle: String?,

    val category: String,

    val amountText: String,

    val dateText: String,

    val isIncome: Boolean
)
