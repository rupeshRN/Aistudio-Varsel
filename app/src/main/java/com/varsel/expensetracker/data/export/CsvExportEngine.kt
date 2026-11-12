package com.varsel.expensetracker.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExportEngine @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Builds RFC 4180 standard CSV content from transaction list.
     */
    fun generateCsvString(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        
        // Header
        sb.append("ID,Date,Time,Description,Category,Type,Amount (INR),Bank,Account Last 4,Reference Number,Role,Transfer ID,Event ID,Raw Description\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.dateTimestamp))
            val timeStr = timeFormat.format(Date(tx.dateTimestamp))
            
            sb.append(escape(tx.id.toString())).append(",")
            sb.append(escape(dateStr)).append(",")
            sb.append(escape(timeStr)).append(",")
            sb.append(escape(tx.description)).append(",")
            sb.append(escape(tx.category)).append(",")
            sb.append(escape(tx.type)).append(",")
            sb.append(String.format(Locale.US, "%.2f", tx.amount)).append(",")
            sb.append(escape(tx.bankName ?: "")).append(",")
            sb.append(escape(tx.accountLast4 ?: "")).append(",")
            sb.append(escape(tx.referenceNumber ?: "")).append(",")
            sb.append(escape(tx.role)).append(",")
            sb.append(escape(tx.transferLinkId ?: "")).append(",")
            sb.append(escape(tx.transactionLinkId ?: "")).append(",")
            sb.append(escape(tx.rawDescription ?: ""))
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes CSV string directly to an output stream (e.g. from Storage Access Framework).
     */
    suspend fun exportToStream(
        transactions: List<TransactionEntity>,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val csv = generateCsvString(transactions)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(csv)
            writer.flush()
        }
    }

    /**
     * Creates a temporary CSV file in app cache and returns the shared FileProvider URI.
     */
    suspend fun exportToCacheAndGetUri(
        context: Context,
        transactions: List<TransactionEntity>
    ): FileExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "varsel_transactions_${fileNameDateFormat.format(Date())}.csv"
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            exportToStream(transactions, fos)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        FileExportResult(
            file = file,
            uri = uri,
            fileName = fileName,
            mimeType = "text/csv",
            recordCount = transactions.size
        )
    }

    /**
     * Creates a system share intent for the exported CSV file.
     */
    fun createShareIntent(context: Context, result: FileExportResult): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, "Varsel Transactions Export (${result.recordCount} records)")
            putExtra(Intent.EXTRA_TEXT, "Here is your exported financial transaction history from Varsel.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}

data class FileExportResult(
    val file: File,
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val recordCount: Int
)
