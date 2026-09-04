package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import com.varsel.expensetracker.category.CategoryRuleEngine
import com.varsel.expensetracker.category.CustomRuleEngine
import com.varsel.expensetracker.category.Category
import com.varsel.expensetracker.category.CategoryResult

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
        val header = rawText.lines().take(30).joinToString("\n").uppercase()

        val hasIndianBankBrand = header.contains("INDIAN BANK") ||
                header.contains("INDIANBANK") ||
                header.contains("IND BL") ||
                header.contains("IDIB") ||
                upper.contains("INDIAN BANK")

        val hasIndianBankLayout = upper.contains("ACCOUNT ACTIVITY") ||
                (upper.contains("DATE TRANSACTION DETAILS") && upper.contains("DEBITS") && upper.contains("CREDITS"))

        val hasIndianBankDates = Regex("""\b\d{1,2}\s+(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s+\d{4}\b""", RegexOption.IGNORE_CASE).containsMatchIn(rawText)

        return (hasIndianBankBrand || hasIndianBankLayout) && (hasIndianBankDates || upper.contains("INDIAN BANK"))
    }

    override fun parse(rawText: String): List<Transaction> {

        val blocks = blockBuilder.build(rawText)

        val transactions = mutableListOf<Transaction>()

        val dateRegex =
            Regex("^\\d{1,2}\\s*[A-Za-z]{3}\\s+\\d{4}")

        val dateFormatter =
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

        for (block in blocks) {

            if (block.lines.isEmpty())
                continue

            val firstLine = block.lines.first()

            val dateMatch =
                dateRegex.find(firstLine) ?: continue

            val date = try {
                dateFormatter.parse(dateMatch.value)
            } catch (e: Exception) {
                null
            } ?: continue

            //--------------------------------------------------
            // Amount + Type
            //--------------------------------------------------

            val parsedAmount =
                amountInterpreter.parse(firstLine)
                    ?: continue

            //--------------------------------------------------
            // Description
            //--------------------------------------------------

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

val isIncome = parsedAmount.type == TransactionType.INCOME || parsedAmount.type == TransactionType.CREDIT
val category =
    categoryRuleEngine.categorize(description, isIncome)

            //--------------------------------------------------
            // Transaction
            //--------------------------------------------------

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

        return transactions
    }
}
