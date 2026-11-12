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
 * Dedicated parser for State Bank of India (SBI) Savings, Current, and YONO e-statements.
 *
 * Supported Layouts & Features:
 * - Table Columns: Txn Date | Value Date | Description / Narration | Ref No./Cheque No. | Debit | Credit | Balance
 * - Date Formats: dd MMM yyyy, dd/MM/yyyy, dd-MM-yyyy, dd-MMM-yyyy, yyyy-MM-dd
 * - Transaction channels: UPI, IMPS, NEFT, RTGS, ATM withdrawals, Standing Orders, ACH mandates, Interest credits, Charges
 */
@Singleton
class SbiBankParser @Inject constructor(
    private val categoryRuleEngine: CategoryRuleEngine,
    private val descriptionCleaner: DescriptionCleaner,
    private val descriptionNormalizer: DescriptionNormalizer
) : StatementParser {

    private val supportedDateFormats = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    )

    // Match start of line with optional serial number and valid date
    private val transactionDateRegex = Regex(
        """^\s*(?:(\d{1,4})[.)]?\s+)?(\d{1,2}\s+[A-Za-z]{3}\s+\d{4}|\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}|\d{4}-\d{2}-\d{2})""",
        RegexOption.IGNORE_CASE
    )

    private val amountRegex = Regex("""(?<![.\d])([0-9]{1,3}(?:,[0-9]{3})*|\d+)\.(\d{2})(?![.\d])""")

    private var lastParsedRows: List<Pair<Transaction, Double?>> = emptyList()

    override fun canParse(rawText: String): Boolean {
        if (rawText.isBlank()) return false

        val upper = rawText.uppercase()
        val compact = upper.replace(Regex("[^A-Z0-9]"), "")

        // Reject if document explicitly belongs to competitor banks without SBI identity
        val hasOwnIdentity = upper.contains("STATE BANK OF INDIA") ||
                compact.contains("STATEBANKOFINDIA") ||
                upper.contains("YONO SBI") ||
                compact.contains("YONOSBI") ||
                upper.contains("ONLINESBI.COM") ||
                upper.contains("ONLINESBI.SBM") ||
                upper.contains("SBIN0")

        val isCompetitorBank = !hasOwnIdentity && (
            upper.contains("ICICI BANK") ||
            upper.contains("HDFC BANK") ||
            upper.contains("AXIS BANK") ||
            upper.contains("INDIAN BANK") ||
            upper.contains("KOTAK MAHINDRA") ||
            upper.contains("PUNJAB NATIONAL BANK")
        )

        if (isCompetitorBank) {
            return false
        }

        val hasSbiIdentity = hasOwnIdentity || (upper.contains("SBI") && (upper.contains("ACCOUNT STATEMENT") || upper.contains("YONO")))

        val hasStatementEvidence =
            upper.contains("STATEMENT OF ACCOUNT") ||
            upper.contains("ACCOUNT STATEMENT") ||
            upper.contains("TRANSACTION DETAILS") ||
            upper.contains("TRANSACTION DATE") ||
            upper.contains("VALUE DATE") ||
            (upper.contains("DEBIT") && upper.contains("CREDIT") && upper.contains("BALANCE"))

        val hasTransactionDate = rawText.lines().any { line ->
            transactionDateRegex.containsMatchIn(line)
        }

        return hasSbiIdentity && (hasStatementEvidence || hasTransactionDate)
    }

    override fun parse(rawText: String): List<Transaction> {
        return parseWithBalances(rawText).map { it.first }
    }

    fun parseWithBalances(rawText: String): List<Pair<Transaction, Double?>> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val blocks = groupIntoTransactionBlocks(lines)

        val results = mutableListOf<Pair<Transaction, Double?>>()
        var previousBalance: Double? = null

        for (block in blocks) {
            val parsedResult = parseTransactionBlock(block, previousBalance)
            if (parsedResult != null) {
                results.add(Pair(parsedResult.transaction, parsedResult.balance))
                if (parsedResult.balance != null) {
                    previousBalance = parsedResult.balance
                }
            }
        }

        lastParsedRows = results
        return results
    }

    override fun extractSummary(rawText: String, transactions: List<Transaction>): StatementSummary? {
        val upper = rawText.uppercase()

        // 1. Try extracting explicit opening balance from header
        var openingBalance: Double? = null
        val openingRegexes = listOf(
            Regex("""(?:OPENING\s+BALANCE|BROUGHT\s+FORWARD|B/F|STARTING\s+BALANCE)\s*[:\-]?\s*(?:INR|RS\.?|₹)?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE),
            Regex("""(?:OPENING\s+BAL)\s*[:\-]?\s*(?:INR|RS\.?|₹)?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        )
        for (rgx in openingRegexes) {
            val match = rgx.find(rawText)
            if (match != null) {
                openingBalance = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (openingBalance != null) break
            }
        }

        // 2. Try extracting explicit closing balance from footer
        var endingBalance: Double? = null
        val closingRegexes = listOf(
            Regex("""(?:CLOSING\s+BALANCE|CARRIED\s+FORWARD|C/F|ENDING\s+BALANCE|AVAILABLE\s+BALANCE)\s*[:\-]?\s*(?:INR|RS\.?|₹)?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE),
            Regex("""(?:CLOSING\s+BAL)\s*[:\-]?\s*(?:INR|RS\.?|₹)?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE)
        )
        for (rgx in closingRegexes) {
            val match = rgx.find(rawText)
            if (match != null) {
                endingBalance = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (endingBalance != null) break
            }
        }

        // 3. Fallback to row ledgers if not found in headers
        if (lastParsedRows.isNotEmpty()) {
            if (endingBalance == null) {
                endingBalance = lastParsedRows.last().second
            }
            if (openingBalance == null && lastParsedRows.first().second != null) {
                val firstTx = lastParsedRows.first().first
                val firstBal = lastParsedRows.first().second!!
                openingBalance = if (firstTx.type == TransactionType.INCOME || firstTx.type == TransactionType.CREDIT) {
                    firstBal - firstTx.amount
                } else {
                    firstBal + firstTx.amount
                }
            }
        }

        val totalDebits = transactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val totalCredits = transactions.filter { it.type == TransactionType.INCOME || it.type == TransactionType.CREDIT }.sumOf { it.amount }

        return StatementSummary(
            openingBalance = openingBalance,
            totalCredits = totalCredits,
            totalDebits = totalDebits,
            endingBalance = endingBalance
        )
    }

    private fun isStatementHeaderOrFooter(line: String): Boolean {
        val upper = line.uppercase()
        if (upper.startsWith("STATE BANK OF INDIA") || upper.startsWith("YONO SBI") || upper.startsWith("ONLINESBI")) {
            return true
        }
        if (upper.contains("TXN DATE") && upper.contains("VALUE DATE") && upper.contains("DESCRIPTION")) {
            return true
        }
        if (upper.contains("DATE") && upper.contains("NARRATION") && upper.contains("BALANCE")) {
            return true
        }
        if (upper.contains("OPENING BALANCE") || upper.contains("CLOSING BALANCE") ||
            upper.contains("BROUGHT FORWARD") || upper.contains("CARRIED FORWARD") ||
            upper.contains("STARTING BALANCE") || upper.contains("ENDING BALANCE") ||
            upper.contains("AVAILABLE BALANCE") || upper.contains("TOTAL DEBIT") ||
            upper.contains("TOTAL CREDIT") || upper.contains("TOTAL WITHDRAWAL") ||
            upper.contains("TOTAL DEPOSIT") || upper.startsWith("B/F") || upper.startsWith("C/F")
        ) {
            return true
        }
        return upper.contains("STATEMENT OF ACCOUNT") ||
                upper.contains("ACCOUNT NUMBER") ||
                upper.contains("ACCOUNT NO") ||
                upper.contains("CIF NO") ||
                upper.contains("IFS CODE") ||
                upper.contains("MICR CODE") ||
                upper.contains("BRANCH CODE") ||
                upper.contains("BRANCH NAME") ||
                upper.contains("NOMINATION") ||
                upper.contains("PAGE NO") ||
                upper.contains("COMPUTER GENERATED STATEMENT")
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
                currentBlock = mutableListOf(line)
                blocks.add(currentBlock)
            } else {
                currentBlock?.add(line)
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
        val textWithoutDates = fullBlockText
            .replace(Regex("""\b\d{1,2}\s+[A-Za-z]{3}\s+\d{4}\b"""), " ")
            .replace(Regex("""\b\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}\b"""), " ")
            .replace(Regex("""\b\d{4}-\d{2}-\d{2}\b"""), " ")

        val amountsFound = amountRegex.findAll(textWithoutDates).mapNotNull { match ->
            val numStr = (match.groupValues[1] + "." + match.groupValues[2]).replace(",", "")
            numStr.toDoubleOrNull()
        }.toList()

        if (amountsFound.isEmpty()) return null

        var txAmount: Double
        var runningBalance: Double? = null
        var txType: TransactionType = TransactionType.EXPENSE

        if (amountsFound.size >= 3) {
            val debit = amountsFound[0]
            val credit = amountsFound[1]
            val balance = amountsFound[2]
            runningBalance = balance

            if (credit > 0.0 && debit == 0.0) {
                txAmount = credit
                txType = TransactionType.INCOME
            } else if (debit > 0.0 && credit == 0.0) {
                txAmount = debit
                txType = TransactionType.EXPENSE
            } else if (credit > 0.0) {
                txAmount = credit
                txType = TransactionType.INCOME
            } else {
                txAmount = debit
                txType = TransactionType.EXPENSE
            }
        } else if (amountsFound.size == 2) {
            txAmount = amountsFound[0]
            runningBalance = amountsFound[1]

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

        val remarksInfo = extractRemarksInfo(blockLines, dateMatch.value)

        val isIncome = (txType == TransactionType.INCOME || txType == TransactionType.CREDIT)
        val categoryResult = categoryRuleEngine.categorize(remarksInfo.displayDescription, isIncome)

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
            bankName = "State Bank of India (SBI)",
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
        val firstLineWithoutDate = blockLines[0].substringAfter(datePrefix).trim()
        val allRemarksLines = mutableListOf<String>()
        if (firstLineWithoutDate.isNotBlank()) {
            allRemarksLines.add(firstLineWithoutDate)
        }
        for (i in 1 until blockLines.size) {
            allRemarksLines.add(blockLines[i])
        }

        val combinedRemarks = allRemarksLines.joinToString(" ")
            .replace(amountRegex, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val refNumber = extractReferenceNumber(combinedRemarks)
        val cleanedDesc = descriptionCleaner.clean(combinedRemarks)
        val normalizedDesc = descriptionNormalizer.normalize(cleanedDesc)

        val displayDesc = if (normalizedDesc.isBlank()) {
            "SBI Transaction"
        } else {
            normalizedDesc
        }

        return RemarksInfo(
            displayDescription = displayDesc,
            referenceNumber = refNumber,
            rawRemarks = combinedRemarks
        )
    }

    private fun extractReferenceNumber(text: String): String? {
        val patterns = listOf(
            Regex("""(?:UPI[/:]|UPI\s+Ref(?:\s+No)?[:\-]?\s*|RRN[:\-]?\s*)(\d{12})""", RegexOption.IGNORE_CASE),
            Regex("""(?:IMPS[/:]|IMPS\s+Ref(?:\s+No)?[:\-]?\s*)(\d{12})""", RegexOption.IGNORE_CASE),
            Regex("""(?:REF[/:]|REF\s+NO[:\-]?\s*)([A-Za-z0-9]{8,22})""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{12})\b""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun inferTypeFromText(text: String): TransactionType {
        val upper = text.uppercase()
        if (upper.contains(" CR") || upper.contains("CREDIT") || upper.contains("BY TRANSFER") || upper.contains("DEPOSIT") || upper.contains("INTEREST")) {
            return TransactionType.INCOME
        }
        if (upper.contains(" DR") || upper.contains("DEBIT") || upper.contains("TO TRANSFER") || upper.contains("WITHDRAWAL") || upper.contains("ATM WDL")) {
            return TransactionType.EXPENSE
        }
        return TransactionType.EXPENSE
    }

    private fun parseDate(dateStr: String): Long? {
        val cleaned = dateStr.trim().replace(".", "-").replace("/", "-")
        for (format in supportedDateFormats) {
            try {
                format.isLenient = false
                val date = format.parse(cleaned)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }
}
