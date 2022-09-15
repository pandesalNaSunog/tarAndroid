package com.example.tapandrepair

data class ProfileWithBookings(
    val bookings: List<Booking>,
    val user: User
)