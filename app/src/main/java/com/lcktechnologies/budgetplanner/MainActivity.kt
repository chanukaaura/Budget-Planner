package com.lcktechnologies.budgetplanner

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.lcktechnologies.budgetplanner.data.Kind
import com.lcktechnologies.budgetplanner.data.Tx
import com.lcktechnologies.budgetplanner.ui.BudgetTheme
import com.lcktechnologies.budgetplanner.ui.BudgetViewModel
import com.lcktechnologies.budgetplanner.ui.Fmt
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.LocalLang
import com.lcktechnologies.budgetplanner.ui.THEMES
import com.lcktechnologies.budgetplanner.ui.screens.AboutScreen
import com.lcktechnologies.budgetplanner.ui.screens.AddEntrySheet
import com.lcktechnologies.budgetplanner.ui.screens.DashboardScreen
import com.lcktechnologies.budgetplanner.ui.screens.GoalsScreen
import com.lcktechnologies.budgetplanner.ui.screens.HistoryScreen
import com.lcktechnologies.budgetplanner.ui.screens.PlanScreen
import com.lcktechnologies.budgetplanner.ui.screens.ReportsScreen
import com.lcktechnologies.budgetplanner.ui.screens.SettingsSheet
import com.lcktechnologies.budgetplanner.ui.tr
import java.time.LocalDate
import java.time.YearMonth

enum class Tab { Dashboard, Goals, Reports, History, Plan }

class MainActivity : ComponentActivity() {
    private val vm: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(vm) }
    }
}

@Composable
private fun App(vm: BudgetViewModel) {
    val state = vm.state
    val systemDark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val dark = when (state.appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val accent = THEMES.firstOrNull { it.id == state.theme } ?: THEMES.first()
    val fmt = remember(state.appLang, state.currency) { Fmt(state.appLang, state.currency) }

    BudgetTheme(dark = dark, accent = accent) {
        CompositionLocalProvider(LocalFmt provides fmt, LocalLang provides state.appLang) {
            Surface(Modifier.fillMaxSize()) {
                MainScaffold(vm)
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: BudgetViewModel) {
    val state = vm.state
    val context = androidx.compose.ui.platform.LocalContext.current
    val fmt = LocalFmt.current

    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var sheetKind by remember { mutableStateOf<String?>(null) }
    var sheetId by remember { mutableStateOf<String?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }

    fun openEntry(kind: String, id: String?) {
        sheetKind = kind; sheetId = id
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavItem(tab == Tab.Dashboard, { tab = Tab.Dashboard }, Icons.Filled.Home, tr("tab_dashboard"))
                NavItem(tab == Tab.Goals, { tab = Tab.Goals }, Icons.Outlined.EmojiEvents, tr("tab_goals"))
                NavigationBarItem(
                    selected = false,
                    onClick = { openEntry(Kind.EXPENSE, null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = tr("tab_add")) },
                    label = { Text(tr("tab_add")) }
                )
                NavItem(tab == Tab.Reports, { tab = Tab.Reports }, Icons.Filled.BarChart, tr("tab_reports"))
                NavItem(tab == Tab.History, { tab = Tab.History }, Icons.Filled.History, tr("tab_history"))
                NavItem(tab == Tab.Plan, { tab = Tab.Plan }, Icons.Filled.Tune, tr("tab_plan"))
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    Tab.Dashboard -> DashboardScreen(
                        state = state, month = month, onMonth = { month = it },
                        onAddEntry = { k, id -> openEntry(k, id) },
                        onOpenTab = { tab = it }
                    )
                    Tab.Goals -> GoalsScreen(state = state, vm = vm, onAddMoney = { openEntry(Kind.GOAL, it) })
                    Tab.Reports -> ReportsScreen(state = state)
                    Tab.History -> HistoryScreen(
                        state = state, month = month, onMonth = { month = it },
                        onDelete = { vm.deleteTx(it) },
                        onAdd = { openEntry(Kind.EXPENSE, null) }
                    )
                    Tab.Plan -> PlanScreen(state = state, vm = vm, onOpenSettings = { settingsOpen = true })
                }
            }
        }
    }

    sheetKind?.let { k ->
        AddEntrySheet(
            state = state, month = month, initialKind = k, initialId = sheetId,
            onDismiss = { sheetKind = null },
            onSave = { tx: Tx ->
                vm.addTx(tx)
                month = tx.date.substring(0, 7)
                sheetKind = null
            }
        )
    }

    if (settingsOpen) {
        SettingsSheet(
            state = state, vm = vm,
            onDismiss = { settingsOpen = false },
            onOpenAbout = { aboutOpen = true }
        )
    }

    if (aboutOpen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { aboutOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AboutScreen(lastUpdated = fmt.longDate(LocalDate.of(2026, 9, 27)), onBack = { aboutOpen = false })
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    NavigationBarItem(
        selected = selected, onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}
