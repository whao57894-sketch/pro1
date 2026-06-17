package com.example.myapplication.models

data class Budget(
    val id: Long? = null,
    val phone: String,
    val category: String,
    val amount: Double,
    val month: String,
    val currentSpent: Double = 0.0
) {
    val percentage: Float
        get() = if (amount > 0) ((currentSpent / amount) * 100).toFloat() else 0f

    val isOverBudget: Boolean
        get() = currentSpent > amount

    val remaining: Double
        get() = amount - currentSpent
}
