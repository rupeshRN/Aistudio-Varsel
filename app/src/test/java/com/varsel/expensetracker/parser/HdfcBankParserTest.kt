package com.varsel.expensetracker.parser

import com.varsel.expensetracker.category.Category
import com.varsel.expensetracker.category.CategoryRuleEngine
import com.varsel.expensetracker.category.CustomRuleEngine
import com.varsel.expensetracker.category.DescriptionNormalizer
import com.varsel.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HdfcBankParserTest {

    private lateinit var hdfcBankParser: HdfcBankParser

    @Before
    fun setup() {
        val descriptionNormalizer = DescriptionNormalizer()
        val customRuleEngine = CustomRuleEngine(descriptionNormalizer)
        val categoryRuleEngine = CategoryRuleEngine(customRuleEngine)
        val descriptionCleaner = DescriptionCleaner()

        hdfcBankParser = HdfcBankParser(
            categoryRuleEngine = categoryRuleEngine,
            descriptionCleaner = descriptionCleaner,
            descriptionNormalizer = descriptionNormalizer
        )
    }

    @Test
    fun testUpiNarrationWithGenericReasonFallsBackToPayee() {
        val sampleStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            01/08/2026 UPI-423512345678-SWIGGY-HDFC-xxxxxx-NA 000000000000000 01/08/2026 450.00 9,550.00
        """.trimIndent()

        val transactions = hdfcBankParser.parse(sampleStatement)
        assertEquals(1, transactions.size)
        val tx = transactions.first()
        assertEquals(450.00, tx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("UPI: Swiggy", tx.description)
        assertEquals("423512345678", tx.referenceNumber)
        assertEquals(Category.FOOD, tx.category)
    }

    @Test
    fun testUpiNarrationWithMeaningfulReasonPreserved() {
        val sampleStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            02/08/2026 UPI-423599998888-RAVI SHARMA-SBIN-xxxxxx-ROOM RENT 000000000000000 02/08/2026 12,000.00 2,550.00
        """.trimIndent()

        val transactions = hdfcBankParser.parse(sampleStatement)
        assertEquals(1, transactions.size)
        val tx = transactions.first()
        assertEquals(12000.00, tx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Room Rent", tx.description)
        assertEquals("423599998888", tx.referenceNumber)
    }

    @Test
    fun testPosMerchantNormalization() {
        val sampleStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            03/08/2026 POS 416021XXXXXX1234 STARBUCKS BANGALORE KA IN 000000000000000 03/08/2026 350.00 2,200.00
        """.trimIndent()

        val transactions = hdfcBankParser.parse(sampleStatement)
        assertEquals(1, transactions.size)
        val tx = transactions.first()
        assertEquals(350.00, tx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("POS: Starbucks Bangalore", tx.description)
    }

    @Test
    fun testInterestPaidExtraction() {
        val sampleStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            04/08/2026 INTEREST PAID TILL 31-JUL-2026 000000000000000 04/08/2026 125.50 2,325.50
        """.trimIndent()

        val transactions = hdfcBankParser.parse(sampleStatement)
        assertEquals(1, transactions.size)
        val tx = transactions.first()
        assertEquals(125.50, tx.amount, 0.001)
        assertEquals(TransactionType.INCOME, tx.type)
        assertTrue(tx.description.contains("Interest Paid till"))
        assertEquals(Category.OTHER_INCOME, tx.category)
    }
}
