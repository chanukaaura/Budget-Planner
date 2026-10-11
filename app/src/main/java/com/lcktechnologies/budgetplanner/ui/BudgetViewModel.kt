package com.lcktechnologies.budgetplanner.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.Category
import com.lcktechnologies.budgetplanner.data.Goal
import com.lcktechnologies.budgetplanner.data.Loan
import com.lcktechnologies.budgetplanner.data.Repository
import com.lcktechnologies.budgetplanner.data.Tx
import com.lcktechnologies.budgetplanner.data.defaultState

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    /** The single source of truth. Compose re-draws whenever this changes. */
    var state by mutableStateOf(repo.load())
        private set

    fun mutate(block: (AppState) -> AppState) {
        state = block(state)
        repo.save(state)
    }

    fun addTx(t: Tx) = mutate { it.copy(tx = it.tx + t) }

    fun deleteTx(id: String) = mutate { s -> s.copy(tx = s.tx.filter { it.id != id }) }

    fun updateCat(id: String, f: (Category) -> Category) =
        mutate { s -> s.copy(cats = s.cats.map { if (it.id == id) f(it) else it }) }

    fun updateLoan(id: String, f: (Loan) -> Loan) =
        mutate { s -> s.copy(loans = s.loans.map { if (it.id == id) f(it) else it }) }

    fun updateGoal(id: String, f: (Goal) -> Goal) =
        mutate { s -> s.copy(goals = s.goals.map { if (it.id == id) f(it) else it }) }

    /** Erase everything and start again with the default categories and goals. */
    fun reset() {
        state = defaultState()
        repo.save(state)
    }
}
