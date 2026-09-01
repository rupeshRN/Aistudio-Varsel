package com.varsel.expensetracker.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDetector @Inject constructor(
    private val indianBankParser: IndianBankParser,
    private val iciciBankParser: IciciBankParser
) {

    fun detect(rawText: String): StatementParser {
        val upper = rawText.uppercase()

        // 1. Check Indian Bank explicit match
        if (indianBankParser.canParse(rawText)) {
            return indianBankParser
        }

        // 2. Check ICICI Bank explicit match
        if (iciciBankParser.canParse(rawText)) {
            return iciciBankParser
        }

        // 3. Fallback heuristic based on layout indicators
        val hasIndianBankDates = Regex("""\b\d{1,2}\s+(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+\d{4}\b""", RegexOption.IGNORE_CASE).containsMatchIn(rawText)
        if (hasIndianBankDates && (upper.contains("INR") || upper.contains("ACCOUNT"))) {
            return indianBankParser
        }

        if (upper.contains("TRANSACTION") || upper.contains("STATEMENT") || upper.contains("BALANCE")) {
            return iciciBankParser
        }

        throw IllegalArgumentException(
            "Unsupported bank statement. Supported banks: Indian Bank, ICICI Bank."
        )
    }
}

