package com.varsel.expensetracker.parser

import javax.inject.Inject

class DescriptionCleaner @Inject constructor() {

    fun clean(description: String): String {
        var text = description

        // Remove IFSC-like bank codes
        text = text.replace(
            Regex("\\b[A-Z]{4}0[A-Z0-9]{6}\\b"),
            " "
        )

        // Remove masked account/reference numbers
        text = text.replace(
            Regex("X{3,}\\d*"),
            " "
        )

        // Remove UPI / IMPS / RRN reference numeric IDs
        text = text.replace(
            Regex("\\b\\d{8,20}\\b"),
            " "
        )

        // Remove UPI handles (e.g. user@okhdfcbank, merchant@paytm)
        text = text.replace(
            Regex("\\b[A-Za-z0-9._-]+@[A-Za-z0-9._-]+\\b", RegexOption.IGNORE_CASE),
            " "
        )

        // Remove common Indian banking noise prefixes
        val noisePrefixes = listOf(
            "UPI/", "UPI-", "UPI ", "IMPS-", "IMPS/", "NEFT-", "NEFT/", "RTGS-", "RTGS/",
            "NACH/", "NACH-", "ACH/", "ACH-", "POS ", "POS/", "E-COM/", "BIL/", "IN/",
            "REV-", "DR-", "CR-", "PAY TO ", "PAID TO ", "TRANSFER TO ", "COLLECT FROM ",
            "BY TRANSFER-", "TO TRANSFER-"
        )
        for (prefix in noisePrefixes) {
            text = text.replace(Regex("\\b$prefix", RegexOption.IGNORE_CASE), " ")
        }

        // Remove INR
        text = text.replace("INR", " ", ignoreCase = true)

        // Remove separators
        text = text.replace("/", " ")
        text = text.replace("-", " ")
        text = text.replace(":", " ")
        text = text.replace("_", " ")

        // Remove repeated spaces
        text = text.replace(
            Regex("\\s+"),
            " "
        )

        return text.trim()
    }
}
