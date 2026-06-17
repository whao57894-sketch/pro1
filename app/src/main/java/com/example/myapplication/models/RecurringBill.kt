package com.example.myapplication.models

data class RecurringBill(
    val id: Long? = null,
    val phone: String,
    val type: String, // 收入/支出
    val category: String,
    val amount: Double,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: String,
    val endDate: String? = null,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val isActive: Boolean = true,
    val remark: String? = null
)
