package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.Kind
import com.lcktechnologies.budgetplanner.data.PAY_METHODS
import com.lcktechnologies.budgetplanner.data.Tx
import com.lcktechnologies.budgetplanner.data.INCOME_SOURCES
import com.lcktechnologies.budgetplanner.data.loanLeft
import com.lcktechnologies.budgetplanner.data.newId
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.ui.ChipFlow
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.SegTabs
import com.lcktechnologies.budgetplanner.ui.SelectChip
import com.lcktechnologies.budgetplanner.ui.plain
import com.lcktechnologies.budgetplanner.ui.tr
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Bottom sheet for adding an expense, income, loan/KOKO payment or goal contribution.
 * [initialKind]/[initialId] let a tap on a Dashboard row (e.g. a specific loan) pre-select it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    state: AppState,
    month: String,
    initialKind: String,
    initialId: String?,
    onDismiss: () -> Unit,
    onSave: (Tx) -> Unit
) {
    val fmt = LocalFmt.current
    var kind by remember { mutableStateOf(initialKind) }
    var catId by remember { mutableStateOf(if (initialKind == Kind.EXPENSE) initialId else null) }
    var subId by remember { mutableStateOf<String?>(null) }
    var src by remember { mutableStateOf(INCOME_SOURCES.first()) }
    var loanId by remember { mutableStateOf(if (initialKind == Kind.LOAN) initialId else state.loans.firstOrNull()?.id) }
    var goalId by remember { mutableStateOf(if (initialKind == Kind.GOAL) initialId else state.goals.firstOrNull()?.id) }
    var method by remember { mutableStateOf("Cash") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dateStr by remember {
        mutableStateOf(
            if (month == java.time.YearMonth.now().toString()) LocalDate.now().toString() else "$month-01"
        )
    }
    var error by remember { mutableStateOf("") }

    fun ensureSelections() {
        if (catId == null) catId = state.cats.firstOrNull()?.id
        if (loanId == null) loanId = state.loans.firstOrNull()?.id
        if (goalId == null) goalId = state.goals.firstOrNull()?.id
    }
    ensureSelections()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            val titleKey = when (kind) {
                Kind.EXPENSE -> "title_expense"
                Kind.INCOME -> "title_income"
                Kind.LOAN -> "title_loan"
                else -> "title_goal"
            }
            Text(tr(titleKey), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            SegTabs(
                options = listOf(
                    Kind.EXPENSE to tr("kind_expense"),
                    Kind.INCOME to tr("kind_income"),
                    Kind.LOAN to tr("kind_loan"),
                    Kind.GOAL to tr("kind_goal")
                ),
                selected = kind,
                onSelect = { kind = it; error = "" },
                modifier = Modifier.padding(top = 14.dp, bottom = 14.dp)
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(tr("amount")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                tr(
                    when (kind) {
                        Kind.EXPENSE -> "category"; Kind.INCOME -> "source"
                        Kind.LOAN -> "loan_or_koko"; else -> "goal_label"
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            when (kind) {
                Kind.EXPENSE -> ChipFlow {
                    state.cats.forEach { c ->
                        SelectChip(c.name.ifEmpty { tr("new_category") }, c.id == catId, Color(c.color)) {
                            catId = c.id; subId = null
                        }
                    }
                }
                Kind.INCOME -> ChipFlow {
                    INCOME_SOURCES.forEach { s -> SelectChip(tr("src_$s"), s == src) { src = s } }
                }
                Kind.LOAN -> ChipFlow {
                    state.loans.forEach { l ->
                        SelectChip(
                            l.name.ifEmpty { tr("new_loan") }, l.id == loanId,
                            Color(if (l.koko) com.lcktechnologies.budgetplanner.data.KOKO_COLOR else com.lcktechnologies.budgetplanner.data.LOAN_COLOR)
                        ) {
                            loanId = l.id
                            if (amountText.isEmpty() && l.monthly > 0) amountText = plain(l.monthly)
                        }
                    }
                }
                else -> ChipFlow {
                    state.goals.forEach { g ->
                        SelectChip(g.name.ifEmpty { tr("new_goal") }, g.id == goalId, Color(g.color)) { goalId = g.id }
                    }
                }
            }

            val cat = state.cats.firstOrNull { it.id == catId }
            if (kind == Kind.EXPENSE && cat != null && cat.subs.isNotEmpty()) {
                Text(
                    tr("sub_category_optional"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                )
                ChipFlow {
                    cat.subs.forEach { sc ->
                        SelectChip(sc.name, sc.id == subId) { subId = if (subId == sc.id) null else sc.id }
                    }
                }
            }

            Text(
                tr("payment_method"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
            )
            ChipFlow {
                PAY_METHODS.forEach { m -> SelectChip(tr("pm_$m"), m == method) { method = m } }
            }

            OutlinedTextField(
                value = dateStr,
                onValueChange = { dateStr = it },
                label = { Text(tr("date") + " (yyyy-MM-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(tr("note_optional")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            )

            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
            }

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    val date = try {
                        LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE); dateStr
                    } catch (e: Exception) {
                        null
                    }
                    when {
                        amt <= 0 -> error = tr("err_amount")
                        kind == Kind.EXPENSE && catId == null -> error = tr("err_pick_category")
                        kind == Kind.LOAN && loanId == null -> error = tr("err_pick_loan")
                        kind == Kind.GOAL && goalId == null -> error = tr("err_pick_goal")
                        date == null -> error = tr("err_date")
                        else -> {
                            onSave(
                                Tx(
                                    id = newId(), ts = System.currentTimeMillis(), date = date, kind = kind,
                                    amount = r2(amt), method = method, note = note.trim(),
                                    cat = catId ?: "", sub = subId ?: "", src = src,
                                    loan = loanId ?: "", goal = goalId ?: ""
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp)
            ) {
                Text(
                    tr(
                        when (kind) {
                            Kind.EXPENSE -> "save_expense"; Kind.INCOME -> "save_income"
                            Kind.LOAN -> "save_loan"; else -> "save_goal"
                        }
                    )
                )
            }
        }
    }
}
