package com.varsel.expensetracker.parser

import javax.inject.Inject
import javax.inject.Singleton

sealed class BankMetadataResult {
    data class Supported(val bankId: String, val displayName: String) : BankMetadataResult()
    data class Unsupported(val detectedBankName: String, val ifscPrefix: String? = null) : BankMetadataResult()
    object Unknown : BankMetadataResult()
}

/**
 * High-speed heuristic metadata scanner that inspects the pre-transaction document header.
 *
 * Design Principles:
 * 1. Strict Boundary Isolation: Slices off raw transaction ledgers before scanning to guarantee
 *    that counterpart banks mentioned in transaction narrations (e.g. "NEFT-IOBA0001234-TRANSFER")
 *    never trigger false positive bank detection.
 * 2. Official Brand Cue Priority: Official institution branding (e.g. "ICICI BANK", "HDFC BANK")
 *    in the header takes precedence over unanchored alphanumeric strings.
 * 3. Contextual Branch Anchoring: IFSC codes are only evaluated if they appear alongside
 *    official branch/account labels (e.g. "IFSC:", "Branch Code:", "RTGS/NEFT IFSC").
 */
@Singleton
class HeuristicMetadataScanner @Inject constructor() {

    private val ifscRegex = Regex("""\b([A-Z]{4})0[A-Z0-9]{6}\b""")

    private val anchoredIfscRegex = Regex(
        """(?:IFSC|RTGS/NEFT\s+IFSC|NEFT\s+IFSC|BRANCH\s+IFSC|IFS\s+CODE|BRANCH\s+CODE)\s*[:\-]?\s*([A-Za-z]{4}0[A-Za-z0-9]{6})""",
        RegexOption.IGNORE_CASE
    )

    companion object {
        // Supported bank IFSC prefixes
        val SUPPORTED_IFSC_MAP = mapOf(
            "IDIB" to Pair("indian_bank", "Indian Bank"),
            "ICIC" to Pair("icici_bank", "ICICI Bank"),
            "HDFC" to Pair("hdfc_bank", "HDFC Bank"),
            "SBIN" to Pair("sbi_bank", "State Bank of India (SBI)"),
            "UTIB" to Pair("axis_bank", "Axis Bank")
        )

        // Comprehensive registry of major Indian Banks operated in India mapped by 4-letter IFSC prefix
        val INDIAN_BANKS_IFSC_MAP = mapOf(
            "KKBK" to "Kotak Mahindra Bank",
            "PUNB" to "Punjab National Bank",
            "BARB" to "Bank of Baroda",
            "CNRB" to "Canara Bank",
            "UBIN" to "Union Bank of India",
            "IOBA" to "Indian Overseas Bank",
            "BKDN" to "Dena Bank",
            "MAHB" to "Bank of Maharashtra",
            "CBIN" to "Central Bank of India",
            "PSIB" to "Punjab & Sind Bank",
            "UCOB" to "UCO Bank",
            "YESB" to "Yes Bank",
            "INDB" to "IndusInd Bank",
            "IDFB" to "IDFC First Bank",
            "FDRL" to "Federal Bank",
            "SIBL" to "South Indian Bank",
            "KARB" to "Karnataka Bank",
            "KVBL" to "Karur Vysya Bank",
            "RBLN" to "RBL Bank",
            "BDBL" to "Bandhan Bank",
            "CSBK" to "CSB Bank",
            "TMBL" to "Tamilnad Mercantile Bank",
            "CITI" to "Citibank India",
            "HSBC" to "HSBC India",
            "SCBL" to "Standard Chartered Bank",
            "DBSS" to "DBS Bank India",
            "AIRP" to "Airtel Payments Bank",
            "PYTM" to "Paytm Payments Bank",
            "IPOS" to "India Post Payments Bank",
            "AUBL" to "AU Small Finance Bank",
            "ESFB" to "Equitas Small Finance Bank",
            "USFB" to "Ujjivan Small Finance Bank",
            "SURY" to "Suryoday Small Finance Bank",
            "JAKA" to "Jammu & Kashmir Bank"
        )

        // Known brand & website cues present in document headers
        val BRAND_HEADER_MAP = listOf(
            "Kotak Mahindra Bank" to listOf("KOTAK MAHINDRA", "KOTAK.COM", "KOTAK BANK", "KOTAK 811"),
            "Punjab National Bank" to listOf("PUNJAB NATIONAL BANK", "PNBINDIA.IN", "PNB NET BANKING"),
            "Bank of Baroda" to listOf("BANK OF BARODA", "BANKOFBARODA.IN", "BOB FINANCIAL", "BOB WORLD"),
            "Canara Bank" to listOf("CANARA BANK", "CANARABANK.COM"),
            "Union Bank of India" to listOf("UNION BANK OF INDIA", "UNIONBANKOFINDIA.CO.IN", "UBI NET"),
            "Indian Overseas Bank" to listOf("INDIAN OVERSEAS BANK", "IOB.IN"),
            "Bank of Maharashtra" to listOf("BANK OF MAHARASHTRA", "BOM.CO.IN"),
            "Central Bank of India" to listOf("CENTRAL BANK OF INDIA", "CENTRALBANKOFINDIA.CO.IN"),
            "Punjab & Sind Bank" to listOf("PUNJAB & SIND BANK", "PUNJAB AND SIND BANK"),
            "UCO Bank" to listOf("UCO BANK", "UCOBANK.COM"),
            "Yes Bank" to listOf("YES BANK", "YESBANK.IN"),
            "IndusInd Bank" to listOf("INDUSIND BANK", "INDUSIND.COM"),
            "IDFC First Bank" to listOf("IDFC FIRST BANK", "IDFC BANK", "IDFCFIRSTBANK.COM"),
            "Federal Bank" to listOf("FEDERAL BANK", "FEDERALBANK.CO.IN"),
            "South Indian Bank" to listOf("SOUTH INDIAN BANK", "SOUTHINDIANBANK.COM"),
            "Karnataka Bank" to listOf("KARNATAKA BANK", "KARNATAKABANK.COM"),
            "Karur Vysya Bank" to listOf("KARUR VYSYA BANK", "KVB.CO.IN"),
            "RBL Bank" to listOf("RBL BANK", "RBLBANK.COM"),
            "Bandhan Bank" to listOf("BANDHAN BANK", "BANDHANBANK.COM"),
            "Standard Chartered Bank" to listOf("STANDARD CHARTERED", "SC.COM/IN"),
            "HSBC India" to listOf("HSBC BANK", "HSBC.CO.IN"),
            "Citibank India" to listOf("CITIBANK", "ONLINE.CITIBANK.CO.IN"),
            "DBS Bank India" to listOf("DBS BANK", "DIGIBANK"),
            "Paytm Payments Bank" to listOf("PAYTM PAYMENTS BANK"),
            "Airtel Payments Bank" to listOf("AIRTEL PAYMENTS BANK"),
            "India Post Payments Bank" to listOf("INDIA POST PAYMENTS BANK", "IPPB")
        )
    }

    /**
     * Isolates the statement header lines by terminating as soon as the transaction ledger
     * table header or transaction rows are encountered.
     */
    fun isolateHeaderLines(rawText: String): List<String> {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val headerLines = mutableListOf<String>()

        for (line in lines) {
            val upper = line.uppercase()

            // Stop immediately if the ledger table column headers are reached
            if (isLedgerTableHeader(upper)) {
                break
            }

            // Stop immediately if an actual transaction row has started
            if (isTransactionRow(upper)) {
                break
            }

            headerLines.add(line)

            // Safety cap: statement headers do not exceed 35 non-blank lines
            if (headerLines.size >= 35) {
                break
            }
        }

        return headerLines
    }

    private fun isLedgerTableHeader(line: String): Boolean {
        val hasDate = line.contains("DATE") || line.contains("TXN DATE") || line.contains("VALUE DATE")
        val hasDesc = line.contains("PARTICULARS") || line.contains("DESCRIPTION") ||
                line.contains("TRANSACTION REMARKS") || line.contains("NARRATION") ||
                line.contains("DETAILS") || line.contains("REMARKS")
        val hasFinancialCol = line.contains("WITHDRAWAL") || line.contains("DEPOSIT") ||
                line.contains("DEBIT") || line.contains("CREDIT") ||
                line.contains("BALANCE") || line.contains("CHQ") || line.contains("CHEQUE")

        if (hasDate && (hasDesc || hasFinancialCol)) {
            return true
        }

        if ((line.contains("WITHDRAWAL") || line.contains("DEBIT")) &&
            (line.contains("DEPOSIT") || line.contains("CREDIT")) &&
            line.contains("BALANCE")
        ) {
            return true
        }

        return false
    }

    private fun isTransactionRow(line: String): Boolean {
        val startsWithDate = Regex(
            """^\s*(?:\d{1,4}[.)]?\s+)?(?:\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}|\d{4}-\d{2}-\d{2})"""
        ).containsMatchIn(line)

        if (!startsWithDate) return false

        val hasTxnIndicators = line.contains("UPI/") || line.contains("NEFT") || line.contains("RTGS") ||
                line.contains("IMPS") || line.contains("TRANSFER") || line.contains("BY CLG") ||
                line.contains("TO CLG") || line.contains("ATM-") || line.contains("POS-") ||
                Regex("""\d+\.\d{2}""").containsMatchIn(line)

        return hasTxnIndicators
    }

    private fun isNarrationOrTransactionLine(line: String): Boolean {
        return line.contains("UPI/") ||
                line.contains("NEFT-") ||
                line.contains("RTGS-") ||
                line.contains("IMPS/") ||
                line.contains("TRANSFER TO") ||
                line.contains("TRANSFER FROM") ||
                line.contains("BY TRANSFER") ||
                line.contains("TO TRANSFER") ||
                line.contains("CHQ NO") ||
                line.contains("REF NO") ||
                line.contains("/CR") ||
                line.contains("/DR")
    }

    private fun extractAnchoredIfsc(line: String): String? {
        val anchoredMatch = anchoredIfscRegex.find(line)
        if (anchoredMatch != null) {
            return anchoredMatch.groupValues[1].uppercase()
        }

        // Secondary check: line contains an IFSC code AND explicitly mentions "IFSC" or "BRANCH"
        if (line.contains("IFSC") || line.contains("BRANCH")) {
            val match = ifscRegex.find(line)
            if (match != null) {
                return match.groupValues[0].uppercase()
            }
        }

        return null
    }

    fun scan(rawText: String): BankMetadataResult {
        if (rawText.isBlank()) return BankMetadataResult.Unknown

        // 1. Extract exclusively the pre-transaction document header
        val headerLines = isolateHeaderLines(rawText)
        val headerText = headerLines.joinToString("\n").uppercase()

        // 2. High-priority official brand identity check in header
        val hasIciciBank = headerText.contains("ICICI BANK") ||
                headerText.contains("WWW.ICICIBANK.COM") ||
                headerText.contains("ICICIBANK")
        val hasHdfcBank = headerText.contains("HDFC BANK") ||
                headerText.contains("WWW.HDFCBANK.COM") ||
                headerText.contains("HDFCBANK")
        val hasIndianBank = (headerText.contains("INDIAN BANK") || headerText.contains("WWW.INDIANBANK.IN")) &&
                !headerText.contains("INDIAN OVERSEAS BANK")
        val hasSbiBank = headerText.contains("STATE BANK OF INDIA") ||
                headerText.contains("YONO SBI") ||
                headerText.contains("ONLINESBI.COM") ||
                headerText.contains("ONLINESBI.SBM") ||
                headerText.contains("SBIN0")
        val hasAxisBank = headerText.contains("AXIS BANK") ||
                headerText.contains("AXISBANK.COM") ||
                headerText.contains("AXIS DIRECT") ||
                headerText.contains("UTIB0") ||
                headerText.contains("UTI BANK")

        val supportedBrandsFound = listOfNotNull(
            if (hasIciciBank) "icici_bank" to "ICICI Bank" else null,
            if (hasHdfcBank) "hdfc_bank" to "HDFC Bank" else null,
            if (hasIndianBank) "indian_bank" to "Indian Bank" else null,
            if (hasSbiBank) "sbi_bank" to "State Bank of India (SBI)" else null,
            if (hasAxisBank) "axis_bank" to "Axis Bank" else null
        )

        if (supportedBrandsFound.size == 1) {
            val match = supportedBrandsFound.first()
            return BankMetadataResult.Supported(match.first, match.second)
        }

        // 3. Check for known unsupported bank brands in header (only if no supported brand matched)
        if (supportedBrandsFound.isEmpty()) {
            for ((bankName, markers) in BRAND_HEADER_MAP) {
                if (markers.any { marker -> headerText.contains(marker) }) {
                    return BankMetadataResult.Unsupported(detectedBankName = bankName)
                }
            }
        }

        // 4. Check for contextually anchored branch IFSC codes in header lines
        for (line in headerLines) {
            val upperLine = line.uppercase()
            if (isNarrationOrTransactionLine(upperLine)) {
                continue
            }

            val anchoredIfsc = extractAnchoredIfsc(upperLine)
            if (anchoredIfsc != null) {
                val prefix = anchoredIfsc.take(4)

                // Check supported IFSC
                val supported = SUPPORTED_IFSC_MAP[prefix]
                if (supported != null) {
                    return BankMetadataResult.Supported(supported.first, supported.second)
                }

                // Check known unsupported Indian bank
                val knownBankName = INDIAN_BANKS_IFSC_MAP[prefix]
                if (knownBankName != null) {
                    return BankMetadataResult.Unsupported(detectedBankName = knownBankName, ifscPrefix = prefix)
                } else {
                    return BankMetadataResult.Unsupported(detectedBankName = "Bank ($prefix)", ifscPrefix = prefix)
                }
            }
        }

        return BankMetadataResult.Unknown
    }
}
