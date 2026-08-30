package com.varsel.expensetracker.parser

data class ParsedTransaction(

    val date: String,

    val description: String,

    val debit: Double?,

    val credit: Double?,

    val balance: Double?,

    val reference: String?

)
