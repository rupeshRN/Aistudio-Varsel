package com.varsel.expensetracker.parser

import javax.inject.Inject
import com.varsel.expensetracker.developer.ParserDiagnosticsManager

class TransactionBlockBuilder @Inject constructor(

    private val statementEndDetector: StatementEndDetector

) {

    private val transactionStartRegex =
    Regex("^\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}.*")

    private val anyDateRegex =
    Regex("\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}")

    fun build(normalizedText: String): List<TransactionBlock> {

        val lines = normalizedText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val transactionLines = mutableListOf<String>()

        val missedDateLines = mutableListOf<String>()

        var accountActivityFound = false

        for (line in lines) {

    val upper = line.uppercase()

    if (!accountActivityFound) {

        if (upper.contains("ACCOUNT ACTIVITY")) {
            accountActivityFound = true
        }

        continue
    }

    // Skip table header
    if (upper.contains("DATE TRANSACTION DETAILS")) {
        continue
    }

    // Stop when footer starts
if (statementEndDetector.isStatementEnd(line)) {

    ParserDiagnosticsManager.latest =
        ParserDiagnosticsManager.latest.copy(

            stopReason =
                "Stopped by statement end\nLine: $line"

        )

    break
}

    transactionLines.add(line)
        }

        val blocks = mutableListOf<TransactionBlock>()

        var current = mutableListOf<String>()

        for (line in transactionLines) {

    // Ignore table header
    if (line.uppercase().startsWith("DATE TRANSACTION")) {
        continue
    }

    val hasDateAnywhere =
    anyDateRegex.containsMatchIn(line)

val startsWithDate =
    transactionStartRegex.matches(line)

if (hasDateAnywhere && !startsWithDate) {

    missedDateLines.add(line)

}

if (startsWithDate) {

    if (current.isNotEmpty()) {

        blocks.add(
            TransactionBlock(current.toList())
        )

        current.clear()
    }
}

current.add(line)
        }
        
if (current.isNotEmpty()) {

    blocks.add(
        TransactionBlock(current.toList())
    )
}

        if (ParserDiagnosticsManager.latest.stopReason == "Not Stopped") {

    ParserDiagnosticsManager.latest =
        ParserDiagnosticsManager.latest.copy(

            stopReason =
                "Reached end of normalized text normally"

        )
        }

        ParserDiagnosticsManager.latest =
    ParserDiagnosticsManager.latest.copy(

        blocksBuilt = blocks.size,

        rejectedBlocks =
            maxOf(
                0,
                ParserDiagnosticsManager.latest.datesDetected - blocks.size
            ),

        missedDateLines = missedDateLines.take(10)

    )

return blocks
    }
}
