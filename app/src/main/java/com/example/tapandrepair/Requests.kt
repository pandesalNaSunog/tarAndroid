package com.example.tapandrepair

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface Requests {
    @POST("tarapi/public/api/register")
    suspend fun register(@Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/login")
    suspend fun login(@Body request: RequestBody): Token

    @GET("tarapi/public/api/profile")
    suspend fun profile(@Header("Authorization") token: String): ProfileWithBookings

    @GET("tarapi/public/api/mechanics")
    suspend fun getMechanics(@Header("Authorization") token: String): MechanicDetails

    @POST("tarapi/public/api/book")
    suspend fun book(@Header("Authorization") token: String, @Body request: RequestBody): BookingId

    @POST("tarapi/public/api/check-booking-status")
    suspend fun checkBookingStatus(@Header("Authorization") token: String, @Body request: RequestBody): BookingStatus

    @POST("tarapi/public/api/cancel-booking")
    suspend fun cancelBooking(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>
}