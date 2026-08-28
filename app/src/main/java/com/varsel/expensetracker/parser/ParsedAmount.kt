package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.TransactionType

data class ParsedAmount(
    val amount: Double,
    val balance: Double,
    val type: TransactionType
)
