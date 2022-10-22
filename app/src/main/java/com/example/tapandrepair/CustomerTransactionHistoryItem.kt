package com.example.tapandrepair

data class CustomerTransactionHistoryItem(
    val amount_charged: String,
    val date: String,
    val id: Int,
    val mechanic: String,
    val service: String,
    val status: String,
    val vehicle_type: String
)