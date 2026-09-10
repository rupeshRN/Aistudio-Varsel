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

        val accountBranch = extractAccountBranch(rawText)

        val cleanLines = extractTransactionTableLines(lines, accountBranch)
        if (cleanLines.isEmpty()) {
            lastParsedRows = emptyList()
            return emptyList()
        }

        val blocks = groupIntoTransactionBlocks(cleanLines, accountBranch)

        val transactions = mutableListOf<Transaction>()
        val parsedRows = mutableListOf<Pair<Transaction, Double?>>()
        
        val explicitSummary = parseHdfcStatementSummary(rawText)
        var previousBalance: Double? = explicitSummary?.openingBalance

        for ((index, block) in blocks.withIndex()) {
            val parsedTx = parseTransactionBlock(block, previousBalance, index, accountBranch)
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

    private fun extractAccountBranch(text: String): String? {
        val match = Regex("""Account Branch\s*:\s*([^\n\r]+)""", RegexOption.IGNORE_CASE).find(text)
        if (match != null) {
            val raw = match.groupValues[1].trim()
            val branch = raw.split("-", ",", ";").firstOrNull()?.trim()
            if (!branch.isNullOrBlank() && branch.length >= 3) {
                return branch
            }
            return raw.take(30).trim()
        }
        val reqBranchMatch = Regex("""Requesting Branch\s*:\s*([^\n\r]+)""", RegexOption.IGNORE_CASE).find(text)
        if (reqBranchMatch != null) {
            val raw = reqBranchMatch.groupValues[1].trim()
            val branch = raw.split("-", ",", ";").firstOrNull()?.trim()
            if (!branch.isNullOrBlank() && branch.length >= 3) {
                return branch
            }
        }
        return null
    }

    override fun extractSummary(rawText: String, transactions: List<Transaction>): StatementSummary? {
        if (transactions.isEmpty()) return null

        val rowsWithBalance = lastParsedRows.filter { it.second != null }
        val latestRow = rowsWithBalance.lastOrNull()
        val earliestRow = rowsWithBalance.firstOrNull()

        // Parse explicit STATEMENT SUMMARY section if present
        val explicitSummary = parseHdfcStatementSummary(rawText)

        val endingBalance = explicitSummary?.closingBalance ?: latestRow?.second
        val openingBalance = explicitSummary?.openingBalance ?: earliestRow?.let { (tx, balance) ->
            if (balance != null) {
                if (tx.type == TransactionType.INCOME) balance - tx.amount else balance + tx.amount
            } else null
        }

        val totalCredits = explicitSummary?.totalCredits ?: transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalDebits = explicitSummary?.totalDebits ?: transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
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

    private data class ExplicitSummary(
        val openingBalance: Double?,
        val closingBalance: Double?,
        val totalCredits: Double?,
        val totalDebits: Double?
    )

    private fun parseHdfcStatementSummary(rawText: String): ExplicitSummary? {
        val summaryMatch = Regex("""STATEMENT\s+SUMMARY\s*:?[\s\S]*""", RegexOption.IGNORE_CASE).find(rawText)
            ?: return null
        val text = summaryMatch.value

        // Extract decimal amounts from the summary section
        val amountMatches = Regex("""\b\d{1,3}(?:,\d{3})*\.\d{2}\b""").findAll(text)
            .mapNotNull { it.value.replace(",", "").toDoubleOrNull() }
            .toList()

        if (amountMatches.size >= 4) {
            val n1 = amountMatches[0]
            val n2 = amountMatches[1]
            val n3 = amountMatches[2]
            val n4 = amountMatches[3]

            // Case A: [Opening, Debits, Credits, Closing] -> Opening + Credits - Debits == Closing
            if (kotlin.math.abs((n1 + n3 - n2) - n4) < 0.05) {
                return ExplicitSummary(
                    openingBalance = n1,
                    totalDebits = n2,
                    totalCredits = n3,
                    closingBalance = n4
                )
            }

            // Case B: [Opening, Credits, Debits, Closing] -> Opening + Credits - Debits == Closing
            if (kotlin.math.abs((n1 + n2 - n3) - n4) < 0.05) {
                return ExplicitSummary(
                    openingBalance = n1,
                    totalCredits = n2,
                    totalDebits = n3,
                    closingBalance = n4
                )
            }
        }

        val opening = Regex("""Opening\s*Bal(?:ance)?\s*[:=-]?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val closing = Regex("""Closing\s*Bal(?:ance)?\s*[:=-]?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val debits = Regex("""Debits\s*[:=-]?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val credits = Regex("""Credits\s*[:=-]?\s*([0-9,]+\.\d{2})""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        if (opening != null || closing != null || debits != null || credits != null) {
            return ExplicitSummary(
                openingBalance = opening,
                closingBalance = closing,
                totalCredits = credits,
                totalDebits = debits
            )
        }

        return null
    }

    private fun extractTransactionTableLines(lines: List<String>, accountBranch: String? = null): List<String> {
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
                    transactionDateRegex.containsMatchIn(line)) && !isFinalStatementSummary(upper) && !isStatementNoise(upper, accountBranch)
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

            if (isStatementHeaderOrFooter(upper, accountBranch)) {
                continue
            }

            tableLines.add(line)
        }

        if (tableLines.isEmpty()) {
            return lines.filter { line ->
                val upper = line.uppercase()
                !isStatementHeaderOrFooter(upper, accountBranch)
            }
        }

        return tableLines
    }

    private fun isFinalStatementSummary(upper: String): Boolean {
        return upper.contains("STATEMENT SUMMARY") ||
                upper.contains("SUMMARY OF ACCOUNT") ||
                (upper.contains("OPENING BALANCE") && (upper.contains("DR COUNT") || upper.contains("CR COUNT") || upper.contains("CLOSING BAL")))
    }

    private fun isTableHeader(upper: String): Boolean {
        return (upper.contains("NARRATION") && upper.contains("DATE")) ||
                (upper.contains("WITHDRAWAL") && upper.contains("DEPOSIT")) ||
                (upper.contains("PARTICULARS") && upper.contains("CHQ")) ||
                (upper.contains("DATE") && upper.contains("CHQ./REF.NO.")) ||
                upper.contains("STATEMENT OF ACCOUNT") ||
                upper.contains("ACCOUNT STATEMENT")
    }

    private fun isStatementNoise(upper: String, accountBranch: String? = null): Boolean {
        if (accountBranch != null && upper.contains(accountBranch.uppercase())) {
            return true
        }
        return upper.contains("HDFC BANK LIMITED") ||
                upper.contains("HDFC BANK HOUSE") ||
                upper.contains("WWW.HDFCBANK.COM") ||
                upper.contains("THIS IS A COMPUTER GENERATED STATEMENT") ||
                upper.contains("REGISTERED OFFICE") ||
                upper.contains("CONTENTS OF THIS STATEMENT") ||
                upper.contains("THIS STATEMENT") ||
                upper.contains("CONSIDERED CORRECT") ||
                upper.contains("NOT REQUIRE SIGNATURE") ||
                upper.contains("DOES NOT REQUIRE SIGNATURE") ||
                upper.contains("CLOSING BALANCE INCLUDES") ||
                upper.contains("EARMARKED FOR HOLD") ||
                upper.contains("UNCLEARED FUNDS") ||
                upper.contains("STATE ACCOUNT BRANCH GSTN") ||
                upper.contains("BRANCH GSTN") ||
                upper.contains("HDFC BANK GSTIN") ||
                upper.contains("ONLINE-TAX-PAYMENT") ||
                upper.contains("GOODS-AND-SERVICE-TAX") ||
                upper.contains("SENAPATI BAPAT MARG") ||
                upper.contains("LOWER PAREL") ||
                upper.contains("MUMBAI 400013") ||
                upper.contains("WE UNDERSTAND YOUR WORLD") ||
                upper.contains("TOTAL DEBITS") ||
                upper.contains("TOTAL CREDITS") ||
                upper.contains("PAGE NO") ||
                upper.contains("PAGE NO.") ||
                upper.contains("PAGE NO .:") ||
                upper.contains("THE ADDRESS ON THIS STATEMENT") ||
                upper.contains("DAY OF REQUESTING") ||
                upper.contains("REQUESTING THIS STATEMENT") ||
                upper.contains("REPORTED WITHIN 30 DAYS") ||
                upper.contains("WITHIN 30 DAYS OF RECEIPT") ||
                upper.matches(Regex(""".*PAGE\s*(?:NO)?[\s.:]*\d+.*""")) ||
                upper.matches(Regex(""".*\d+\s+OF\s+\d+.*""")) ||
                upper.matches(Regex(""".*\b[A-Z\s,]+-\s*\d{6}\b.*"""))
    }

    private fun isStatementHeaderOrFooter(upper: String, accountBranch: String? = null): Boolean {
        if (isTableHeader(upper) || isStatementNoise(upper, accountBranch) || isFinalStatementSummary(upper)) {
            return true
        }
        return upper.contains("STATEMENT OF ACCOUNT") ||
                upper.contains("ACCOUNT STATEMENT") ||
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
                upper.contains("NOMINATION") ||
                upper.contains("REGISTERED") ||
                upper.contains("NOT REGISTERED") ||
                upper.contains("OPEN DATE") ||
                upper.contains("A/C OPEN DATE") ||
                upper.contains("ACCOUNT STATUS") ||
                upper.contains("PRODUCT CODE") ||
                upper.contains("PB CUSTOMER") ||
                upper.contains("ACCOUNT BRANCH") ||
                upper.contains("REQUESTING BRANCH") ||
                upper.contains("GENERATED ON") ||
                upper.contains("GENERATED BY") ||
                upper.contains("OD LIMIT") ||
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
                upper.contains("SAVINGS A/C") ||
                upper.contains("SAVINGS ACCOUNT") ||
                upper.contains("CURRENT A/C") ||
                upper.contains("CURRENT ACCOUNT") ||
                upper.contains("CURRENCY :") ||
                upper.contains("CURRENCY:") ||
                upper.contains("STATUS :") ||
                upper.contains("ADDRESS :") ||
                upper.contains("CITY :") ||
                upper.contains("STATE :") ||
                upper.contains("PHONE NO") ||
                upper.contains("EMAIL ID") ||
                upper.contains("EMAIL :") ||
                upper.contains("SHOWROOM") ||
                upper.contains("OPP. TO") ||
                upper.contains("FLAT NO") ||
                upper.contains("HOUSE NO")
    }

    private fun groupIntoTransactionBlocks(lines: List<String>, accountBranch: String? = null): List<List<String>> {
        val blocks = mutableListOf<MutableList<String>>()
        var currentBlock: MutableList<String>? = null

        for (line in lines) {
            val upper = line.uppercase()
            val match = transactionDateRegex.find(line)
            if (match != null) {
                currentBlock = mutableListOf(line)
                blocks.add(currentBlock)
                continue
            }

            if (isStatementHeaderOrFooter(upper, accountBranch)) {
                // If this is statement noise or summary footer, seal the current transaction block!
                if (isStatementNoise(upper, accountBranch) || isFinalStatementSummary(upper)) {
                    currentBlock = null
                }
                continue
            }

            if (currentBlock != null) {
                currentBlock.add(line)
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
        previousBalance: Double?,
        blockIndex: Int = 0,
        accountBranch: String? = null
    ): ParsedBlockResult? {
        if (blockLines.isEmpty()) return null

        val firstLine = blockLines.first()
        val dateMatch = transactionDateRegex.find(firstLine) ?: return null
        val rawDateStr = dateMatch.groupValues[2]
        val rawTimestamp = DateParserUtils.parseDate(rawDateStr) ?: return null
        val dateTimestamp = rawTimestamp + (blockIndex * 1000L)

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

            if (previousBalance != null) {
                val diff = secondAmt - previousBalance
                if (diff > 0.01) {
                    parsedAmount = firstAmt
                    transactionType = TransactionType.INCOME
                } else if (diff < -0.01) {
                    parsedAmount = firstAmt
                    transactionType = TransactionType.EXPENSE
                } else {
                    parsedAmount = firstAmt
                    transactionType = if (upperFull.contains(" CR") || upperFull.contains("(CR)") || upperFull.contains("CREDIT") || upperFull.contains("BY TRANSFER") || upperFull.contains("BY ")) {
                        TransactionType.INCOME
                    } else {
                        TransactionType.EXPENSE
                    }
                }
            } else if (upperFull.contains(" CR") || upperFull.contains("(CR)") || upperFull.contains("CREDIT") || upperFull.contains("BY TRANSFER") || upperFull.contains("BY ") || upperFull.contains("INTEREST PAID")) {
                parsedAmount = firstAmt
                transactionType = TransactionType.INCOME
            } else if (upperFull.contains(" DR") || upperFull.contains("(DR)") || upperFull.contains("DEBIT") || upperFull.contains("TO TRANSFER") || upperFull.contains("TO ")) {
                parsedAmount = firstAmt
                transactionType = TransactionType.EXPENSE
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

        // Extract reference number from the Chq./Ref.No. column before cleaning narration
        val columnRefNumber = extractColumnReferenceNumber(fullBlockText)

        // Check for 6-digit cheque number in the block (distinct from 10+ zeroes)
        val chequeMatch = Regex("""\b(?:CHQ[\s.:]*)?([0-9]{6})\b""").findAll(fullBlockText)
            .firstOrNull { m ->
                val v = m.groupValues[1]
                v != "000000" && !v.startsWith("00000")
            }?.groupValues?.get(1)

        val rawDescription = extractNarration(fullBlockText, dateMatch.value, amountMatches.map { it.first }, accountBranch, columnRefNumber)
        val remarksInfo = parseRemarks(rawDescription, transactionType == TransactionType.INCOME, accountBranch)

        val resolvedRefNumber = remarksInfo.referenceNumber ?: columnRefNumber ?: chequeMatch?.let { "CHQ $it" }

        val isIncome = (transactionType == TransactionType.INCOME || transactionType == TransactionType.CREDIT)
        val categoryResult = categoryRuleEngine.categorize(remarksInfo.displayDescription, isIncome)

        val upperDesc = remarksInfo.displayDescription.uppercase()
        val finalCategory = when {
            isIncome && (upperDesc.contains("INTEREST") || upperDesc.contains("INT PAID")) -> Category.OTHER_INCOME
            isIncome && upperDesc.contains("SALARY") -> Category.SALARY
            !isIncome && (upperDesc.contains("MIN BAL") || upperDesc.contains("MINIMUM BAL") || upperDesc.contains("CHARGE")) -> Category.UTILITIES
            !isIncome && (upperDesc.contains("EMI") || upperDesc.contains("LOAN")) -> Category.HOUSING
            !isIncome && (upperDesc.contains("BILLPAY") || upperDesc.contains("CARDS") || upperDesc.contains("CREDIT CARD")) -> Category.UTILITIES
            !isIncome && (upperDesc.contains("ATM") || upperDesc.contains("NWD") || upperDesc.contains("ATW") || rawDescription.uppercase().startsWith("ATW") || upperDesc.contains("CASH WDL") || upperDesc.contains("CASH WITHDRAWAL")) -> Category.TRANSFER
            else -> categoryResult.category
        }

        val transaction = Transaction(
            id = 0,
            amount = parsedAmount,
            type = transactionType,
            description = remarksInfo.displayDescription,
            category = finalCategory,
            dateTimestamp = dateTimestamp,
            referenceNumber = resolvedRefNumber,
            bankName = "HDFC Bank",
            rawDescription = rawDescription
        )

        return ParsedBlockResult(transaction, closingBalance)
    }

    private fun extractNarration(
        fullText: String,
        dateMatchStr: String,
        amounts: List<Double>,
        accountBranch: String? = null,
        columnRefNumber: String? = null
    ): String {
        var text = fullText
        text = text.replace(dateMatchStr, " ")

        // Remove Value Dt or other embedded dates
        text = text.replace(Regex("""\b\d{1,2}[./-](?:\d{1,2}|[A-Za-z]{3})[./-]\d{2,4}\b"""), " ")
        text = text.replace(Regex("""\b\d{4}-\d{2}-\d{2}\b"""), " ")

        // Remove all table amounts cleanly using amountRegex to prevent residual fragments
        text = amountRegex.replace(text, " ")

        // Remove repeated zeroes dummy reference column (e.g., 00000000000000000)
        text = text.replace(Regex("""\b0{5,}\b"""), " ")

        // Remove extracted zero-padded column reference number from the narration text
        if (!columnRefNumber.isNullOrBlank()) {
            text = text.replace(Regex("""\b0{3,}$columnRefNumber\b"""), " ")
        }
        text = text.replace(Regex("""\b0000\d{8,16}\b"""), " ")

        // Truncate at footer keywords or disclaimer patterns if any slipped into multiline block
        val footerKeywords = listOf(
            "STATEMENT SUMMARY", "Opening Balance", "Dr Count", "Cr Count", "Closing Bal",
            "Generated On", "Generated By", "Requesting Branch", "not require signature",
            "does not require signature", "Closing balance includes", "State account branch GSTN",
            "HDFC BANK LIMITED", "Registered Office Address", "Contents of this statement",
            "Contents of This Statement", "This Statement", "The address on this statement",
            "as at the day of requesting", "day of requesting this statement", "requesting this statement",
            "will be considered correct", "reported within 30 days", "Page No", "Page No.",
            "Statement of account", "Account Branch", "We understand your world", "Cust ID",
            "A/c Open Date", "Account Status", "Nomination", "Statement From", "HDFC Bank GSTIN",
            "PB Customer", "Product Code", "Opp. to", "Showroom", "OD Limit", "Branch :"
        )
        for (kw in footerKeywords) {
            val pos = text.indexOf(kw, ignoreCase = true)
            if (pos >= 0) {
                text = text.substring(0, pos)
            }
        }

        // Dynamically truncate account branch if present
        if (!accountBranch.isNullOrBlank()) {
            val posBranch = text.indexOf(accountBranch, ignoreCase = true)
            if (posBranch >= 0) {
                text = text.substring(0, posBranch)
            }
        }

        // Remove any trailing postal addresses / PIN patterns (e.g. "Kodambakkam, Chennai - 600024")
        val pinMatch = Regex("""[,\s]+[A-Za-z\s,]+-\s*\d{6}\b.*""").find(text)
        if (pinMatch != null) {
            text = text.substring(0, pinMatch.range.first)
        }

        text = text.replace(Regex("""\s+"""), " ").trim()
        return text.ifBlank { "HDFC Transaction" }
    }

    private data class RemarksInfo(
        val displayDescription: String,
        val merchant: String?,
        val referenceNumber: String?
    )

    private fun parseRemarks(rawDescription: String, isIncome: Boolean = false, accountBranch: String? = null): RemarksInfo {
        val cleanText = rawDescription.replace(Regex("""\s+"""), " ").trim()
        val upper = cleanText.uppercase()

        // 1. INTEREST PAID TILL DD-MMM-YYYY / CREDIT INTEREST CAPITALISED
        if (upper.contains("INTEREST PAID") || upper.contains("INT. PAID") || upper.contains("CREDIT INTEREST")) {
            val titleCased = if (upper.contains("CREDIT INTEREST CAPITALISED")) {
                "Credit Interest Capitalised"
            } else {
                cleanText.split(" ").joinToString(" ") { word ->
                    if (word.uppercase() == "TILL") "till"
                    else word.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
            return RemarksInfo(titleCased, "HDFC Bank", extractRefNumber(cleanText))
        }

        // 2. MICRO ATM CASH DEP
        if (upper.contains("MICRO ATM") || upper.contains("CASH DEP")) {
            val location = if (upper.contains("THANE")) " - Thane" else ""
            return RemarksInfo("Micro ATM Cash Deposit$location", "HDFC Bank", extractRefNumber(cleanText))
        }

        // 3. IB BILLPAY DR
        if (upper.contains("IB BILLPAY")) {
            return RemarksInfo("HDFC BillPay", "HDFC BillPay", extractRefNumber(cleanText))
        }

        // 4. NetBanking BillPay (NHDF6376325463/SBI CARDS, NHDF6385796167/BILLDKVODAFONEINDIAL)
        if (upper.startsWith("NHDF") || (upper.contains("/") && (upper.contains("CARDS") || upper.contains("BILL")))) {
            val slashIndex = cleanText.indexOf('/')
            if (slashIndex >= 0 && slashIndex < cleanText.length - 1) {
                var billPayee = cleanText.substring(slashIndex + 1).trim()
                val upperPayee = billPayee.uppercase()
                if (upperPayee.startsWith("BILLDK")) {
                    billPayee = billPayee.substring(6)
                }
                if (upperPayee.contains("VODAFONE")) {
                    billPayee = "Vodafone India Bill"
                } else if (upperPayee.contains("KOTAK")) {
                    billPayee = "Kotak Cards"
                } else if (upperPayee.contains("SBI")) {
                    billPayee = "SBI Cards"
                } else {
                    billPayee = formatTitleCase(billPayee)
                }
                return RemarksInfo(billPayee, billPayee, extractRefNumber(cleanText))
            }
        }

        // 5. EMI Loans (e.g. EMI 4923306 CHQ S49233060051 0618492330 6)
        if (upper.startsWith("EMI ")) {
            return RemarksInfo("EMI Payment", "EMI", extractRefNumber(cleanText))
        }

        // 6. MIN BAL MAINTAIN / MINIMUM BALANCE CHARGES
        if (upper.contains("MIN BAL") || upper.contains("MINIMUM BAL")) {
            val titleCased = "Min Balance Maintenance"
            return RemarksInfo(titleCased, "HDFC Bank", extractRefNumber(cleanText))
        }

        // 7. ATM / Cash Withdrawal (ATW / NWD)
        // If the narration of HDFC starts with ATW (or NWD), consider that transaction as ATM withdrawal
        // and list the last segment (purpose) usually the ATM location (e.g. "ATM Withdrawal: Kodambakkam")
        if (upper.startsWith("ATW") || upper.startsWith("NWD") || upper.startsWith("ATM WDL") || upper.contains("ATM CASH") || upper.contains("CASH WDL")) {
            return parseAtmWithdrawal(cleanText)
        }

        // 8. Hyphenated or Slash-Separated Narration Format (Payment Mode - Ref - Name - Bank - Acc - Reason)
        if (cleanText.contains("-") || (cleanText.contains("/") && (upper.startsWith("IMPS") || upper.startsWith("UPI") || upper.startsWith("NEFT") || upper.startsWith("RTGS")))) {
            return parseHyphenatedHdfcNarration(cleanText, accountBranch)
        }

        // 8. POS / Card purchases
        if (upper.startsWith("POS") || upper.contains("POS ")) {
            var merchantName: String? = null
            val parts = cleanText.split(" ")
            val merchantParts = parts.filter { part ->
                val pUpper = part.uppercase()
                !pUpper.startsWith("POS") &&
                        !pUpper.matches(Regex("""\d+""")) &&
                        !pUpper.contains("XXXX") &&
                        pUpper != "IN" &&
                        pUpper != "IND" &&
                        pUpper != "DE" &&
                        pUpper != "BIT" &&
                        pUpper != "DEBIT" &&
                        pUpper.length > 1
            }
            if (merchantParts.isNotEmpty()) {
                val cleanedMerchantParts = mutableListOf<String>()
                val stateCodes = setOf("KA", "MH", "TN", "DL", "TS", "AP", "WB", "GJ", "UP", "HR", "KL", "MP")
                for (p in merchantParts) {
                    var candidate = p
                    if (candidate.uppercase().startsWith("DCSI")) {
                        candidate = candidate.substring(4)
                    }
                    if (stateCodes.contains(candidate.uppercase()) && candidate == merchantParts.last()) continue
                    if (candidate.isNotBlank()) cleanedMerchantParts.add(candidate)
                }

                merchantName = (if (cleanedMerchantParts.isNotEmpty()) cleanedMerchantParts else merchantParts)
                    .joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { it.uppercase() }
                    }
                return RemarksInfo("POS: $merchantName", merchantName, extractRefNumber(cleanText))
            }
        }

        // 9. Default Title Case
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

    private fun parseAtmWithdrawal(cleanText: String): RemarksInfo {
        val refNumber = extractRefNumber(cleanText)

        val rawSegments = if (cleanText.contains("-")) {
            cleanText.split("-")
        } else if (cleanText.contains("/")) {
            cleanText.split("/")
        } else {
            cleanText.split(" ")
        }.map { it.trim() }.filter { it.isNotBlank() }

        val noiseTokens = setOf(
            "ATW", "NWD", "ATM", "WDL", "CASH", "ATM CASH", "CASH WDL", "CASH WITHDRAWAL",
            "IN", "IND", "DEBIT", "DR"
        )

        val locationCandidates = rawSegments.filter { seg ->
            val u = seg.uppercase()
            !noiseTokens.contains(u) &&
                    !u.matches(Regex("""\d+""")) &&
                    !u.contains("X") &&
                    !u.contains("*") &&
                    !u.startsWith("ATM") &&
                    !u.startsWith("CHQ") &&
                    !u.startsWith("REF") &&
                    seg != refNumber
        }

        // List the last segment (purpose) usually the ATM location
        val lastSegment = locationCandidates.lastOrNull()
        val location = if (!lastSegment.isNullOrBlank()) {
            val cleanedLoc = lastSegment
                .replace(Regex("""\(?\s*(?:for\s+)?card\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\(?\s*(?:for\s+)?ac(?:count)?\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\b[A-Za-z0-9]*[Xx*]{2,}[A-Za-z0-9]*\b"""), "")
                .replace(Regex("""\bending\s+\d+\b""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (cleanedLoc.isNotBlank()) formatTitleCase(cleanedLoc) else null
        } else null

        val desc = if (location != null) "ATM Withdrawal: $location" else "ATM Cash Withdrawal"

        return RemarksInfo(
            displayDescription = desc,
            merchant = "HDFC ATM",
            referenceNumber = refNumber
        )
    }

    private fun parseHyphenatedHdfcNarration(cleanText: String, accountBranch: String? = null): RemarksInfo {
        val delimiter = if (cleanText.contains("-")) "-" else "/"
        val rawParts = cleanText.split(delimiter).map { it.trim() }.filter { it.isNotBlank() }
        if (rawParts.size < 2) {
            val titleCased = formatTitleCase(cleanText)
            return RemarksInfo(titleCased, null, extractRefNumber(cleanText))
        }

        var refNumber: String? = null
        var mode: String? = null
        val textParts = mutableListOf<String>()

        val knownModes = setOf(
            "UPI", "IMPS", "TIMPS", "NEFT", "NEFT DR", "NEFT CR", "RTGS", "RTGS DR", "RTGS CR",
            "FT", "POS", "ACH", "ACH D", "ACH C", "ATW", "INB", "NWD", "REV", "VISA", "MC", "CHQ", "PAY", "BILL"
        )
        val channelNoise = setOf("NETBANK", "MUM", "DEL", "CHE", "BLR", "P2A", "P2P", "P2U")
        val bankCodeRegex = Regex("""^(HDFC|HDF|ICICI|ICIC|SBIN|SBI|UTIB|AXIS|YESB|YES|KKBK|KOTAK|BARB|BOB|CNRB|CANARA|PUNB|PNB|INDB|FDRL|FEDERAL|IDFB|IDFC|CITI|HSBC|SCBL|PAYTM|PYTM|STATE|BOI)$""", RegexOption.IGNORE_CASE)
        val ifscRegex = Regex("""^[A-Z]{4}0[A-Z0-9]{6}$""", RegexOption.IGNORE_CASE)

        for ((index, part) in rawParts.withIndex()) {
            val upperPart = part.uppercase()

            // Check if first part is Payment Mode
            if (index == 0 && knownModes.contains(upperPart)) {
                mode = when {
                    upperPart.startsWith("NEFT") -> "NEFT"
                    upperPart.startsWith("RTGS") -> "RTGS"
                    upperPart.startsWith("ACH") -> "ACH"
                    else -> upperPart
                }
                continue
            }

            // Skip masked account / card number (e.g. 123456XXXXXX1234, XXXXXX0123, xxxx0123)
            val isMasked = part.contains("X", ignoreCase = true) || part.contains("*")
            if (isMasked) {
                continue
            }

            // Skip account number patterns (e.g. ACC1234, A/C 0123, SB0123, 0123, etc.)
            if (upperPart.startsWith("ACC") || upperPart.startsWith("A/C") || upperPart.startsWith("SB-") || upperPart.startsWith("CA-") || upperPart.matches(Regex("""^X*\d{3,6}$"""))) {
                continue
            }

            // In HDFC UPI narrations (UPI-ID-PAYEE-RRN-REASON), a 12-digit segment is strictly the NPCI RRN
            if (mode == "UPI" && part.matches(Regex("""\d{12}"""))) {
                refNumber = part
                continue
            }
            if (mode == "UPI" && index == 1 && part.matches(Regex("""\d{10,18}"""))) {
                if (refNumber == null) refNumber = part
                continue
            }

            // Check if part is a reference number (e.g. UTR N155180555427618, 12-digit RRN, or internal FT)
            if (refNumber == null && (part.matches(Regex("""\d{10,18}""")) ||
                        part.matches(Regex("""[A-Z0-9]{16,22}""")) ||
                        upperPart.startsWith("TIMPS") || upperPart.startsWith("000FT") || upperPart.startsWith("FTIMPS") || upperPart.startsWith("UTRN"))) {
                val cleaned = part.replace(Regex("""^0+"""), "")
                if (cleaned.isNotBlank()) {
                    refNumber = cleaned
                    if (refNumber.length < 6) refNumber = part
                }
                continue
            }

            // Skip bank code or IFSC
            if (bankCodeRegex.matches(upperPart) || ifscRegex.matches(upperPart)) {
                continue
            }

            // Skip channel noise (e.g. NETBANK, MUM, P2A, P2P)
            if (channelNoise.contains(upperPart) || upperPart.startsWith("NETBANK")) {
                continue
            }

            // Ignore pure numbers or zero padding
            if (part.matches(Regex("""\d+"""))) {
                if (refNumber == null && part.length >= 8) {
                    refNumber = part
                }
                continue
            }

            // Meaningful text part (Name, Location, or Reason)
            textParts.add(part)
        }

        if (refNumber == null) {
            refNumber = extractRefNumber(cleanText)
        }

        val cleanUpper = cleanText.uppercase()
        val isAtm = (mode == "ATW" || mode == "NWD" || cleanUpper.startsWith("ATW") || cleanUpper.startsWith("NWD") || cleanUpper.contains("ATM CASH") || cleanUpper.contains("CASH WDL"))
        if (isAtm) {
            // Find the last segment (purpose), which in HDFC ATM transactions is usually the ATM location
            val locationCandidate = textParts.lastOrNull { p ->
                val u = p.uppercase()
                u != "ATM" && u != "NWD" && u != "ATW" && u != "ATM CASH" && u != "CASH WDL" &&
                        u != "CASH WITHDRAWAL" && u != "WDL" && u != "CASH" &&
                        !u.matches(Regex("""\d+"""))
            }

            val location = if (!locationCandidate.isNullOrBlank()) formatTitleCase(locationCandidate) else null
            val displayDesc = if (location != null) "ATM Withdrawal: $location" else "ATM Cash Withdrawal"

            return RemarksInfo(
                displayDescription = displayDesc,
                merchant = "HDFC ATM",
                referenceNumber = refNumber
            )
        }

        var reason: String? = null
        var name: String? = null

        if (textParts.size >= 2) {
            // HDFC layout: Payment mode - Ref - Receiver/Sender Name - Bank - Acc - Reason (Purpose)
            name = formatTitleCase(textParts.first())
            val rawReasonCandidate = textParts.last()
            reason = cleanReasonString(rawReasonCandidate, refNumber, accountBranch)

            if (reason == null && textParts.size > 2) {
                val middleCandidate = textParts[textParts.size - 2]
                val cleanedMiddle = cleanReasonString(middleCandidate, refNumber, accountBranch)
                if (cleanedMiddle != null && cleanedMiddle != name) {
                    reason = cleanedMiddle
                }
            }
        } else if (textParts.size == 1) {
            val singleText = textParts.first()
            val cleanedSingle = cleanReasonString(singleText, refNumber, accountBranch)
            if (cleanedSingle != null) {
                name = cleanedSingle
            }
        }

        // If the extracted reason is identical to the payee name, clear it to avoid repetition
        if (reason != null && name != null && reason.equals(name, ignoreCase = true)) {
            reason = null
        }

        // If name is a UPI VPA (e.g. 9307676700@UPI or swiggy@icici), format it cleanly
        if (name != null && name.contains("@")) {
            val vpaParts = name.split("@")
            val userPart = vpaParts[0].trim()
            if (userPart.matches(Regex("""\d{10}"""))) {
                name = "UPI: $userPart"
            } else if (userPart.length > 2) {
                name = formatTitleCase(userPart)
            }
        }

        // Purpose is more than enough for primary description when available.
        // Fallback to Mode: Name (e.g. "IMPS: John Doe", "POS: Starbucks") if no purpose.
        var displayDesc = when {
            reason != null -> reason
            name != null -> {
                if (mode != null && mode != "FT" && !name.startsWith(mode)) {
                    "$mode: $name"
                } else {
                    name
                }
            }
            mode != null -> "$mode Transfer"
            else -> formatTitleCase(cleanText)
        }

        // Guarantee no masked numbers or card ending phrases leak through
        displayDesc = displayDesc
            .replace(Regex("""\(?\s*(?:for\s+)?card\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(?\s*(?:for\s+)?ac(?:count)?\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b[A-Za-z0-9]*[Xx*]{2,}[A-Za-z0-9]*\b"""), "")
            .replace(Regex("""\bending\s+\d+\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return RemarksInfo(
            displayDescription = displayDesc,
            merchant = name,
            referenceNumber = refNumber
        )
    }

    private fun cleanReasonString(rawReason: String, refNumber: String?, accountBranch: String? = null): String? {
        var text = rawReason.trim()
        if (text.isBlank()) return null

        // Remove masked card/account patterns and card ending suffixes
        text = text.replace(Regex("""\b[A-Za-z0-9]*[Xx*]{2,}[A-Za-z0-9]*\b"""), " ")
        text = text.replace(Regex("""\(?\s*(?:for\s+)?card\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("""\(?\s*(?:for\s+)?ac(?:count)?\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("""\bending\s+\d+\b""", RegexOption.IGNORE_CASE), " ")

        val upper = text.uppercase()
        if (isStatementHeaderOrFooter(upper, accountBranch) ||
            upper.contains("NOMINATION") ||
            upper.contains("OPEN DATE") ||
            upper.contains("SHOWROOM") ||
            upper.contains("THIS STATEMENT") ||
            upper.contains("PAGE NO") ||
            upper.contains("STATEMENT OF") ||
            upper.contains("ACCOUNT NO") ||
            upper.contains("CUST ID") ||
            upper.contains("PB CUSTOMER") ||
            upper.contains("PRODUCT CODE") ||
            upper.contains("REGISTERED") ||
            upper.contains("BRANCH") ||
            (accountBranch != null && upper.contains(accountBranch.uppercase())) ||
            upper.contains("CONSIDERED CORRECT") ||
            upper.contains("REQUESTING")
        ) {
            return null
        }

        // Filter out non-informative generic reason tokens so we fall back to the actual Payee/Merchant name
        val genericNoiseTokens = setOf(
            "NA", "NIL", "NONE", "PAYMENT", "TRANSFER", "IMPS", "UPI", "NEFT", "RTGS",
            "SENT USING UPI", "PAID VIA UPI", "PAY", "BILLPAY", "BILL", "OK", "SUCCESS",
            "SUCCESSFUL", "COMPLETED", "DR", "CR", "NETBANK", "PERSONAL"
        )
        if (genericNoiseTokens.contains(upper)) {
            return null
        }

        // Remove refNumber if present in text
        if (!refNumber.isNullOrBlank()) {
            text = text.replace(refNumber, " ", ignoreCase = true)
            val refClean = refNumber.replace(Regex("""^0+"""), "")
            if (refClean.length >= 6) {
                text = text.replace(refClean, " ", ignoreCase = true)
            }
        }

        // Remove common reference/cheque patterns
        text = text.replace(Regex("""(?:CHQ|REF|CHQ\./REF\.NO|CHQ/REF|UTRN|IMPS|TIMPS|NEFT|RTGS)[\s./:-]*[A-Z0-9]{6,22}""", RegexOption.IGNORE_CASE), " ")

        // Remove standalone 8+ digit numbers or zero-padded sequences
        text = text.replace(Regex("""\b\d{8,22}\b"""), " ")
        text = text.replace(Regex("""\b0{4,}\d*\b"""), " ")

        // Remove standalone reference noise tokens
        text = text.replace(Regex("""\b(?:CHQ|REF|CHQ\./REF\.NO|NO|UTRN|TIMPS|IMPS)\b""", RegexOption.IGNORE_CASE), " ")

        // Clean up slashes, hyphens, colons, dots at edges
        text = text.replace(Regex("""^[/\-:\s.,]+|[/\-:\s.,]+$"""), " ")
        text = text.replace(Regex("""\s+"""), " ").trim()

        if (text.isBlank() || text.matches(Regex("""^[\d\W]+$"""))) {
            return null
        }

        if (genericNoiseTokens.contains(text.uppercase())) {
            return null
        }

        return formatTitleCase(text)
    }

    private fun extractRefNumber(text: String): String? {
        val utrMatch = Regex("""\b\d{12}\b""").find(text)
        if (utrMatch != null) return utrMatch.value
        val impsMatch = Regex("""(?:IMPS|TIMPS)[/-]?(\d{10,12})""", RegexOption.IGNORE_CASE).find(text)
        if (impsMatch != null) return impsMatch.groupValues[1]
        val refMatch = Regex("""(?:NEFT|RTGS)[/-]?([A-Z0-9]{10,22})""", RegexOption.IGNORE_CASE).find(text)
        if (refMatch != null) {
            val v = refMatch.groupValues[1]
            if (!v.contains("X", ignoreCase = true) && !v.contains("*")) return v
        }
        val ftMatch = Regex("""\b(?:0000)?(FT(?:IMPS)?[A-Z0-9]{6,16})\b""", RegexOption.IGNORE_CASE).find(text)
        if (ftMatch != null) return ftMatch.groupValues[1]
        return null
    }

    private fun extractColumnReferenceNumber(fullBlockText: String): String? {
        // Look for zero-padded tokens in the block, e.g. 00000000000018945, 0000FTIMPS012345, 0000818411585956
        val matches = Regex("""\b0{3,}([A-Za-z0-9]+)\b""").findAll(fullBlockText)
        for (match in matches) {
            val raw = match.value
            val cleaned = raw.replace(Regex("""^0+"""), "")
            // If all zeroes (e.g. 00000000000000000 for EMI debit or bank credit deposit), cleaned is empty -> dummy ref
            if (cleaned.isBlank()) continue
            // Never classify a masked card or account as a reference number
            if (cleaned.contains("X", ignoreCase = true) || cleaned.contains("*")) continue
            return cleaned
        }
        return null
    }

    private fun formatTitleCase(str: String): String {
        var text = str.trim()
        text = text.replace(Regex("""\(?\s*(?:for\s+)?card\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("""\(?\s*(?:for\s+)?ac(?:count)?\s+ending\s+\d+\s*\)?""", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("""\b[A-Za-z0-9]*[Xx*]{2,}[A-Za-z0-9]*\b"""), " ")
        text = text.replace(Regex("""\bending\s+\d+\b""", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("""\s+"""), " ").trim()

        val upper = text.uppercase()
        if (upper == "MIN BAL MAINTAIN" || upper == "MIN BAL MAINTENANCE" || upper.contains("MIN BAL")) {
            return "Min Balance Maintenance"
        }
        val acronyms = setOf("ATM", "POS", "UPI", "NWD", "EMI", "IB", "NEFT", "RTGS", "ACH", "IMPS", "FT", "CHQ", "RRN", "UTR", "VPA", "ATW")
        return text.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            val wUpper = word.uppercase()
            if (wUpper == "FOR" || wUpper == "TILL" || wUpper == "AND" || wUpper == "TO" || wUpper == "OF") {
                wUpper.lowercase()
            } else if (acronyms.contains(wUpper)) {
                wUpper
            } else {
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
            }
        }
    }
}
