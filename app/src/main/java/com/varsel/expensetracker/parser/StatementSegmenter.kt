package com.varsel.expensetracker.parser

import javax.inject.Inject

class StatementSegmenter @Inject constructor() {

    private val dateRegex =
        Regex("^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}")

    fun segment(rawText: String): List<StatementBlock> {

        val blocks = mutableListOf<StatementBlock>()

        val current = mutableListOf<String>()

        var started = false

        rawText.lines().forEach { raw ->

            val line = raw.trim()

            if (line.isBlank())
                return@forEach

            if (!started) {

                if (line.uppercase().contains("ACCOUNT ACTIVITY")) {
                    started = true
                }

                return@forEach
            }

            if (line.startsWith("Date Transaction"))
                return@forEach

            if (dateRegex.containsMatchIn(line)) {

                if (current.isNotEmpty()) {

                    blocks.add(
                        StatementBlock(current.toList())
                    )

                    current.clear()
                }
            }

            current.add(line)
        }

        if (current.isNotEmpty()) {

            blocks.add(
                StatementBlock(current.toList())
            )
        }

        return blocks
    }
}
