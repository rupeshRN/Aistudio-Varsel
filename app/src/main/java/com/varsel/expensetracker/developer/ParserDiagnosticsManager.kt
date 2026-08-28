package com.varsel.expensetracker.developer

object ParserDiagnosticsManager {

    var latest = ParserDiagnostics()

    fun reset() {
        latest = ParserDiagnostics()
    }
}
