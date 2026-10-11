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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.lcktechnologies.budgetplanner.data.Goal
import com.lcktechnologies.budgetplanner.data.PALETTE
import com.lcktechnologies.budgetplanner.data.goalSaved
import com.lcktechnologies.budgetplanner.data.newId
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.ui.BudgetViewModel
import com.lcktechnologies.budgetplanner.ui.ConfirmDialog
import com.lcktechnologies.budgetplanner.ui.HeroCard
import com.lcktechnologies.budgetplanner.ui.LocalFmt
import com.lcktechnologies.budgetplanner.ui.MutedText
import com.lcktechnologies.budgetplanner.ui.NumField
import com.lcktechnologies.budgetplanner.ui.ScreenColumn
import com.lcktechnologies.budgetplanner.ui.SectionTitle
import com.lcktechnologies.budgetplanner.ui.tr

@Composable
fun GoalsScreen(state: AppState, vm: BudgetViewModel, onAddMoney: (goalId: String) -> Unit) {
    val fmt = LocalFmt.current
    val open = remember { mutableStateMapOf<String, Boolean>() }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val totalSaved = r2(state.goals.sumOf { state.goalSaved(it.id) })
    val totalTarget = state.goals.sumOf { it.target }

    ScreenColumn {
        HeroCard(
            label = tr("saved_toward_goals"),
            big = fmt.money(totalSaved),
            sub = if (totalTarget > 0) fmt.money(totalTarget) + " · " + state.goals.size
            else tr("set_target_hint"),
            negative = false,
            segments = state.goals.map { Pair(state.goalSaved(it.id), Color(it.color)) },
            base = maxOf(totalTarget, totalSaved, 1.0).takeIf { totalTarget > 0 } ?: 0.0
        )

        SectionTitle(tr("your_goals_h"))

        if (state.goals.isEmpty()) {
            MutedText(tr("no_goals_yet"), Modifier.padding(top = 8.dp))
            MutedText(tr("goals_empty_hint"), Modifier.padding(top = 4.dp))
        }

        state.goals.forEach { g ->
            GoalCard(
                state = state, goal = g,
                expanded = open[g.id] == true,
                onToggle = { open[g.id] = !(open[g.id] ?: false) },
                vm = vm,
                onAddMoney = { onAddMoney(g.id) },
                onDelete = { pendingDelete = g.id }
            )
        }

        OutlinedButton(
            onClick = {
                val id = newId()
                vm.mutate { s ->
                    s.copy(goals = s.goals + Goal(id = id, name = "", target = 0.0, color = PALETTE[s.goals.size % PALETTE.size]))
                }
                open[id] = true
            },
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        ) { Text(tr("add_goal")) }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            message = tr("confirm_delete"),
            confirmLabel = tr("delete"),
            onConfirm = {
                vm.mutate { s -> s.copy(goals = s.goals.filter { it.id != id }) }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun GoalCard(
    state: AppState,
    goal: Goal,
    expanded: Boolean,
    onToggle: () -> Unit,
    vm: BudgetViewModel,
    onAddMoney: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = LocalFmt.current
    val saved = state.goalSaved(goal.id)
    val target = goal.target
    val color = Color(goal.color)

    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(10.dp))
            Text(
                goal.name.ifEmpty { tr("new_goal") },
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                fmt.money(saved) + if (target > 0) " / " + fmt.money(target) else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            val progress = if (target > 0) (saved / target).toFloat() else if (saved > 0) 1f else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            MutedText(
                when {
                    target <= 0 -> tr("no_target_yet")
                    saved >= target -> tr("goal_reached")
                    else -> fmt.money(r2(target - saved)) + " " + tr("left")
                },
                Modifier.padding(top = 6.dp, bottom = 10.dp)
            )
            OutlinedTextField(
                value = goal.name,
                onValueChange = { v -> vm.updateGoal(goal.id) { it.copy(name = v) } },
                label = { Text(tr("name")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            NumField(
                value = target,
                onValue = { v -> vm.updateGoal(goal.id) { it.copy(target = v) } },
                label = tr("target_amount"),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddMoney, modifier = Modifier.weight(1f)) { Text(tr("add_money_to_goal")) }
                TextButton(onClick = onDelete) { Text(tr("delete"), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.padding(bottom = 6.dp))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
