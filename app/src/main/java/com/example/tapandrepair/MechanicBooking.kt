package com.example.tapandrepair

data class MechanicBooking(
    val booking_id: Int,
    val customer: Customer,
    val lat: String,
    val long: String,
    val service: String,
    val vehicle_type: String
)