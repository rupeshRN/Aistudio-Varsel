package com.varsel.expensetracker.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDetector @Inject constructor(
    private val indianBankParser: IndianBankParser,
    private val iciciBankParser: IciciBankParser
) {

    fun detect(rawText: String): StatementParser {

        if (iciciBankParser.canParse(rawText)) {
            return iciciBankParser
        }

        if (indianBankParser.canParse(rawText)) {
            return indianBankParser
        }

        throw IllegalArgumentException(
            "Unsupported bank statement. Supported banks: Indian Bank, ICICI Bank."
        )
    }
}

