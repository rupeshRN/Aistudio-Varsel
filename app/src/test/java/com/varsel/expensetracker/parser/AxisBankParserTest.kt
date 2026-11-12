package com.varsel.expensetracker.parser

import com.varsel.expensetracker.category.Category
import com.varsel.expensetracker.category.CategoryRuleEngine
import com.varsel.expensetracker.category.CustomRuleEngine
import com.varsel.expensetracker.category.DescriptionNormalizer
import com.varsel.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AxisBankParserTest {

    private lateinit var axisBankParser: AxisBankParser

    @Before
    fun setup() {
        val descriptionNormalizer = DescriptionNormalizer()
        val customRuleEngine = CustomRuleEngine(descriptionNormalizer)
        val categoryRuleEngine = CategoryRuleEngine(customRuleEngine)
        val descriptionCleaner = DescriptionCleaner()

        axisBankParser = AxisBankParser(
            categoryRuleEngine = categoryRuleEngine,
            descriptionCleaner = descriptionCleaner,
            descriptionNormalizer = descriptionNormalizer
        )
    }

    @Test
    fun `test canParse detects Axis Bank statements`() {
        val sampleStatement = """
            AXIS BANK LIMITED
            Statement of Account
            Account No: 918010012345678
            Branch: KORAMANGALA, BENGALURU
            IFSC: UTIB0000123
            Tran Date   Value Date  Particulars                         Chq No  Debit       Credit      Balance
            10-02-2024  10-02-2024  UPI/404123456789/ZOMATO/BANGALORE           620.00                  38500.00
        """.trimIndent()

        assertTrue(axisBankParser.canParse(sampleStatement))
    }

    @Test
    fun `test canParse rejects competitor bank statements`() {
        val iciciStatement = """
            ICICI BANK LIMITED
            STATEMENT OF ACCOUNT
            Account Number: 001105001234
            Date        Particulars                     Amount
            15/03/2024  NEFT-TRANSFER                   1500.00
        """.trimIndent()

        assertFalse(axisBankParser.canParse(iciciStatement))
    }

    @Test
    fun `test parse extracts Axis transactions with UPI POS and NEFT details`() {
        val axisStatement = """
            AXIS BANK LIMITED
            Statement of Account for Account No: 918010012345678
            Customer ID: 123456789
            Tran Date   Value Date  Transaction Details                         Chq No  Debit       Credit      Balance
            15-02-2024  15-02-2024  UPI/404612345678/AMAZON/RETAIL                      1299.00                 45200.00
            20-02-2024  20-02-2024  NEFT-AXISR01234567890-SALARY FOR FEB 2024                       85000.00    130200.00
            25-02-2024  25-02-2024  POS/DEBIT CARD/STARBUCKS/BANGALORE                  420.00                  129780.00
        """.trimIndent()

        val transactions = axisBankParser.parse(axisStatement)
        assertEquals(3, transactions.size)

        // Txn 1: Amazon UPI expense
        assertEquals(TransactionType.EXPENSE, transactions[0].type)
        assertEquals(1299.00, transactions[0].amount, 0.01)
        assertEquals("404612345678", transactions[0].referenceNumber)
        assertEquals("Axis Bank", transactions[0].bankName)

        // Txn 2: Salary credit
        assertEquals(TransactionType.INCOME, transactions[1].type)
        assertEquals(85000.00, transactions[1].amount, 0.01)
        assertEquals(Category.SALARY, transactions[1].category)

        // Txn 3: Starbucks POS
        assertEquals(TransactionType.EXPENSE, transactions[2].type)
        assertEquals(420.00, transactions[2].amount, 0.01)
    }

    @Test
    fun `test parseWithBalances extracts running balances and summary`() {
        val axisStatement = """
            AXIS BANK LIMITED
            STATEMENT OF ACCOUNT
            OPENING BALANCE: 46,499.00
            Tran Date   Value Date  Transaction Details             Debit       Credit      Balance
            15-02-2024  15-02-2024  UPI/404612345678/AMAZON         1299.00                 45200.00
            20-02-2024  20-02-2024  SALARY CREDIT                               85000.00    130200.00
            CLOSING BALANCE: 130,200.00
        """.trimIndent()

        val parsedRows = axisBankParser.parseWithBalances(axisStatement)
        assertEquals(2, parsedRows.size)
        assertEquals(45200.00, parsedRows[0].second ?: 0.0, 0.01)
        assertEquals(130200.00, parsedRows[1].second ?: 0.0, 0.01)

        val transactions = parsedRows.map { it.first }
        val summary = axisBankParser.extractSummary(axisStatement, transactions)
        assertNotNull(summary)
        assertEquals(46499.00, summary?.openingBalance ?: 0.0, 0.01)
        assertEquals(1299.00, summary?.totalDebits ?: 0.0, 0.01)
        assertEquals(85000.00, summary?.totalCredits ?: 0.0, 0.01)
        assertEquals(130200.00, summary?.endingBalance ?: 0.0, 0.01)
    }
}
