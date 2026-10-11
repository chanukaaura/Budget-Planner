package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.Category
import com.lcktechnologies.budgetplanner.data.KOKO_COLOR
import com.lcktechnologies.budgetplanner.data.Kind
import com.lcktechnologies.budgetplanner.data.LOAN_COLOR
import com.lcktechnologies.budgetplanner.data.Loan
import com.lcktechnologies.budgetplanner.data.NONE_COLOR
import com.lcktechnologies.budgetplanner.data.assigned
import com.lcktechnologies.budgetplanner.data.avgMonthlyTotal
import com.lcktechnologies.budgetplanner.data.daysUntilDay
import com.lcktechnologies.budgetplanner.data.goalSaved
import com.lcktechnologies.budgetplanner.data.loanCount
import com.lcktechnologies.budgetplanner.data.loanLeft
import com.lcktechnologies.budgetplanner.data.loanPaid
import com.lcktechnologies.budgetplanner.data.monthSummary
import com.lcktechnologies.budgetplanner.data.monthsForAverage
import com.lcktechnologies.budgetplanner.data.plannedTotal
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.ui.DonutSection
import com.lcktechnologies.budgetplanner.ui.HeroCard
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.LocalPositive
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.PeriodNav
import com.lcktechnologies.budgetplanner.ui.ProgressRow
import com.lcktechnologies.budgetplanner.ui.ScreenColumn
import com.lcktechnologies.budgetplanner.ui.SectionTitle
import com.lcktechnologies.budgetplanner.ui.SegTabs
import com.lcktechnologies.budgetplanner.ui.Slice
import com.lcktechnologies.budgetplanner.Tab
import com.lcktechnologies.budgetplanner.ui.tr
import com.lcktechnologies.budgetplanner.ui.trf
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DashboardScreen(
    state: AppState,
    month: String,
    onMonth: (String) -> Unit,
    onAddEntry: (kind: String, id: String?) -> Unit,
    onOpenTab: (Tab) -> Unit
) {
    val fmt = LocalFmt.current
    val today = LocalDate.now()

    var ovTab by rememberSaveable { mutableStateOf("expenses") }
    var showExpenses by rememberSaveable { mutableStateOf(true) }
    var showAllCats by rememberSaveable { mutableStateOf(false) }
    var showLoans by rememberSaveable { mutableStateOf(true) }

    val sum = remember(state, month) { state.monthSummary(month) }
    val orphan = sum.byCat.filterKeys { id -> state.cats.none { it.id == id } }.values.sum()
    val inc = r2(state.income + sum.otherIncome)
    val spent = r2(sum.spentExp + sum.loanOut + sum.kokoOut)
    val left = r2(inc - spent)

    ScreenColumn {
        PeriodNav(
            title = fmt.monthLabel(month),
            onPrev = { onMonth(YearMonth.parse(month).minusMonths(1).toString()) },
            onNext = { onMonth(YearMonth.parse(month).plusMonths(1).toString()) }
        )

        // ----- Hero -----
        val segs = ArrayList<Pair<Double, Color>>()
        state.cats.forEach { segs.add(Pair(sum.byCat[it.id] ?: 0.0, Color(it.color))) }
        segs.add(Pair(orphan, Color(NONE_COLOR)))
        segs.add(Pair(sum.loanOut, Color(LOAN_COLOR)))
        segs.add(Pair(sum.kokoOut, Color(KOKO_COLOR)))
        val base = maxOf(inc, spent, 1.0)

        if (inc <= 0) {
            HeroCard(
                label = tr("spent_in") + " " + fmt.monthName(month),
                big = fmt.money(spent),
                sub = tr("set_income_hint"),
                negative = false, segments = segs, base = base
            )
        } else {
            var sub = trf("spent_of", fmt.money(spent), fmt.money(inc))
            if (left > 0 && month == YearMonth.from(today).toString()) {
                val rem = today.lengthOfMonth() - today.dayOfMonth + 1
                sub += " " + trf("per_day_hint", fmt.money(left / rem), rem)
            }
            HeroCard(
                label = tr(if (left < 0) "over_income_by" else "left_to_spend"),
                big = fmt.money(Math.abs(left)),
                sub = sub,
                negative = left < 0, segments = segs, base = base
            )
        }

        // ----- Salary / advance countdown -----
        val salaryParts = ArrayList<String>()
        daysUntilDay(state.salaryDay, today)?.let { salaryParts.add(trf("salary_in_n", it)) }
        daysUntilDay(state.advanceDay, today)?.let { salaryParts.add(trf("advance_in_n", it)) }
        if (salaryParts.isNotEmpty()) MutedText(salaryParts.joinToString(" · "), Modifier.padding(top = 12.dp))

        SegTabs(
            options = listOf("expenses" to tr("tab_expenses"), "income" to tr("tab_income")),
            selected = ovTab,
            onSelect = { ovTab = it },
            modifier = Modifier.padding(top = 14.dp)
        )

        if (ovTab == "income") {
            IncomeTab(state, sum.incomeBySrc, inc, onAddIncome = { onAddEntry(Kind.INCOME, null) })
            return@ScreenColumn
        }

        // ----- Assigned note -----
        if (state.income <= 0) {
            Button(onClick = { onOpenTab(Tab.Plan) }, modifier = Modifier.padding(top = 12.dp)) {
                Text(tr("set_monthly_income"))
            }
        } else {
            val a = state.assigned()
            val text = when (a.kind) {
                1 -> tr("all_assigned")
                2 -> trf("unassigned", fmt.money(a.amount))
                else -> trf("over_income_plan", fmt.money(a.amount))
            }
            Text(
                text,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = if (a.kind == 3) androidx.compose.material3.MaterialTheme.colorScheme.error
                else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // ----- Budget plan donut -----
        val planSlices = ArrayList<Slice>()
        state.cats.filter { it.budget > 0 }.forEach { planSlices.add(Slice(it.name, it.budget, Color(it.color))) }
        state.loans.filter { it.monthly > 0 && state.loanLeft(it) > 0 }
            .forEach { planSlices.add(Slice(it.name, it.monthly, Color(if (it.koko) KOKO_COLOR else LOAN_COLOR))) }
        val planTotal = planSlices.sumOf { it.value }
        DonutSection(
            title = tr("budget_plan_h"),
            slices = planSlices,
            centerText = tr("budget_plan_h"),
            emptyText = tr("plan_empty_hint"),
            footer = if (planTotal > 0) trf("planned_total", fmt.money(r2(planTotal))) else null,
            money = fmt::money
        )

        // ----- Expenses donut -----
        val expSlices = ArrayList<Slice>()
        state.cats.forEach { c ->
            val v = sum.byCat[c.id] ?: 0.0
            if (v > 0) expSlices.add(Slice(c.name, v, Color(c.color)))
        }
        if (orphan > 0) expSlices.add(Slice(tr("cat_uncategorized"), orphan, Color(NONE_COLOR)))
        if (sum.loanOut > 0) expSlices.add(Slice(tr("loans_h"), sum.loanOut, Color(LOAN_COLOR)))
        if (sum.kokoOut > 0) expSlices.add(Slice(tr("koko_h"), sum.kokoOut, Color(KOKO_COLOR)))
        expSlices.sortByDescending { it.value }
        DonutSection(
            title = tr("expenses_pie_h"),
            slices = expSlices,
            centerText = tr("pie_expenses"),
            emptyText = tr("no_expenses_yet"),
            footer = if (expSlices.isNotEmpty()) fmt.money(spent) + " · " + fmt.monthName(month) else null,
            money = fmt::money
        )

        // ----- Next month, based on history -----
        if (state.tx.any { it.kind == Kind.EXPENSE }) {
            SectionTitle(tr("next_month_h"))
            val months = state.monthsForAverage(today)
            var text = trf("avg_text", months.size, fmt.money(state.avgMonthlyTotal(today)))
            val planned = state.plannedTotal()
            if (planned > 0) text += " " + trf("budget_total_text", fmt.money(planned))
            MutedText(text, Modifier.padding(top = 4.dp))
        }

        // ----- Goals summary -----
        if (state.goals.isNotEmpty()) {
            SectionTitle(tr("goals_h"))
            val saved = r2(state.goals.sumOf { state.goalSaved(it.id) })
            val target = state.goals.sumOf { it.target }
            MutedText(
                trf("goals_summary", fmt.money(saved), fmt.money(target), state.goals.size),
                Modifier.padding(top = 4.dp)
            )
            state.goals.filter { it.target > 0 || state.goalSaved(it.id) > 0 }.forEach { g ->
                val s = state.goalSaved(g.id)
                ProgressRow(
                    dot = Color(g.color), name = g.name,
                    amount = fmt.money(s) + if (g.target > 0) " / " + fmt.money(g.target) else "",
                    progress = if (g.target > 0) (s / g.target).toFloat() else if (s > 0) 1f else 0f,
                    barColor = Color(g.color)
                )
            }
            OutlinedButton(onClick = { onOpenTab(Tab.Goals) }, modifier = Modifier.padding(top = 10.dp)) {
                Text(tr("view_goals"))
            }
        }

        // ----- Expenses list (with show / hide) -----
        OutlinedButton(onClick = { showExpenses = !showExpenses }, modifier = Modifier.padding(top = 14.dp)) {
            Text(tr(if (showExpenses) "hide_expenses" else "show_expenses"))
        }
        if (showExpenses) {
            val all = ArrayList<Category>(state.cats)
            if (orphan > 0) all.add(Category(id = "__none", name = tr("cat_uncategorized"), color = NONE_COLOR))
            val byAll = if (orphan > 0) sum.byCat + ("__none" to orphan) else sum.byCat

            val active = all.filter { showAllCats || (byAll[it.id] ?: 0.0) > 0 || it.budget > 0 }
            val hidden = all.size - active.size
            val recurring = active.filter { it.recurring }
            val others = active.filter { !it.recurring }

            @Composable
            fun catRow(c: Category) {
                val s = byAll[c.id] ?: 0.0
                val b = c.budget
                val over = b > 0 && s > b
                val caption = when {
                    b <= 0 -> tr("no_budget_set")
                    over -> tr("over_by") + " " + fmt.money(r2(s - b))
                    else -> fmt.money(r2(b - s)) + " " + tr("left")
                }
                val subLine = c.subs.mapNotNull { sc ->
                    val v = sum.subBy[c.id]?.get(sc.id) ?: 0.0
                    if (v > 0) sc.name + " " + fmt.money(v) else null
                }.joinToString(" · ")
                ProgressRow(
                    dot = Color(c.color), name = c.name,
                    amount = fmt.money(s) + if (b > 0) " / " + fmt.money(b) else "",
                    progress = if (b > 0) (s / b).toFloat() else if (s > 0) 1f else 0f,
                    barColor = if (over) androidx.compose.material3.MaterialTheme.colorScheme.error else Color(c.color),
                    caption = caption, captionError = over,
                    extra = subLine.ifEmpty { null },
                    onClick = if (c.id == "__none") null else ({ onAddEntry(Kind.EXPENSE, c.id) })
                )
            }

            if (active.isEmpty()) {
                MutedText(tr("no_categories_yet"), Modifier.padding(top = 8.dp))
                Button(onClick = { onOpenTab(Tab.Plan) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(tr("add_categories"))
                }
            } else {
                if (recurring.isNotEmpty()) {
                    if (others.isNotEmpty()) SectionTitle(tr("recurring_expenses_h"))
                    recurring.forEach { catRow(it) }
                }
                if (others.isNotEmpty()) {
                    if (recurring.isNotEmpty()) SectionTitle(tr("other_expenses_h"))
                    others.forEach { catRow(it) }
                }
            }
            if (hidden > 0 || showAllCats) {
                OutlinedButton(onClick = { showAllCats = !showAllCats }, modifier = Modifier.padding(top = 10.dp)) {
                    Text(if (showAllCats) tr("hide_inactive") else trf("show_more_cats", hidden))
                }
            }
        }

        // ----- Loans and KOKO -----
        if (state.loans.isNotEmpty()) {
            OutlinedButton(onClick = { showLoans = !showLoans }, modifier = Modifier.padding(top = 14.dp)) {
                Text(tr(if (showLoans) "hide_loans" else "show_loans"))
            }
            if (showLoans) {
                val plain = state.loans.filter { !it.koko }
                val koko = state.loans.filter { it.koko }
                if (plain.isNotEmpty()) {
                    SectionTitle(tr("loans_h"))
                    plain.forEach { LoanRow(state, it, month, onAddEntry) }
                }
                if (koko.isNotEmpty()) {
                    SectionTitle(tr("koko_h"))
                    val open = koko.filter { state.loanLeft(it) > 0 }
                    val total = r2(open.sumOf { state.loanLeft(it) })
                    if (total > 0) MutedText(trf("koko_balance", fmt.money(total)), Modifier.padding(top = 2.dp))
                    koko.forEach { LoanRow(state, it, month, onAddEntry) }
                }
            }
        }
    }
}

@Composable
private fun IncomeTab(state: AppState, bySrc: Map<String, Double>, total: Double, onAddIncome: () -> Unit) {
    val fmt = LocalFmt.current
    val pos = LocalPositive.current
    val rows = ArrayList<Pair<String, Double>>()
    if (state.income > 0) rows.add(Pair(tr("regular_income"), state.income))
    bySrc.forEach { (k, v) -> rows.add(Pair(k, r2(v))) }
    rows.sortByDescending { it.second }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (rows.isEmpty()) {
            MutedText(tr("nothing_here_yet"), Modifier.padding(top = 12.dp))
            Button(onClick = onAddIncome, modifier = Modifier.padding(top = 10.dp)) { Text(tr("add_income_btn")) }
        } else {
            rows.forEach { (name, v) ->
                ProgressRow(
                    dot = pos, name = name, amount = fmt.money(v),
                    progress = if (total > 0) (v / total).toFloat() else 0f, barColor = pos
                )
            }
        }
    }
}

@Composable
private fun LoanRow(state: AppState, l: Loan, month: String, onAddEntry: (String, String?) -> Unit) {
    val fmt = LocalFmt.current
    val paid = state.loanPaid(l.id)
    val rem = state.loanLeft(l)
    val pm = state.loanPaid(l.id, month)
    val caption = when {
        l.principal <= 0 -> tr("set_amount_in_plan")
        rem <= 0 -> tr("paid_off")
        l.koko -> trf("installments_paid", state.loanCount(l.id), l.count) + " · " + fmt.money(rem) + " " + tr("left")
        else -> {
            var c = fmt.money(rem) + " " + tr("left")
            if (l.monthly > 0) {
                c += " · " + if (pm >= l.monthly) tr("month_done")
                else fmt.money(r2(l.monthly - pm)) + " " + tr("due_this_month")
            }
            c
        }
    }
    val color = Color(if (l.koko) KOKO_COLOR else LOAN_COLOR)
    ProgressRow(
        dot = color, name = l.name,
        amount = fmt.money(paid) + if (l.principal > 0) " / " + fmt.money(l.principal) else "",
        progress = if (l.principal > 0) (paid / l.principal).toFloat() else 0f,
        barColor = color, caption = caption,
        onClick = { onAddEntry(Kind.LOAN, l.id) }
    )
}
