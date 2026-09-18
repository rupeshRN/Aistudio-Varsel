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
            upper.contains("CASH") -> "Cash"
            upper.contains("WALLET") -> "Wallet"
            clean.isNotBlank() -> clean
            else -> "Bank"
        }
    }

    /**
     * Detects bank name from transaction metadata safely.
     * Prioritizes transaction.bankName, and checks reference numbers (IFSC codes).
     * Never inspects SHA256 hex fingerprints or generic description counterparties.
     */
    fun detectBankForTransaction(transaction: Transaction): String {
        val explicitBank = transaction.bankName?.trim().orEmpty()
        if (explicitBank.isNotBlank() &&
            !explicitBank.equals("Bank Account", ignoreCase = true) &&
            !explicitBank.equals("Bank Statement", ignoreCase = true)
        ) {
            return getBankShortName(explicitBank)
        }

        val ref = transaction.referenceNumber?.uppercase().orEmpty()
        if (ref.isNotBlank()) {
            return when {
                ref.contains("IDIB") -> "IB"
                ref.contains("ICIC0") || ref.startsWith("ICIC") -> "ICICI"
                ref.contains("HDFC0") || ref.startsWith("HDFC") -> "HDFC"
                ref.contains("SBIN0") || ref.startsWith("SBIN") -> "SBI"
                ref.contains("UTIB0") || ref.startsWith("UTIB") -> "AXIS"
                ref.contains("SCBL0") || ref.startsWith("SCBL") -> "SC"
                ref.contains("KKBK0") || ref.startsWith("KKBK") -> "KOTAK"
                ref.contains("CNRB0") || ref.startsWith("CNRB") -> "Canara Bank"
                ref.contains("PUNB0") || ref.startsWith("PUNB") -> "PNB"
                ref.contains("BARB0") || ref.startsWith("BARB") -> "BOB"
                else -> ""
            }
        }

        return ""
    }

    /**
     * Resolves the short bank name for a transaction using explicit bank name,
     * account mapping, or safe reference detection.
     */
    fun resolveBankShortName(
        transaction: Transaction,
        accountBankMap: Map<String, String> = emptyMap()
    ): String {
        // 1. Explicit bank name from transaction
        val explicit = transaction.bankName?.trim().orEmpty()
        if (explicit.isNotBlank() &&
            !explicit.equals("Bank Account", ignoreCase = true) &&
            !explicit.equals("Bank Statement", ignoreCase = true)
        ) {
            return getBankShortName(explicit)
        }

        // 2. Lookup via accountId
        transaction.accountId?.takeIf { it.isNotBlank() }?.let { id ->
            val mapped = accountBankMap[id]
            if (!mapped.isNullOrBlank() &&
                !mapped.equals("Bank Account", ignoreCase = true) &&
                !mapped.equals("Bank Statement", ignoreCase = true)
            ) {
                return getBankShortName(mapped)
            }
        }

        // 3. Lookup via accountLast4
        transaction.accountLast4?.takeIf { it.isNotBlank() }?.let { last4 ->
            val mapped = accountBankMap[last4]
            if (!mapped.isNullOrBlank() &&
                !mapped.equals("Bank Account", ignoreCase = true) &&
                !mapped.equals("Bank Statement", ignoreCase = true)
            ) {
                return getBankShortName(mapped)
            }
        }

        // 4. Safe reference detection
        val detected = detectBankForTransaction(transaction)
        if (detected.isNotBlank()) {
            return getBankShortName(detected)
        }

        return "Bank"
    }

    /**
     * Resolves the display bank name for a transaction.
     */
    fun resolveBankName(transaction: Transaction): String {
        val explicitBank = transaction.bankName?.trim().orEmpty()
        if (explicitBank.isNotBlank() && explicitBank != "Bank Account" && explicitBank != "Bank Statement") {
            return getBankFullName(explicitBank)
        }

        val detected = detectBankForTransaction(transaction)
        if (detected.isNotBlank()) {
            return getBankFullName(detected)
        }

        return "Not specified"
    }

    /**
     * Returns full official bank name.
     */
    fun getBankFullName(bankNameOrShort: String): String {
        val upper = bankNameOrShort.trim().uppercase()
        return when {
            upper == "IB" || upper.contains("INDIAN BANK") || upper.contains("INDIANBANK") -> "Indian Bank"
            upper == "ICICI" || upper.contains("ICICI BANK") -> "ICICI Bank"
            upper == "HDFC" || upper.contains("HDFC BANK") -> "HDFC Bank"
            upper == "SBI" || upper.contains("STATE BANK") -> "State Bank of India (SBI)"
            upper == "AXIS" || upper.contains("AXIS BANK") -> "Axis Bank"
            upper == "SC" || upper.contains("STANDARD CHARTERED") -> "Standard Chartered"
            upper == "KOTAK" || upper.contains("KOTAK") -> "Kotak Mahindra Bank"
            upper == "PNB" || upper.contains("PUNJAB NATIONAL") -> "Punjab National Bank"
            upper == "BOB" || upper.contains("BARODA") -> "Bank of Baroda"
            upper == "CANARA" || upper.contains("CANARA BANK") -> "Canara Bank"
            upper == "UNION BANK" || upper == "UBI" -> "Union Bank of India"
            upper == "IDFC" || upper.contains("IDFC FIRST") -> "IDFC FIRST Bank"
            upper == "BOI" || upper.contains("BANK OF INDIA") -> "Bank of India"
            upper == "CBI" || upper.contains("CENTRAL BANK") -> "Central Bank of India"
            upper == "YES BANK" -> "Yes Bank"
            upper == "FEDERAL" || upper.contains("FEDERAL BANK") -> "Federal Bank"
            upper == "INDUSIND" -> "IndusInd Bank"
            upper.isNotBlank() -> bankNameOrShort
            else -> "Bank Account"
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
