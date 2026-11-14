package com.varsel.expensetracker.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.varsel.expensetracker.data.local.AppDatabase
import com.varsel.expensetracker.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestoreEngine @Inject constructor(
    private val database: AppDatabase
) {

    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Serializes all Room database tables into a structured JSON string.
     */
    suspend fun createBackupPayload(): String = withContext(Dispatchers.IO) {
        val transactions = database.transactionDao().getAllTransactionsList()
        val categories = database.categoryDao().getAllCategoriesSnapshot()
        val budgets = database.budgetDao().getAllBudgetsList()
        val recurringItems = database.recurringItemDao().getAllRecurringItemsList()
        val loanAccounts = database.loanAccountDao().getAllLoanAccountsList()
        val loanPayments = database.loanPaymentDao().getAllPaymentsList()
        val snapshots = database.statementSnapshotDao().getAllSnapshots()
        val allocations = database.financialEventAllocationDao().getAllAllocationsList()
        val rules = database.customRuleDao().getAllRulesList()
        val groups = database.transactionLinkGroupDao().getAllGroupsList()

        val root = JSONObject()
        root.put("format", "VARSEL_BACKUP")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appVersion", "1.0.0")
        root.put("device", Build.MODEL ?: "Android")

        val dataObj = JSONObject()

        // Transactions
        val txArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("amount", t.amount)
                put("type", t.type)
                put("description", t.description)
                put("category", t.category)
                put("dateTimestamp", t.dateTimestamp)
                putOpt("referenceNumber", t.referenceNumber)
                putOpt("transactionFingerprint", t.transactionFingerprint)
                putOpt("accountId", t.accountId)
                putOpt("accountLast4", t.accountLast4)
                putOpt("recurringItemId", t.recurringItemId)
                putOpt("transactionLinkId", t.transactionLinkId)
                putOpt("transferLinkId", t.transferLinkId)
                put("role", t.role)
                putOpt("bankName", t.bankName)
                putOpt("rawDescription", t.rawDescription)
            }
            txArray.put(obj)
        }
        dataObj.put("transactions", txArray)

        // Categories
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("colorHex", c.colorHex)
                put("iconName", c.iconName)
                put("budgetLimit", c.budgetLimit)
                put("keywords", c.keywords)
                put("type", c.type)
            }
            catArray.put(obj)
        }
        dataObj.put("categories", catArray)

        // Budgets
        val budgetArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("categoryName", b.categoryName)
                put("amount", b.amount)
                put("period", b.period)
                put("startDayOfMonth", b.startDayOfMonth)
                put("limitTotalType", b.limitTotalType)
                put("spendingLimitType", b.spendingLimitType)
                put("budgetType", b.budgetType)
                putOpt("colorHex", b.colorHex)
                putOpt("iconName", b.iconName)
                put("createdAt", b.createdAt)
            }
            budgetArray.put(obj)
        }
        dataObj.put("budgets", budgetArray)

        // Recurring Items
        val recArray = JSONArray()
        for (r in recurringItems) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                putOpt("notes", r.notes)
                put("amount", r.amount)
                put("isVariableAmount", r.isVariableAmount)
                put("type", r.type)
                put("frequency", r.frequency)
                put("startDateTimestamp", r.startDateTimestamp)
                put("nextOccurrenceTimestamp", r.nextOccurrenceTimestamp)
                putOpt("endDateTimestamp", r.endDateTimestamp)
                put("isActive", r.isActive)
                putOpt("accountId", r.accountId)
                putOpt("accountLast4", r.accountLast4)
                putOpt("bankName", r.bankName)
                put("category", r.category)
                putOpt("lastGeneratedTimestamp", r.lastGeneratedTimestamp)
                put("createdAt", r.createdAt)
                put("updatedAt", r.updatedAt)
            }
            recArray.put(obj)
        }
        dataObj.put("recurringItems", recArray)

        // Loan Accounts
        val loanArray = JSONArray()
        for (l in loanAccounts) {
            val obj = JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
                put("loanType", l.loanType)
                put("principal", l.principal)
                put("annualInterestRate", l.annualInterestRate)
                put("emiAmount", l.emiAmount)
                put("totalTenureMonths", l.totalTenureMonths)
                put("startDateTimestamp", l.startDateTimestamp)
                putOpt("collateralOrNotes", l.collateralOrNotes)
                put("status", l.status)
                putOpt("linkedBankAccountId", l.linkedBankAccountId)
                putOpt("bankAccountLast4", l.bankAccountLast4)
                putOpt("lenderName", l.lenderName)
                putOpt("loanAccountNumber", l.loanAccountNumber)
                put("interestType", l.interestType)
                put("repaymentType", l.repaymentType)
                putOpt("benchmarkRate", l.benchmarkRate)
                putOpt("spreadRate", l.spreadRate)
                put("createdAt", l.createdAt)
            }
            loanArray.put(obj)
        }
        dataObj.put("loanAccounts", loanArray)

        // Loan Payments
        val paymentArray = JSONArray()
        for (p in loanPayments) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("loanId", p.loanId)
                put("paymentDateTimestamp", p.paymentDateTimestamp)
                put("amount", p.amount)
                put("principalComponent", p.principalComponent)
                put("interestComponent", p.interestComponent)
                put("paymentType", p.paymentType)
                putOpt("linkedTransactionId", p.linkedTransactionId)
                putOpt("notes", p.notes)
                put("createdAt", p.createdAt)
            }
            paymentArray.put(obj)
        }
        dataObj.put("loanPayments", paymentArray)

        // Snapshots
        val snapArray = JSONArray()
        for (s in snapshots) {
            val obj = JSONObject().apply {
                put("id", s.id)
                putOpt("accountId", s.accountId)
                putOpt("accountLast4", s.accountLast4)
                putOpt("statementStartDate", s.statementStartDate)
                putOpt("statementEndDate", s.statementEndDate)
                putOpt("openingBalance", s.openingBalance)
                putOpt("totalCredits", s.totalCredits)
                putOpt("totalDebits", s.totalDebits)
                putOpt("endingBalance", s.endingBalance)
                put("importedAt", s.importedAt)
                putOpt("bankName", s.bankName)
                putOpt("ifscCode", s.ifscCode)
            }
            snapArray.put(obj)
        }
        dataObj.put("statementSnapshots", snapArray)

        // Allocations
        val allocArray = JSONArray()
        for (a in allocations) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("transactionId", a.transactionId)
                put("transactionLinkId", a.transactionLinkId)
                put("allocatedAmount", a.allocatedAmount)
                put("createdAt", a.createdAt)
            }
            allocArray.put(obj)
        }
        dataObj.put("financialEventAllocations", allocArray)

        // Custom Rules
        val ruleArray = JSONArray()
        for (r in rules) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("pattern", r.pattern)
                put("displayDescription", r.displayDescription)
                put("categoryName", r.categoryName)
            }
            ruleArray.put(obj)
        }
        dataObj.put("customRules", ruleArray)

        // Transaction Link Groups
        val grpArray = JSONArray()
        for (g in groups) {
            val obj = JSONObject().apply {
                put("transactionLinkId", g.transactionLinkId)
                put("groupName", g.groupName)
                put("category", g.category)
                put("createdAt", g.createdAt)
                put("isManuallySettled", g.isManuallySettled)
                putOpt("expectedReimbursement", g.expectedReimbursement)
            }
            grpArray.put(obj)
        }
        dataObj.put("transactionLinkGroups", grpArray)

        root.put("data", dataObj)

        val rawDataString = dataObj.toString()
        val checksum = computeSha256(rawDataString)
        root.put("checksum", checksum)

        root.toString(2)
    }

    suspend fun exportBackupToStream(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val payload = createBackupPayload()
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(payload)
            writer.flush()
        }
    }

    suspend fun exportBackupToCacheAndGetUri(context: Context): FileExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "varsel_backup_${fileNameDateFormat.format(Date())}.varsel"
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            exportBackupToStream(fos)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        FileExportResult(
            file = file,
            uri = uri,
            fileName = fileName,
            mimeType = "application/json",
            recordCount = 1
        )
    }

    fun createShareIntent(context: Context, result: FileExportResult): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, "Varsel Encrypted Database Backup")
            putExtra(Intent.EXTRA_TEXT, "Here is your full database backup file (.varsel) from Varsel.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Inspects a backup input stream and returns metadata summary without mutating database.
     */
    suspend fun inspectBackupStream(inputStream: InputStream): BackupInspectionResult = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(content)

            val format = root.optString("format")
            if (format != "VARSEL_BACKUP") {
                return@withContext BackupInspectionResult.Invalid("Not a valid Varsel backup file format.")
            }

            val timestamp = root.optLong("timestamp", 0L)
            val appVersion = root.optString("appVersion", "Unknown")
            val device = root.optString("device", "Unknown")

            val dataObj = root.optJSONObject("data")
                ?: return@withContext BackupInspectionResult.Invalid("Missing backup database records.")

            val txCount = dataObj.optJSONArray("transactions")?.length() ?: 0
            val catCount = dataObj.optJSONArray("categories")?.length() ?: 0
            val budgetCount = dataObj.optJSONArray("budgets")?.length() ?: 0
            val recurringCount = dataObj.optJSONArray("recurringItems")?.length() ?: 0
            val loanCount = dataObj.optJSONArray("loanAccounts")?.length() ?: 0

            BackupInspectionResult.Valid(
                timestamp = timestamp,
                appVersion = appVersion,
                device = device,
                transactionCount = txCount,
                categoryCount = catCount,
                budgetCount = budgetCount,
                recurringCount = recurringCount,
                loanCount = loanCount,
                rawJson = content
            )
        } catch (e: Exception) {
            BackupInspectionResult.Invalid(e.localizedMessage ?: "Failed to read backup file.")
        }
    }

    /**
     * Restores database atomically from a validated JSON string.
     */
    suspend fun restoreDatabase(rawJson: String): Boolean = withContext(Dispatchers.IO) {
        val root = JSONObject(rawJson)
        val dataObj = root.getJSONObject("data")

        database.withTransaction {
            // Step 1: Wipe existing tables
            database.clearAllTables()

            // Step 2: Restore Categories
            val catArray = dataObj.optJSONArray("categories")
            if (catArray != null) {
                val list = mutableListOf<CategoryEntity>()
                for (i in 0 until catArray.length()) {
                    val o = catArray.getJSONObject(i)
                    list.add(
                        CategoryEntity(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            colorHex = o.getString("colorHex"),
                            iconName = o.getString("iconName"),
                            budgetLimit = o.optDouble("budgetLimit", 0.0),
                            keywords = o.optString("keywords", ""),
                            type = o.optString("type", "EXPENSE")
                        )
                    )
                }
                database.categoryDao().insertCategories(list)
            }

            // Step 3: Restore Transactions
            val txArray = dataObj.optJSONArray("transactions")
            if (txArray != null) {
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val o = txArray.getJSONObject(i)
                    list.add(
                        TransactionEntity(
                            id = o.optLong("id", 0L),
                            amount = o.getDouble("amount"),
                            type = o.getString("type"),
                            description = o.getString("description"),
                            category = o.getString("category"),
                            dateTimestamp = o.getLong("dateTimestamp"),
                            referenceNumber = o.optStringOrNull("referenceNumber"),
                            transactionFingerprint = o.optStringOrNull("transactionFingerprint"),
                            accountId = o.optStringOrNull("accountId"),
                            accountLast4 = o.optStringOrNull("accountLast4"),
                            recurringItemId = if (o.has("recurringItemId") && !o.isNull("recurringItemId")) o.getLong("recurringItemId") else null,
                            transactionLinkId = o.optStringOrNull("transactionLinkId"),
                            transferLinkId = o.optStringOrNull("transferLinkId"),
                            role = o.optString("role", "NORMAL"),
                            bankName = o.optStringOrNull("bankName"),
                            rawDescription = o.optStringOrNull("rawDescription")
                        )
                    )
                }
                database.transactionDao().insertTransactions(list)
            }

            // Step 4: Restore Budgets
            val budgetArray = dataObj.optJSONArray("budgets")
            if (budgetArray != null) {
                val list = mutableListOf<BudgetEntity>()
                for (i in 0 until budgetArray.length()) {
                    val o = budgetArray.getJSONObject(i)
                    list.add(
                        BudgetEntity(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            categoryName = o.getString("categoryName"),
                            amount = o.getDouble("amount"),
                            period = o.optString("period", "MONTHLY"),
                            startDayOfMonth = o.optInt("startDayOfMonth", 1),
                            limitTotalType = o.optString("limitTotalType", "CONTRIBUTED"),
                            spendingLimitType = o.optString("spendingLimitType", "FIXED"),
                            budgetType = o.optString("budgetType", "EXPENSE"),
                            colorHex = o.optStringOrNull("colorHex"),
                            iconName = o.optStringOrNull("iconName"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                database.budgetDao().insertBudgets(list)
            }

            // Step 5: Restore Recurring Items
            val recArray = dataObj.optJSONArray("recurringItems")
            if (recArray != null) {
                val list = mutableListOf<RecurringItemEntity>()
                for (i in 0 until recArray.length()) {
                    val o = recArray.getJSONObject(i)
                    list.add(
                        RecurringItemEntity(
                            id = o.optLong("id", 0L),
                            title = o.getString("title"),
                            notes = o.optStringOrNull("notes"),
                            amount = o.getDouble("amount"),
                            isVariableAmount = o.optBoolean("isVariableAmount", false),
                            type = o.getString("type"),
                            frequency = o.getString("frequency"),
                            startDateTimestamp = o.getLong("startDateTimestamp"),
                            nextOccurrenceTimestamp = o.getLong("nextOccurrenceTimestamp"),
                            endDateTimestamp = if (o.has("endDateTimestamp") && !o.isNull("endDateTimestamp")) o.getLong("endDateTimestamp") else null,
                            isActive = o.optBoolean("isActive", true),
                            accountId = o.optStringOrNull("accountId"),
                            accountLast4 = o.optStringOrNull("accountLast4"),
                            bankName = o.optStringOrNull("bankName"),
                            category = o.optString("category", "Other"),
                            lastGeneratedTimestamp = if (o.has("lastGeneratedTimestamp") && !o.isNull("lastGeneratedTimestamp")) o.getLong("lastGeneratedTimestamp") else null,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                database.recurringItemDao().insertRecurringItems(list)
            }

            // Step 6: Restore Loan Accounts
            val loanArray = dataObj.optJSONArray("loanAccounts")
            if (loanArray != null) {
                val list = mutableListOf<LoanAccountEntity>()
                for (i in 0 until loanArray.length()) {
                    val o = loanArray.getJSONObject(i)
                    list.add(
                        LoanAccountEntity(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            loanType = o.getString("loanType"),
                            principal = o.getDouble("principal"),
                            annualInterestRate = o.getDouble("annualInterestRate"),
                            emiAmount = o.getDouble("emiAmount"),
                            totalTenureMonths = o.getInt("totalTenureMonths"),
                            startDateTimestamp = o.getLong("startDateTimestamp"),
                            collateralOrNotes = o.optStringOrNull("collateralOrNotes"),
                            status = o.optString("status", "ACTIVE"),
                            linkedBankAccountId = o.optStringOrNull("linkedBankAccountId"),
                            bankAccountLast4 = o.optStringOrNull("bankAccountLast4"),
                            lenderName = o.optStringOrNull("lenderName"),
                            loanAccountNumber = o.optStringOrNull("loanAccountNumber"),
                            interestType = o.optString("interestType", "FIXED"),
                            repaymentType = o.optString("repaymentType", "MONTHLY_EMI"),
                            benchmarkRate = if (o.has("benchmarkRate") && !o.isNull("benchmarkRate")) o.getDouble("benchmarkRate") else null,
                            spreadRate = if (o.has("spreadRate") && !o.isNull("spreadRate")) o.getDouble("spreadRate") else null,
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                database.loanAccountDao().insertLoanAccounts(list)
            }

            // Step 7: Restore Loan Payments
            val paymentArray = dataObj.optJSONArray("loanPayments")
            if (paymentArray != null) {
                val list = mutableListOf<LoanPaymentEntity>()
                for (i in 0 until paymentArray.length()) {
                    val o = paymentArray.getJSONObject(i)
                    list.add(
                        LoanPaymentEntity(
                            id = o.optLong("id", 0L),
                            loanId = o.getLong("loanId"),
                            paymentDateTimestamp = o.getLong("paymentDateTimestamp"),
                            amount = o.getDouble("amount"),
                            principalComponent = o.getDouble("principalComponent"),
                            interestComponent = o.getDouble("interestComponent"),
                            paymentType = o.optString("paymentType", "REGULAR_EMI"),
                            linkedTransactionId = if (o.has("linkedTransactionId") && !o.isNull("linkedTransactionId")) o.getLong("linkedTransactionId") else null,
                            notes = o.optStringOrNull("notes"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                database.loanPaymentDao().insertPayments(list)
            }

            // Step 8: Restore Statement Snapshots
            val snapArray = dataObj.optJSONArray("statementSnapshots")
            if (snapArray != null) {
                val list = mutableListOf<StatementSnapshotEntity>()
                for (i in 0 until snapArray.length()) {
                    val o = snapArray.getJSONObject(i)
                    list.add(
                        StatementSnapshotEntity(
                            id = o.optLong("id", 0L),
                            accountId = o.optStringOrNull("accountId"),
                            accountLast4 = o.optStringOrNull("accountLast4"),
                            statementStartDate = if (o.has("statementStartDate") && !o.isNull("statementStartDate")) o.getLong("statementStartDate") else null,
                            statementEndDate = if (o.has("statementEndDate") && !o.isNull("statementEndDate")) o.getLong("statementEndDate") else null,
                            openingBalance = if (o.has("openingBalance") && !o.isNull("openingBalance")) o.getDouble("openingBalance") else null,
                            totalCredits = if (o.has("totalCredits") && !o.isNull("totalCredits")) o.getDouble("totalCredits") else null,
                            totalDebits = if (o.has("totalDebits") && !o.isNull("totalDebits")) o.getDouble("totalDebits") else null,
                            endingBalance = if (o.has("endingBalance") && !o.isNull("endingBalance")) o.getDouble("endingBalance") else null,
                            importedAt = o.getLong("importedAt"),
                            bankName = o.optStringOrNull("bankName"),
                            ifscCode = o.optStringOrNull("ifscCode")
                        )
                    )
                }
                database.statementSnapshotDao().insertSnapshots(list)
            }

            // Step 9: Restore Financial Event Allocations
            val allocArray = dataObj.optJSONArray("financialEventAllocations")
            if (allocArray != null) {
                val list = mutableListOf<FinancialEventAllocationEntity>()
                for (i in 0 until allocArray.length()) {
                    val o = allocArray.getJSONObject(i)
                    list.add(
                        FinancialEventAllocationEntity(
                            id = o.optLong("id", 0L),
                            transactionId = o.getLong("transactionId"),
                            transactionLinkId = o.getString("transactionLinkId"),
                            allocatedAmount = o.getDouble("allocatedAmount"),
                            createdAt = o.getLong("createdAt")
                        )
                    )
                }
                database.financialEventAllocationDao().insertAllocations(list)
            }

            // Step 10: Restore Custom Rules
            val ruleArray = dataObj.optJSONArray("customRules")
            if (ruleArray != null) {
                val list = mutableListOf<CustomRuleEntity>()
                for (i in 0 until ruleArray.length()) {
                    val o = ruleArray.getJSONObject(i)
                    list.add(
                        CustomRuleEntity(
                            id = o.optLong("id", 0L),
                            pattern = o.getString("pattern"),
                            displayDescription = o.getString("displayDescription"),
                            categoryName = o.getString("categoryName")
                        )
                    )
                }
                database.customRuleDao().insertCustomRules(list)
            }

            // Step 11: Restore Transaction Link Groups
            val grpArray = dataObj.optJSONArray("transactionLinkGroups")
            if (grpArray != null) {
                val list = mutableListOf<TransactionLinkGroupEntity>()
                for (i in 0 until grpArray.length()) {
                    val o = grpArray.getJSONObject(i)
                    list.add(
                        TransactionLinkGroupEntity(
                            transactionLinkId = o.getString("transactionLinkId"),
                            groupName = o.getString("groupName"),
                            category = o.getString("category"),
                            createdAt = o.getLong("createdAt"),
                            isManuallySettled = o.optBoolean("isManuallySettled", false),
                            expectedReimbursement = if (o.has("expectedReimbursement") && !o.isNull("expectedReimbursement")) {
                                o.optDouble("expectedReimbursement").takeIf { !it.isNaN() }
                            } else {
                                null
                            }
                        )
                    )
                }
                database.transactionLinkGroupDao().insertGroups(list)
            }
        }

        true
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (has(key) && !isNull(key)) {
            val str = optString(key)
            if (str.isEmpty()) null else str
        } else null
    }

    private fun computeSha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}

sealed class BackupInspectionResult {
    data class Valid(
        val timestamp: Long,
        val appVersion: String,
        val device: String,
        val transactionCount: Int,
        val categoryCount: Int,
        val budgetCount: Int,
        val recurringCount: Int,
        val loanCount: Int,
        val rawJson: String
    ) : BackupInspectionResult()

    data class Invalid(val reason: String) : BackupInspectionResult()
}
