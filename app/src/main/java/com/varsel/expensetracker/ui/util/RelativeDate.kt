package com.varsel.expensetracker.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object RelativeDate {

    private val dayFormatter =
        DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

    private val dateFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun format(
        timestamp: Long
    ): String {

        val transactionDate = Instant
            .ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val today = LocalDate.now()

        return when {

            transactionDate == today ->
                "Today"

            transactionDate == today.minusDays(1) ->
                "Yesterday"

            transactionDate.isAfter(today.minusDays(7)) ->
                transactionDate.format(dayFormatter)

            else ->
                transactionDate.format(dateFormatter)
        }
    }
}
