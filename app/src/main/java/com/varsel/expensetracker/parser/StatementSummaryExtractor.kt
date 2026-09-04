package com.varsel.expensetracker.parser

import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class StatementSummary(

    val statementStartDate: Long? = null,

    val statementEndDate: Long? = null,

    val openingBalance: Double? = null,

    val totalCredits: Double? = null,

    val totalDebits: Double? = null,

    val endingBalance: Double? = null
)

class StatementSummaryExtractor @Inject constructor() {

    private val moneyRegex =
        Regex(
            """(?:INR|Rs\.?|₹)?\s*([0-9]{1,3}(?:,[0-9]{3})*|\d+)\.(\d{2})""",
            RegexOption.IGNORE_CASE
        )

    private val periodRegexes = listOf(
        Regex("""For\s+period\s*:\s*(\d{1,2}\s+[A-Za-z]{3}\s+\d{4})\s*-\s*(\d{1,2}\s+[A-Za-z]{3}\s+\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""for\s+the\s+period\s+([A-Za-z]+\s+\d{1,2},\s+\d{4})\s*-\s*([A-Za-z]+\s+\d{1,2},\s+\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""(?:period|from)\s*[:\-]?\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\s*(?:to|-)\s*(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})""", RegexOption.IGNORE_CASE)
    )

    private val dateParsers = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
    )

    private fun tryParseDate(str: String): Long? {
        val clean = str.trim()
        for (parser in dateParsers) {
            try {
                val d = parser.parse(clean)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }

    fun extract(
        rawText: String
    ): StatementSummary {

        var statementStartDate: Long? = null

        var statementEndDate: Long? = null

        var opening: Double? = null

        var credits: Double? = null

        var debits: Double? = null

        var ending: Double? = null

        //--------------------------------------------------
        // Statement period
        //--------------------------------------------------

        for (regex in periodRegexes) {
            val periodMatch = regex.find(rawText)
            if (periodMatch != null) {
                statementStartDate = tryParseDate(periodMatch.groupValues[1])
                statementEndDate = tryParseDate(periodMatch.groupValues[2])
                if (statementStartDate != null && statementEndDate != null) {
                    break
                }
            }
        }

        //--------------------------------------------------
        // Account summary
        //--------------------------------------------------

        val lines =
            rawText.lines()

        for (line in lines) {

            val amount =
                moneyRegex
                    .find(line)
                    ?.let { match ->
                        val intPart = match.groupValues[1].replace(",", "")
                        val decPart = match.groupValues[2]
                        "$intPart.$decPart".toDoubleOrNull()
                    }
                    ?: continue

            val upper =
                line.uppercase()

            when {
                upper.contains("OPENING BALANCE") || upper.contains("OPENING BAL") || upper.contains("BROUGHT FORWARD") || upper.contains("B/F") -> {
                    if (opening == null) opening = amount
                }

                upper.contains("TOTAL CREDIT") || upper.contains("TOTAL DEPOSIT") || upper.contains("TOTAL DEPOSITS") || upper.contains("DEPOSIT AMOUNT") -> {
                    if (credits == null) credits = amount
                }

                upper.contains("TOTAL DEBIT") || upper.contains("TOTAL WITHDRAWAL") || upper.contains("TOTAL WITHDRAWALS") || upper.contains("WITHDRAWAL AMOUNT") -> {
                    if (debits == null) debits = amount
                }

                upper.contains("ENDING BALANCE") || upper.contains("CLOSING BALANCE") || upper.contains("CLOSING BAL") || upper.contains("CARRIED FORWARD") || upper.contains("C/F") -> {
                    if (ending == null) ending = amount
                }
            }
        }

        return StatementSummary(

            statementStartDate =
                statementStartDate,

            statementEndDate =
                statementEndDate,

            openingBalance =
                opening,

            totalCredits =
                credits,

            totalDebits =
                debits,

            endingBalance =
                ending
        )
    }
}
