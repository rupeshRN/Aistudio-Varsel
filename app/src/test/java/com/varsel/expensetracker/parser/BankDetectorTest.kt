package com.varsel.expensetracker.parser

import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankDetectorTest {

    private lateinit var bankDetector: BankDetector
    private lateinit var dummyIndianBankParser: StatementParser
    private lateinit var dummyIciciBankParser: StatementParser
    private lateinit var dummyHdfcBankParser: StatementParser
    private lateinit var dummySbiBankParser: StatementParser
    private lateinit var dummyAxisBankParser: StatementParser

    private class FakeStatementSnapshotDao(
        private val snapshots: List<StatementSnapshotEntity> = emptyList()
    ) : StatementSnapshotDao {
        override suspend fun insertSnapshot(snapshot: StatementSnapshotEntity) {}
        override suspend fun insertSnapshots(snapshots: List<StatementSnapshotEntity>) {}
        override suspend fun getLatestSnapshot(accountId: String): StatementSnapshotEntity? =
            snapshots.firstOrNull { it.accountId == accountId }
        override suspend fun getAllSnapshots(): List<StatementSnapshotEntity> = snapshots
        override fun observeAllSnapshots(): Flow<List<StatementSnapshotEntity>> = flowOf(snapshots)
        override suspend fun deleteSnapshotById(id: Long) {}
        override suspend fun deleteTransactionsForAccountPeriod(accountId: String, startDate: Long, endDate: Long) {}
    }

    @Before
    fun setup() {
        dummyIndianBankParser = object : StatementParser {
            override fun canParse(rawText: String) =
                rawText.contains("INDIAN BANK") && rawText.contains("STATEMENT OF ACCOUNT") && !rawText.contains("AMBIGUOUS")
            override fun parse(rawText: String) = emptyList<com.varsel.expensetracker.domain.model.Transaction>()
        }
        dummyIciciBankParser = object : StatementParser {
            override fun canParse(rawText: String) =
                rawText.contains("ICICI BANK") && rawText.contains("STATEMENT OF ACCOUNT") && !rawText.contains("AMBIGUOUS")
            override fun parse(rawText: String) = emptyList<com.varsel.expensetracker.domain.model.Transaction>()
        }
        dummyHdfcBankParser = object : StatementParser {
            override fun canParse(rawText: String) =
                rawText.contains("HDFC BANK") && rawText.contains("STATEMENT OF ACCOUNT") && !rawText.contains("AMBIGUOUS")
            override fun parse(rawText: String) = emptyList<com.varsel.expensetracker.domain.model.Transaction>()
        }
        dummySbiBankParser = object : StatementParser {
            override fun canParse(rawText: String) =
                (rawText.contains("STATE BANK OF INDIA", ignoreCase = true) || rawText.contains("SBIN")) &&
                    rawText.contains("Account Statement", ignoreCase = true) && !rawText.contains("AMBIGUOUS")
            override fun parse(rawText: String) = emptyList<com.varsel.expensetracker.domain.model.Transaction>()
        }
        dummyAxisBankParser = object : StatementParser {
            override fun canParse(rawText: String) =
                (rawText.contains("AXIS BANK", ignoreCase = true) || rawText.contains("UTIB")) &&
                    rawText.contains("Statement of Account", ignoreCase = true) && !rawText.contains("AMBIGUOUS")
            override fun parse(rawText: String) = emptyList<com.varsel.expensetracker.domain.model.Transaction>()
        }

        val registry = BankParserRegistry {
            listOf(
                RegisteredBankParser("indian_bank", "Indian Bank", dummyIndianBankParser),
                RegisteredBankParser("icici_bank", "ICICI Bank", dummyIciciBankParser),
                RegisteredBankParser("hdfc_bank", "HDFC Bank", dummyHdfcBankParser),
                RegisteredBankParser("sbi_bank", "State Bank of India (SBI)", dummySbiBankParser),
                RegisteredBankParser("axis_bank", "Axis Bank", dummyAxisBankParser)
            )
        }

        bankDetector = BankDetector(bankParserRegistry = registry)
    }

    @Test
    fun `test detectResult returns Supported for valid State Bank of India statement`() {
        val sbiText = """
            STATE BANK OF INDIA
            Branch: Connaught Place, New Delhi
            IFSC: SBIN0001234
            Account Statement for 01/01/2024 to 31/01/2024
        """.trimIndent()

        val result = bankDetector.detectResult(sbiText)
        assertTrue(result is BankDetectionResult.Supported)
        assertEquals("State Bank of India (SBI)", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test detectResult returns UnsupportedBank for Kotak Mahindra Bank`() {
        val kotakText = """
            KOTAK MAHINDRA BANK
            Branch: Nariman Point, Mumbai
            IFSC: KKBK0000123
            Account Statement for 01/01/2024 to 31/01/2024
        """.trimIndent()

        val result = bankDetector.detectResult(kotakText)
        assertTrue(result is BankDetectionResult.UnsupportedBank)
        assertEquals("Kotak Mahindra Bank", (result as BankDetectionResult.UnsupportedBank).detectedBankName)
    }

    @Test(expected = UnsupportedBankException::class)
    fun `test detect throws UnsupportedBankException for Kotak Mahindra Bank`() {
        val kotakText = """
            KOTAK MAHINDRA BANK
            Branch: Nariman Point, Mumbai
            IFSC: KKBK0000123
            Account Statement for 01/01/2024 to 31/01/2024
        """.trimIndent()

        bankDetector.detect(kotakText)
    }

    @Test
    fun `test detectResult returns Supported for valid Axis Bank statement`() {
        val axisText = """
            AXIS BANK LIMITED
            Statement of Account
            IFSC: UTIB0000001
        """.trimIndent()

        val result = bankDetector.detectResult(axisText)
        assertTrue(result is BankDetectionResult.Supported)
        assertEquals("Axis Bank", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test detectResult returns UnsupportedFormat for supported bank with unrecognized layout`() {
        val brokenIndianBankText = """
            INDIAN BANK
            Random receipt without tabular format or statement dates
        """.trimIndent()

        val result = bankDetector.detectResult(brokenIndianBankText)
        assertTrue(result is BankDetectionResult.UnsupportedFormat)
        assertEquals("Indian Bank", (result as BankDetectionResult.UnsupportedFormat).bankName)
    }

    @Test(expected = UnsupportedStatementFormatException::class)
    fun `test detect throws UnsupportedStatementFormatException for unsupported layout`() {
        val brokenIndianBankText = """
            INDIAN BANK
            Random receipt without tabular format or statement dates
        """.trimIndent()

        bankDetector.detect(brokenIndianBankText)
    }

    @Test
    fun `test detectResult returns Supported for valid Indian Bank statement`() {
        val validIndianBankText = """
            INDIAN BANK
            STATEMENT OF ACCOUNT
        """.trimIndent()

        val result = bankDetector.detectResult(validIndianBankText)
        assertTrue(result is BankDetectionResult.Supported)
        assertEquals("Indian Bank", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test Axis bank statement with ICICI UPI transactions correctly detects as Axis Bank`() {
        val axisWithIciciNarration = """
            AXIS BANK LIMITED
            Statement of Account
            IFSC: UTIB0000123
            Date        Particulars                     Amount
            01/05/2023  UPI/12345/Ram/ICIC0001234/test  500.00
            02/05/2023  UPI/67890/payee@icici/bill      200.00
        """.trimIndent()

        val result = bankDetector.detectResult(axisWithIciciNarration)
        assertTrue(result is BankDetectionResult.Supported)
        assertEquals("Axis Bank", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test unknown Indian bank detected by IFSC code prefix`() {
        val federalBankStatement = """
            FEDERAL BANK
            Customer Statement
            Branch IFSC: FDRL0001234
        """.trimIndent()

        val result = bankDetector.detectResult(federalBankStatement)
        assertTrue(result is BankDetectionResult.UnsupportedBank)
        assertEquals("Federal Bank", (result as BankDetectionResult.UnsupportedBank).detectedBankName)
    }

    @Test
    fun `test ICICI Bank statement with IOB transaction narration is correctly detected as ICICI Bank`() {
        val iciciWithIobNarration = """
            ICICI BANK
            STATEMENT OF ACCOUNT
            Account Number: 001105001234
            Branch: BANDRA BRANCH
            IFSC: ICIC0000001
            Date        Particulars                             Chq/Ref No   Withdrawal  Deposit   Balance
            15/03/2024  NEFT-IOBA0001234-TRANSFER TO RAJESH    54321        1500.00     0.00      45000.00
            16/03/2024  UPI/407812/Ram/IOB/Grocery             12345        250.00      0.00      44750.00
        """.trimIndent()

        val result = bankDetector.detectResult(iciciWithIobNarration)
        assertTrue("Expected ICICI Bank, got: $result", result is BankDetectionResult.Supported)
        assertEquals("ICICI Bank", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test genuine Indian Overseas Bank statement is correctly detected as Indian Overseas Bank`() {
        val iobStatement = """
            INDIAN OVERSEAS BANK
            Central Office, Chennai
            Statement of Account
            Branch: ANNA NAGAR
            Branch IFSC: IOBA0000123
            Date        Particulars     Debit       Credit      Balance
            01/04/2024  Salary          0.00        50000.00    50000.00
        """.trimIndent()

        val result = bankDetector.detectResult(iobStatement)
        assertTrue("Expected UnsupportedBank, got: $result", result is BankDetectionResult.UnsupportedBank)
        assertEquals("Indian Overseas Bank", (result as BankDetectionResult.UnsupportedBank).detectedBankName)
    }

    @Test
    fun `test ICICI Bank statement with multiple competitor transfer narrations correctly detects as ICICI Bank`() {
        val iciciMultiCompetitor = """
            ICICI BANK LIMITED
            STATEMENT OF ACCOUNT
            Account Number: 123456789012
            Date        Particulars                         Amount
            01/01/2024  NEFT-UTIB0001234-AXIS BANK TRANSFER 2000.00
            02/01/2024  NEFT-HDFC0000123-HDFC BANK CC PAY   5000.00
            03/01/2024  UPI/SBIN0001234/STATE BANK OF INDIA 300.00
        """.trimIndent()

        val result = bankDetector.detectResult(iciciMultiCompetitor)
        assertTrue(result is BankDetectionResult.Supported)
        assertEquals("ICICI Bank", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test ambiguous statement matched with historical account number resolves to historical bank`() {
        val historicalDao = FakeStatementSnapshotDao(
            listOf(
                StatementSnapshotEntity(
                    accountId = "987654321012",
                    accountLast4 = "1012",
                    bankName = "ICICI Bank",
                    importedAt = System.currentTimeMillis()
                )
            )
        )
        val historyMatcher = AccountHistoryMatcher(
            statementSnapshotDao = historicalDao,
            accountDetailsExtractor = AccountDetailsExtractor()
        )

        // Statement has no clear bank header name, but has the known account number and general format
        val unbrandedStatement = """
            STATEMENT OF ACCOUNT
            Account Number: 987654321012
            Statement Period: 01/01/2024 to 31/01/2024
            Date        Particulars     Withdrawal  Deposit Balance
            10/01/2024  Grocery Store   500.00      0.00    10000.00
        """.trimIndent()

        // Create a detector with registry and history matcher
        val detectorWithHistory = BankDetector(
            bankParserRegistry = BankParserRegistry {
                listOf(
                    RegisteredBankParser("icici_bank", "ICICI Bank", dummyIciciBankParser),
                    RegisteredBankParser("hdfc_bank", "HDFC Bank", dummyHdfcBankParser)
                )
            },
            accountHistoryMatcher = historyMatcher
        )

        val result = detectorWithHistory.detectResult(unbrandedStatement)
        assertTrue("Expected Supported via historical account match, got: $result", result is BankDetectionResult.Supported)
        assertEquals("ICICI Bank", (result as BankDetectionResult.Supported).displayName)
    }

    @Test
    fun `test unrecognized statement with unknown account returns UnrecognizedAccount`() {
        val unknownStatement = """
            FINANCIAL REPORT
            Account Number: 999988887777
            Date        Description     Amount
            01/01/2024  Payment         100.00
        """.trimIndent()

        val result = bankDetector.detectResult(unknownStatement)
        assertTrue("Expected UnrecognizedAccount, got: $result", result is BankDetectionResult.UnrecognizedAccount)
        val unrecognized = result as BankDetectionResult.UnrecognizedAccount
        assertEquals("999988887777", unrecognized.detectedAccountNumber)
    }

    @Test(expected = UnrecognizedAccountStatementException::class)
    fun `test detect throws UnrecognizedAccountStatementException for unrecognized account`() {
        val unknownStatement = """
            FINANCIAL REPORT
            Account Number: 999988887777
            Date        Description     Amount
            01/01/2024  Payment         100.00
        """.trimIndent()

        bankDetector.detect(unknownStatement)
    }
}
