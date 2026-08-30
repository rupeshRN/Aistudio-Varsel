package com.varsel.expensetracker.parser

import javax.inject.Inject

class OcrWordRepair @Inject constructor() {

    fun repair(text: String): String {

        var result = text

        // Merge OCR line breaks inside words
        result = result.replace(
            Regex("([a-z])\\s+([a-z])")
        ) {
            "${it.groupValues[1]}${it.groupValues[2]}"
        }

        // Remove repeated spaces
        result = result.replace(
            Regex("\\s+"),
            " "
        )

        return result.trim()
    }
}
