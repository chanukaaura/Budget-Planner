package com.lcktechnologies.budgetplanner.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.lcktechnologies.budgetplanner.i18n.I18n
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Money, month and day formatting for the chosen app language and currency. */
class Fmt(val lang: String, val currency: String) {
    val locale: Locale = Locale.Builder()
        .setLanguage(if (lang == "si" || lang == "ta") lang else "en")
        .setRegion("LK")
        .build()

    fun money(n: Double): String {
        val nf = NumberFormat.getCurrencyInstance(locale)
        try {
            nf.currency = Currency.getInstance(currency)
        } catch (e: Exception) {
            // Unknown code: keep the locale's default currency.
        }
        val whole = Math.abs(n - Math.round(n).toDouble()) < 0.005
        val digits = if (whole) 0 else 2
        nf.minimumFractionDigits = digits
        nf.maximumFractionDigits = digits
        return nf.format(n)
    }

    fun monthLabel(ym: String): String =
        YearMonth.parse(ym).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    fun monthName(ym: String): String =
        YearMonth.parse(ym).format(DateTimeFormatter.ofPattern("MMMM", locale))

    fun dayLabel(date: String): String =
        LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))

    fun longDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

    fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d", locale))
}

val LocalFmt = compositionLocalOf { Fmt("en", "LKR") }
val LocalLang = compositionLocalOf { "en" }

@Composable
fun tr(key: String): String = I18n.get(LocalLang.current, key)

@Composable
fun trf(key: String, vararg args: Any): String = I18n.fmt(LocalLang.current, key, *args)

/** 5000.0 -> "5000", 12.5 -> "12.5" (used to pre-fill number fields). */
fun plain(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()
