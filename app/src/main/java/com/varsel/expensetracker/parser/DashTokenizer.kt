package com.varsel.expensetracker.parser

import javax.inject.Inject

class DashTokenizer @Inject constructor() : TransactionTokenizer {

    override fun tokenize(
        description: String
    ): List<String> {

        return description
            .split("-")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
