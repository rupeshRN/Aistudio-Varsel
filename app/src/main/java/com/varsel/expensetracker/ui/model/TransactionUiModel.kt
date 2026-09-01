package com.varsel.expensetracker.ui.model

data class TransactionUiModel(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val category: String,
    val amountText: String,
    val dateText: String,
    val isIncome: Boolean,
    val accountInfoText: String? = null,
    val isTransfer: Boolean = false,
    val isEventLinked: Boolean = false,
    val eventName: String? = null
)

