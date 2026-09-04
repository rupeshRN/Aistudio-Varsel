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

    @Test
    fun testLastTransactionDoesNotContainAppendedBranchAddressOrThisStatement() {
        val sampleStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            04/08/2026 UPI-423512345678-SWIGGY-HDFC-xxxxxx-NA 000000000000000 04/08/2026 450.00 9,550.00
            05/08/2026 UPI-423588889999-AMAZON-HDFC-xxxxxx-SHOPPING 000000000000000 05/08/2026 1,200.00 8,350.00
            This Statement. Kodambakkam, Chennai - 600024
            Requesting Branch : Kodambakkam
            Contents of this statement will be considered correct if no error is reported within 30 days.
        """.trimIndent()

        val transactions = hdfcBankParser.parse(sampleStatement)
        assertEquals(2, transactions.size)
        val lastTx = transactions.last()
        assertEquals(1200.00, lastTx.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, lastTx.type)
        assertEquals("Shopping", lastTx.description)
        org.junit.Assert.assertFalse(lastTx.description.contains("This Statement"))
        org.junit.Assert.assertFalse(lastTx.description.contains("Kodambakkam"))
        org.junit.Assert.assertFalse(lastTx.description.contains("Chennai"))
        org.junit.Assert.assertFalse(lastTx.rawDescription.orEmpty().contains("This Statement"))
        org.junit.Assert.assertFalse(lastTx.rawDescription.orEmpty().contains("Kodambakkam"))
    }

    @Test
    fun testMultiPageBranchFootersAreSkippedWithoutStoppingNextPages() {
        val multiPageStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            01/08/2026 UPI-423512345678-SWIGGY-HDFC-xxxxxx-NA 000000000000000 01/08/2026 300.00 10,000.00
            Requesting Branch: Kodambakkam
            Kodambakkam, Chennai - 600024
            Page No .: 1
            STATEMENT OF ACCOUNT
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            02/08/2026 UPI-423588889999-ZOMATO-HDFC-xxxxxx-FOOD 000000000000000 02/08/2026 500.00 9,500.00
            Requesting Branch: Kodambakkam
            Kodambakkam, Chennai - 600024
            Page No .: 2
        """.trimIndent()

        val transactions = hdfcBankParser.parse(multiPageStatement)
        assertEquals(2, transactions.size)
        assertEquals(300.00, transactions[0].amount, 0.001)
        assertEquals(500.00, transactions[1].amount, 0.001)
        org.junit.Assert.assertFalse(transactions[0].description.contains("Kodambakkam"))
        org.junit.Assert.assertFalse(transactions[1].description.contains("Kodambakkam"))
    }

    @Test
    fun testRealHdfcStatementSampleFromUserImage() {
        val sampleStatement = """
            HDFC BANK LIMITED
            ACCOUNT STATEMENT
            Account Branch : Kodambakkam
            Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
            03/07/18 IMPS-818411585956-ARUNARAVINDRADUBEY-HDF 0000818411585956 03/07/18 3,000.00 10,633.81
            03/07/18 NEFT DR-UBIN0539686-RAJU DUBEY-NETBANK, MUM-N155180555427618-PERSONAL 000000000000000 03/07/18 5,000.00 5,633.81
            04/07/18 NHDF6376325463/SBI CARDS 000000000000000 04/07/18 2,150.00 3,483.81
            04/07/18 UPI-303702011409044-9307676700@UPI-815518551633-OK 000000000000000 04/07/18 100.00 3,383.81
            05/07/18 CREDIT INTEREST CAPITALISED 000000000000000 05/07/18 152.00 3,535.81
            Contents of this statement will be considered correct if no error is reported within 30 days of receipt of statement. The address on this statement is that on record with the Bank as at the day of requesting this statement.
            Kodambakkam,
        """.trimIndent()

        val transactions = hdfcBankParser.parse(sampleStatement)
        assertEquals(5, transactions.size)

        // 1. IMPS
        assertEquals(3000.00, transactions[0].amount, 0.001)
        assertEquals("IMPS: Arunaravindradubey", transactions[0].description)
        assertEquals("818411585956", transactions[0].referenceNumber)

        // 2. NEFT
        assertEquals(5000.00, transactions[1].amount, 0.001)
        assertEquals("NEFT: Raju Dubey", transactions[1].description)
        assertEquals("N155180555427618", transactions[1].referenceNumber)

        // 3. BillPay / Cards
        assertEquals(2150.00, transactions[2].amount, 0.001)
        assertEquals("SBI Cards", transactions[2].description)

        // 4. UPI
        assertEquals(100.00, transactions[3].amount, 0.001)
        assertEquals("UPI: 9307676700", transactions[3].description)
        assertEquals("815518551633", transactions[3].referenceNumber)

        // 5. Credit Interest
        assertEquals(152.00, transactions[4].amount, 0.001)
        assertEquals(TransactionType.INCOME, transactions[4].type)
        assertEquals("Credit Interest Capitalised", transactions[4].description)

        // Assert all transactions are clean of "Kodambakkam" and "This Statement"
        for (tx in transactions) {
            org.junit.Assert.assertFalse(tx.description.contains("Kodambakkam", ignoreCase = true))
            org.junit.Assert.assertFalse(tx.description.contains("This Statement", ignoreCase = true))
            org.junit.Assert.assertFalse(tx.rawDescription.orEmpty().contains("Kodambakkam", ignoreCase = true))
        }
    }
}
