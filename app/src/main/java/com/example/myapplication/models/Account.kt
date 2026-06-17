package com.example.myapplication.models

data class Account(
    val id: Long? = null,
    val phone: String,
    val accountName: String,
    val accountType: String, // 现金、银行卡、支付宝、微信等
    val balance: Double = 0.0,
    val icon: String? = null,
    val color: String? = null
)
