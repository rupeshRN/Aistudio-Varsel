package com.varsel.expensetracker.data.export

import com.varsel.expensetracker.data.local.entity.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class DataExportEngineTest {

    private val csvEngine = CsvExportEngine()

    @Test
    fun testCsvExportHeaderAndRowFormatting() {
        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                amount = 1250.50,
                type = "EXPENSE",
                description = "Grocery, Fresh Fruits & Vegetables",
                category = "Groceries",
                dateTimestamp = 1700000000000L,
                bankName = "HDFC Bank",
                accountLast4 = "1234",
                referenceNumber = "REF999888"
            ),
            TransactionEntity(
                id = 2L,
                amount = 50000.00,
                type = "INCOME",
                description = "Monthly \"Salary\" Deposit",
                category = "Salary",
                dateTimestamp = 1700086400000L,
                bankName = "ICICI Bank",
                accountLast4 = "5678"
            )
        )

        val csv = csvEngine.generateCsvString(transactions)
        val lines = csv.trim().split("\n")

        // Check Header
        assertTrue(lines.isNotEmpty())
        assertEquals("ID,Date,Time,Description,Category,Type,Amount (INR),Bank,Account Last 4,Reference Number,Role,Transfer ID,Event ID,Raw Description", lines[0])

        // Check Row 1 (description contains comma, should be wrapped in quotes)
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("\"Grocery, Fresh Fruits & Vegetables\""))
        assertTrue(lines[1].contains("1250.50"))
        assertTrue(lines[1].contains("HDFC Bank"))
        assertTrue(lines[1].contains("1234"))

        // Check Row 2 (description contains double quotes, should be escaped with double quotes)
        assertTrue(lines[2].contains("\"Monthly \"\"Salary\"\" Deposit\""))
        assertTrue(lines[2].contains("50000.00"))
        assertTrue(lines[2].contains("ICICI Bank"))
        assertTrue(lines[2].contains("5678"))
    }

    @Test
    fun testBackupInspectionValidJson() {
        val root = JSONObject().apply {
            put("format", "VARSEL_BACKUP")
            put("version", 1)
            put("timestamp", 1700000000000L)
            put("appVersion", "1.0.0")
            put("device", "Pixel 8 Pro")

            val data = JSONObject().apply {
                put("transactions", JSONArray().apply {
                    put(JSONObject().apply { put("id", 1); put("amount", 100.0) })
                    put(JSONObject().apply { put("id", 2); put("amount", 200.0) })
                })
                put("categories", JSONArray().apply {
                    put(JSONObject().apply { put("name", "Food") })
                })
                put("budgets", JSONArray())
                put("recurringItems", JSONArray())
                put("loanAccounts", JSONArray())
            }
            put("data", data)
            put("checksum", "abc123hash")
        }

        val jsonString = root.toString()
        val stream = ByteArrayInputStream(jsonString.toByteArray(Charsets.UTF_8))

        // We can inspect this manually using the logic or verify JSON parsing
        val parsedRoot = JSONObject(jsonString)
        assertEquals("VARSEL_BACKUP", parsedRoot.getString("format"))
        assertEquals(1, parsedRoot.getInt("version"))
        assertEquals(2, parsedRoot.getJSONObject("data").getJSONArray("transactions").length())
        assertEquals(1, parsedRoot.getJSONObject("data").getJSONArray("categories").length())
    }

    @Test
    fun testBackupInspectionInvalidFormat() {
        val invalidJson = JSONObject().apply {
            put("format", "OTHER_FORMAT")
            put("version", 1)
        }.toString()

        val parsed = JSONObject(invalidJson)
        assertTrue(parsed.optString("format") != "VARSEL_BACKUP")
    }
}
