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

class SbiBankParserTest {

    private lateinit var sbiBankParser: SbiBankParser

    @Before
    fun setup() {
        val descriptionNormalizer = DescriptionNormalizer()
        val customRuleEngine = CustomRuleEngine(descriptionNormalizer)
        val categoryRuleEngine = CategoryRuleEngine(customRuleEngine)
        val descriptionCleaner = DescriptionCleaner()

        sbiBankParser = SbiBankParser(
            categoryRuleEngine = categoryRuleEngine,
            descriptionCleaner = descriptionCleaner,
            descriptionNormalizer = descriptionNormalizer
        )
    }

    @Test
    fun `test canParse detects State Bank of India statements`() {
        val sampleStatement = """
            STATE BANK OF INDIA
            Account Statement for Account: 00000030123456789
            Branch: CONNAUGHT PLACE NEW DELHI
            IFS Code: SBIN0000691
            Txn Date    Value Date  Description                     Ref No./Cheque No.  Debit       Credit      Balance
            01 Jan 2024 01 Jan 2024 UPI/400123456789/SWIGGY/PAYMENT                     450.00                  25000.00
        """.trimIndent()

        assertTrue(sbiBankParser.canParse(sampleStatement))
    }

    @Test
    fun `test canParse detects YONO SBI statement`() {
        val yonoStatement = """
            YONO SBI
            ACCOUNT STATEMENT
            Account Number: 20451234567
            Statement Period: 01-Jan-2024 to 31-Jan-2024
        """.trimIndent()

        assertTrue(sbiBankParser.canParse(yonoStatement))
    }

    @Test
    fun `test canParse rejects competitor bank statements`() {
        val hdfcStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Account Number: 5010023456789
            Date        Narration                       Chq/Ref No   Withdrawal  Deposit   Balance
            01/01/2024  UPI-SWIGGY-123456789012                      250.00                15000.00
        """.trimIndent()

        assertFalse(sbiBankParser.canParse(hdfcStatement))
    }

    @Test
    fun `test parse extracts SBI transactions with UPI and IMPS details`() {
        val sbiStatement = """
            STATE BANK OF INDIA
            Account Name: RAJESH KUMAR
            Account Number: 00000030123456789
            CIF No: 85201234567
            IFS Code: SBIN0000691
            Txn Date    Value Date  Description                                 Ref No./Cheque    Debit       Credit      Balance
            05 Jan 2024 05 Jan 2024 TO TRANSFER-UPI/DR/400512345678/SWIGGY/Bangalore TRANSFER       350.00                  48200.00
            08 Jan 2024 08 Jan 2024 BY TRANSFER-INB SALARY CREDIT FOR DEC 2023   TRANSFER                    75000.00    123200.00
            12 Jan 2024 12 Jan 2024 TO TRANSFER-IMPS/401298765432/RENT/LANDLORD  TRANSFER        18000.00                 105200.00
        """.trimIndent()

        val transactions = sbiBankParser.parse(sbiStatement)
        assertEquals(3, transactions.size)

        // Txn 1: Swiggy expense
        assertEquals(TransactionType.EXPENSE, transactions[0].type)
        assertEquals(350.00, transactions[0].amount, 0.01)
        assertEquals("400512345678", transactions[0].referenceNumber)
        assertEquals("State Bank of India (SBI)", transactions[0].bankName)

        // Txn 2: Salary credit
        assertEquals(TransactionType.INCOME, transactions[1].type)
        assertEquals(75000.00, transactions[1].amount, 0.01)
        assertEquals(Category.SALARY, transactions[1].category)

        // Txn 3: Rent expense
        assertEquals(TransactionType.EXPENSE, transactions[2].type)
        assertEquals(18000.00, transactions[2].amount, 0.01)
        assertEquals("401298765432", transactions[2].referenceNumber)
    }

    @Test
    fun `test parseWithBalances extracts running balances and calculates statement summary`() {
        val sbiStatement = """
            STATE BANK OF INDIA
            STATEMENT OF ACCOUNT
            OPENING BALANCE: 48,550.00
            Txn Date    Value Date  Description                     Debit       Credit      Balance
            05 Jan 2024 05 Jan 2024 UPI/400512345678/SWIGGY/DELIVERY 350.00                  48200.00
            08 Jan 2024 08 Jan 2024 BY TRANSFER-SALARY                         75000.00    123200.00
            CLOSING BALANCE: 123,200.00
        """.trimIndent()

        val parsedRows = sbiBankParser.parseWithBalances(sbiStatement)
        assertEquals(2, parsedRows.size)
        assertEquals(48200.00, parsedRows[0].second ?: 0.0, 0.01)
        assertEquals(123200.00, parsedRows[1].second ?: 0.0, 0.01)

        val transactions = parsedRows.map { it.first }
        val summary = sbiBankParser.extractSummary(sbiStatement, transactions)
        assertNotNull(summary)
        assertEquals(48550.00, summary?.openingBalance ?: 0.0, 0.01)
        assertEquals(350.00, summary?.totalDebits ?: 0.0, 0.01)
        assertEquals(75000.00, summary?.totalCredits ?: 0.0, 0.01)
        assertEquals(123200.00, summary?.endingBalance ?: 0.0, 0.01)
    }
}
