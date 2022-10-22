package com.example.tapandrepair

data class DoneResponse(
    val booking_id: Int,
    val customer_name: String,
    val service: String,
    val vehicle_type: String
)
