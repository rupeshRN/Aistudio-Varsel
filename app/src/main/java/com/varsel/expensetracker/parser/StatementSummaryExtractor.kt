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
            "INR\\s*([\\d,]+\\.\\d{2})",
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
                    ?.groupValues
                    ?.get(1)
                    ?.replace(",", "")
                    ?.toDoubleOrNull()
                    ?: continue

            val upper =
                line.uppercase()

            when {

                upper.contains(
                    "OPENING BALANCE"
                ) -> {
                    opening = amount
                }

                upper.contains(
                    "TOTAL CREDIT"
                ) -> {
                    credits = amount
                }

                upper.contains(
                    "TOTAL DEBIT"
                ) -> {
                    debits = amount
                }

                upper.contains(
                    "ENDING BALANCE"
                ) ||
                upper.contains(
                    "CLOSING BALANCE"
                ) -> {
                    ending = amount
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
