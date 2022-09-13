package com.example.tapandrepair

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val retro by lazy{
        Retrofit.Builder()
            .baseUrl("https://tapandrepair.online")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Requests::class.java)
    }
}