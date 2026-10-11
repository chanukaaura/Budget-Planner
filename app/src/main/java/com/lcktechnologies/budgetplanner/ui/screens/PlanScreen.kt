package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.Category
import com.lcktechnologies.budgetplanner.data.assigned
import com.lcktechnologies.budgetplanner.data.KOKO_COLOR
import com.lcktechnologies.budgetplanner.data.LOAN_COLOR
import com.lcktechnologies.budgetplanner.data.Loan
import com.lcktechnologies.budgetplanner.data.PALETTE
import com.lcktechnologies.budgetplanner.data.SubCategory
import com.lcktechnologies.budgetplanner.data.avgMonthlySpend
import com.lcktechnologies.budgetplanner.data.loanLeft
import com.lcktechnologies.budgetplanner.data.monthsForAverage
import com.lcktechnologies.budgetplanner.data.newId
import com.lcktechnologies.budgetplanner.data.plannedTotal
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.ui.BudgetViewModel
import com.lcktechnologies.budgetplanner.ui.ChipFlow
import com.lcktechnologies.budgetplanner.ui.ConfirmDialog
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.NumField
import com.lcktechnologies.budgetplanner.ui.ScreenColumn
import com.lcktechnologies.budgetplanner.ui.SectionTitle
import com.lcktechnologies.budgetplanner.ui.tr
import com.lcktechnologies.budgetplanner.ui.trf
import java.time.LocalDate

@Composable
fun PlanScreen(state: AppState, vm: BudgetViewModel, onOpenSettings: () -> Unit) {
    val fmt = LocalFmt.current
    val today = remember { LocalDate.now() }

    var showBudgets by remember { mutableStateOf(false) }
    var showLoans by remember { mutableStateOf(false) }
    val openCats = remember { mutableStateMapOf<String, Boolean>() }
    var pendingErase by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }

    val catTotal = state.cats.sumOf { it.budget }
    val plannedAll = state.plannedTotal()
    val hasShare = false // shares only matter for the built-in defaults' suggested split, kept simple here
    val hasHistory = state.tx.any { it.kind == com.lcktechnologies.budgetplanner.data.Kind.EXPENSE }

    ScreenColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("monthly_plan_h"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = tr("settings_title")) }
        }

        NumField(
            value = state.income,
            onValue = { v -> vm.mutate { it.copy(income = v) } },
            label = tr("regular_monthly_income"),
            resetKey = state.income,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        MutedText(tr("one_off_income_hint"), Modifier.padding(top = 4.dp))

        SectionTitle(tr("budget_for_each_h"))
        val a = state.assigned()
        MutedText(
            when (a.kind) {
                0 -> trf("plan_totals", fmt.money(plannedAll))
                1 -> tr("all_assigned")
                2 -> trf("unassigned", fmt.money(a.amount))
                else -> trf("over_income_plan", fmt.money(a.amount))
            },
            Modifier.padding(top = 2.dp)
        )
        MutedText(trf("yearly_projection", fmt.money(r2(catTotal * 12))), Modifier.padding(top = 2.dp))

        OutlinedButton(onClick = { showBudgets = !showBudgets }, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
            Text(tr(if (showBudgets) "hide_categories" else "show_categories"))
        }

        if (showBudgets) {
            state.cats.forEach { c ->
                CategoryCard(
                    state = state, cat = c, vm = vm,
                    expanded = openCats[c.id] == true,
                    onToggle = { openCats[c.id] = !(openCats[c.id] ?: false) }
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val id = newId()
                    vm.mutate { s -> s.copy(cats = s.cats + Category(id = id, name = "", color = PALETTE[s.cats.size % PALETTE.size])) }
                    openCats[id] = true
                }) { Text(tr("add_category")) }
                if (hasHistory) {
                    OutlinedButton(onClick = {
                        vm.mutate { s -> s.copy(cats = s.cats.map { it.copy(budget = s.avgMonthlySpend(it.id, today)) }) }
                    }) { Text(tr("use_average_budget")) }
                }
            }
            if (hasHistory) {
                MutedText(
                    trf("average_hint", state.monthsForAverage(today).size),
                    Modifier.padding(top = 6.dp)
                )
            }
        }

        SectionTitle(tr("loans_koko_h"))
        OutlinedButton(onClick = { showLoans = !showLoans }, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
            Text(tr(if (showLoans) "hide_loans" else "show_loans"))
        }
        if (showLoans) {
            if (state.loans.isEmpty()) {
                MutedText(tr("loans_empty_hint"), Modifier.padding(bottom = 8.dp))
            }
            state.loans.forEach { l -> LoanCard(state, l, vm) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    vm.mutate { s -> s.copy(loans = s.loans + Loan(id = newId(), name = "", koko = false)) }
                }) { Text(tr("add_loan")) }
                OutlinedButton(onClick = {
                    vm.mutate { s -> s.copy(loans = s.loans + Loan(id = newId(), name = "", koko = true, count = 6)) }
                }) { Text(tr("add_koko")) }
            }
        }

        Spacer(Modifier.padding(top = 24.dp))
        TextButton(onClick = { passwordInput = ""; passwordError = false; pendingErase = true }) {
            Text(tr("erase_all_data"), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.padding(bottom = 24.dp))
    }

    if (pendingErase) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingErase = false },
            title = { Text(tr("erase_all_data")) },
            text = {
                Column {
                    Text(tr("erase_password_prompt"))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; passwordError = false },
                        singleLine = true,
                        isError = passwordError,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    if (passwordError) MutedText(tr("wrong_password"), Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (passwordInput == state.erasePassword) {
                        vm.reset(); pendingErase = false
                    } else passwordError = true
                }) { Text(tr("erase_all_data"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingErase = false }) { Text(tr("cancel")) } }
        )
    }
}

@Composable
private fun CategoryCard(state: AppState, cat: Category, vm: BudgetViewModel, expanded: Boolean, onToggle: () -> Unit) {
    val fmt = LocalFmt.current
    var newSub by remember(cat.id) { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(cat.color)))
            Spacer(Modifier.width(10.dp))
            Text(cat.name.ifEmpty { tr("new_category") }, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                if (cat.budget > 0) fmt.money(cat.budget) else tr("no_budget_set"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            OutlinedTextField(
                value = cat.name,
                onValueChange = { v -> vm.updateCat(cat.id) { it.copy(name = v) } },
                label = { Text(tr("name")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumField(
                    value = cat.budget,
                    onValue = { v -> vm.updateCat(cat.id) { it.copy(budget = v) } },
                    label = tr("monthly_budget"),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.mutate { s -> s.copy(cats = s.cats.filter { it.id != cat.id }) } }) {
                    Icon(Icons.Filled.Close, contentDescription = tr("delete"))
                }
            }
            FilterChip(
                selected = cat.recurring,
                onClick = { vm.updateCat(cat.id) { it.copy(recurring = !it.recurring) } },
                label = { Text(tr("recurring")) },
                modifier = Modifier.padding(top = 8.dp)
            )
            MutedText(
                trf("sub_categories_count", cat.subs.size),
                Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
            if (cat.subs.isNotEmpty()) {
                ChipFlow {
                    cat.subs.forEach { sc ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text(sc.name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = tr("delete"),
                                    modifier = Modifier.size(16.dp).clickable {
                                        vm.updateCat(cat.id) { c -> c.copy(subs = c.subs.filter { it.id != sc.id }) }
                                    }
                                )
                            }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSub, onValueChange = { newSub = it },
                    label = { Text(tr("new_sub_category")) }, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    if (newSub.isNotBlank()) {
                        vm.updateCat(cat.id) { c -> c.copy(subs = c.subs + SubCategory(newId(), newSub.trim())) }
                        newSub = ""
                    }
                }) { Text(tr("add")) }
            }
            Spacer(Modifier.padding(bottom = 4.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun LoanCard(state: AppState, loan: Loan, vm: BudgetViewModel) {
    val fmt = LocalFmt.current
    val color = Color(if (loan.koko) KOKO_COLOR else LOAN_COLOR)

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = loan.name,
                onValueChange = { v -> vm.updateLoan(loan.id) { it.copy(name = v) } },
                label = { Text(if (loan.koko) tr("koko_name") else tr("loan_name")) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.mutate { s -> s.copy(loans = s.loans.filter { it.id != loan.id }) } }) {
                Icon(Icons.Filled.Close, contentDescription = tr("delete"))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(
                value = loan.principal,
                onValue = { v ->
                    vm.updateLoan(loan.id) { l ->
                        val nl = l.copy(principal = v)
                        if (nl.koko && nl.count > 0) nl.copy(monthly = r2(v / nl.count)) else nl
                    }
                },
                label = if (loan.koko) tr("purchase_total") else tr("amount_borrowed"),
                modifier = Modifier.weight(1f)
            )
            if (loan.koko) {
                NumField(
                    value = loan.count.toDouble(),
                    onValue = { v ->
                        val c = v.toInt().coerceIn(1, 60)
                        vm.updateLoan(loan.id) { it.copy(count = c, monthly = r2(it.principal / c)) }
                    },
                    label = tr("installments"),
                    modifier = Modifier.weight(1f)
                )
            } else {
                NumField(
                    value = loan.monthly,
                    onValue = { v -> vm.updateLoan(loan.id) { it.copy(monthly = v) } },
                    label = tr("monthly_payment"),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (loan.koko) {
            MutedText(trf("each_installment", fmt.money(loan.monthly)), Modifier.padding(top = 4.dp))
        }
        val left = state.loanLeft(loan)
        if (loan.principal > 0) {
            MutedText(
                if (left <= 0) tr("paid_off") else trf("remaining_balance", fmt.money(left)),
                Modifier.padding(top = 4.dp)
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
