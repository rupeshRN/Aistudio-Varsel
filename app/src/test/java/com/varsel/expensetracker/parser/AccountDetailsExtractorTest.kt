package com.varsel.expensetracker.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccountDetailsExtractorTest {

    private val extractor = AccountDetailsExtractor()

    @Test
    fun `test extracts IFSC code from Indian Bank statement text`() {
        val rawText = """
            INDIAN BANK
            CHENNAI MAIN BRANCH
            Branch IFSC : IDIB000C001
            Account Number: 50123456789
            Statement of Account
        """.trimIndent()

        val ifsc = extractor.extractIfscCode(rawText)
        assertEquals("IDIB000C001", ifsc)
    }

    @Test
    fun `test extracts IFSC code from ICICI Bank statement text`() {
        val rawText = """
            ICICI Bank Limited
            RTGS/NEFT IFSC code: ICIC0000001
            Account no: 000101567890
        """.trimIndent()

        val ifsc = extractor.extractIfscCode(rawText)
        assertEquals("ICIC0000001", ifsc)
    }

    @Test
    fun `test extracts IFSC code from HDFC Bank statement text`() {
        val rawText = """
            HDFC BANK LIMITED
            KORAMANGALA BRANCH
            RTGS/NEFT IFSC: HDFC0000123
            Account No : 50100234567890
        """.trimIndent()

        val ifsc = extractor.extractIfscCode(rawText)
        assertEquals("HDFC0000123", ifsc)
    }
}
