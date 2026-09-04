package com.varsel.expensetracker.parser

import javax.inject.Inject

class TextNormalizer @Inject constructor() {

    fun normalize(rawText: String): String {

        var text = rawText

        // ------------------------------------------------------------
        // Remove everything before ACCOUNT ACTIVITY for Indian Bank statements
        // ------------------------------------------------------------

        val activityIndex = text.indexOf("ACCOUNT ACTIVITY", ignoreCase = true)

        if (activityIndex >= 0 && (text.contains("INDIAN BANK", ignoreCase = true) || text.contains("IDIB", ignoreCase = true))) {
            text = text.substring(activityIndex)
        }

        // ------------------------------------------------------------
        // Put first transaction onto a new line (Indian Bank & ICICI)
        // ------------------------------------------------------------

        text = text.replace(
            Regex("(Balance)\\s+(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})"),
            "$1\n$2"
        )

        text = text.replace(
            Regex("(\\S)\\s+(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})"),
            "$1\n$2"
        )

        text = text.replace(
            Regex("""(?i)(Balance(?:\s*\(INR\))?)\s+(?:(\d{1,4})\s+)?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})"""),
            "$1\n$2 $3"
        )

        text = text.replace(
            Regex("""(\d+\.\d{2})\s+(?:(\d{1,4})\s+)?(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})"""),
            "$1\n$2 $3"
        )

        // ------------------------------------------------------------
        // Split merged balances
        //
        // INR 1774.00INR 3298.59
        //
        // becomes
        //
        // INR 1774.00
        // INR 3298.59
        // ------------------------------------------------------------

        text = text.replace(
            Regex("(INR\\s*[\\d,]+\\.\\d{2})(INR\\s*[\\d,]+\\.\\d{2})"),
            "$1\n$2"
        )

        // ------------------------------------------------------------
        // Remove duplicate blank lines
        // ------------------------------------------------------------

        text = text.replace(
            Regex("\\n{3,}"),
            "\n\n"
        )

        return text.trim()
    }
}
