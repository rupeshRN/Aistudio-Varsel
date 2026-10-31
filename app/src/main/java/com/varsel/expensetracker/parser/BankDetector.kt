
package com.varsel.expensetracker.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDetector @Inject constructor(
    private val bankParserRegistry: BankParserRegistry,
    private val heuristicMetadataScanner: HeuristicMetadataScanner = HeuristicMetadataScanner()
) {

    /**
     * Detects the bank using fast header metadata first, then falling back to registered parsers.
     *
     * 1. Heuristic Metadata Scanner checks the top 45 lines for RBI IFSC prefix and official branding.
     * 2. If an unsupported bank is found (e.g. Axis Bank, SBI, Kotak), it immediately returns UnsupportedBank.
     * 3. If a supported bank is definitively identified, it routes to that parser directly.
     * 4. Otherwise, it queries registered parsers.
     */
    fun detectResult(rawText: String): BankDetectionResult {
        // Step 1: High-speed header metadata scan (RBI IFSC + Official Branding)
        when (val metadataResult = heuristicMetadataScanner.scan(rawText)) {
            is BankMetadataResult.Unsupported -> {
                return BankDetectionResult.UnsupportedBank(detectedBankName = metadataResult.detectedBankName)
            }
            is BankMetadataResult.Supported -> {
                val matchedParser = bankParserRegistry.all().firstOrNull { it.bankId == metadataResult.bankId }
                if (matchedParser != null && matchedParser.parser.canParse(rawText)) {
                    return BankDetectionResult.Supported(
                        parser = matchedParser.parser,
                        bankId = matchedParser.bankId,
                        displayName = matchedParser.displayName
                    )
                }
            }
            is BankMetadataResult.Unknown -> {
                // Fall through to general registry scan
            }
        }

        val matchingParsers = bankParserRegistry
            .all()
            .filter { registeredParser ->
                registeredParser.parser.canParse(rawText)
            }

        return when {

            matchingParsers.isEmpty() -> {
                resolveUnsupportedResult(rawText)
            }

            matchingParsers.size == 1 -> {

                val match = matchingParsers.first()

                BankDetectionResult.Supported(
                    parser = match.parser,
                    bankId = match.bankId,
                    displayName = match.displayName
                )
            }

            else -> {

                BankDetectionResult.Ambiguous(
                    parsers = matchingParsers.map { it.parser },
                    possibleBankIds = matchingParsers.map { it.bankId },
                    possibleBankNames = matchingParsers.map { it.displayName }
                )
            }
        }
    }

    private fun resolveUnsupportedResult(rawText: String): BankDetectionResult {
        val upper = rawText.uppercase()

        // 1. Check for known unsupported financial institutions
        val knownUnsupportedBanks = listOf(
            "State Bank of India (SBI)" to listOf("STATE BANK OF INDIA", "SBIN0", "YONO SBI", "ONLINESBI.COM", "SBI"),
            "Axis Bank" to listOf("AXIS BANK", "UTIB0"),
            "Kotak Mahindra Bank" to listOf("KOTAK MAHINDRA", "KKBK0"),
            "Punjab National Bank" to listOf("PUNJAB NATIONAL BANK", "PUNB0"),
            "Bank of Baroda" to listOf("BANK OF BARODA", "BARB0"),
            "Canara Bank" to listOf("CANARA BANK", "CNRB0"),
            "Union Bank of India" to listOf("UNION BANK OF INDIA", "UBIN0")
        )

        for ((bankName, markers) in knownUnsupportedBanks) {
            if (markers.any { marker -> upper.contains(marker) }) {
                return BankDetectionResult.UnsupportedBank(detectedBankName = bankName)
            }
        }

        // 2. Check if a supported bank identity is present, but its format wasn't parsed
        val knownSupportedBankCues = listOf(
            "Indian Bank" to listOf("INDIAN BANK", "IDIB0", "INDIANBANK.IN"),
            "ICICI Bank" to listOf("ICICI BANK", "ICIC0", "ICICIBANK.COM"),
            "HDFC Bank" to listOf("HDFC BANK", "HDFC0", "HDFCBANK.COM")
        )

        for ((bankName, cues) in knownSupportedBankCues) {
            if (cues.any { cue -> upper.contains(cue) }) {
                return BankDetectionResult.UnsupportedFormat(
                    bankName = bankName,
                    reason = "This $bankName statement format is not supported yet."
                )
            }
        }

        // 3. Completely unrecognized format
        return BankDetectionResult.Unsupported(
            reason = "No supported bank statement format was detected."
        )
    }

    /**
     * Compatibility method used by StatementParserEngine.
     */
    fun detect(rawText: String): StatementParser {

        return when (val result = detectResult(rawText)) {

            is BankDetectionResult.Supported -> {
                result.parser
            }

            is BankDetectionResult.Ambiguous -> {
                throw AmbiguousBankException(result.possibleBankNames)
            }

            is BankDetectionResult.UnsupportedBank -> {
                throw UnsupportedBankException(result.detectedBankName)
            }

            is BankDetectionResult.UnsupportedFormat -> {
                throw UnsupportedStatementFormatException(result.bankName, result.reason)
            }

            is BankDetectionResult.Unsupported -> {
                throw UnsupportedStatementFormatException(reason = result.reason)
            }
            
        }
    }
}
