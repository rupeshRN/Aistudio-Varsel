package com.varsel.expensetracker.parser

import javax.inject.Inject

class StatementEndDetector @Inject constructor() {

    /**
     * Returns true only for genuine end-of-statement
     * financial summary sections.
     */
    fun isStatementEnd(
        line: String
    ): Boolean {

        val text = line
            .trim()
            .uppercase()

        return summaryMarkers.any {

            text == it ||
            text.startsWith("$it ")

        }
    }

    companion object {

        /**
         * These are genuine financial summaries,
         * not page decorations or metadata.
         */
        private val summaryMarkers = listOf(

            "ENDING BALANCE",

            "CLOSING BALANCE",

            "OPENING BALANCE",

            "TOTAL CREDITS",

            "TOTAL DEBITS",

            "GRAND TOTAL"
        )
    }
}
