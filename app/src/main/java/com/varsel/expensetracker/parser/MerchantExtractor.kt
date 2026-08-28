package com.varsel.expensetracker.parser

import javax.inject.Inject

class MerchantExtractor @Inject constructor() {

    fun clean(text: String): String {

        var result = text

        val keywords = listOf(

            "UPI",
            "IMPS",
            "NEFT",
            "RTGS",

            "ACH",
            "ACHCR",
            "ACHDR",

            "CREDIT",
            "DEBIT",

            "TRANSFER",

            "SBIN",
            "UTIB",
            "YESBOM",

            "ATM",
            "POS",

            "GPAY",
"GOOGLEPAY",
"PHONEPE",
"PAYTM",
"BHIM",

        )

        keywords.forEach {

            result = result.replace(
                Regex("\\b$it\\b", RegexOption.IGNORE_CASE),
                " "
            )
        }

        result = result.replace(
            Regex("\\s+"),
            " "
        )

        return result.trim()
    }
}
