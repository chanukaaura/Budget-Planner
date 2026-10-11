package com.lcktechnologies.budgetplanner.data

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

fun r2(n: Double): Double = Math.round(n * 100.0) / 100.0

fun AppState.catById(id: String): Category? = cats.firstOrNull { it.id == id }
fun AppState.loanById(id: String): Loan? = loans.firstOrNull { it.id == id }
fun AppState.goalById(id: String): Goal? = goals.firstOrNull { it.id == id }

fun AppState.loanPaid(id: String, prefix: String? = null): Double =
    r2(tx.filter {
        it.kind == Kind.LOAN && it.loan == id && (prefix == null || it.date.startsWith(prefix))
    }.sumOf { it.amount })

fun AppState.loanCount(id: String): Int = tx.count { it.kind == Kind.LOAN && it.loan == id }

fun AppState.loanLeft(l: Loan): Double = maxOf(0.0, r2(l.principal - loanPaid(l.id)))

fun AppState.goalSaved(id: String): Double =
    r2(tx.filter { it.kind == Kind.GOAL && it.goal == id }.sumOf { it.amount })

/** Category budgets plus the monthly payment of every loan that still has a balance. */
fun AppState.plannedTotal(): Double =
    cats.sumOf { it.budget } + loans.filter { loanLeft(it) > 0 }.sumOf { it.monthly }

/** kind: 0 no income set, 1 fully assigned, 2 some income unassigned, 3 plan exceeds income. */
data class Assigned(val kind: Int, val amount: Double)

fun AppState.assigned(): Assigned {
    val p = plannedTotal()
    if (income <= 0) return Assigned(0, p)
    val d = r2(income - p)
    return when {
        d == 0.0 -> Assigned(1, 0.0)
        d > 0 -> Assigned(2, d)
        else -> Assigned(3, -d)
    }
}

data class MonthSummary(
    val byCat: Map<String, Double>,
    val subBy: Map<String, Map<String, Double>>,
    val spentExp: Double,
    val loanOut: Double,
    val kokoOut: Double,
    val otherIncome: Double,
    val incomeBySrc: Map<String, Double>
)

fun AppState.monthSummary(ym: String): MonthSummary {
    val byCat = HashMap<String, Double>()
    val subBy = HashMap<String, HashMap<String, Double>>()
    val incSrc = LinkedHashMap<String, Double>()
    var spentExp = 0.0
    var loanOut = 0.0
    var kokoOut = 0.0
    var otherInc = 0.0
    for (t in tx) {
        if (!t.date.startsWith(ym)) continue
        when (t.kind) {
            Kind.EXPENSE -> {
                byCat[t.cat] = (byCat[t.cat] ?: 0.0) + t.amount
                spentExp += t.amount
                if (t.sub.isNotEmpty()) {
                    val m = subBy.getOrPut(t.cat) { HashMap() }
                    m[t.sub] = (m[t.sub] ?: 0.0) + t.amount
                }
            }
            Kind.LOAN -> {
                if (loanById(t.loan)?.koko == true) kokoOut += t.amount else loanOut += t.amount
            }
            Kind.INCOME -> {
                otherInc += t.amount
                val k = t.src.ifEmpty { "Other" }
                incSrc[k] = (incSrc[k] ?: 0.0) + t.amount
            }
        }
    }
    return MonthSummary(byCat, subBy, r2(spentExp), r2(loanOut), r2(kokoOut), r2(otherInc), incSrc)
}

/** Past months (yyyy-MM) used for averages. The current, unfinished month is left out once there is older data. */
fun AppState.monthsForAverage(today: LocalDate = LocalDate.now()): List<String> {
    val cur = YearMonth.from(today)
    var m = try {
        YearMonth.parse(startMonth)
    } catch (e: Exception) {
        cur
    }
    val list = ArrayList<YearMonth>()
    while (!m.isAfter(cur) && list.size < 600) {
        list.add(m)
        m = m.plusMonths(1)
    }
    val res = if (list.size > 1) list.filter { it != cur } else list
    return res.map { it.toString() }
}

fun AppState.avgMonthlySpend(catId: String, today: LocalDate = LocalDate.now()): Double {
    val months = monthsForAverage(today)
    if (months.isEmpty()) return 0.0
    val set = months.toHashSet()
    val sum = tx.filter {
        it.kind == Kind.EXPENSE && it.cat == catId && set.contains(it.date.take(7))
    }.sumOf { it.amount }
    return r2(sum / months.size)
}

fun AppState.avgMonthlyTotal(today: LocalDate = LocalDate.now()): Double =
    r2(cats.sumOf { avgMonthlySpend(it.id, today) })

/** Days until the next occurrence of a day-of-month, or null when unset. */
fun daysUntilDay(day: Int, today: LocalDate = LocalDate.now()): Int? {
    if (day < 1 || day > 31) return null
    var d = today.withDayOfMonth(minOf(day, today.lengthOfMonth()))
    if (d.isBefore(today)) {
        val next = today.plusMonths(1)
        d = next.withDayOfMonth(minOf(day, next.lengthOfMonth()))
    }
    return ChronoUnit.DAYS.between(today, d).toInt()
}

// ---------- Reports ----------

enum class Period { DAY, WEEK, MONTH, YEAR }

data class Bucket(val label: String, val value: Double)

data class Report(
    val exp: Map<String, Double>,
    val sub: Map<String, Map<String, Double>>,
    val spentExp: Double,
    val loans: Double,
    val koko: Double,
    val other: Map<String, Double>,
    val otherTotal: Double,
    val goals: Double,
    val regular: Double,
    val buckets: List<Bucket>
) {
    val income: Double get() = r2(regular + otherTotal)
    val out: Double get() = r2(spentExp + loans + koko)
    val net: Double get() = r2(income - out)
}

fun periodBounds(period: Period, anchor: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    Period.DAY -> Pair(anchor, anchor)
    Period.WEEK -> {
        val s = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
        Pair(s, s.plusDays(6))
    }
    Period.MONTH -> Pair(anchor.withDayOfMonth(1), anchor.withDayOfMonth(anchor.lengthOfMonth()))
    Period.YEAR -> Pair(anchor.withDayOfYear(1), anchor.withDayOfYear(anchor.lengthOfYear()))
}

/** Regular monthly income earned inside a period, spread evenly over the days of each month. */
fun AppState.regularIncome(period: Period, s: LocalDate, e: LocalDate, today: LocalDate): Double {
    if (income <= 0) return 0.0
    val startKey = "$startMonth-01"
    val todayStr = today.toString()
    var sum = 0.0
    if (period == Period.DAY || period == Period.WEEK) {
        var d = s
        while (!d.isAfter(e)) {
            val ds = d.toString()
            if (ds >= startKey && ds <= todayStr) sum += income / d.lengthOfMonth()
            d = d.plusDays(1)
        }
    } else {
        val curKey = YearMonth.from(today).toString()
        var m = YearMonth.from(s)
        val last = YearMonth.from(e)
        while (!m.isAfter(last)) {
            val key = m.toString()
            if (key >= startMonth && key <= curKey) sum += income
            m = m.plusMonths(1)
        }
    }
    return r2(sum)
}

fun AppState.report(period: Period, anchor: LocalDate, today: LocalDate, locale: Locale): Report {
    val (s, e) = periodBounds(period, anchor)
    val sStr = s.toString()
    val eStr = e.toString()
    val exp = HashMap<String, Double>()
    val sub = HashMap<String, HashMap<String, Double>>()
    val other = LinkedHashMap<String, Double>()
    var spentExp = 0.0
    var loansV = 0.0
    var kokoV = 0.0
    var otherTotal = 0.0
    var goalsV = 0.0

    val n = when (period) {
        Period.DAY -> 0
        Period.WEEK -> 7
        Period.MONTH -> e.dayOfMonth
        Period.YEAR -> 12
    }
    val vals = DoubleArray(n)

    for (t in tx) {
        if (t.date < sStr || t.date > eStr) continue
        when (t.kind) {
            Kind.EXPENSE -> {
                exp[t.cat] = (exp[t.cat] ?: 0.0) + t.amount
                spentExp += t.amount
                if (t.sub.isNotEmpty()) {
                    val m = sub.getOrPut(t.cat) { HashMap() }
                    m[t.sub] = (m[t.sub] ?: 0.0) + t.amount
                }
            }
            Kind.LOAN -> {
                if (loanById(t.loan)?.koko == true) kokoV += t.amount else loansV += t.amount
            }
            Kind.GOAL -> goalsV += t.amount
            Kind.INCOME -> {
                val k = t.src.ifEmpty { "Other" }
                other[k] = (other[k] ?: 0.0) + t.amount
                otherTotal += t.amount
            }
        }
        if (n > 0 && (t.kind == Kind.EXPENSE || t.kind == Kind.LOAN)) {
            val d = try {
                LocalDate.parse(t.date)
            } catch (ex: Exception) {
                null
            }
            if (d != null) {
                val idx = when (period) {
                    Period.WEEK -> ChronoUnit.DAYS.between(s, d).toInt()
                    Period.MONTH -> d.dayOfMonth - 1
                    Period.YEAR -> d.monthValue - 1
                    Period.DAY -> -1
                }
                if (idx in 0 until n) vals[idx] += t.amount
            }
        }
    }

    val buckets = (0 until n).map { i ->
        val label = when (period) {
            Period.WEEK -> s.plusDays(i.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
            Period.MONTH -> if (i == 0 || (i + 1) % 5 == 0) (i + 1).toString() else ""
            Period.YEAR -> Month.of(i + 1).getDisplayName(TextStyle.NARROW, locale)
            Period.DAY -> ""
        }
        Bucket(label, vals[i])
    }

    return Report(
        exp = exp, sub = sub, spentExp = r2(spentExp), loans = r2(loansV), koko = r2(kokoV),
        other = other, otherTotal = r2(otherTotal), goals = r2(goalsV),
        regular = regularIncome(period, s, e, today), buckets = buckets
    )
}

// ---------- Describing a transaction (used by History and Export) ----------

/** [typeKey] is a translation key. [label] is empty when the category/loan/goal no longer exists. */
data class TxInfo(val typeKey: String, val label: String, val color: Long)

fun AppState.txInfo(t: Tx): TxInfo = when (t.kind) {
    Kind.INCOME -> TxInfo("kind_income", t.src.ifEmpty { "Other" }, POS_COLOR)
    Kind.LOAN -> {
        val l = loanById(t.loan)
        val koko = l?.koko == true
        TxInfo(if (koko) "koko_h" else "loans_h", l?.name ?: "", if (koko) KOKO_COLOR else LOAN_COLOR)
    }
    Kind.GOAL -> {
        val g = goalById(t.goal)
        TxInfo("kind_goal", g?.name ?: "", g?.color ?: NONE_COLOR)
    }
    else -> {
        val c = catById(t.cat)
        val subName = c?.subs?.firstOrNull { it.id == t.sub }?.name
        val label = when {
            c == null -> ""
            subName != null -> c.name + " · " + subName
            else -> c.name
        }
        TxInfo("kind_expense", label, c?.color ?: NONE_COLOR)
    }
}
