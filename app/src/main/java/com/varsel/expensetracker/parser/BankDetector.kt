package com.varsel.expensetracker.parser

import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDetector @Inject constructor(
    private val bankParserRegistry: BankParserRegistry,
    private val heuristicMetadataScanner: HeuristicMetadataScanner = HeuristicMetadataScanner(),
    private val accountDetailsExtractor: AccountDetailsExtractor = AccountDetailsExtractor(),
    private val accountHistoryMatcher: AccountHistoryMatcher? = null
) {

    /**
     * Detects the issuing bank using pre-transaction header metadata first,
     * falling back to registered parsers, and finally checking historical account metadata.
     *
     * 1. Heuristic Metadata Scanner checks the isolated document header for official branding and anchored IFSC.
     * 2. Transaction narrations are excluded to avoid misidentifying counterpart banks (e.g. NEFT to IOB).
     * 3. Registered parsers are queried for format match.
     * 4. If auto-detection is inconclusive, matches statement account number against historical snapshots in Room.
     * 5. If unrecognized, returns detailed UnrecognizedAccount result with diagnostic details.
     */
    fun detectResult(rawText: String): BankDetectionResult {
        // Step 1: Pre-transaction header metadata scan (Official Branding + Anchored IFSC)
        when (val metadataResult = heuristicMetadataScanner.scan(rawText)) {
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
            is BankMetadataResult.Unsupported -> {
                // If official branding or contextual branch IFSC identified an unsupported bank,
                // ensure no supported parser has definitive format claim
                val supportedMatches = bankParserRegistry.all().filter { it.parser.canParse(rawText) }
                if (supportedMatches.isEmpty()) {
                    return BankDetectionResult.UnsupportedBank(detectedBankName = metadataResult.detectedBankName)
                }
            }
            is BankMetadataResult.Unknown -> {
                // Fall through to general registry scan
            }
        }

        // Step 2: Query registered parsers
        val matchingParsers = bankParserRegistry
            .all()
            .filter { registeredParser ->
                registeredParser.parser.canParse(rawText)
            }

        if (matchingParsers.size == 1) {
            val match = matchingParsers.first()
            return BankDetectionResult.Supported(
                parser = match.parser,
                bankId = match.bankId,
                displayName = match.displayName
            )
        } else if (matchingParsers.size > 1) {
            return BankDetectionResult.Ambiguous(
                parsers = matchingParsers.map { it.parser },
                possibleBankIds = matchingParsers.map { it.bankId },
                possibleBankNames = matchingParsers.map { it.displayName }
            )
        }

        // Step 3: Check historical account metadata if available
        if (accountHistoryMatcher != null) {
            val historyResult = try {
                runBlocking {
                    accountHistoryMatcher.matchBankByAccountHistory(rawText)
                }
            } catch (e: Exception) {
                null
            }

            if (historyResult is AccountHistoryMatchResult.Matched) {
                val matchedParser = bankParserRegistry.all().firstOrNull { registered ->
                    registered.bankId.equals(historyResult.bankName, ignoreCase = true) ||
                            registered.displayName.equals(historyResult.bankName, ignoreCase = true) ||
                            historyResult.bankName.contains(registered.displayName, ignoreCase = true) ||
                            registered.displayName.contains(historyResult.bankName, ignoreCase = true)
                }

                if (matchedParser != null) {
                    return BankDetectionResult.Supported(
                        parser = matchedParser.parser,
                        bankId = matchedParser.bankId,
                        displayName = matchedParser.displayName
                    )
                }
            }
        }

        // Step 4: Fallback to unsupported bank or unrecognized account resolution
        return resolveUnsupportedResult(rawText)
    }

    private fun resolveUnsupportedResult(rawText: String): BankDetectionResult {
        val headerLines = heuristicMetadataScanner.isolateHeaderLines(rawText)
        val headerText = headerLines.joinToString("\n").uppercase()

        // 1. Check for known unsupported financial institutions in isolated header
        val knownUnsupportedBanks = listOf(
            "State Bank of India (SBI)" to listOf("STATE BANK OF INDIA", "SBIN0", "YONO SBI", "ONLINESBI.COM", "SBI"),
            "Axis Bank" to listOf("AXIS BANK", "UTIB0"),
            "Kotak Mahindra Bank" to listOf("KOTAK MAHINDRA", "KKBK0"),
            "Punjab National Bank" to listOf("PUNJAB NATIONAL BANK", "PUNB0"),
            "Bank of Baroda" to listOf("BANK OF BARODA", "BARB0"),
            "Canara Bank" to listOf("CANARA BANK", "CNRB0"),
            "Union Bank of India" to listOf("UNION BANK OF INDIA", "UBIN0"),
            "Indian Overseas Bank" to listOf("INDIAN OVERSEAS BANK", "IOBA0")
        )

        for ((bankName, markers) in knownUnsupportedBanks) {
            if (markers.any { marker -> headerText.contains(marker) }) {
                return BankDetectionResult.UnsupportedBank(detectedBankName = bankName)
            }
        }

        // 2. Check if a supported bank identity is present in header, but its format was not parsed
        val knownSupportedBankCues = listOf(
            "Indian Bank" to listOf("INDIAN BANK", "IDIB0", "INDIANBANK.IN"),
            "ICICI Bank" to listOf("ICICI BANK", "ICIC0", "ICICIBANK.COM"),
            "HDFC Bank" to listOf("HDFC BANK", "HDFC0", "HDFCBANK.COM")
        )

        for ((bankName, cues) in knownSupportedBankCues) {
            if (cues.any { cue -> headerText.contains(cue) }) {
                return BankDetectionResult.UnsupportedFormat(
                    bankName = bankName,
                    reason = "This $bankName statement format is not supported yet."
                )
            }
        }

        // 3. Statement does not match any known bank or history
        val extractedAccount = accountDetailsExtractor.extractAccountNumber(rawText)

        return BankDetectionResult.UnrecognizedAccount(
            detectedAccountNumber = extractedAccount,
            details = if (!extractedAccount.isNullOrBlank()) {
                "Account number ($extractedAccount) is unrecognized from previous imports and the issuing bank could not be identified from the statement header."
            } else {
                "No supported bank branding or account number could be identified in the document header."
            }
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
            is BankDetectionResult.UnrecognizedAccount -> {
                throw UnrecognizedAccountStatementException(
                    detectedAccountNumber = result.detectedAccountNumber,
                    details = result.details
                )
            }
            is BankDetectionResult.Unsupported -> {
                throw UnsupportedStatementFormatException(reason = result.reason)
            }
        }
    }
}
