package com.lcktechnologies.budgetplanner.data

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

val PALETTE: List<Long> = listOf(
    0xFF2F6BFFL, 0xFFE8833AL, 0xFF1FA37AL, 0xFFB45AE0L, 0xFFE0475BL,
    0xFFC9A227L, 0xFF00A5C9L, 0xFF7A8B2EL, 0xFFD6539BL
)
const val LOAN_COLOR = 0xFF64748BL
const val KOKO_COLOR = 0xFF7C5CFFL
const val NONE_COLOR = 0xFF8A958FL
const val POS_COLOR = 0xFF0E7A4FL

val CURRENCIES = listOf(
    "LKR", "USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD", "CHF", "CNY",
    "BRL", "MXN", "ZAR", "NGN", "KES", "PKR", "BDT", "TRY", "SEK", "PLN", "AED"
)
val INCOME_SOURCES = listOf("Salary", "Business", "Freelance", "Bonus", "Gift", "Loan received", "Other")
val PAY_METHODS = listOf("Cash", "Bank", "Card", "Online")

fun newId(): String = UUID.randomUUID().toString().substring(0, 8)

fun defaultState(today: LocalDate = LocalDate.now()): AppState {
    val noSubs = emptyList<String>()
    val cats = mutableListOf<Category>()

    listOf("Housing", "Groceries", "Transport", "Eating out", "Fun", "Health", "Other", "Savings")
        .forEachIndexed { i, n ->
            cats.add(Category(id = newId(), name = n, color = PALETTE[i % PALETTE.size]))
        }

    cats.add(
        Category(
            id = newId(),
            name = "Umandi",
            color = PALETTE[cats.size % PALETTE.size],
            subs = listOf("Class fee", "School", "Transport fare", "Sweets").map { SubCategory(newId(), it) }
        )
    )

    val recurring = listOf(
        "Rent" to noSubs,
        "Electricity" to noSubs,
        "Water" to noSubs,
        "Internet" to noSubs,
        "Phone" to listOf("Mother", "Wife", "Me")
    )
    for ((name, subs) in recurring) {
        cats.add(
            Category(
                id = newId(),
                name = name,
                color = PALETTE[cats.size % PALETTE.size],
                recurring = true,
                subs = subs.map { SubCategory(newId(), it) }
            )
        )
    }

    val goals = listOf("Emergency fund", "Travel", "Education", "Vehicle", "A house", "Other")
        .mapIndexed { i, n -> Goal(id = newId(), name = n, target = 0.0, color = PALETTE[(i + 3) % PALETTE.size]) }

    return AppState(
        startMonth = YearMonth.from(today).toString(),
        cats = cats,
        goals = goals
    )
}
