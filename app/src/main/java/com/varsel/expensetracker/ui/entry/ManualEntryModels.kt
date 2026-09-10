package com.varsel.expensetracker.ui.entry

enum class ManualEntryType(val title: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer")
}

data class ManualEntryAccount(
    val id: String,
    val bankName: String,
    val displayName: String,
    val last4: String? = null,
    val isCashWallet: Boolean = false,
    val balance: Double = 0.0
)
