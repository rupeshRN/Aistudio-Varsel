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

        // Remove UPI numeric IDs
        text = text.replace(
            Regex("\\b\\d{10,18}\\b"),
            " "
        )

        // Remove UPI handles
        text = text.replace(
    Regex("\\b[A-Za-z0-9._-]+@[A-Za-z0-9._-]+\\b", RegexOption.IGNORE_CASE),
    " "
)

        // Remove INR
        text = text.replace("INR", " ", ignoreCase = true)

        // Remove separators
        text = text.replace("/", " ")
        text = text.replace("-", " ")

        // Remove repeated spaces
        text = text.replace(
            Regex("\\s+"),
            " "
        )

        return text.trim()
    }
}
