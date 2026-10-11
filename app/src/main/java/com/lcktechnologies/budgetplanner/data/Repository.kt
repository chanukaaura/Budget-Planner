package com.lcktechnologies.budgetplanner.data

import android.content.Context
import com.google.gson.Gson
import java.time.LocalDate
import java.time.YearMonth

/** Saves the whole app state as one JSON string in SharedPreferences. */
class Repository(context: Context) {
    private val prefs = context.getSharedPreferences("budget_planner", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): AppState {
        val json = prefs.getString(KEY, null)
        if (json != null) {
            try {
                val s = gson.fromJson(json, AppState::class.java)
                if (s != null) return migrate(s)
            } catch (e: Exception) {
                // Corrupt data: fall through to a fresh start.
            }
        }
        return defaultState(LocalDate.now())
    }

    fun save(state: AppState) {
        prefs.edit().putString(KEY, gson.toJson(state)).apply()
    }

    private fun migrate(s: AppState): AppState {
        var out = s
        if (out.startMonth.isBlank()) out = out.copy(startMonth = YearMonth.now().toString())
        if (out.appLang != "en" && out.appLang != "si" && out.appLang != "ta") out = out.copy(appLang = "en")
        if (out.erasePassword.isBlank()) out = out.copy(erasePassword = "8787")
        return out
    }

    companion object {
        private const val KEY = "state"
    }
}
