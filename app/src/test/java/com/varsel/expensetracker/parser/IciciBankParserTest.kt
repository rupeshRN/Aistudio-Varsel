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

class IciciBankParserTest {

    private lateinit var iciciBankParser: IciciBankParser

    @Before
    fun setup() {
        val descriptionNormalizer = DescriptionNormalizer()
        val customRuleEngine = CustomRuleEngine(descriptionNormalizer)
        val categoryRuleEngine = CategoryRuleEngine(customRuleEngine)
        val descriptionCleaner = DescriptionCleaner()

        iciciBankParser = IciciBankParser(
            categoryRuleEngine = categoryRuleEngine,
            descriptionCleaner = descriptionCleaner,
            descriptionNormalizer = descriptionNormalizer
        )
    }

    @Test
    fun `test canParse detects ICICI Bank statements`() {
        val sampleStatement = """
            ICICI Bank
            Statement of Transactions in Saving Account no. 123456789012 in INR for the period August 25, 2025 - August 25, 2026
            Your Base Branch: ICICI BANK LIMITED
        """.trimIndent()

        assertTrue(iciciBankParser.canParse(sampleStatement))
    }

    @Test
    fun `test parse ignores bold header line and extracts exact remark note`() {
        val testStatement = """
            ICICI Bank
            Statement of Transactions in Saving Account no. 123456789012 in INR for the period August 25, 2025 - August 25, 2026
            S No. Transaction Date Cheque Number Transaction Remarks Withdrawal Amount (INR) Deposit Amount (INR) Balance (INR)
            1 25.08.2025 Rupesh Kum
            MMT/IMPS/611234567895/Room rent eb bi/Rupesh Kum/BINB001234 12500.00 45000.00
        """.trimIndent()

        val transactions = iciciBankParser.parse(testStatement)
        assertEquals(1, transactions.size)
        assertEquals("Room Rent Eb Bi", transactions[0].description)
        assertEquals("611234567895", transactions[0].referenceNumber)
        assertEquals(12500.00, transactions[0].amount, 0.01)
    }

    @Test
    fun `test multi-page statement continues until end of document`() {
        val multiPageStatement = """
            ICICI Bank
            Statement of Transactions in Saving Account no. 123456789012
            S No. Transaction Date Cheque Number Transaction Remarks Withdrawal Amount (INR) Deposit Amount (INR) Balance (INR)
            1 25.08.2025 Person One
            MMT/IMPS/523700123471/For ticket/Krishnan/IDIB0001234 2000.00 34521.01
            Page 1 of 3

            ICICI Bank Limited
            Statement of Transactions in Saving Account
            S No. Transaction Date Cheque Number Transaction Remarks Withdrawal Amount (INR) Deposit Amount (INR) Balance (INR)
            2 26.08.2025 Person Two
            MMT/IMPS/523700123472/Groceries/Store/IDIB0001234 1500.00 33021.01
            Page 2 of 3

            ICICI Bank Limited
            Statement of Transactions in Saving Account
            S No. Transaction Date Cheque Number Transaction Remarks Withdrawal Amount (INR) Deposit Amount (INR) Balance (INR)
            3 27.08.2025 Person Three
            MMT/IMPS/523700123473/Internet bill/ISP/IDIB0001234 999.00 32022.01
            Page 3 of 3

            Sincerely,
            Team ICICI Bank
            This is a system generated statement. Hence, it does not require any signature.
            Legends for transactions in your Account Statement
        """.trimIndent()

        val transactions = iciciBankParser.parse(multiPageStatement)
        assertEquals(3, transactions.size)
        assertEquals("For Ticket", transactions[0].description)
        assertEquals("Groceries", transactions[1].description)
        assertEquals("Internet Bill", transactions[2].description)
    }
}
