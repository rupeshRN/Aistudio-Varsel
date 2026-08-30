package com.varsel.expensetracker.parser

import javax.inject.Inject

class DescriptionBuilder @Inject constructor() {

    fun build(parts: List<String>): String {

        if (parts.isEmpty()) return "Unknown Transaction"

        val cleaned = parts
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // ----------------------------------------
        // Remove numeric-only fields
        // ----------------------------------------

        val withoutNumbers = cleaned.filterNot {

            it.matches(Regex("^\\d+$")) ||
            it.matches(Regex("^\\d{6,}$"))
        }

        // ----------------------------------------
        // Remove generic payment words
        // ----------------------------------------

        val ignored = setOf(
            "PAY",
            "TO",
            "UPI",
            "TRANSFER",
            "PAYMENT",
            "IMPS",
            "NEFT",
            "RTGS"
        )

        val filtered = withoutNumbers.filter {

            it.uppercase() !in ignored
        }

        if (filtered.isEmpty())
            return "Unknown Transaction"

        return filtered.joinToString(" ")
    }
}
