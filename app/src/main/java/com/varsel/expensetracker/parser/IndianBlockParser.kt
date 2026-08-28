package com.varsel.expensetracker.parser

import javax.inject.Inject

class IndianBlockParser @Inject constructor() {

    private val amountRegex =
        Regex("INR\\s*([\\d,]+\\.\\d{2})")

    fun parse(block: String): ParsedBlock {

        val lines = block
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            throw IllegalArgumentException("Empty transaction block")
        }

        val date = lines.first().substring(0, 11).trim()

        val amounts = amountRegex
            .findAll(block)
            .map {
                it.groupValues[1]
                    .replace(",", "")
                    .toDouble()
            }
            .toList()

        val debit =
            if (amounts.isNotEmpty()) amounts[0] else null

        val balance =
            if (amounts.size >= 2) amounts[1] else null

        val description = lines
            .drop(1)
            .joinToString(" ")
            .replace(amountRegex, "")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return ParsedBlock(
            date = date,
            description = description,
            debit = debit,
            credit = null,
            balance = balance
        )
    }
}
