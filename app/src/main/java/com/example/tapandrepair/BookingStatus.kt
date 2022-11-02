package com.example.tapandrepair

data class BookingStatus(
    val status: String,
    val shop_mechanic_id: Int,
    val shop_mechanic_name: String,
    val booking_id: Int
)
