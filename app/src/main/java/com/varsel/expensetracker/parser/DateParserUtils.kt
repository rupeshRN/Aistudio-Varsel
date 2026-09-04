package com.varsel.expensetracker.parser

import java.text.SimpleDateFormat
import java.util.Locale

object DateParserUtils {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
    )

    fun parseDate(dateStr: String): Long? {
        val normalized = normalizeDateString(dateStr)
        for (fmt in supportedDateFormats) {
            try {
                val parsed = fmt.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun normalizeDateString(dateStr: String): String {
        var clean = dateStr.trim()

        // Replace dots and hyphens with / if numeric or standard separator
        clean = clean.replace(".", "/").replace("-", "/")

        val parts = clean.split("/").map { it.trim() }
        if (parts.size == 3) {
            val p1 = parts[0]
            val p2 = parts[1]
            val p3 = parts[2]

            // Case: dd/MM/yy or dd/MMM/yy (e.g. 01/07/26 -> 01/07/2026 or 15/JUN/26 -> 15/JUN/2026)
            if (p3.length == 2 && p3.all { it.isDigit() }) {
                val fullYear = "20$p3"
                return "$p1/$p2/$fullYear"
            }
        }

        return clean
    }
}
