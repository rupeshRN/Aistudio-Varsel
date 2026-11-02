package com.varsel.expensetracker.parser

import javax.inject.Inject

class AccountDetailsExtractor @Inject constructor() {

    private val accountNumberRegex =
        Regex(
            """(?:Saving\s+|Current\s+)?(?:Account\s+(?:Number|no\.?|nos\.?|id)|(?:A/C|A/c|Acc)\s*(?:Number|no\.?|nos\.?|id)?|Account\s*[:\-])\s*[:\-]?\s*([A-Za-z0-9Xx]*\d[A-Za-z0-9Xx]{3,29})""",
            RegexOption.IGNORE_CASE
        )

    private val ifscRegex =
        Regex(
            """(?:IFSC|RTGS/NEFT\s+IFSC|NEFT\s+IFSC|BRANCH\s+IFSC|IFS\s+CODE|BRANCH\s+CODE)(?:\s+Code)?\s*[:\-]?\s*([A-Za-z]{4}0[A-Za-z0-9]{6})""",
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

    fun extractIfscCode(
        rawText: String
    ): String? {
        return ifscRegex
            .find(rawText)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.uppercase()
    }
}
