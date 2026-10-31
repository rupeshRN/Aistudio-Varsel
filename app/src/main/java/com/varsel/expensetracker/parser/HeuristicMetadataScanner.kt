package com.varsel.expensetracker.parser

import javax.inject.Inject
import javax.inject.Singleton

sealed class BankMetadataResult {
    data class Supported(val bankId: String, val displayName: String) : BankMetadataResult()
    data class Unsupported(val detectedBankName: String, val ifscPrefix: String? = null) : BankMetadataResult()
    object Unknown : BankMetadataResult()
}

/**
 * High-speed heuristic metadata scanner that inspects the document header (first 45 lines).
 *
 * It uses standardized RBI branch IFSC prefixes and official bank brand / domain markers
 * to deterministically identify the issuing financial institution before deep table
 * or layout parsing is attempted.
 */
@Singleton
class HeuristicMetadataScanner @Inject constructor() {

    private val ifscRegex = Regex("""\b([A-Z]{4})0[A-Z0-9]{6}\b""")

    companion object {
        // Supported bank IFSC prefixes
        val SUPPORTED_IFSC_MAP = mapOf(
            "IDIB" to Pair("indian_bank", "Indian Bank"),
            "ICIC" to Pair("icici_bank", "ICICI Bank"),
            "HDFC" to Pair("hdfc_bank", "HDFC Bank")
        )

        // Comprehensive registry of major Indian Banks operated in India mapped by 4-letter IFSC prefix
        val INDIAN_BANKS_IFSC_MAP = mapOf(
            "SBIN" to "State Bank of India (SBI)",
            "UTIB" to "Axis Bank",
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
            "State Bank of India (SBI)" to listOf("STATE BANK OF INDIA", "YONO SBI", "ONLINESBI.COM", "ONLINESBI.SBM"),
            "Axis Bank" to listOf("AXIS BANK", "AXISBANK.COM", "AXIS DIRECT", "UTI BANK"),
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

    fun scan(rawText: String): BankMetadataResult {
        if (rawText.isBlank()) return BankMetadataResult.Unknown

        // 1. Extract exclusively the document header (first 45 non-blank lines)
        val headerLines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(45)

        val headerText = headerLines.joinToString("\n").uppercase()

        // 2. Check for IFSC codes in header lines
        for (line in headerLines) {
            val upperLine = line.uppercase()
            // Ignore transaction narration lines that accidentally leak into the header
            if (upperLine.startsWith("UPI/") || upperLine.startsWith("NEFT-") || upperLine.startsWith("RTGS-")) {
                continue
            }

            val match = ifscRegex.find(upperLine)
            if (match != null) {
                val prefix = match.groupValues[1]

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
                    // Unknown Indian bank with genuine RBI IFSC
                    return BankMetadataResult.Unsupported(detectedBankName = "Bank ($prefix)", ifscPrefix = prefix)
                }
            }
        }

        // 3. Check for supported bank brands in the header
        val hasIndianBank = headerText.contains("INDIAN BANK") || headerText.contains("WWW.INDIANBANK.IN") || headerText.contains("IDIB")
        val hasIciciBank = headerText.contains("ICICI BANK") || headerText.contains("WWW.ICICIBANK.COM") || headerText.contains("ICIC0")
        val hasHdfcBank = headerText.contains("HDFC BANK") || headerText.contains("WWW.HDFCBANK.COM") || headerText.contains("HDFC0")

        val supportedBrandsFound = listOfNotNull(
            if (hasIndianBank) "indian_bank" to "Indian Bank" else null,
            if (hasIciciBank) "icici_bank" to "ICICI Bank" else null,
            if (hasHdfcBank) "hdfc_bank" to "HDFC Bank" else null
        )

        // 4. Check for known unsupported bank brands in header
        for ((bankName, markers) in BRAND_HEADER_MAP) {
            if (markers.any { marker -> headerText.contains(marker) }) {
                // If a competitor brand like Axis Bank is explicitly present in the header,
                // it takes precedence over secondary payee references
                return BankMetadataResult.Unsupported(detectedBankName = bankName)
            }
        }

        // 5. If supported brand found uniquely in header
        if (supportedBrandsFound.size == 1) {
            val match = supportedBrandsFound.first()
            return BankMetadataResult.Supported(match.first, match.second)
        }

        return BankMetadataResult.Unknown
    }
}
