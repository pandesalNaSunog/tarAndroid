package com.example.tapandrepair

data class ActivityLogItem(
    val id: Int,
    val user_id: Int,
    val activity: String,
    val created_at: String,
    val updated_at: String
)