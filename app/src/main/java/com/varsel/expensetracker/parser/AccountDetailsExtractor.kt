package com.varsel.expensetracker.parser

import javax.inject.Inject

class AccountDetailsExtractor @Inject constructor() {

    private val accountNumberRegex =
        Regex(
            """(?:Saving\s+|Current\s+)?Account\s+(?:Number|no\.?)\s*[:\-]?\s*([A-Za-z0-9Xx]+)""",
            RegexOption.IGNORE_CASE
        )

    fun extractAccountNumber(
        rawText: String
    ): String? {

        return accountNumberRegex
            .find(rawText)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }
}
