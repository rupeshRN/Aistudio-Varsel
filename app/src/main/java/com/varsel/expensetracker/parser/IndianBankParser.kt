package com.varsel.expensetracker.parser

import com.varsel.expensetracker.category.CategoryRuleEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class IndianBankParser @Inject constructor(
    private val blockBuilder: TransactionBlockBuilder,
    private val merchantExtractor: MerchantExtractor,
    private val descriptionCleaner: DescriptionCleaner,
    private val slashTokenizer: SlashTokenizer,
    private val tokenNormalizer: TokenNormalizer,
    private val fieldInterpreter: FieldInterpreter,
    private val amountInterpreter: AmountInterpreter,
    private val parserConfidenceEngine: ParserConfidenceEngine,
    private val displayDescriptionBuilder: DisplayDescriptionBuilder,
    private val categoryRuleEngine: CategoryRuleEngine,
) : StatementParser {

    override fun canParse(rawText: String): Boolean {

        val upper = rawText.uppercase()

        // 1. Strict negative check against competing banks to prevent false detection
        val isCompetitorBank =
            upper.contains("ICICI BANK") ||
            upper.contains("HDFC BANK") ||
            upper.contains("STATE BANK OF INDIA") ||
            upper.contains("YONO SBI") ||
            upper.contains("AXIS BANK") ||
            upper.contains("KOTAK") ||
            upper.contains("PUNJAB NATIONAL BANK")

        if (isCompetitorBank) {
            return false
        }

        /*
         * ------------------------------------------------------------
         * 1. Examine the statement header separately.
         *
         * Bank names appearing inside transaction narrations should
         * not be sufficient to identify the statement.
         * ------------------------------------------------------------
         */
        val header = rawText
            .lines()
            .take(40)
            .joinToString("\n")
            .uppercase()

        /*
         * ------------------------------------------------------------
         * 2. Strong Indian Bank identity evidence
         * ------------------------------------------------------------
         */
        val hasStrongIndianBankBrand =
            header.contains("INDIAN BANK") ||
            header.contains("INDIANBANK") ||
            header.contains("IND BL") ||
            header.contains("IDIB") ||
            upper.contains("WWW.INDIANBANK.IN") ||
            upper.contains("INDIAN BANK")

        /*
         * ------------------------------------------------------------
         * 3. Indian Bank statement-layout evidence
         *
         * These are deliberately based on combinations of fields,
         * rather than a single generic word.
         * ------------------------------------------------------------
         */
        val hasAccountActivity =
            upper.contains("ACCOUNT ACTIVITY")

        val hasTransactionDetails =
            upper.contains("TRANSACTION DETAILS")

        val hasDebits =
            upper.contains("DEBITS")

        val hasCredits =
            upper.contains("CREDITS")

        val hasBalance =
            upper.contains("BALANCE")

        val hasDateColumn =
            Regex(
                """\bDATE\b"""
            ).containsMatchIn(header)

        /*
         * Full layout characteristic of modern Indian Bank statement format.
         */
        val hasFullIndianBankTable =
            hasAccountActivity &&
            hasTransactionDetails &&
            hasDebits &&
            hasCredits &&
            hasBalance

        /*
         * Alternate table evidence.
         */
        val hasIndianBankTransactionTable =
            hasTransactionDetails &&
            hasDebits &&
            hasCredits &&
            hasBalance &&
            hasDateColumn

        /*
         * Legacy / standard statement table evidence (passbooks and older PDF layouts).
         */
        val hasLegacyParticulars =
            upper.contains("PARTICULARS") ||
            upper.contains("DESCRIPTION") ||
            upper.contains("NARRATION") ||
            upper.contains("STATEMENT OF ACCOUNT")

        val hasLegacyDebits =
            upper.contains("WITHDRAWAL") ||
            upper.contains("DEBIT") ||
            upper.contains("DEBITS") ||
            upper.contains("DR")

        val hasLegacyCredits =
            upper.contains("DEPOSIT") ||
            upper.contains("CREDIT") ||
            upper.contains("CREDITS") ||
            upper.contains("CR")

        val hasLegacyTable =
            hasLegacyParticulars &&
            hasLegacyDebits &&
            hasLegacyCredits &&
            hasBalance

        /*
         * ------------------------------------------------------------
         * 4. Date evidence
         * ------------------------------------------------------------
         */
        val hasAlphaDate =
            Regex(
                """\b\d{1,2}(?:\s+|[-/.])(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:\s+|[-/.])\d{2,4}\b"""
            ).containsMatchIn(upper)

        val hasNumericDate =
            Regex(
                """\b\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}\b"""
            ).containsMatchIn(upper)

        val hasSupportedIndianBankDate =
            hasAlphaDate || hasNumericDate

        /*
         * ------------------------------------------------------------
         * 5. Strong identity + statement evidence
         * ------------------------------------------------------------
         */
        if (hasStrongIndianBankBrand) {
            return hasSupportedIndianBankDate &&
                    (hasFullIndianBankTable || hasIndianBankTransactionTable || hasLegacyTable || upper.contains("STATEMENT") || upper.contains("ACCOUNT"))
        }

        /*
         * ------------------------------------------------------------
         * 6. Layout-only detection (OCR fallback)
         * ------------------------------------------------------------
         */
        if (hasFullIndianBankTable && hasSupportedIndianBankDate) {
            return true
        }

        if (hasIndianBankTransactionTable && hasSupportedIndianBankDate) {
            return true
        }

        return false
    }

    override fun parse(rawText: String): List<Transaction> {

        val blocks = blockBuilder.build(rawText)

        val transactions = mutableListOf<Transaction>()

        val dateRegex =
            Regex("""^\s*(\d{1,2}(?:\s+|[-/.])(?:[A-Za-z]{3}|\d{1,2})(?:\s+|[-/.])\d{2,4})""")

        val dateFormatters = listOf(
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yy", Locale.ENGLISH)
        )

        for (block in blocks) {

            if (block.lines.isEmpty())
                continue

            val firstLine = block.lines.first()

            val dateMatch =
                dateRegex.find(firstLine) ?: continue

            val dateStr = dateMatch.groupValues[1].trim()
            var parsedDate: java.util.Date? = null
            for (formatter in dateFormatters) {
                try {
                    parsedDate = formatter.parse(dateStr)
                    if (parsedDate != null) break
                } catch (_: Exception) {
                }
            }

            val date = parsedDate ?: continue

            val parsedAmount =
                amountInterpreter.parse(firstLine)
                    ?: continue

            val allText =
                block.lines.joinToString(" ")

            var rawDescription = allText

            rawDescription =
                rawDescription.replace(dateMatch.value, "")

            rawDescription =
                rawDescription.replaceFirst(
                    Regex("INR\\s*[\\d,]+\\.\\d{2}"),
                    ""
                )

            rawDescription =
                rawDescription.replaceFirst(
                    Regex("INR\\s*[\\d,]+\\.\\d{2}"),
                    ""
                )

            rawDescription =
                rawDescription.replaceFirst(
                    Regex("""(?<![.\d])[\d,]+\.\d{2}(?![.\d])"""),
                    ""
                )

            rawDescription =
                rawDescription.replaceFirst(
                    Regex("""(?<![.\d])[\d,]+\.\d{2}(?![.\d])"""),
                    ""
                )

            rawDescription = rawDescription.trim()

            val tokens =
                slashTokenizer.tokenize(rawDescription)

            val normalizedTokens =
                tokenNormalizer.normalize(tokens)

            val fields =
                fieldInterpreter.interpret(normalizedTokens)

            val confidence =
                parserConfidenceEngine.evaluate(fields)

            val description =
                displayDescriptionBuilder.build(
                    fields = fields,
                    fallback =
                        descriptionCleaner.clean(rawDescription)
                )

            val isIncome =
                parsedAmount.type == TransactionType.INCOME ||
                        parsedAmount.type == TransactionType.CREDIT

            val category =
                categoryRuleEngine.categorize(
                    description,
                    isIncome
                )

            transactions.add(
                Transaction(
                    amount = parsedAmount.amount,
                    type = parsedAmount.type,
                    description = description,
                    category = category.category,
                    dateTimestamp = date.time,
                    referenceNumber = fields.reference,
                    bankName = "Indian Bank",
                    rawDescription = rawDescription
                )
            )
        }

        if (transactions.isNotEmpty()) {
            return transactions
        }

        // Fallback for legacy format statements where blockBuilder produces no blocks
        return parseLegacyFormat(rawText)
    }

    private fun parseLegacyFormat(rawText: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val dateRegex = Regex("""^\s*(\d{1,2}(?:\s+|[-/.])(?:[A-Za-z]{3}|\d{1,2})(?:\s+|[-/.])\d{2,4})""")
        val amountRegex = Regex("""(?<![.\d])([0-9]{1,3}(?:,[0-9]{3})*\.\d{2}|\d+\.\d{2})(?![.\d])""")

        val dateFormatters = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yy", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        )

        for (line in lines) {
            val dateMatch = dateRegex.find(line) ?: continue
            val dateStr = dateMatch.groupValues[1].trim()
            var date: java.util.Date? = null
            for (formatter in dateFormatters) {
                try {
                    date = formatter.parse(dateStr)
                    if (date != null) break
                } catch (_: Exception) {
                }
            }

            val parsedDate = date ?: continue
            val amountMatches = amountRegex.findAll(line).toList()
            if (amountMatches.isEmpty()) continue

            val txAmount = amountMatches[0].groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            if (txAmount <= 0.0) continue

            val upperLine = line.uppercase()
            val isIncome = upperLine.contains(" CR") || upperLine.endsWith("CR") || upperLine.contains("CREDIT") || upperLine.contains("DEPOSIT")
            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

            var desc = line
                .replace(dateMatch.value, "")
                .replace(amountMatches[0].value, "")
            if (amountMatches.size > 1) {
                desc = desc.replace(amountMatches[1].value, "")
            }
            desc = desc.replace(Regex("""\b(CR|DR|INR|Rs\.?)\b""", RegexOption.IGNORE_CASE), "")
                .replace("|", " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (desc.isBlank() || desc.length < 3) {
                desc = if (isIncome) "Indian Bank Deposit" else "Indian Bank Withdrawal"
            }

            val cleanedDesc = descriptionCleaner.clean(desc)
            val category = categoryRuleEngine.categorize(cleanedDesc, isIncome)

            transactions.add(
                Transaction(
                    amount = txAmount,
                    type = type,
                    description = cleanedDesc,
                    category = category.category,
                    dateTimestamp = parsedDate.time,
                    referenceNumber = null,
                    bankName = "Indian Bank",
                    rawDescription = desc
                )
            )
        }

        return transactions
    }
}
