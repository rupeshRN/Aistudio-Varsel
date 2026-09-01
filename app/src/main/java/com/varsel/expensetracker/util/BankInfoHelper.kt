package com.varsel.expensetracker.util

import com.varsel.expensetracker.domain.model.Transaction

object BankInfoHelper {

    /**
     * Converts a raw or detected bank name to its standard concise abbreviation/short name.
     * Examples:
     * - "Indian Bank" -> "IB"
     * - "ICICI Bank" -> "ICICI"
     * - "HDFC Bank" -> "HDFC"
     * - "State Bank of India" -> "SBI"
     * - "Axis Bank" -> "AXIS"
     * - "Standard Chartered" -> "SC"
     * - "Kotak Mahindra Bank" -> "KOTAK"
     * - "Punjab National Bank" -> "PNB"
     * - "Bank of Baroda" -> "BOB"
     * - "Canara Bank" -> "Canara Bank"
     */
    fun getBankShortName(bankName: String): String {
        val clean = bankName.trim()
        val upper = clean.uppercase()

        return when {
            upper == "IB" || upper.contains("INDIAN BANK") || upper.contains("INDIANBANK") -> "IB"
            upper.contains("ICICI") -> "ICICI"
            upper.contains("HDFC") -> "HDFC"
            upper.contains("SBI") || upper.contains("STATE BANK OF INDIA") -> "SBI"
            upper.contains("AXIS") -> "AXIS"
            upper.contains("STANDARD CHARTERED") || upper == "SC" || upper.contains("STANCHAR") -> "SC"
            upper.contains("KOTAK") -> "KOTAK"
            upper.contains("PUNJAB NATIONAL") || upper.contains("PNB") -> "PNB"
            upper.contains("BARODA") || upper.contains("BOB") -> "BOB"
            upper.contains("CANARA") -> "Canara Bank"
            upper.contains("UNION BANK") || upper.contains("UNIONBANK") -> "Union Bank"
            upper.contains("INDUSIND") -> "IndusInd"
            upper.contains("YES BANK") || upper.contains("YESBANK") -> "Yes Bank"
            upper.contains("FEDERAL") -> "Federal Bank"
            upper.contains("IDFC") -> "IDFC FIRST"
            upper.contains("BANK OF INDIA") || upper == "BOI" -> "BOI"
            upper.contains("CENTRAL BANK") || upper == "CBI" -> "CBI"
            upper.contains("RBL") -> "RBL"
            upper.contains("DBS") -> "DBS"
            upper.contains("HSBC") -> "HSBC"
            upper.contains("CITI") -> "Citi"
            clean.isNotBlank() -> clean
            else -> "Bank"
        }
    }

    /**
     * Detects bank name from transaction metadata.
     */
    fun detectBankForTransaction(transaction: Transaction): String {
        val fingerprint = transaction.transactionFingerprint?.uppercase().orEmpty()
        val ref = transaction.referenceNumber?.uppercase().orEmpty()
        val desc = transaction.description.uppercase()

        return when {
            fingerprint.contains("INDIAN_BANK") || fingerprint.contains("INDIANBANK") || fingerprint.contains("IB_") ||
                ref.contains("INDIAN BANK") || desc.contains("INDIAN BANK") || desc.contains("IDIB") -> "IB"

            fingerprint.contains("ICICI") || ref.contains("ICICI") || desc.contains("ICICI") || desc.contains("ICIC0") -> "ICICI"
            fingerprint.contains("HDFC") || ref.contains("HDFC") || desc.contains("HDFC") || desc.contains("HDFC0") -> "HDFC"
            fingerprint.contains("SBI") || ref.contains("SBI") || desc.contains("SBI") || desc.contains("SBIN0") -> "SBI"
            fingerprint.contains("AXIS") || ref.contains("AXIS") || desc.contains("AXIS") || desc.contains("UTIB0") -> "AXIS"
            fingerprint.contains("SC") || ref.contains("SCBL") || desc.contains("STANDARD CHARTERED") || desc.contains("SCBL0") -> "SC"
            fingerprint.contains("KOTAK") || ref.contains("KKBK") || desc.contains("KOTAK") || desc.contains("KKBK0") -> "KOTAK"
            fingerprint.contains("CANARA") || ref.contains("CNRB") || desc.contains("CANARA") || desc.contains("CNRB0") -> "Canara Bank"
            fingerprint.contains("PNB") || ref.contains("PUNB") || desc.contains("PUNJAB NATIONAL") -> "PNB"
            fingerprint.contains("BOB") || ref.contains("BARB") || desc.contains("BARODA") -> "BOB"
            else -> ""
        }
    }

    /**
     * Formats account number into "BankShortName •••• 1234"
     */
    fun formatAccountBadge(bankName: String, accountNumber: String?): String {
        val shortName = getBankShortName(bankName)
        val cleanAcc = accountNumber?.trim().orEmpty()

        if (cleanAcc.isBlank()) {
            return shortName
        }

        val last4 = if (cleanAcc.length >= 4) {
            cleanAcc.takeLast(4)
        } else {
            cleanAcc
        }

        return "$shortName •••• $last4"
    }
}
