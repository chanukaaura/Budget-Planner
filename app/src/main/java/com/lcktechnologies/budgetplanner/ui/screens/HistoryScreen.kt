package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.Kind
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.data.txInfo
import com.lcktechnologies.budgetplanner.ui.ChipFlow
import com.lcktechnologies.budgetplanner.ui.ConfirmDialog
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.LocalPositive
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.PeriodNav
import com.lcktechnologies.budgetplanner.ui.SelectChip
import com.lcktechnologies.budgetplanner.ui.tr
import com.lcktechnologies.budgetplanner.ui.trf
import java.time.YearMonth

@Composable
fun HistoryScreen(
    state: AppState,
    month: String,
    onMonth: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    val fmt = LocalFmt.current
    val pos = LocalPositive.current
    var filter by rememberSaveable { mutableStateOf("all") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val rows = state.tx
        .filter { it.date.startsWith(month) && (filter == "all" || it.kind == filter) }
        .sortedWith(compareByDescending<com.lcktechnologies.budgetplanner.data.Tx> { it.date }.thenByDescending { it.ts })
    val grouped = rows.groupBy { it.date }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
        item {
            PeriodNav(
                title = fmt.monthLabel(month),
                onPrev = { onMonth(YearMonth.parse(month).minusMonths(1).toString()) },
                onNext = { onMonth(YearMonth.parse(month).plusMonths(1).toString()) }
            )
            Box(Modifier.padding(vertical = 6.dp)) {
                ChipFlow {
                    listOf(
                        "all" to tr("filter_all"),
                        Kind.EXPENSE to tr("tab_expenses"),
                        Kind.INCOME to tr("tab_income"),
                        Kind.LOAN to tr("filter_loans"),
                        Kind.GOAL to tr("filter_goals")
                    ).forEach { (id, label) -> SelectChip(label, filter == id) { filter = id } }
                }
            }
        }

        if (rows.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MutedText(tr("nothing_here_yet"))
                    OutlinedButton(onClick = onAdd, modifier = Modifier.padding(top = 12.dp)) { Text(tr("add_an_entry")) }
                }
            }
        } else {
            val inSum = r2(rows.filter { it.kind == Kind.INCOME }.sumOf { it.amount })
            val outSum = r2(rows.filter { it.kind != Kind.INCOME }.sumOf { it.amount })
            item {
                val parts = ArrayList<String>()
                if (inSum > 0) parts.add(trf("in_amt", fmt.money(inSum)))
                if (outSum > 0) parts.add(trf("out_amt", fmt.money(outSum)))
                MutedText(parts.joinToString(" · "), Modifier.padding(vertical = 6.dp))
            }
            grouped.forEach { (date, list) ->
                item(key = "h$date") {
                    Text(
                        fmt.dayLabel(date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(list, key = { it.id }) { t ->
                    val info = state.txInfo(t)
                    val uncategorized = tr("cat_uncategorized")
                    val label = info.label.ifEmpty { if (t.kind == Kind.EXPENSE) uncategorized else "" }
                    val typeText = tr(info.typeKey)
                    val title = t.note.ifEmpty { label.ifEmpty { typeText } }
                    val subParts = ArrayList<String>()
                    if (t.note.isNotEmpty() && label.isNotEmpty()) subParts.add(label)
                    if (t.kind != Kind.EXPENSE && t.kind != Kind.INCOME) subParts.add(typeText)
                    if (t.method.isNotEmpty()) subParts.add(tr("pm_" + t.method))

                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(info.color)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Medium)
                            if (subParts.isNotEmpty()) {
                                Text(
                                    subParts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            (if (t.kind == Kind.INCOME) "+" else "") + fmt.money(t.amount),
                            fontWeight = FontWeight.SemiBold,
                            color = if (t.kind == Kind.INCOME) pos else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { pendingDelete = t.id }) {
                            Icon(Icons.Filled.Close, contentDescription = tr("delete"))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 24.dp)) }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            message = tr("confirm_delete"),
            confirmLabel = tr("delete"),
            onConfirm = { onDelete(id); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }
}
