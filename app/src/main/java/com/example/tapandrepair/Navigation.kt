package com.example.tapandrepair

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException

class Navigation : AppCompatActivity() {
    private lateinit var uintent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val navigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val controller = findNavController(R.id.fragmentContainerView)
        navigation.setupWithNavController(controller)

        val db = TokenDB(this)
        val token = db.getToken()
        checkHasBooking(token)
    }
    private fun checkHasBooking(token: String){
        val progress = Progress(this)
        val alerts = Alerts(this)
        CoroutineScope(Dispatchers.IO).launch {
            val hasBooking = try{RetrofitInstance.retro.hasBooking("Bearer $token")}
            catch(e: Exception){
                withContext(Dispatchers.Main){
                    AlertDialog.Builder(this@Navigation)
                        .setTitle("Error")
                        .setMessage("No Internet Connection. Please Try Again")
                        .setCancelable(false)
                        .setPositiveButton("OK"){_,_->
                            checkHasBooking(token)
                        }
                        .show()
                }
                return@launch
            }

            withContext(Dispatchers.Main){
                if(hasBooking.message == "has booking"){
                    var hasAccepted = false
                    val bookingSuccessAlert = android.app.AlertDialog.Builder(this@Navigation)
                    val bookingSuccessAlertView = LayoutInflater.from(this@Navigation).inflate(R.layout.waiting_to_accept_booking, null)
                    bookingSuccessAlert.setCancelable(false)
                    bookingSuccessAlert.setView(bookingSuccessAlertView)
                    val showBookingSuccessAlert = bookingSuccessAlert.show()

                    val cancelBooking = bookingSuccessAlertView.findViewById<Button>(R.id.cancel)

                    cancelBooking.setOnClickListener {
                        Log.e("booking id", hasBooking.id.toString())
                        android.app.AlertDialog.Builder(this@Navigation)
                            .setTitle("Cancel")
                            .setMessage("Cancel Booking?")
                            .setPositiveButton("YES"){_,_->
                                progress.showProgress("Please Wait...")
                                val cancelBookingJson = JSONObject()

                                cancelBookingJson.put("booking_id", hasBooking.id)

                                val cancelBookingRequest = cancelBookingJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                CoroutineScope(Dispatchers.IO).launch {
                                    val cancelBookingResponse = try{ RetrofitInstance.retro.cancelBooking("Bearer $token", cancelBookingRequest) }
                                    catch(e: SocketTimeoutException){
                                        withContext(Dispatchers.Main){
                                            progress.dismiss()
                                            alerts.socketTimeOut()
                                        }
                                        return@launch
                                    }catch(e: Exception){
                                        withContext(Dispatchers.Main){
                                            progress.dismiss()
                                            alerts.error(e.toString())
                                        }
                                        return@launch
                                    }

                                    withContext(Dispatchers.Main){
                                        progress.dismiss()
                                        if(cancelBookingResponse.status == "cancelled by the customer"){
                                            showBookingSuccessAlert.dismiss()
                                        }
                                    }
                                }
                            }.setNegativeButton("NO", null)
                            .show()
                    }

                    val thisJsonObject = JSONObject()
                    thisJsonObject.put("booking_id", hasBooking.id)
                    val thisRequest = thisJsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

                    CoroutineScope(Dispatchers.IO).launch {
                        while(!hasAccepted){
                            val statusResponse = try{ RetrofitInstance.retro.checkBookingStatus("Bearer $token", thisRequest) }
                            catch(e: Exception){
                                Log.e("MechanicsAdapter", e.toString())
                                return@launch
                            }

                            withContext(Dispatchers.Main){
                                Log.e("MechanicsAdapter", statusResponse.status)
                                if(statusResponse.status == "accepted"){
                                    hasAccepted = true
                                    showBookingSuccessAlert.dismiss()
                                    android.app.AlertDialog.Builder(this@Navigation)
                                        .setTitle("Success")
                                        .setMessage("Your booking has been accepted by the mechanic/shop")
                                        .setCancelable(false)
                                        .setPositiveButton("OK"){_,_->
                                            Log.e("Shop Mechanic Id", statusResponse.shop_mechanic_id.toString())
                                            val intent = Intent(this@Navigation, MechanicArrival::class.java)
                                            intent.putExtra("shop_mechanic_id", statusResponse.shop_mechanic_id)
                                            intent.putExtra("name", statusResponse.shop_mechanic_name)
                                            ContextCompat.startActivity(this@Navigation, intent, null)
                                        }
                                        .show()

                                }else if(statusResponse.status == "denied"){
                                    hasAccepted = true
                                    showBookingSuccessAlert.dismiss()
                                    android.app.AlertDialog.Builder(this@Navigation)
                                        .setTitle("Message")
                                        .setMessage("Your booking has been rejected by the mechanic/shop")
                                        .setCancelable(false)
                                        .setPositiveButton("OK", null)
                                        .show()
                                }else{
                                    hasAccepted = false
                                }
                            }
                            delay(5000)
                        }
                    }
                }
            }
        }
    }
}