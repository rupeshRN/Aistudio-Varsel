package com.varsel.expensetracker.developer

data class ParserDiagnostics(

    val pdfPages: Int = 0,

    val rawLines: Int = 0,

    val normalizedLines: Int = 0,

    val datesDetected: Int = 0,

    val blocksBuilt: Int = 0,

    val transactionsParsed: Int = 0,

    val calculatedCredits: Double = 0.0,

val statementCredits: Double? = null,

val calculatedDebits: Double = 0.0,

val statementDebits: Double? = null,

val creditDifference: Double = 0.0,

val debitDifference: Double = 0.0,

    val rejectedBlocks: Int = 0,

    val lastParsedDate: String = "—",

    val notes: List<String> = emptyList(),

    val missedDateLines: List<String> = emptyList(),

    val stopReason: String = "Not Stopped",

    val tokenizerOutput: List<String> = emptyList()
)
