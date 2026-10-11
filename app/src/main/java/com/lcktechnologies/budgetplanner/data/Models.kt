package com.lcktechnologies.budgetplanner.data

/** Transaction kinds. Stored as strings so the saved JSON stays readable. */
object Kind {
    const val EXPENSE = "expense"
    const val INCOME = "income"
    const val LOAN = "loan"
    const val GOAL = "goal"
}

// Every constructor parameter has a default value. That gives Kotlin a no-argument
// constructor, which Gson needs so that fields missing from older saved data
// simply fall back to these defaults.

data class SubCategory(
    val id: String = "",
    val name: String = ""
)

data class Category(
    val id: String = "",
    val name: String = "",
    val budget: Double = 0.0,
    val color: Long = 0xFF2F6BFFL,
    val recurring: Boolean = false,
    val subs: List<SubCategory> = emptyList()
)

/** A loan, or a KOKO installment purchase when [koko] is true. */
data class Loan(
    val id: String = "",
    val name: String = "",
    val koko: Boolean = false,
    val principal: Double = 0.0,
    val monthly: Double = 0.0,
    val count: Int = 6
)

data class Goal(
    val id: String = "",
    val name: String = "",
    val target: Double = 0.0,
    val color: Long = 0xFF1FA37AL
)

/** One saved entry. Which of cat/sub/src/loan/goal is used depends on [kind]. */
data class Tx(
    val id: String = "",
    val ts: Long = 0L,
    val date: String = "",          // yyyy-MM-dd
    val kind: String = Kind.EXPENSE,
    val amount: Double = 0.0,
    val method: String = "Cash",
    val note: String = "",
    val cat: String = "",
    val sub: String = "",
    val src: String = "",
    val loan: String = "",
    val goal: String = ""
)

data class AppState(
    val currency: String = "LKR",
    val income: Double = 0.0,
    val startMonth: String = "",
    val appLang: String = "en",          // en | si | ta
    val appearance: String = "system",   // system | light | dark
    val theme: String = "green",
    val salaryDay: Int = 0,
    val advanceDay: Int = 0,
    val erasePassword: String = "8787",
    val cats: List<Category> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val tx: List<Tx> = emptyList()
)
