package com.example.tapandrepair

data class ProfileWithBookings(
    val transaction_history: List<CustomerTransactionHistoryItem>,
    val user: User
)