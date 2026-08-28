package com.varsel.expensetracker.util

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

/**
 * Engine responsible for parsing raw text extracted from bank statements or local OCR
 * using a Hybrid Template Matching approach. It inspects statement structure and routes 
 * text payloads to the appropriate template-specific extraction strategy.
 */
class StatementParserEngine @Inject constructor() {

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d/M/yy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
    )

    /**
     * Main entry point to parse a raw statement text string into a structured list of [Transaction] models.
     */
    fun parseStatement(rawText: String): List<Transaction> {
        if (rawText.isBlank()) return emptyList()
        val normalizedText = rawText.replace("\r\n", "\n").replace("\r", "\n")

        // Template Router: Check strict statement structure signatures to route to the correct parser
        return if (isTabularTemplate(normalizedText)) {
            parseTabularTemplateStatement(normalizedText)
        } else {
            parseStandardTemplateStatement(normalizedText)
        }
    }

    /**
     * Inspects text headers or column keywords to determine if the document uses a tabular bank layout.
     */
    private fun isTabularTemplate(text: String): Boolean {
        val upper = text.uppercase()
        return upper.contains("VALUE DATE") || 
               upper.contains("POST DATE") || 
               upper.contains("TRANSACTION DATE") ||
               (upper.contains("DESCRIPTION") && upper.contains("AMOUNT") && upper.contains("BALANCE"))
    }

    /**
     * Specialized strategy for tabular bank statement templates using anchor-based chunking.
     */
    private fun parseTabularTemplateStatement(rawText: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val dateRegex = Regex("\\b\\d{1,2}[-/]\\d{1,2}(?:[-/]\\d{2,4})?\\b|\\b\\d{1,2}[-/][A-Za-z]{3}[-/]\\d{2,4}\\b")
        val dateMatches = dateRegex.findAll(rawText).toList()

        if (dateMatches.isEmpty()) return emptyList()

        for (i in dateMatches.indices) {
            val match = dateMatches[i]
            val startIndex = match.range.first
            val endIndex = if (i + 1 < dateMatches.size) dateMatches[i + 1].range.first else rawText.length
            val chunk = rawText.substring(startIndex, endIndex).replace("\n", " ").trim()

            val dateStr = match.value
            val timestamp = parseDateSafely(dateStr)

            parseChunkIntoTransaction(timestamp, chunk)?.let {
                transactions.add(it)
            }
        }

        return transactions.filterNot { it.type == TransactionType.INCOME && it.amount < 0 }
    }

    /**
     * Specialized strategy for standard vertical statement templates.
     */
    private fun parseStandardTemplateStatement(rawText: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val dateRegex = Regex("\\b\\d{1,2}[-/][A-Za-z]{3}[-/]\\d{2,4}\\b|\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b")
        val amountRegex = Regex("[-+]?\\d{1,3}(?:[\\s,]*\\d{3})*(?:\\.\\d{2})?|[-+]?\\d+(?:\\.\\d{2})?")

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (isMetadataLine(line)) {
                i++
                continue
            }

            val dateMatch = dateRegex.find(line)
            if (dateMatch != null) {
                val dateStr = dateMatch.value
                val timestamp = parseDateSafely(dateStr)

                val blockLines = mutableListOf<String>()
                val remainder = line.substring(dateMatch.range.last + 1).trim()
                if (remainder.isNotEmpty()) {
                    blockLines.add(remainder)
                }

                i++
                while (i < lines.size && dateRegex.find(lines[i]) == null) {
                    if (isMetadataLine(lines[i])) {
                        i++
                        continue
                    }
                    blockLines.add(lines[i])
                    i++
                }

                parseVerticalBlockIntoTransaction(timestamp, blockLines, amountRegex)?.let {
                    transactions.add(it)
                }
            } else {
                i++
            }
        }

        return transactions.filterNot { it.type == TransactionType.INCOME && it.amount < 0 }
    }

    private fun isMetadataLine(line: String): Boolean {
        val upper = line.uppercase()
        return upper.contains("STATEMENT SUMMARY") ||
                upper.contains("OPENING BALANCE") ||
                upper.contains("CLOSING BALANCE") ||
                upper.contains("TOTAL DEBITS") ||
                upper.contains("TOTAL CREDITS") ||
                upper.contains("TRANSACTION HISTORY") ||
                upper.contains("VALUE DATE") ||
                upper.contains("POST DATE") ||
                upper.contains("REMITTER BRANCH") ||
                upper.contains("CHEQUE NO") ||
                (upper.startsWith("DATE") && !upper.contains("DESCRIPTION")) ||
                upper.startsWith("BRANCH:") ||
                upper.startsWith("ACCOUNT:") ||
                upper.startsWith("CURRENCY:") ||
                upper.contains("PAGE ") ||
                upper == "DR" || upper == "CR" || upper == "BALANCE" || upper == "DESCRIPTION"
    }

    private fun parseDateSafely(dateStr: String): Long {
        var formatStr = dateStr
        val hasYear = formatStr.contains(Regex("\\d{4}")) || formatStr.contains(Regex("-\\d{2}$")) || formatStr.contains(Regex("/\\d{2}$"))
        if (!hasYear) {
            formatStr = "$formatStr/2023"
        }
        for (formatter in dateFormats) {
            try {
                val localDate = LocalDate.parse(formatStr, formatter)
                return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                // Try next date format
            }
        }
        return System.currentTimeMillis()
    }

    private fun parseChunkIntoTransaction(timestamp: Long, chunk: String): Transaction? {
        val cleanedChunk = chunk.replace(Regex("\\s+"), " ").trim()
        val upper = cleanedChunk.uppercase()
        if (upper.contains("VALUE DATE") || upper.contains("STATEMENT SUMMARY") || upper.contains("OPENING BALANCE")) {
            return null
        }

        val refPattern = Regex("(?i)\\b(REF|UTR|TXN|ID)[:\\s]*([A-Za-z0-9]+)\\b")
        val implicitRefPattern = Regex("\\b\\d{10,16}\\b")

        var referenceNumber: String? = null
        val refMatch = refPattern.find(cleanedChunk)
        if (refMatch != null) {
            referenceNumber = refMatch.value.trim()
        } else {
            val implicitMatches = implicitRefPattern.findAll(cleanedChunk).toList()
            for (m in implicitMatches) {
                val candidate = m.value
                if (!cleanedChunk.contains(".$candidate") && candidate.length in 10..15) {
                    referenceNumber = candidate
                    break
                }
            }
        }

        val amountRegex = Regex("[-+]?\\b\\d{1,3}(?:,\\d{3})*\\.\\d{2}\\b|[-+]?\\b\\d+\\.\\d{2}\\b")
        val amountMatch = amountRegex.find(cleanedChunk)
        val allAmounts = amountRegex.findAll(cleanedChunk).map { it.value.replace(",", "").toDouble() }.toList()

        if (allAmounts.isEmpty()) return null

        val rawAmountStr = amountMatch?.value ?: allAmounts.last().toString()
        val transactionAmount = rawAmountStr.replace(",", "").toDouble()

        val isDebit = upper.contains("DR") || upper.contains("DEBIT") || upper.contains("WITHDRAWAL") || upper.contains("IMPS") || upper.contains("UPI") || rawAmountStr.startsWith("-")
        val transactionType = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME

        var description = cleanedChunk
            .replace(Regex("^\\d{1,2}[-/]\\d{1,2}(?:[-/]\\d{2,4})?\\b"), "")
            .replace("ATM SERVICE BRANCH", "")
            .replace(amountRegex, "")
            .replace(refPattern, "")
            .replace(Regex("\\b\\d+\\.\\d{2}CR\\b"), "")
            .replace("CR", "")
            .replace("DR", "")
            .replace("|", "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (description.isEmpty() || description.length < 3) {
            description = "Bank Transaction"
        }

        return Transaction(
            amount = kotlin.math.abs(transactionAmount),
            type = transactionType,
            description = description,
            category = "Uncategorized",
            dateTimestamp = timestamp,
            referenceNumber = referenceNumber
        )
    }

    private fun parseVerticalBlockIntoTransaction(
        timestamp: Long,
        blockLines: List<String>,
        amountRegex: Regex
    ): Transaction? {
        val cleanedBlock = blockLines.map { it.replace("|", "").trim() }.filter { it.isNotEmpty() }
        var description = "Transaction"
        var transactionAmount: Double? = null
        var transactionType = TransactionType.EXPENSE
        var referenceNumber: String? = null

        val refPattern = Regex("(?i)\\b(REF|UTR|TXN|ID)[:\\s]*([A-Za-z0-9]+)\\b")
        val implicitRefPattern = Regex("\\b\\d{10,16}\\b")
        val descBuilder = StringBuilder()
        var foundDebitIndicator = false
        var foundCreditIndicator = false

        for (item in cleanedBlock) {
            val upperItem = item.uppercase()
            if (upperItem.contains("DR") || upperItem.contains("DEBIT") || upperItem.contains("WITHDRAWAL")) {
                foundDebitIndicator = true
            }
            if (upperItem.contains("CR") || upperItem.contains("CREDIT") || upperItem.contains("DEPOSIT")) {
                foundCreditIndicator = true
            }

            val refMatch = refPattern.find(item)
            if (refMatch != null && referenceNumber == null) {
                referenceNumber = refMatch.value.trim()
                val textWithoutRef = item.replace(refMatch.value, "").trim()
                if (textWithoutRef.isNotEmpty()) {
                    descBuilder.append(" ").append(textWithoutRef)
                }
                continue
            }

            if (referenceNumber == null) {
                val implicitMatch = implicitRefPattern.find(item)
                if (implicitMatch != null && !item.contains(".") && implicitMatch.value.length >= 10) {
                    referenceNumber = implicitMatch.value
                }
            }

            val amountMatch = amountRegex.find(item)
            if (amountMatch != null && transactionAmount == null) {
                val rawAmountStr = amountMatch.value.replace(" ", "").replace(",", "")
                val parsedAmt = rawAmountStr.toDoubleOrNull()
                if (parsedAmt != null) {
                    if (upperItem.endsWith("CR") && !upperItem.contains("DR") && item.contains("BALANCE")) {
                        descBuilder.append(" ").append(item)
                        continue
                    }
                    transactionAmount = parsedAmt
                    transactionType = if (upperItem.contains("(DR)") || upperItem.contains(" DR") || rawAmountStr.startsWith("-") || upperItem == "DR") {
                        TransactionType.EXPENSE
                    } else if (upperItem.contains("(CR)") || upperItem.contains(" CR") || upperItem.contains("DEPOSIT") || upperItem == "CR") {
                        TransactionType.INCOME
                    } else {
                        if (foundCreditIndicator && !foundDebitIndicator) TransactionType.INCOME else TransactionType.EXPENSE
                    }
                    continue
                }
            }

            descBuilder.append(" ").append(item)
        }

        val rawDesc = descBuilder.toString().trim().replace(Regex("\\s+"), " ")
        if (rawDesc.isNotEmpty()) {
            description = rawDesc
        }

        val finalAmount = transactionAmount ?: 0.0
        if (finalAmount == 0.0) return null

        return Transaction(
            amount = kotlin.math.abs(finalAmount),
            type = transactionType,
            description = description,
            category = "Uncategorized",
            dateTimestamp = timestamp,
            referenceNumber = referenceNumber
        )
    }
}
