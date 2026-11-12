package com.varsel.expensetracker.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfReportGenerator @Inject constructor() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val fileNameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f

    suspend fun generateReportToStream(
        transactions: List<TransactionEntity>,
        periodTitle: String,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        val totalIncome = transactions.filter { it.type.equals("INCOME", ignoreCase = true) || it.type.equals("CREDIT", ignoreCase = true) }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) || it.type.equals("DEBIT", ignoreCase = true) }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

        val categorySpends = transactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) || it.type.equals("DEBIT", ignoreCase = true) }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        // Paints
        val brandPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subheadPaint = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val cardBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.rgb(5, 150, 105) // Emerald 600
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(220, 38, 38) // Red 600
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bluePaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // Blue 600
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = margin + 10f

        // Top Brand Accent Line
        val accentLinePaint = Paint().apply {
            color = Color.rgb(99, 102, 241) // Indigo 500
            strokeWidth = 3f
        }
        canvas.drawLine(margin, y, pageWidth - margin, y, accentLinePaint)
        y += 18f

        // Header
        canvas.drawText("VARSEL", margin, y + 4f, brandPaint)
        val reportTag = "FINANCIAL STATEMENT"
        val tagWidth = boldTextPaint.measureText(reportTag)
        canvas.drawText(reportTag, pageWidth - margin - tagWidth, y + 2f, boldTextPaint)
        y += 18f

        canvas.drawText("Period: $periodTitle • Generated on ${dateFormat.format(Date())} ${timeFormat.format(Date())}", margin, y, subheadPaint)
        val privateTag = "100% Offline & Private"
        val privateWidth = subheadPaint.measureText(privateTag)
        canvas.drawText(privateTag, pageWidth - margin - privateWidth, y, subheadPaint)
        y += 24f

        // Executive KPI Cards (Row of 4 cards)
        val cardSpacing = 8f
        val usableWidth = (pageWidth - margin * 2)
        val cardWidth = (usableWidth - (cardSpacing * 3)) / 4f
        val cardHeight = 52f

        val kpiData = listOf(
            Triple("TOTAL INFLOW", formatCurrency(totalIncome), greenPaint),
            Triple("TOTAL OUTFLOW", formatCurrency(totalExpense), redPaint),
            Triple("NET SAVINGS", formatCurrency(netSavings), if (netSavings >= 0) greenPaint else redPaint),
            Triple("SAVINGS RATE", String.format(Locale.US, "%.1f%%", savingsRate), bluePaint)
        )

        for (i in kpiData.indices) {
            val left = margin + (i * (cardWidth + cardSpacing))
            val rect = RectF(left, y, left + cardWidth, y + cardHeight)
            canvas.drawRoundRect(rect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(rect, 8f, 8f, cardBorderPaint)

            val (title, value, paint) = kpiData[i]
            canvas.drawText(title, left + 10f, y + 18f, subheadPaint)
            canvas.drawText(value, left + 10f, y + 38f, paint)
        }
        y += cardHeight + 24f

        // Category Breakdown Section
        if (categorySpends.isNotEmpty()) {
            canvas.drawText("TOP EXPENSE CATEGORIES", margin, y, sectionTitlePaint)
            y += 14f

            val catBoxHeight = 30f
            val catCardWidth = (usableWidth - cardSpacing) / 2f

            for (i in categorySpends.indices) {
                val (catName, amount) = categorySpends[i]
                val col = i % 2
                val left = margin + (col * (catCardWidth + cardSpacing))
                val boxY = y + ((i / 2) * (catBoxHeight + 6f))

                val rect = RectF(left, boxY, left + catCardWidth, boxY + catBoxHeight)
                canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)
                canvas.drawRoundRect(rect, 6f, 6f, cardBorderPaint)

                val pct = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                val label = "$catName (${String.format(Locale.US, "%.1f%%", pct)})"
                canvas.drawText(label, left + 8f, boxY + 19f, textPaint)

                val amtStr = formatCurrency(amount)
                val amtWidth = boldTextPaint.measureText(amtStr)
                canvas.drawText(amtStr, left + catCardWidth - amtWidth - 8f, boxY + 19f, boldTextPaint)
            }

            y += ((categorySpends.size + 1) / 2) * (catBoxHeight + 6f) + 16f
        }

        // Transactions Table Header
        fun drawTableHeader(c: Canvas, curY: Float) {
            c.drawText("TRANSACTION LEDGER (${transactions.size} records)", margin, curY, sectionTitlePaint)
            val headerY = curY + 14f
            val tableHeaderRect = RectF(margin, headerY, pageWidth - margin, headerY + 20f)
            c.drawRoundRect(tableHeaderRect, 4f, 4f, cardBgPaint)
            c.drawRoundRect(tableHeaderRect, 4f, 4f, cardBorderPaint)

            c.drawText("Date", margin + 8f, headerY + 14f, boldTextPaint)
            c.drawText("Description", margin + 80f, headerY + 14f, boldTextPaint)
            c.drawText("Category", margin + 270f, headerY + 14f, boldTextPaint)
            c.drawText("Bank / Acc", margin + 370f, headerY + 14f, boldTextPaint)
            val amtHeader = "Amount (INR)"
            val amtWidth = boldTextPaint.measureText(amtHeader)
            c.drawText(amtHeader, pageWidth - margin - amtWidth - 8f, headerY + 14f, boldTextPaint)
        }

        drawTableHeader(canvas, y)
        y += 38f

        // Table Rows
        val rowHeight = 20f
        val dividerPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 1f
        }

        for (tx in transactions) {
            // Check for page break
            if (y + rowHeight > pageHeight - margin - 20f) {
                // Draw Footer for current page
                drawPageFooter(canvas, pageNumber)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 10f

                drawTableHeader(canvas, y)
                y += 38f
            }

            // Draw Row Content
            val dateStr = dateFormat.format(Date(tx.dateTimestamp))
            canvas.drawText(dateStr, margin + 8f, y + 14f, textPaint)

            val desc = if (tx.description.length > 30) tx.description.take(28) + "…" else tx.description
            canvas.drawText(desc, margin + 80f, y + 14f, textPaint)

            val cat = if (tx.category.length > 18) tx.category.take(16) + "…" else tx.category
            canvas.drawText(cat, margin + 270f, y + 14f, textPaint)

            val bankInfo = when {
                !tx.accountLast4.isNullOrEmpty() -> "•${tx.accountLast4}"
                !tx.bankName.isNullOrEmpty() -> tx.bankName
                else -> "-"
            }
            canvas.drawText(bankInfo, margin + 370f, y + 14f, textPaint)

            val isIncome = tx.type.equals("INCOME", ignoreCase = true) || tx.type.equals("CREDIT", ignoreCase = true)
            val prefix = if (isIncome) "+ " else "- "
            val amtText = prefix + formatCurrency(tx.amount)
            val amtPaint = if (isIncome) greenPaint else redPaint
            val amtWidth = amtPaint.measureText(amtText)
            canvas.drawText(amtText, pageWidth - margin - amtWidth - 8f, y + 14f, amtPaint)

            // Row Bottom Divider
            canvas.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, dividerPaint)
            y += rowHeight
        }

        // Draw Footer for last page
        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        // Write document to stream
        document.writeTo(outputStream)
        document.close()
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val footerText = "Varsel • Confidential Financial Statement"
        canvas.drawText(footerText, margin, pageHeight - margin + 10f, footerPaint)

        val pageText = "Page $pageNumber"
        val pageWidthMeasure = footerPaint.measureText(pageText)
        canvas.drawText(pageText, pageWidth - margin - pageWidthMeasure, pageHeight - margin + 10f, footerPaint)
    }

    suspend fun generateReportToCacheAndGetUri(
        context: Context,
        transactions: List<TransactionEntity>,
        periodTitle: String
    ): FileExportResult = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "varsel_statement_${fileNameDateFormat.format(Date())}.pdf"
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            generateReportToStream(transactions, periodTitle, fos)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        FileExportResult(
            file = file,
            uri = uri,
            fileName = fileName,
            mimeType = "application/pdf",
            recordCount = transactions.size
        )
    }

    fun createShareIntent(context: Context, result: FileExportResult): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, "Varsel Financial Statement (${result.recordCount} records)")
            putExtra(Intent.EXTRA_TEXT, "Here is your printable financial statement generated by Varsel.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            currencyFormat.format(amount)
        } catch (e: Exception) {
            String.format(Locale.US, "₹%.2f", amount)
        }
    }
}
