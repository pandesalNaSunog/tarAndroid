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
    suspend fun register(@Body request: RequestBody): RegistrationResponse

    @POST("tarapi/public/api/login")
    suspend fun login(@Body request: RequestBody): Token

    @GET("tarapi/public/api/profile")
    suspend fun profile(@Header("Authorization") token: String): ProfileWithBookings

    @POST("tarapi/public/api/mechanics")
    suspend fun getMechanics(@Header("Authorization") token: String, @Body request: RequestBody): MechanicDetails

    @POST("tarapi/public/api/book")
    suspend fun book(@Header("Authorization") token: String, @Body request: RequestBody): BookingId

    @POST("tarapi/public/api/check-booking-status")
    suspend fun checkBookingStatus(@Header("Authorization") token: String, @Body request: RequestBody): BookingStatus

    @POST("tarapi/public/api/cancel-booking")
    suspend fun cancelBooking(@Header("Authorization") token: String, @Body request: RequestBody): BookingStatus

    @POST("tarapi/public/api/shops")
    suspend fun getShops(@Header("Authorization") token: String, @Body request: RequestBody): MechanicDetails

    @GET("tarapi/public/api/has-booking")
    suspend fun hasBooking(@Header("Authorization") token: String): HasBooking

    @GET("tarapi/public/api/user-type")
    suspend fun getUserType(@Header("Authorization") token: String): UserType

    @GET("tarapi/public/api/mechanic-booking")
    suspend fun getMechanicBooking(@Header("Authorization") token: String): MechanicBooking

    @POST("tarapi/public/api/accept-booking")
    suspend fun acceptBooking(@Header("Authorization") token: String, @Body request: RequestBody): AcceptBookingCustomerId

    @POST("tarapi/public/api/deny-booking")
    suspend fun denyBooking(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/send-otp")
    suspend fun sendOTP(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/mechanic-location")
    suspend fun mechanicLocation(@Header("Authorization") token: String, @Body request: RequestBody): MechanicLocationResponse

    @POST("tarapi/public/api/conversation")
    suspend fun conversation(@Header("Authorization") token: String, @Body request: RequestBody):ConversationDetails

    @POST ("tarapi/public/api/send-message")
    suspend fun sendMessage(@Header("Authorization") token: String, @Body request: RequestBody): Conversation

    @POST("tarapi/public/api/mechanic-data")
    suspend fun mechanicData(@Header("Authorization") token: String, @Body request: RequestBody): MechanicData

    @GET("tarapi/public/api/has-accepted-booking")
    suspend fun hasAcceptedBooking(@Header("Authorization") token: String): CustomerId

    @POST("tarapi/public/api/fix")
    suspend fun fix(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/done")
    suspend fun done(@Header("Authorization") token: String, @Body request: RequestBody): DoneResponse

    @POST("tarapi/public/api/submit-violation")
    suspend fun submitViolation(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @GET("tarapi/public/api/shop-locations")
    suspend fun shopLocations(@Header("Authorization") token: String): ShopLocationArray

    @POST("tarapi/public/api/rate")
    suspend fun rate(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/mark-as-paid")
    suspend fun markAsPaid(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/update-name")
    suspend fun updateName(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/update-password")
    suspend fun updatePassword(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>

    @POST("tarapi/public/api/password-first")
    suspend fun passwordFirst(@Header("Authorization") token: String, @Body request: RequestBody): Response<ResponseBody>
}