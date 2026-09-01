package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.Transaction
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

        val hasIndianBankBrand = upper.contains("INDIAN BANK") ||
                upper.contains("IDIB") ||
                upper.contains("IND BL") ||
                upper.contains("INDIANBANK")

        val hasIndianBankLayout = upper.contains("ACCOUNT ACTIVITY") ||
                (upper.contains("ACCOUNT DETAILS") && upper.contains("ACCOUNT SUMMARY")) ||
                (upper.contains("DATE TRANSACTION DETAILS") && upper.contains("DEBITS") && upper.contains("CREDITS"))

        // Check if it's an ICICI statement with ICICI table headers
        val hasIciciTable = upper.contains("TRANSACTION REMARKS") &&
                (upper.contains("WITHDRAWAL AMOUNT") || upper.contains("DEPOSIT AMOUNT") || upper.contains("BALANCE (INR)"))

        if (hasIciciTable && !hasIndianBankBrand) {
            return false
        }

        return hasIndianBankBrand || hasIndianBankLayout
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

val category =
    categoryRuleEngine.categorize(description)

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
                    referenceNumber = fields.reference
                )
            )
        }

        return transactions
    }
}
