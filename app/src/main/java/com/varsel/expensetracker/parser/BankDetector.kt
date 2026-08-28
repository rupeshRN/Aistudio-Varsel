package com.varsel.expensetracker.parser

import javax.inject.Inject

class BankDetector @Inject constructor(
    private val indianBankParser: IndianBankParser
) {

    fun detect(rawText: String): StatementParser {

        if (indianBankParser.canParse(rawText)) {
            return indianBankParser
        }

        throw IllegalArgumentException(
            "Unsupported bank statement."
        )
    }
}
