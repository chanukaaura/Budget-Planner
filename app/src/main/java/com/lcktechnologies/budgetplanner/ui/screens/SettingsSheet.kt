package com.lcktechnologies.budgetplanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.lcktechnologies.budgetplanner.data.CURRENCIES
import androidx.compose.ui.platform.LocalContext
import com.lcktechnologies.budgetplanner.export.exportCsv
import com.lcktechnologies.budgetplanner.export.exportPdf
import com.lcktechnologies.budgetplanner.ui.BudgetViewModel
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.ChipFlow
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.SegTabs
import com.lcktechnologies.budgetplanner.ui.THEMES
import com.lcktechnologies.budgetplanner.ui.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(state: AppState, vm: BudgetViewModel, onDismiss: () -> Unit, onOpenAbout: () -> Unit) {
    val context = LocalContext.current
    val fmt = LocalFmt.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(tr("settings_title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            var currencyOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = currencyOpen, onExpandedChange = { currencyOpen = it },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                OutlinedTextField(
                    value = state.currency, onValueChange = {}, readOnly = true,
                    label = { Text(tr("currency")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                DropdownMenu(expanded = currencyOpen, onDismissRequest = { currencyOpen = false }) {
                    CURRENCIES.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = {
                            vm.mutate { it.copy(currency = c) }; currencyOpen = false
                        })
                    }
                }
            }

            SectionLabel(tr("app_language"))
            SegTabs(
                options = listOf("en" to tr("english"), "si" to "සිංහල", "ta" to "தமிழ்"),
                selected = state.appLang,
                onSelect = { vm.mutate { s -> s.copy(appLang = it) } }
            )

            SectionLabel(tr("appearance"))
            SegTabs(
                options = listOf("system" to tr("system"), "light" to tr("light"), "dark" to tr("dark")),
                selected = state.appearance,
                onSelect = { vm.mutate { s -> s.copy(appearance = it) } }
            )

            SectionLabel(tr("colour"))
            ChipFlow {
                THEMES.forEach { t ->
                    com.lcktechnologies.budgetplanner.ui.SelectChip(
                        label = t.label,
                        selected = t.id == state.theme,
                        dot = Color(t.accent),
                        onClick = { vm.mutate { s -> s.copy(theme = t.id) } }
                    )
                }
            }

            Text(
                tr("salary_dates"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
            )
            Row(Modifier.fillMaxWidth()) {
                com.lcktechnologies.budgetplanner.ui.NumField(
                    value = state.salaryDay.toDouble(),
                    onValue = { v -> vm.mutate { it.copy(salaryDay = v.toInt().coerceIn(0, 31)) } },
                    label = tr("salary_date"),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                com.lcktechnologies.budgetplanner.ui.NumField(
                    value = state.advanceDay.toDouble(),
                    onValue = { v -> vm.mutate { it.copy(advanceDay = v.toInt().coerceIn(0, 31)) } },
                    label = tr("advance_date"),
                    modifier = Modifier.weight(1f)
                )
            }
            MutedText(tr("hint_salary"), Modifier.padding(top = 4.dp))

            Text(
                tr("security_h"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = state.erasePassword,
                onValueChange = { v -> vm.mutate { it.copy(erasePassword = v.ifBlank { "8787" }) } },
                label = { Text(tr("erase_password_label")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            MutedText(tr("hint_erase"), Modifier.padding(top = 4.dp))

            Text(
                tr("export_h"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
            )
            MutedText(tr("hint_export"))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportCsv(context, state, fmt) }, modifier = Modifier.weight(1f)) {
                    Text(tr("export_csv"))
                }
                OutlinedButton(onClick = { exportPdf(context, state, fmt) }, modifier = Modifier.weight(1f)) {
                    Text(tr("export_pdf"))
                }
            }

            Text(
                tr("about_h"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
            )
            OutlinedButton(onClick = onOpenAbout) { Text(tr("about_btn")) }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp)) {
                Text(tr("done"))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}
