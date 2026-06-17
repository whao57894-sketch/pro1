package com.example.myapplication.models

data class Debt(
    val id: Long? = null,
    val phone: String,
    val type: String, // 借入/借出
    val counterparty: String, // 对方姓名
    val amount: Double,
    val repaidAmount: Double = 0.0,
    val debtDate: String,
    val dueDate: String? = null,
    val status: String = "UNPAID", // UNPAID, PARTIAL, PAID
    val remark: String? = null
) {
    val remainingAmount: Double
        get() = amount - repaidAmount

    val isPaid: Boolean
        get() = repaidAmount >= amount
}
