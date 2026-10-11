package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.KOKO_COLOR
import com.lcktechnologies.budgetplanner.data.LOAN_COLOR
import com.lcktechnologies.budgetplanner.data.NONE_COLOR
import com.lcktechnologies.budgetplanner.data.Period
import com.lcktechnologies.budgetplanner.data.periodBounds
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.data.report
import com.lcktechnologies.budgetplanner.ui.BarChart
import com.lcktechnologies.budgetplanner.ui.HeroCard
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.LocalLang
import com.lcktechnologies.budgetplanner.ui.LocalPositive
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.PeriodNav
import com.lcktechnologies.budgetplanner.ui.ScreenColumn
import com.lcktechnologies.budgetplanner.ui.SectionTitle
import com.lcktechnologies.budgetplanner.ui.SegTabs
import com.lcktechnologies.budgetplanner.ui.tr
import com.lcktechnologies.budgetplanner.ui.trf
import java.time.LocalDate
import java.util.Locale

@Composable
fun ReportsScreen(state: AppState) {
    val fmt = LocalFmt.current
    val lang = LocalLang.current
    val pos = LocalPositive.current
    val today = remember { LocalDate.now() }

    var period by rememberSaveable { mutableStateOf(Period.MONTH) }
    var anchor by rememberSaveable { mutableStateOf(today) }

    val locale = remember(lang) {
        Locale.Builder().setLanguage(if (lang == "si" || lang == "ta") lang else "en").setRegion("LK").build()
    }
    val (s, e) = periodBounds(period, anchor)
    val report = remember(state, period, anchor) { state.report(period, anchor, today, locale) }

    fun shift(dir: Int) {
        anchor = when (period) {
            Period.DAY -> anchor.plusDays(dir.toLong())
            Period.WEEK -> anchor.plusWeeks(dir.toLong())
            Period.MONTH -> anchor.plusMonths(dir.toLong())
            Period.YEAR -> anchor.plusYears(dir.toLong())
        }
    }

    val title = when (period) {
        Period.DAY -> fmt.longDate(anchor)
        Period.WEEK -> fmt.shortDate(s) + " \u2013 " + fmt.shortDate(e)
        Period.MONTH -> fmt.monthLabel("%04d-%02d".format(s.year, s.monthValue))
        Period.YEAR -> s.year.toString()
    }

    ScreenColumn {
        SegTabs(
            options = listOf(
                Period.DAY.name to tr("period_daily"),
                Period.WEEK.name to tr("period_weekly"),
                Period.MONTH.name to tr("period_monthly"),
                Period.YEAR.name to tr("period_yearly")
            ),
            selected = period.name,
            onSelect = { period = Period.valueOf(it); anchor = today }
        )
        Spacer(Modifier.padding(top = 10.dp))
        PeriodNav(title, onPrev = { shift(-1) }, onNext = { shift(1) })

        if (report.income <= 0 && report.out <= 0) {
            MutedText(tr("nothing_recorded"), Modifier.padding(top = 20.dp))
            return@ScreenColumn
        }

        val payTotal = r2(report.loans + report.koko)
        var sub = trf("income_spent", fmt.money(report.income), fmt.money(report.out))
        if (payTotal > 0) sub += " " + trf("incl_loan_koko", fmt.money(payTotal))

        val list = ArrayList<Triple<String, Double, Color>>()
        state.cats.forEach { c ->
            val v = report.exp[c.id] ?: 0.0
            if (v > 0) list.add(Triple(c.name, v, Color(c.color)))
        }
        val orphan = report.exp.filterKeys { id -> state.cats.none { it.id == id } }.values.sum()
        if (orphan > 0) list.add(Triple(tr("cat_uncategorized"), orphan, Color(NONE_COLOR)))
        if (report.loans > 0) list.add(Triple(tr("loans_h"), report.loans, Color(LOAN_COLOR)))
        if (report.koko > 0) list.add(Triple(tr("koko_h"), report.koko, Color(KOKO_COLOR)))
        list.sortByDescending { it.second }

        HeroCard(
            label = tr(if (report.net < 0) "overspent_by" else "left_over"),
            big = fmt.money(Math.abs(report.net)),
            sub = sub, negative = report.net < 0,
            segments = list.map { Pair(it.second, it.third) },
            base = maxOf(report.income, report.out, 1.0)
        )

        if (state.income > 0) {
            MutedText(
                tr(if (period == Period.DAY || period == Period.WEEK) "regular_income_daily_hint" else "regular_income_monthly_hint"),
                Modifier.padding(top = 12.dp)
            )
        }

        if (report.buckets.isNotEmpty() && report.buckets.any { it.value > 0 }) {
            SectionTitle(tr(if (period == Period.YEAR) "spending_by_month" else "spending_by_day"))
            BarChart(report.buckets, MaterialTheme.colorScheme.primary)
        }

        if (list.isNotEmpty()) {
            SectionTitle(tr("where_it_went"))
            list.forEach { (name, v, color) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(10.dp))
                    Text(name, modifier = Modifier.weight(1f))
                    Text(fmt.money(v) + "  " + Math.round(v / report.out * 100) + "%")
                }
            }
        }

        val inc = ArrayList<Pair<String, Double>>()
        if (report.regular > 0) inc.add(Pair(tr("regular_income"), report.regular))
        report.other.forEach { (k, v) -> inc.add(Pair(k, r2(v))) }
        if (inc.isNotEmpty()) {
            SectionTitle(tr("income_h"))
            inc.sortedByDescending { it.second }.forEach { (name, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(Modifier.size(12.dp).clip(CircleShape).background(pos))
                    Spacer(Modifier.width(10.dp))
                    Text(name, modifier = Modifier.weight(1f))
                    Text(fmt.money(v) + "  " + Math.round(v / report.income * 100) + "%")
                }
            }
        }
        Spacer(Modifier.padding(bottom = 20.dp))
    }
}
