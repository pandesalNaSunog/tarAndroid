package com.example.tapandrepair

data class MechanicLocationResponse(
    val me: Me,
    val mechanic: MechanicX,
    val travel: Travel,
    val booking_status: String
)