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
 * Parser for ICICI Bank Savings & Current account statements (PDF & OCR).
 *
 * Supported formats:
 * - Table Columns: S No. | Transaction Date | Cheque Number | Transaction Remarks | Withdrawal Amount (INR) | Deposit Amount (INR) | Balance (INR)
 * - Date Formats: dd.MM.yyyy, dd/MM/yyyy, dd-MM-yyyy
 * - Transaction channels: MMT/IMPS, NEFT, RTGS, UPI, BIL/ONL, POS/Card, ATM, Salary Credits, Cheques
 */
@Singleton
class IciciBankParser @Inject constructor(
    private val categoryRuleEngine: CategoryRuleEngine,
    private val descriptionCleaner: DescriptionCleaner,
    private val descriptionNormalizer: DescriptionNormalizer
) : StatementParser {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    )

    // Regex for date at start of transaction line (with optional serial number)
    private val transactionDateRegex = Regex(
        """^\s*(?:(\d{1,4})[.)]?\s+)?(\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}|\d{4}-\d{2}-\d{2}|\d{1,2}\s+[A-Za-z]{3}\s+\d{4})""",
        RegexOption.IGNORE_CASE
    )

    // Regex to match monetary amounts with 2 decimal places (ensuring not part of a date or version)
    private val amountRegex = Regex("""(?<![.\d])([0-9]{1,3}(?:,[0-9]{3})*|\d+)\.(\d{2})(?![.\d])""")

    private var lastParsedRows: List<Pair<Transaction, Double?>> = emptyList()

    override fun canParse(rawText: String): Boolean {
        val upper = rawText.uppercase()

        val isIndianBankStatement = upper.contains("INDIAN BANK") ||
                (upper.contains("ACCOUNT ACTIVITY") && upper.contains("DATE TRANSACTION DETAILS")) ||
                (upper.contains("IDIB") && upper.contains("DATE TRANSACTION DETAILS"))

        if (isIndianBankStatement && !upper.contains("ICICI BANK") && !upper.contains("ICICIBANK")) {
            return false
        }

        val hasIciciBrand = upper.contains("ICICI BANK") ||
                upper.contains("ICICI.BANK") ||
                upper.contains("ICICIBANK") ||
                upper.contains("TEAM ICICI BANK") ||
                upper.contains("WWW.ICICI.BANK.IN") ||
                (Regex("""\bICICI\b""", RegexOption.IGNORE_CASE).containsMatchIn(rawText) && !upper.contains("@ICICI"))

        val hasIciciTable = upper.contains("TRANSACTION REMARKS") ||
                (upper.contains("WITHDRAWAL") && upper.contains("DEPOSIT") && upper.contains("BALANCE"))

        val hasNumericDates = Regex("""\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b""").containsMatchIn(rawText)

        return hasIciciBrand || (hasIciciTable && hasNumericDates) || (hasNumericDates && !upper.contains("INDIAN BANK"))
    }

    override fun parse(rawText: String): List<Transaction> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            lastParsedRows = emptyList()
            return emptyList()
        }

        // 1. Filter out pre-table headers and page-break artifacts across all pages
        val cleanLines = extractTransactionTableLines(lines)
        if (cleanLines.isEmpty()) {
            lastParsedRows = emptyList()
            return emptyList()
        }

        // 2. Group lines into transaction blocks across all pages till end
        val blocks = groupIntoTransactionBlocks(cleanLines)

        // 3. Parse each block into a Transaction object
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
        val latestRow = rowsWithBalance.lastOrNull()
        val earliestRow = rowsWithBalance.firstOrNull()

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

            // Detect table header start
            if (!tableStarted) {
                if (upper.contains("TRANSACTION REMARKS") ||
                    upper.contains("TRANSACTION DATE") ||
                    upper.contains("WITHDRAWAL AMOUNT") ||
                    upper.contains("DEPOSIT AMOUNT") ||
                    upper.contains("CHEQUE NUMBER") ||
                    upper.contains("PARTICULARS") ||
                    upper.contains("NARRATION") ||
                    transactionDateRegex.containsMatchIn(line)
                ) {
                    tableStarted = true
                    // If this line contains a transaction row, include it
                    if (transactionDateRegex.containsMatchIn(line) && !isTableHeader(upper)) {
                        tableLines.add(line)
                    }
                }
                continue
            }

            // Skip repeated page header lines and disclaimer noise in multi-page statements
            if (isStatementHeaderOrFooter(upper)) {
                continue
            }

            tableLines.add(line)
        }

        // If tableStarted never triggered but document has date lines
        if (tableLines.isEmpty()) {
            return lines.filter { line ->
                val upper = line.uppercase()
                !isStatementHeaderOrFooter(upper)
            }
        }

        return tableLines
    }

    private fun isTableHeader(upper: String): Boolean {
        return (upper.contains("TRANSACTION REMARKS") && upper.contains("AMOUNT")) ||
                (upper.contains("WITHDRAWAL AMOUNT") && upper.contains("DEPOSIT AMOUNT")) ||
                upper.contains("STATEMENT OF TRANSACTIONS IN") ||
                upper.contains("STATEMENT OF TRANSACTIONS") ||
                (upper.contains("S NO.") && upper.contains("DATE")) ||
                (upper.contains("VALUE DATE") && upper.contains("TRANSACTION DATE"))
    }

    private fun isStatementNoise(upper: String): Boolean {
        return upper.contains("YOUR BASE BRANCH") ||
                upper.contains("ICICI BANK LIMITED") ||
                upper.contains("SINCERELY, TEAM ICICI BANK") ||
                upper.contains("TEAM ICICI BANK") ||
                upper.contains("LEGENDS FOR TRANSACTIONS") ||
                upper.contains("THIS IS A SYSTEM GENERATED STATEMENT") ||
                upper.contains("NEVER SHARE YOUR OTP") ||
                upper.contains("WWW.ICICI.BANK.IN") ||
                upper.contains("WWW.ICICIBANK.COM") ||
                upper.contains("DIAL YOUR BANK") ||
                upper.matches(Regex(""".*PAGE\s+\d+.*""")) ||
                upper.matches(Regex(""".*\d+\s+OF\s+\d+.*"""))
    }

    private fun isStatementHeaderOrFooter(upper: String): Boolean {
        if (isTableHeader(upper) || isStatementNoise(upper)) {
            return true
        }
        return upper.contains("STATEMENT OF TRANSACTIONS") ||
                upper.contains("ACCOUNT NUMBER") ||
                upper.contains("ACCOUNT NO") ||
                upper.contains("A/C NO") ||
                upper.contains("A/C NUMBER") ||
                upper.contains("ACC NO") ||
                upper.contains("ACCOUNT HOLDER") ||
                upper.contains("CUSTOMER ID") ||
                upper.contains("CUST ID") ||
                upper.contains("JOINT HOLDER") ||
                upper.contains("NOMINEE") ||
                upper.contains("IFSC") ||
                upper.contains("MICR") ||
                upper.contains("BRANCH CODE") ||
                upper.contains("BRANCH :") ||
                upper.contains("BRANCH:") ||
                upper.contains("STATEMENT PERIOD") ||
                upper.contains("STATEMENT FROM") ||
                upper.contains("FROM DATE") ||
                upper.contains("TO DATE") ||
                upper.contains("ACCOUNT TYPE") ||
                upper.contains("CURRENCY :") ||
                upper.contains("CURRENCY:") ||
                upper.contains("STATUS :") ||
                upper.contains("ADDRESS :") ||
                upper.contains("PHONE NO") ||
                upper.contains("EMAIL ID")
    }

    private fun groupIntoTransactionBlocks(lines: List<String>): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var currentBlock: MutableList<String>? = null

        for (line in lines) {
            val upper = line.uppercase()
            if (isStatementHeaderOrFooter(upper)) {
                continue
            }

            val match = transactionDateRegex.find(line)
            if (match != null) {
                // New transaction starts here
                currentBlock = mutableListOf(line)
                blocks.add(currentBlock)
            } else {
                // Continuation line for the current transaction
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
        val dateTimestamp = parseDate(rawDateStr) ?: return null

        val fullBlockText = blockLines.joinToString("\n")
        val textWithoutDates = fullBlockText.replace(Regex("""\b\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}\b"""), " ")
            .replace(Regex("""\b\d{4}-\d{2}-\d{2}\b"""), " ")

        // 1. Extract Amounts and Running Balance
        val amountsFound = amountRegex.findAll(textWithoutDates).mapNotNull { match ->
            val numStr = (match.groupValues[1] + "." + match.groupValues[2]).replace(",", "")
            numStr.toDoubleOrNull()
        }.toList()

        if (amountsFound.isEmpty()) return null

        var txAmount: Double
        var runningBalance: Double? = null
        var txType: TransactionType = TransactionType.EXPENSE

        if (amountsFound.size >= 3) {
            val withdrawal = amountsFound[0]
            val deposit = amountsFound[1]
            val balance = amountsFound[2]
            runningBalance = balance

            if (deposit > 0.0 && withdrawal == 0.0) {
                txAmount = deposit
                txType = TransactionType.INCOME
            } else if (withdrawal > 0.0 && deposit == 0.0) {
                txAmount = withdrawal
                txType = TransactionType.EXPENSE
            } else if (deposit > 0.0) {
                txAmount = deposit
                txType = TransactionType.INCOME
            } else {
                txAmount = withdrawal
                txType = TransactionType.EXPENSE
            }
        } else if (amountsFound.size == 2) {
            txAmount = amountsFound[0]
            runningBalance = amountsFound[1]

            // Determine type by balance progression if available
            if (previousBalance != null && runningBalance != null) {
                if (runningBalance > previousBalance + 0.01) {
                    txType = TransactionType.INCOME
                } else if (runningBalance < previousBalance - 0.01) {
                    txType = TransactionType.EXPENSE
                } else {
                    txType = inferTypeFromText(fullBlockText)
                }
            } else {
                txType = inferTypeFromText(fullBlockText)
            }
        } else {
            txAmount = amountsFound[0]
            txType = inferTypeFromText(fullBlockText)
        }

        // 2. Extract Remarks, Payee/Merchant, and Reference Number
        val remarksInfo = extractRemarksInfo(blockLines, dateMatch.value)

        // 3. Categorization
        val isIncome = (txType == TransactionType.INCOME || txType == TransactionType.CREDIT)
        val categoryResult = categoryRuleEngine.categorize(remarksInfo.displayDescription, isIncome)

        // Override category for salary credits if remarks explicitly indicate Salary
        val finalCategory = if (txType == TransactionType.INCOME &&
            (fullBlockText.contains("SALARY", ignoreCase = true) || remarksInfo.displayDescription.contains("Salary", ignoreCase = true))
        ) {
            Category.SALARY
        } else {
            categoryResult.category
        }

        val transaction = Transaction(
            amount = txAmount,
            type = txType,
            description = remarksInfo.displayDescription,
            category = finalCategory,
            dateTimestamp = dateTimestamp,
            referenceNumber = remarksInfo.referenceNumber,
            bankName = "ICICI Bank",
            rawDescription = remarksInfo.rawRemarks
        )

        return ParsedBlockResult(transaction, runningBalance)
    }

    private data class RemarksInfo(
        val displayDescription: String,
        val referenceNumber: String?,
        val rawRemarks: String
    )

    private fun extractRemarksInfo(blockLines: List<String>, datePrefix: String): RemarksInfo {
        // Strip the date prefix and serial number from the first line
        val firstLineWithoutDate = blockLines[0].substringAfter(datePrefix).trim()
        val allRemarksLines = mutableListOf<String>()
        if (firstLineWithoutDate.isNotBlank()) {
            allRemarksLines.add(firstLineWithoutDate)
        }
        for (i in 1 until blockLines.size) {
            allRemarksLines.add(blockLines[i])
        }

        // Remove amount tokens from remarks
        val cleanedRemarks = allRemarksLines.joinToString(" ") { line ->
            line.replace(amountRegex, "").trim()
        }.trim()

        var reference: String? = null
        var mainPartyName: String? = null
        var subPurpose: String? = null

        // 1. Check for NEFT pattern: NEFT-<UTR>-<BENEFICIARY>-<DETAILS>
        val neftRegex = Regex("""NEFT-([A-Za-z0-9]+)-([^-]+)(?:-(.+))?""", RegexOption.IGNORE_CASE)
        val neftMatch = neftRegex.find(cleanedRemarks)

        if (neftMatch != null) {
            reference = neftMatch.groupValues[1].trim()
            val rawBeneficiary = neftMatch.groupValues[2].trim()
            val details = neftMatch.groupValues.getOrNull(3)?.trim() ?: ""

            mainPartyName = formatCleanName(rawBeneficiary)

            if (details.contains("SALARY", ignoreCase = true)) {
                subPurpose = "Salary"
            }
        }

        // 2. Check for MMT/IMPS pattern: MMT/IMPS/<RRN>/<Purpose>/<Name>/<IFSC>
        val impsRegex = Regex("""(?:MMT/IMPS|IMPS)/([0-9A-Za-z]+)/([^/]+)(?:/([^/]+))?(?:/([^/]+))?""", RegexOption.IGNORE_CASE)
        val impsMatch = impsRegex.find(cleanedRemarks)

        if (impsMatch != null) {
            reference = impsMatch.groupValues[1].trim()
            val segment1 = impsMatch.groupValues.getOrNull(2)?.trim() ?: ""
            val segment2 = impsMatch.groupValues.getOrNull(3)?.trim() ?: ""
            val segment3 = impsMatch.groupValues.getOrNull(4)?.trim() ?: ""

            // In ICICI statements:
            // segment1 = user note or remark ("Room rent eb bi", "For ticket", "B gym", "cc emi")
            // segment2 = beneficiary name ("Rupesh Kum", "Krishnan", "Trainer")
            // segment3 = IFSC code ("BINB001234", "IDIB0001234")
            //
            // The user explicitly does NOT want the top bold redundant name in the cleaned description.
            // Priority:
            // 1. If segment1 is a valid note/purpose, use segment1 as the primary description (e.g. "Room Rent Eb Bi", "For Ticket", "Gym", "CC EMI").
            // 2. If segment1 is empty or generic, use segment2.
            val cleanSegment1 = formatCleanName(segment1)
            val cleanSegment2 = formatCleanName(segment2)

            if (cleanSegment1.isNotBlank()) {
                mainPartyName = cleanSegment1
            } else if (cleanSegment2.isNotBlank()) {
                mainPartyName = cleanSegment2
            }
        }

        // 3. Check for UPI pattern: UPI/<RRN>/<Payee>/<VPA>/<Note>
        val upiRegex = Regex("""UPI/([0-9]{12})/([^/]+)(?:/([^/]+))?(?:/([^/]+))?""", RegexOption.IGNORE_CASE)
        val upiMatch = upiRegex.find(cleanedRemarks)

        if (upiMatch != null) {
            reference = upiMatch.groupValues[1].trim()
            val payee = upiMatch.groupValues.getOrNull(2)?.trim() ?: ""
            val note = upiMatch.groupValues.getOrNull(4)?.trim() ?: ""
            mainPartyName = formatCleanName(payee)
            if (note.isNotBlank()) subPurpose = formatCleanName(note)
        }

        // 4. Extract standalone RRN or UTR if not yet found
        if (reference == null) {
            val rrnMatch = Regex("""\b([0-9]{10,18})\b""").find(cleanedRemarks)
            if (rrnMatch != null) {
                reference = rrnMatch.groupValues[1]
            }
        }

        // 5. If mainPartyName is still blank, take from the top remarks line
        if (mainPartyName.isNullOrBlank()) {
            val topCandidate = allRemarksLines.firstOrNull { line ->
                val upper = line.uppercase()
                !upper.startsWith("MMT/IMPS") &&
                !upper.startsWith("NEFT-") &&
                !upper.startsWith("UPI/") &&
                !upper.contains("INR") &&
                line.any { it.isLetter() }
            }

            if (topCandidate != null) {
                mainPartyName = formatCleanName(topCandidate.replace(amountRegex, "").trim())
            }
        }

        // Build final description
        val finalDescription = when {
            !mainPartyName.isNullOrBlank() && !subPurpose.isNullOrBlank() && !mainPartyName.equals(subPurpose, ignoreCase = true) -> {
                "$mainPartyName - $subPurpose"
            }
            !mainPartyName.isNullOrBlank() -> {
                mainPartyName
            }
            !subPurpose.isNullOrBlank() -> {
                subPurpose
            }
            else -> {
                descriptionCleaner.clean(cleanedRemarks).takeIf { it.isNotBlank() } ?: "ICICI Transaction"
            }
        }

        return RemarksInfo(
            displayDescription = finalDescription,
            referenceNumber = reference,
            rawRemarks = cleanedRemarks
        )
    }

    private fun formatCleanName(raw: String): String {
        val uppercaseAcronyms = setOf("EMI", "UPI", "IMPS", "NEFT", "RTGS", "ATM", "POS", "INR", "IFSC")
        return raw.replace(Regex("""[/\\_]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .split(" ")
            .joinToString(" ") { word ->
                if (uppercaseAcronyms.contains(word.uppercase())) {
                    word.uppercase()
                } else {
                    word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
                }
            }
    }

    private fun inferTypeFromText(text: String): TransactionType {
        val upper = text.uppercase()
        return when {
            upper.contains("SALARY") ||
            upper.contains("SALARY CR") ||
            upper.contains(" CR") ||
            upper.contains("CREDIT") ||
            upper.contains("DEPOSIT") ||
            upper.contains("REFUND") ||
            upper.contains("CASHBACK") -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
    }

    private fun parseDate(dateStr: String): Long? {
        return DateParserUtils.parseDate(dateStr)
    }
}
