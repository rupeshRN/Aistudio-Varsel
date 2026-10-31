package com.varsel.expensetracker.parser

import com.varsel.expensetracker.category.CategoryRuleEngine
import com.varsel.expensetracker.category.CustomRuleEngine
import com.varsel.expensetracker.category.DescriptionNormalizer
import com.varsel.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IndianBankParserTest {

    private lateinit var indianBankParser: IndianBankParser

    @Before
    fun setup() {
        val statementEndDetector = StatementEndDetector()
        val blockBuilder = TransactionBlockBuilder(statementEndDetector)
        val merchantExtractor = MerchantExtractor()
        val descriptionCleaner = DescriptionCleaner()
        val slashTokenizer = SlashTokenizer()
        val tokenNormalizer = TokenNormalizer()
        val fieldInterpreter = FieldInterpreter()
        val amountInterpreter = AmountInterpreter()
        val parserRuleEngine = ParserRuleEngine()
        val parserConfidenceEngine = ParserConfidenceEngine(parserRuleEngine)
        val displayDescriptionBuilder = DisplayDescriptionBuilder()
        val descriptionNormalizer = DescriptionNormalizer()
        val customRuleEngine = CustomRuleEngine(descriptionNormalizer)
        val categoryRuleEngine = CategoryRuleEngine(customRuleEngine)

        indianBankParser = IndianBankParser(
            blockBuilder = blockBuilder,
            merchantExtractor = merchantExtractor,
            descriptionCleaner = descriptionCleaner,
            slashTokenizer = slashTokenizer,
            tokenNormalizer = tokenNormalizer,
            fieldInterpreter = fieldInterpreter,
            amountInterpreter = amountInterpreter,
            parserConfidenceEngine = parserConfidenceEngine,
            displayDescriptionBuilder = displayDescriptionBuilder,
            categoryRuleEngine = categoryRuleEngine
        )
    }

    @Test
    fun `test canParse detects modern Indian Bank statement`() {
        val modernStatement = """
            INDIAN BANK
            ACCOUNT STATEMENT
            ACCOUNT ACTIVITY
            DATE TRANSACTION DETAILS DEBITS CREDITS BALANCE
            28 Jul 2026 UPI/123456789012/Grocery Store INR 550.00 - INR 12500.00
        """.trimIndent()

        assertTrue(indianBankParser.canParse(modernStatement))
    }

    @Test
    fun `test canParse detects legacy Indian Bank statement with numeric dates`() {
        val legacyStatement = """
            INDIAN BANK
            STATEMENT OF ACCOUNT
            Branch: CHENNAI MAIN (IDIB000M001)
            DATE PARTICULARS WITHDRAWAL DEPOSIT BALANCE
            12/04/2024 ELECTRICITY BILL PAYMENT 1200.00 45000.00
        """.trimIndent()

        assertTrue(indianBankParser.canParse(legacyStatement))
    }

    @Test
    fun `test canParse strictly rejects competitor bank statements`() {
        val sbiStatement = """
            STATE BANK OF INDIA
            ACCOUNT STATEMENT
            DATE PARTICULARS WITHDRAWAL DEPOSIT BALANCE
            12/04/2024 UPI/TRANSFER 500.00 10000.00
        """.trimIndent()

        assertFalse(indianBankParser.canParse(sbiStatement))

        val iciciStatement = """
            ICICI BANK
            Statement of Transactions
            DATE TRANSACTION REMARKS WITHDRAWAL AMOUNT (INR) DEPOSIT AMOUNT (INR) BALANCE (INR)
            12/04/2024 UPI/TRANSFER 500.00 10000.00
        """.trimIndent()

        assertFalse(indianBankParser.canParse(iciciStatement))
    }

    @Test
    fun `test parse extracts transactions from legacy Indian Bank statement`() {
        val legacyStatement = """
            INDIAN BANK
            STATEMENT OF ACCOUNT
            Branch: CHENNAI MAIN (IDIB000M001)
            DATE PARTICULARS WITHDRAWAL DEPOSIT BALANCE
            12/04/2024 ELECTRICITY BILL PAYMENT 1200.00 45000.00
            15/04/2024 SALARY CREDIT 35000.00 80000.00 CR
        """.trimIndent()

        val transactions = indianBankParser.parse(legacyStatement)
        assertEquals(2, transactions.size)

        assertEquals(1200.00, transactions[0].amount, 0.01)
        assertEquals(TransactionType.EXPENSE, transactions[0].type)
        assertEquals("Indian Bank", transactions[0].bankName)

        assertEquals(35000.00, transactions[1].amount, 0.01)
        assertEquals(TransactionType.INCOME, transactions[1].type)
        assertEquals("Indian Bank", transactions[1].bankName)
    }
}
