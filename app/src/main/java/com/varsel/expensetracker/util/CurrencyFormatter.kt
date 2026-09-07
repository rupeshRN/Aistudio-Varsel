package com.varsel.expensetracker.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

/**
 * Centralized, thread-safe currency formatting utility for Varsel Expense Tracker.
 * Standardizes rupee currency formatting across the entire app using the Indian numbering system.
 */
object CurrencyFormatter {

    private val indianLocale = Locale("en", "IN")

    private val symbols = DecimalFormatSymbols(indianLocale).apply {
        currencySymbol = "₹"
    }

    private val standardFormat: DecimalFormat
        get() = (NumberFormat.getCurrencyInstance(indianLocale) as DecimalFormat).apply {
            decimalFormatSymbols = symbols
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    private val wholeFormat: DecimalFormat
        get() = (NumberFormat.getCurrencyInstance(indianLocale) as DecimalFormat).apply {
            decimalFormatSymbols = symbols
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }

    private val compactFormat: DecimalFormat
        get() = DecimalFormat("₹#,##0.##", symbols)

    /**
     * Formats an amount into standard Indian currency representation.
     * e.g., 150000.00 -> "₹1,50,000.00"
     */
    fun format(amount: Double, includeDecimals: Boolean = true): String {
        return if (includeDecimals) {
            standardFormat.format(amount)
        } else {
            wholeFormat.format(round(amount).toLong())
        }
    }

    /**
     * Formats an amount as a whole rupee number without decimal places.
     * e.g., 150000.00 -> "₹1,50,000"
     */
    fun formatWhole(amount: Double): String {
        return wholeFormat.format(round(amount).toLong())
    }

    /**
     * Formats an amount with up to 2 decimals, omitting trailing zeros if applicable.
     * e.g., 150000.5 -> "₹1,50,000.5"
     */
    fun formatCompact(amount: Double): String {
        return compactFormat.format(amount)
    }
}
