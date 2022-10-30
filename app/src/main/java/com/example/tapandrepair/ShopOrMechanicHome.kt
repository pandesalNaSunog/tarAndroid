package com.example.tapandrepair

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.lang.Error
import java.net.SocketTimeoutException
import java.util.jar.Manifest

class ShopOrMechanicHome : AppCompatActivity() {
    private val db = TokenDB(this)
    lateinit var token: String
    val alerts = Alerts(this)
    lateinit var name: TextView
    lateinit var acceptance: TextView
    lateinit var rating: TextView
    lateinit var cancellation: TextView
    val progress = Progress(this)
    var long = 0.0
    var lat = 0.0
    private val locationServiceRequestCode = 100
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_or_mechanic_home)
        val db = TokenDB(this)

        val logout = findViewById<Button>(R.id.logout)

        name = findViewById(R.id.name)
        acceptance = findViewById(R.id.acceptance)
        rating = findViewById(R.id.rating)
        cancellation = findViewById(R.id.cancellation)
        token = db.getToken()



        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), locationServiceRequestCode)
        }else{
            val client = LocationServices.getFusedLocationProviderClient(this)

            val task = client.lastLocation
            task.addOnSuccessListener {
                long = it.longitude
                lat = it.latitude
                getMechanicData(long, lat)
            }

        }


        logout.setOnClickListener {
            db.delete()
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == locationServiceRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            val client = LocationServices.getFusedLocationProviderClient(this)

            val task = client.lastLocation
            task.addOnSuccessListener {
                long = it.longitude
                lat = it.latitude
                getMechanicData(long, lat)
            }
        }
    }
    private fun hasAcceptedBooking(){
        CoroutineScope(Dispatchers.IO).launch {
            val hasAcceptedBookingResponse = try{ RetrofitInstance.retro.hasAcceptedBooking("Bearer $token") }
            catch(e: Exception){
                withContext(Dispatchers.Main){
                    hasAcceptedBooking()
                }
                return@launch
            }

            withContext(Dispatchers.Main){
                if(hasAcceptedBookingResponse.has_booking) {
                    val intent = Intent(this@ShopOrMechanicHome, CustomerMechanicMeetUp::class.java)
                    intent.putExtra("customer_id", hasAcceptedBookingResponse.customer_id)
                    intent.putExtra("booking_id", hasAcceptedBookingResponse.booking_id)
                    startActivity(intent)
                    finishAffinity()
                }else{
                    getMechanicBookings()
                }

            }
        }
    }
    private fun getMechanicData(long: Double, lat: Double){
        progress.showProgress("Loading...")
        val jsonObject = JSONObject()
        jsonObject.put("lat", lat.toString())
        jsonObject.put("long", long.toString())
        val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        CoroutineScope(Dispatchers.IO).launch {
            val mechanicData = try{ RetrofitInstance.retro.mechanicData("Bearer $token", request) }
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
                name.text = "${mechanicData.mechanic.first_name} ${mechanicData.mechanic.last_name}"
                acceptance.text = "${mechanicData.acceptance}%"
                rating.text = mechanicData.rating.toString()
                cancellation.text = "${mechanicData.cancellation}%"
                hasAcceptedBooking()
            }
        }
    }


    private fun getMechanicBookings() {
        var hasBooking: Boolean

        CoroutineScope(Dispatchers.IO).launch {
            do {
                val mechanicBooking = try {
                    RetrofitInstance.retro.getMechanicBooking("Bearer $token")
                } catch (e: HttpException) {
                    getMechanicBookings()
                    return@launch
                } catch (e: Exception) {
                    getMechanicBookings()
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    hasBooking = true

                    val bookingAlert = AlertDialog.Builder(this@ShopOrMechanicHome)
                    val bookingALertView = LayoutInflater.from(this@ShopOrMechanicHome)
                        .inflate(R.layout.mechanic_booking, null)

                    bookingAlert.setView(bookingALertView)
                    bookingAlert.setCancelable(false)
                    bookingAlert.show()

                    val name = bookingALertView.findViewById<TextView>(R.id.name)
                    val service = bookingALertView.findViewById<TextView>(R.id.service)
                    val vehicleType = bookingALertView.findViewById<TextView>(R.id.vehicleType)
                    val accept = bookingALertView.findViewById<Button>(R.id.accept)
                    val deny = bookingALertView.findViewById<Button>(R.id.deny)

                    deny.setOnClickListener {
                        progress.showProgress("Please Wait...")
                        val jsonObject = JSONObject()
                        jsonObject.put("booking_id", mechanicBooking.booking_id)
                        val request = jsonObject.toString()
                            .toRequestBody("application/json".toMediaTypeOrNull())
                        CoroutineScope(Dispatchers.IO).launch {
                            val denyBooking = try {
                                RetrofitInstance.retro.denyBooking("Bearer $token", request)
                            } catch (e: SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    alerts.socketTimeOut()

                                }
                                return@launch
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    alerts.error(e.toString())
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                if (denyBooking.isSuccessful) {
                                    AlertDialog.Builder(this@ShopOrMechanicHome)
                                        .setTitle("Message")
                                        .setMessage("Denied")
                                        .show()
                                    hasBooking = false
                                } else {
                                    AlertDialog.Builder(this@ShopOrMechanicHome)
                                        .setTitle("Error")
                                        .setMessage(denyBooking.errorBody()!!.string())
                                        .show()
                                    hasBooking = false
                                }
                            }
                        }
                    }


                    accept.setOnClickListener {
                        progress.showProgress("Please Wait...")
                        val jsonObject = JSONObject()
                        jsonObject.put("booking_id", mechanicBooking.booking_id)
                        val request = jsonObject.toString()
                            .toRequestBody("application/json".toMediaTypeOrNull())
                        CoroutineScope(Dispatchers.IO).launch {
                            val acceptBooking = try {
                                RetrofitInstance.retro.acceptBooking("Bearer $token", request)
                            } catch (e: SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    alerts.socketTimeOut()

                                }
                                return@launch
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    alerts.error(e.toString())
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                val intent = Intent(this@ShopOrMechanicHome, CustomerMechanicMeetUp::class.java)
                                intent.putExtra("customer_id", acceptBooking.customer_id)
                                intent.putExtra("booking_id", mechanicBooking.booking_id)
                                startActivity(intent)

                            }
                        }
                    }
                    name.text =
                        "${mechanicBooking.customer.first_name} ${mechanicBooking.customer.last_name}"
                    service.text = mechanicBooking.service
                    vehicleType.text = mechanicBooking.vehicle_type
                }
                delay(5000)
            }while(!hasBooking)
        }
    }
}