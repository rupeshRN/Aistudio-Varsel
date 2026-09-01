package com.varsel.expensetracker.parser

import com.varsel.expensetracker.category.Category
import com.varsel.expensetracker.category.CategoryRuleEngine
import com.varsel.expensetracker.category.DescriptionNormalizer
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated parser for HDFC Bank Savings, Current, and Credit Card statements.
 *
 * Supported Layouts & Features:
 * - Table Columns: Date | Narration / Particulars | Chq./Ref.No. | Value Dt | Withdrawal Amt. | Deposit Amt. | Closing Balance
 * - Date Formats: dd/MM/yy, dd/MM/yyyy, dd-MM-yyyy, dd-MM-yy, dd-MMM-yyyy, dd MMM yyyy, yyyy-MM-dd
 * - Transaction channels: UPI, POS, IMPS, NEFT, RTGS, ACH/Mandates, ATM Withdrawals (ATW/NWD), Interest, Charges, Cheques
 */
@Singleton
class HdfcBankParser @Inject constructor(
    private val categoryRuleEngine: CategoryRuleEngine,
    private val descriptionCleaner: DescriptionCleaner,
    private val descriptionNormalizer: DescriptionNormalizer
) : StatementParser {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    )

    // Match line start with optional serial number and a valid HDFC date format
    private val transactionDateRegex = Regex(
        """^\s*(?:(\d{1,4})[.)]?\s+)?(\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}|\d{4}-\d{2}-\d{2}|\d{1,2}\s+[A-Za-z]{3}\s+\d{2,4})""",
        RegexOption.IGNORE_CASE
    )

    private val amountRegex = Regex("""(?<![.\d])([0-9]{1,3}(?:,[0-9]{3})*|\d+)\.(\d{2})(?![.\d])""")

    private var lastParsedRows: List<Pair<Transaction, Double?>> = emptyList()

    override fun canParse(rawText: String): Boolean {
        val upper = rawText.uppercase()
        val header = rawText.lines().take(30).joinToString("\n").uppercase()

        // Exclude other bank statements if explicit
        if ((upper.contains("INDIAN BANK") || upper.contains("INDIANBANK") || upper.contains("IDIB")) && !upper.contains("HDFC")) {
            return false
        }
        if ((upper.contains("ICICI BANK") || upper.contains("ICICIBANK") || upper.contains("ICIC0")) && !upper.contains("HDFC")) {
            return false
        }

        val hasHdfcBrand = header.contains("HDFC") ||
                header.contains("HDFCBANK") ||
                header.contains("HDFC BANK") ||
                header.contains("WWW.HDFCBANK.COM") ||
                upper.contains("HDFC BANK") ||
                upper.contains("HDFCBANK")

        val hasHdfcTableHeaders = (upper.contains("NARRATION") || upper.contains("PARTICULARS")) &&
                (upper.contains("WITHDRAWAL") || upper.contains("DEPOSIT") || upper.contains("CHQ") || upper.contains("CLOSING BALANCE"))

        val hasHdfcDateMatch = Regex("""\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b""").containsMatchIn(rawText) ||
                Regex("""\b\d{1,2}-[A-Za-z]{3}-\d{2,4}\b""").containsMatchIn(rawText)

        return hasHdfcBrand || (hasHdfcTableHeaders && hasHdfcDateMatch)
    }

    override fun parse(rawText: String): List<Transaction> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            lastParsedRows = emptyList()
            return emptyList()
        }

        val cleanLines = extractTransactionTableLines(lines)
        if (cleanLines.isEmpty()) {
            lastParsedRows = emptyList()
            return emptyList()
        }

        val blocks = groupIntoTransactionBlocks(cleanLines)

        val transactions = mutableListOf<Transaction>()
        val parsedRows = mutableListOf<Pair<Transaction, Double?>>()
        var previousBalance: Double? = null

        for (block in blocks) {
            val parsedTx = parseTransactionBlock(block, previousBalance)
            if (parsedTx != null) {
                transactions.add(parsedTx.transaction)
                parsedRows.add(Pair(parsedTx.transaction, parsedTx.balance))
                if (parsedTx.balance != null) {
                    previousBalance = parsedTx.balance
                }
            }
        }

        lastParsedRows = parsedRows
        return transactions
    }

    override fun extractSummary(rawText: String, transactions: List<Transaction>): StatementSummary? {
        if (transactions.isEmpty()) return null

        val rowsWithBalance = lastParsedRows.filter { it.second != null }
        val latestRow = rowsWithBalance.maxByOrNull { it.first.dateTimestamp }
        val earliestRow = rowsWithBalance.minByOrNull { it.first.dateTimestamp }

        val endingBalance = latestRow?.second
        val openingBalance = earliestRow?.let { (tx, balance) ->
            if (balance != null) {
                if (tx.type == TransactionType.INCOME) balance - tx.amount else balance + tx.amount
            } else null
        }

        val totalCredits = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalDebits = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val startDate = transactions.minOfOrNull { it.dateTimestamp }
        val endDate = transactions.maxOfOrNull { it.dateTimestamp }

        return StatementSummary(
            statementStartDate = startDate,
            statementEndDate = endDate,
            openingBalance = openingBalance,
            totalCredits = totalCredits,
            totalDebits = totalDebits,
            endingBalance = endingBalance
        )
    }

    private fun extractTransactionTableLines(lines: List<String>): List<String> {
        val tableLines = mutableListOf<String>()
        var tableStarted = false

        for (line in lines) {
            val upper = line.uppercase()

            if (!tableStarted) {
                if ((upper.contains("NARRATION") ||
                    upper.contains("TRANSACTION DESCRIPTION") ||
                    upper.contains("PARTICULARS") ||
                    upper.contains("WITHDRAWAL AMT") ||
                    upper.contains("DEPOSIT AMT") ||
                    upper.contains("VALUE DT") ||
                    upper.contains("CLOSING BALANCE") ||
                    transactionDateRegex.containsMatchIn(line)) && !isFinalStatementSummary(upper) && !isStatementNoise(upper)
                ) {
                    tableStarted = true
                    if (transactionDateRegex.containsMatchIn(line) && !isTableHeader(upper)) {
                        tableLines.add(line)
                    }
                }
                continue
            }

            if (isFinalStatementSummary(upper)) {
                // Statement table finished! Stop adding subsequent summary lines
                break
            }

            if (isTableHeader(upper) || isStatementNoise(upper)) {
                continue
            }

            tableLines.add(line)
        }

        if (tableLines.isEmpty()) {
            return lines.filter { line ->
                val upper = line.uppercase()
                !isTableHeader(upper) && !isStatementNoise(upper) && !isFinalStatementSummary(upper)
            }
        }

        return tableLines
    }

    private fun isFinalStatementSummary(upper: String): Boolean {
        return upper.contains("STATEMENT SUMMARY") ||
                upper.contains("SUMMARY OF ACCOUNT") ||
                (upper.contains("OPENING BALANCE") && (upper.contains("DR COUNT") || upper.contains("CR COUNT") || upper.contains("CLOSING BAL"))) ||
                upper.contains("GENERATED ON") ||
                upper.contains("GENERATED BY") ||
                upper.contains("REQUESTING BRANCH")
    }

    private fun isTableHeader(upper: String): Boolean {
        return (upper.contains("NARRATION") && upper.contains("DATE")) ||
                (upper.contains("WITHDRAWAL") && upper.contains("DEPOSIT")) ||
                (upper.contains("PARTICULARS") && upper.contains("CHQ")) ||
                upper.contains("STATEMENT OF ACCOUNT") ||
                upper.contains("ACCOUNT STATEMENT")
    }

    private fun isStatementNoise(upper: String): Boolean {
        return upper.contains("HDFC BANK LIMITED") ||
                upper.contains("WWW.HDFCBANK.COM") ||
                upper.contains("THIS IS A COMPUTER GENERATED STATEMENT") ||
                upper.contains("REGISTERED OFFICE") ||
                upper.contains("CONTENTS OF THIS STATEMENT WILL BE CONSIDERED CORRECT") ||
                upper.contains("NOT REQUIRE SIGNATURE") ||
                upper.contains("CLOSING BALANCE INCLUDES FUNDS") ||
                upper.contains("STATE ACCOUNT BRANCH GSTN") ||
                upper.contains("HDFC BANK GSTIN") ||
                upper.contains("TOTAL DEBITS") ||
                upper.contains("TOTAL CREDITS") ||
                upper.matches(Regex(""".*PAGE\s+\d+\s+OF\s+\d+.*""")) ||
                upper.matches(Regex(""".*\d+\s+OF\s+\d+.*"""))
    }

    private fun groupIntoTransactionBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var currentBlock: MutableList<String>? = null

        for (line in lines) {
            val match = transactionDateRegex.find(line)
            if (match != null) {
                currentBlock = mutableListOf(line)
                blocks.add(currentBlock)
            } else {
                if (currentBlock != null) {
                    currentBlock.add(line)
                }
            }
        }

        return blocks
    }

    private data class ParsedBlockResult(
        val transaction: Transaction,
        val balance: Double?
    )

    private fun parseTransactionBlock(
        blockLines: List<String>,
        previousBalance: Double?
    ): ParsedBlockResult? {
        if (blockLines.isEmpty()) return null

        val firstLine = blockLines.first()
        val dateMatch = transactionDateRegex.find(firstLine) ?: return null
        val rawDateStr = dateMatch.groupValues[2]
        val dateTimestamp = DateParserUtils.parseDate(rawDateStr) ?: return null

        val fullBlockText = blockLines.joinToString("\n")
        val textWithoutDates = fullBlockText.replace(Regex("""\b\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}\b"""), " ")
            .replace(Regex("""\b\d{4}-\d{2}-\d{2}\b"""), " ")

        // Extract numbers with 2 decimal places
        val amountMatches = amountRegex.findAll(textWithoutDates)
            .mapNotNull { match ->
                val strVal = match.value.replace(",", "")
                val doubleVal = strVal.toDoubleOrNull()
                if (doubleVal != null) Pair(doubleVal, match.range) else null
            }
            .toList()

        if (amountMatches.isEmpty()) return null

        val parsedAmount: Double
        val transactionType: TransactionType
        val closingBalance: Double?

        val upperFull = fullBlockText.uppercase()

        if (amountMatches.size >= 3) {
            // Standard HDFC Table: [Withdrawal, Deposit, Balance]
            val firstAmt = amountMatches[amountMatches.size - 3].first
            val secondAmt = amountMatches[amountMatches.size - 2].first
            val thirdAmt = amountMatches.last().first

            closingBalance = thirdAmt

            if (firstAmt > 0.001 && (secondAmt < 0.001 || firstAmt != secondAmt)) {
                parsedAmount = firstAmt
                transactionType = TransactionType.EXPENSE
            } else if (secondAmt > 0.001) {
                parsedAmount = secondAmt
                transactionType = TransactionType.INCOME
            } else {
                parsedAmount = firstAmt
                transactionType = TransactionType.EXPENSE
            }
        } else if (amountMatches.size == 2) {
            val firstAmt = amountMatches[0].first
            val secondAmt = amountMatches[1].first

            closingBalance = secondAmt

            if (upperFull.contains(" CR") || upperFull.contains("(CR)") || upperFull.contains("CREDIT") || upperFull.contains("INTEREST PAID")) {
                parsedAmount = firstAmt
                transactionType = TransactionType.INCOME
            } else if (upperFull.contains(" DR") || upperFull.contains("(DR)") || upperFull.contains("DEBIT")) {
                parsedAmount = firstAmt
                transactionType = TransactionType.EXPENSE
            } else if (previousBalance != null) {
                val diff = secondAmt - previousBalance
                if (diff > 0.01) {
                    parsedAmount = firstAmt
                    transactionType = TransactionType.INCOME
                } else {
                    parsedAmount = firstAmt
                    transactionType = TransactionType.EXPENSE
                }
            } else {
                parsedAmount = firstAmt
                transactionType = TransactionType.EXPENSE
            }
        } else {
            parsedAmount = amountMatches.first().first
            closingBalance = null
            transactionType = if (upperFull.contains(" CR") || upperFull.contains("CREDIT") || upperFull.contains("REFUND") || upperFull.contains("INTEREST PAID")) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
        }

        val rawDescription = extractNarration(fullBlockText, dateMatch.value, amountMatches.map { it.first })
        val remarksInfo = parseRemarks(rawDescription, transactionType == TransactionType.INCOME)

        val isIncome = (transactionType == TransactionType.INCOME || transactionType == TransactionType.CREDIT)
        val categoryResult = categoryRuleEngine.categorize(remarksInfo.displayDescription, isIncome)

        val upperDesc = remarksInfo.displayDescription.uppercase()
        val finalCategory = when {
            isIncome && (upperDesc.contains("INTEREST") || upperDesc.contains("INT PAID")) -> Category.OTHER_INCOME
            isIncome && upperDesc.contains("SALARY") -> Category.SALARY
            !isIncome && (upperDesc.contains("MIN BAL") || upperDesc.contains("MINIMUM BAL") || upperDesc.contains("CHARGE")) -> Category.UTILITIES
            else -> categoryResult.category
        }

        val transaction = Transaction(
            id = 0,
            amount = parsedAmount,
            type = transactionType,
            description = remarksInfo.displayDescription,
            category = finalCategory,
            dateTimestamp = dateTimestamp,
            referenceNumber = remarksInfo.referenceNumber,
            bankName = "HDFC Bank"
        )

        return ParsedBlockResult(transaction, closingBalance)
    }

    private fun extractNarration(
        fullText: String,
        dateMatchStr: String,
        amounts: List<Double>
    ): String {
        var text = fullText
        text = text.replace(dateMatchStr, " ")

        for (amt in amounts) {
            val formatted1 = String.format(Locale.ENGLISH, "%.2f", amt)
            val formatted2 = String.format(Locale.ENGLISH, "%,.2f", amt)
            text = text.replace(formatted1, " ")
            text = text.replace(formatted2, " ")
            text = text.replace(amt.toString(), " ")
        }

        // Remove Value Dt or other embedded dates
        text = text.replace(Regex("""\b\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}\b"""), " ")
        text = text.replace(Regex("""\b\d{4}-\d{2}-\d{2}\b"""), " ")

        // Remove 10+ repeated zeroes (e.g., 000000000000000 in Chq/Ref column)
        text = text.replace(Regex("""\b0{5,}\b"""), " ")
        text = text.replace(Regex("""\b0000[A-Za-z0-9]+\b"""), " ")

        // Truncate at footer keywords if any slipped in
        val footerKeywords = listOf(
            "STATEMENT SUMMARY", "Opening Balance", "Dr Count", "Cr Count", "Closing Bal",
            "Generated On", "Generated By", "Requesting Branch", "not require signature",
            "Closing balance includes funds", "State account branch GSTN", "HDFC BANK LIMITED",
            "Registered Office Address", "Contents of this statement"
        )
        for (kw in footerKeywords) {
            val pos = text.indexOf(kw, ignoreCase = true)
            if (pos >= 0) {
                text = text.substring(0, pos)
            }
        }

        text = text.replace(Regex("""\s+"""), " ").trim()
        return text.ifBlank { "HDFC Transaction" }
    }

    private data class RemarksInfo(
        val displayDescription: String,
        val merchant: String?,
        val referenceNumber: String?
    )

    private fun parseRemarks(rawDescription: String, isIncome: Boolean = false): RemarksInfo {
        val cleanText = rawDescription.replace(Regex("""\s+"""), " ").trim()
        val upper = cleanText.uppercase()

        // 1. INTEREST PAID TILL DD-MMM-YYYY
        if (upper.contains("INTEREST PAID") || upper.contains("INT. PAID") || upper.contains("CREDIT INTEREST")) {
            val titleCased = cleanText.split(" ").joinToString(" ") { word ->
                if (word.uppercase() == "TILL") "till"
                else word.lowercase().replaceFirstChar { it.uppercase() }
            }
            return RemarksInfo(titleCased, "HDFC Bank", extractRefNumber(cleanText))
        }

        // 2. MIN BAL MAINTAIN / MINIMUM BALANCE CHARGES
        if (upper.contains("MIN BAL") || upper.contains("MINIMUM BAL")) {
            val titleCased = "Min Balance Maintenance"
            return RemarksInfo(titleCased, "HDFC Bank", extractRefNumber(cleanText))
        }

        // 3. Hyphenated Narration Format (Payment Mode - Ref - Name - Bank - Acc - Reason)
        if (cleanText.contains("-")) {
            return parseHyphenatedHdfcNarration(cleanText)
        }

        // 4. POS / Card purchases
        if (upper.startsWith("POS") || upper.contains("POS ")) {
            var merchantName: String? = null
            val parts = cleanText.split(" ")
            val merchantParts = parts.filter { part ->
                val pUpper = part.uppercase()
                !pUpper.startsWith("POS") &&
                        !pUpper.matches(Regex("""\d+""")) &&
                        !pUpper.contains("XXXX") &&
                        pUpper.length > 1
            }
            if (merchantParts.isNotEmpty()) {
                merchantName = merchantParts.joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
                return RemarksInfo("POS: $merchantName", merchantName, extractRefNumber(cleanText))
            }
        }

        // 5. Default Title Case
        val defaultTitle = cleanText.split(" ").joinToString(" ") { word ->
            if (word.length <= 3 && word.all { it.isLetter() }) word.uppercase()
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }

        return RemarksInfo(
            displayDescription = defaultTitle,
            merchant = null,
            referenceNumber = extractRefNumber(cleanText)
        )
    }

    private fun parseHyphenatedHdfcNarration(cleanText: String): RemarksInfo {
        val rawParts = cleanText.split("-").map { it.trim() }.filter { it.isNotBlank() }
        if (rawParts.size < 2) {
            val titleCased = formatTitleCase(cleanText)
            return RemarksInfo(titleCased, null, extractRefNumber(cleanText))
        }

        var refNumber: String? = null
        var mode: String? = null
        val textParts = mutableListOf<String>()

        val knownModes = setOf(
            "UPI", "IMPS", "TIMPS", "NEFT", "RTGS", "FT", "POS", "ACH", "ATW", "INB",
            "NWD", "REV", "VISA", "MC", "CHQ", "PAY", "BILL"
        )
        val bankCodeRegex = Regex("""^(HDFC|ICIC|SBIN|UTIB|YESB|KKBK|BARB|CNRB|PUNB|INDB|FDRL|IDFB|CITI|HSBC|SCBL|PAYTM|PYTM|STATE|AXIS|BOI)$""", RegexOption.IGNORE_CASE)
        val maskedAccRegex = Regex("""^[X*]+\d*$|^[X*\d]{8,}$""", RegexOption.IGNORE_CASE)

        for ((index, part) in rawParts.withIndex()) {
            val upperPart = part.uppercase()

            // Check if first part is Payment Mode
            if (index == 0 && knownModes.contains(upperPart)) {
                mode = upperPart
                continue
            }

            // Check if part is a reference number
            if (refNumber == null && (part.matches(Regex("""\d{10,18}""")) || upperPart.startsWith("TIMPS") || upperPart.startsWith("000FT") || upperPart.startsWith("UTRN"))) {
                refNumber = part.replace(Regex("""^0+"""), "")
                if (refNumber.length < 6) refNumber = part
                continue
            }

            // Check if part is bank code or masked account
            if (bankCodeRegex.matches(upperPart) || maskedAccRegex.matches(upperPart)) {
                continue
            }

            // Ignore pure numbers or zero padding
            if (part.matches(Regex("""\d+"""))) {
                if (refNumber == null && part.length >= 8) {
                    refNumber = part
                }
                continue
            }

            // Meaningful text part (Name or Reason)
            textParts.add(part)
        }

        if (refNumber == null) {
            refNumber = extractRefNumber(cleanText)
        }

        var reason: String? = null
        var name: String? = null

        if (textParts.size >= 2) {
            // HDFC layout: Payment mode - Ref - Receiver/Sender Name - Bank - Acc - Reason
            name = formatTitleCase(textParts.first())
            reason = formatTitleCase(textParts.last())
        } else if (textParts.size == 1) {
            // Unconditionally use the text part as the reason/purpose without keyword restriction
            reason = formatTitleCase(textParts.first())
        }

        val displayDesc = when {
            reason != null -> reason
            name != null -> if (mode != null && mode != "FT") "$mode: $name" else name
            mode != null -> "$mode Transfer"
            else -> formatTitleCase(cleanText)
        }

        return RemarksInfo(
            displayDescription = displayDesc,
            merchant = name,
            referenceNumber = refNumber
        )
    }

    private fun extractRefNumber(text: String): String? {
        val utrMatch = Regex("""\b\d{12}\b""").find(text)
        if (utrMatch != null) return utrMatch.value
        val impsMatch = Regex("""(?:IMPS|TIMPS)[/-]?(\d{10,12})""", RegexOption.IGNORE_CASE).find(text)
        if (impsMatch != null) return impsMatch.groupValues[1]
        val refMatch = Regex("""(?:NEFT|RTGS)[/-]?([A-Z0-9]{10,22})""", RegexOption.IGNORE_CASE).find(text)
        if (refMatch != null) return refMatch.groupValues[1]
        return null
    }

    private fun formatTitleCase(str: String): String {
        val upper = str.trim().uppercase()
        if (upper == "MIN BAL MAINTAIN" || upper == "MIN BAL MAINTENANCE" || upper.contains("MIN BAL")) {
            return "Min Balance Maintenance"
        }
        return str.trim().split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            val wUpper = word.uppercase()
            if (wUpper == "FOR" || wUpper == "TILL" || wUpper == "AND" || wUpper == "TO" || wUpper == "OF") {
                wUpper.lowercase()
            } else if (wUpper.length <= 3 && wUpper.all { it.isLetter() }) {
                wUpper
            } else {
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            }
        }
    }
}
