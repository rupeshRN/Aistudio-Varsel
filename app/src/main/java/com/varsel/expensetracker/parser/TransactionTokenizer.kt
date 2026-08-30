package com.varsel.expensetracker.parser

interface TransactionTokenizer {

    fun tokenize(
        description: String
    ): List<String>
}
