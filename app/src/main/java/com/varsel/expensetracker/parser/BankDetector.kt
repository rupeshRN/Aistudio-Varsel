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
        val header = rawText.lines().take(30).joinToString("\n").uppercase()

        // 1. Primary Header Branding Check
        val hasIndianBankInHeader = header.contains("INDIAN BANK") ||
                header.contains("INDIANBANK") ||
                header.contains("IND BL") ||
                header.contains("IDIB")

        val hasIciciInHeader = header.contains("ICICI") ||
                header.contains("ICIC0")

        if (hasIndianBankInHeader && !hasIciciInHeader) {
            return indianBankParser
        }

        if (hasIciciInHeader && !hasIndianBankInHeader) {
            return iciciBankParser
        }

        // 2. Structural & Layout Checks
        val iciciTableSignals = listOf(
            "TRANSACTION REMARKS",
            "WITHDRAWAL AMOUNT",
            "DEPOSIT AMOUNT",
            "CHEQUE NUMBER",
            "BALANCE (INR)",
            "SAVING ACCOUNT",
            "CURRENT ACCOUNT",
            "STATEMENT OF TRANSACTIONS"
        ).count { upper.contains(it) }

        val indianBankSignals = listOf(
            "ACCOUNT ACTIVITY",
            "DATE TRANSACTION DETAILS",
            "TOTAL CREDITS",
            "TOTAL DEBITS",
            "OPENING BALANCE",
            "CLOSING BALANCE"
        ).count { upper.contains(it) }

        val indianBankDateCount = Regex("""\b\d{1,2}\s+(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+\d{4}\b""", RegexOption.IGNORE_CASE).findAll(rawText).count()
        val numericDateCount = Regex("""\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b""").findAll(rawText).count()

        var indianScore = (if (hasIndianBankInHeader) 10 else 0) + (indianBankSignals * 2) + (if (indianBankDateCount > 0) 5 else 0)
        var iciciScore = (if (hasIciciInHeader) 10 else 0) + (iciciTableSignals * 2) + (if (numericDateCount > 0) 5 else 0)

        if (upper.contains("INDIAN BANK")) indianScore += 5
        if (upper.contains("ICICI")) iciciScore += 5

        if (iciciScore > indianScore) {
            return iciciBankParser
        } else if (indianScore > iciciScore) {
            return indianBankParser
        }

        // 3. Fallbacks
        if (iciciBankParser.canParse(rawText)) {
            return iciciBankParser
        }
        if (indianBankParser.canParse(rawText)) {
            return indianBankParser
        }

        return iciciBankParser
    }
}


