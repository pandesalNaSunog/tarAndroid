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
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.lang.Error
import java.net.SocketTimeoutException
import java.util.jar.Manifest

class ShopOrMechanicHome : FragmentActivity(), OnMapReadyCallback {
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
    lateinit var economy: TextView
    var customerLat = 0.0
    var customerLong = 0.0
    lateinit var shopAddress: TextView
    lateinit var addressCard: CardView
    private val locationServiceRequestCode = 100
    private lateinit var map: GoogleMap
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
        addressCard = findViewById(R.id.addressCard)
        shopAddress = findViewById(R.id.shopAddress)

        economy = findViewById(R.id.textView7)

        addressCard.isVisible = false



        val editProfile = findViewById<Button>(R.id.editProfile)

        editProfile.setOnClickListener {
            val editProfileBottomSheet = BottomSheetDialog(this)
            val editProfileBottomSheetView = LayoutInflater.from(this).inflate(R.layout.update_profile_layout, null)
            editProfileBottomSheet.setContentView(editProfileBottomSheetView)
            editProfileBottomSheet.show()

            val sname = editProfileBottomSheetView.findViewById<TextView>(R.id.name)
            val updateName = editProfileBottomSheetView.findViewById<Button>(R.id.updateName)

            sname.text = name.text.toString()

            updateName.setOnClickListener {
                val updateNameAlert = AlertDialog.Builder(this)
                val updateNameAlertView = LayoutInflater.from(this).inflate(R.layout.update_name_form, null)
                updateNameAlert.setView(updateNameAlertView)
                val showUpdateNameAlert = updateNameAlert.show()

                val firstName = updateNameAlertView.findViewById<TextInputEditText>(R.id.firstName)
                val lastName = updateNameAlertView.findViewById<TextInputEditText>(R.id.lastName)
                val confirm = updateNameAlertView.findViewById<Button>(R.id.confirm)


                confirm.setOnClickListener {
                    if(firstName.text.toString().isEmpty()){
                        firstName.error = "Please fill out this field"
                    }else if(lastName.text.toString().isEmpty()){
                        lastName.error = "Please fill out this field"
                    }else{
                        progress.showProgress("Please Wait...")
                        val updateNameJson = JSONObject()
                        updateNameJson.put("first_name", firstName.text.toString())
                        updateNameJson.put("last_name", lastName.text.toString())
                        val request = updateNameJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

                        CoroutineScope(Dispatchers.IO).launch {
                            val updateNameResponse = try{ RetrofitInstance.retro.updateName("Bearer $token", request) }
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
                                if(updateNameResponse.isSuccessful){
                                    showUpdateNameAlert.dismiss()
                                    sname.text = "${firstName.text} ${lastName.text}"
                                    name.text = "${firstName.text} ${lastName.text}"
                                }
                            }
                        }
                    }
                }
            }
        }



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
        progress.showProgress("Getting Data...")
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
                if(mechanicData.mechanic.user_type == "owner"){
                    economy.text = "Repair Shop Economy"
                    addressCard.isVisible = true
                    shopAddress.text = "${mechanicData.mechanic.postal_code} ${mechanicData.mechanic.street_name}, ${mechanicData.mechanic.barangay}, ${mechanicData.mechanic.municipality}, Philippines"
                }else{
                    addressCard.isVisible = false
                    economy.text = "Mechanic Economy"
                }
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
                    Log.e("http exception", e.toString())
                    getMechanicBookings()

                    return@launch
                } catch (e: Exception) {
                    Log.e("exception", e.toString())
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
                    val showBookingAlert = bookingAlert.show()

                    val name = bookingALertView.findViewById<TextView>(R.id.name)
                    val service = bookingALertView.findViewById<TextView>(R.id.service)
                    val vehicleType = bookingALertView.findViewById<TextView>(R.id.vehicleType)
                    val accept = bookingALertView.findViewById<Button>(R.id.accept)
                    val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
                    mapFragment.getMapAsync(this@ShopOrMechanicHome)
                    val deny = bookingALertView.findViewById<Button>(R.id.deny)


                    Log.e("render", "render")
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
                                if(denyBooking.isSuccessful){
                                    AlertDialog.Builder(this@ShopOrMechanicHome)
                                        .setTitle("Message")
                                        .setMessage("Denied")
                                        .setCancelable(false)
                                        .setPositiveButton("OK"){_,_->
                                            showBookingAlert.dismiss()
                                        }
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
                    customerLat = mechanicBooking.lat.toDouble()
                    customerLong = mechanicBooking.long.toDouble()
                }
                delay(5000)
            }while(!hasBooking)
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.isTrafficEnabled = true

        val customerLocation = LatLng(customerLat, customerLong)
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION), locationServiceRequestCode)
        }else{
            val client = LocationServices.getFusedLocationProviderClient(this)

            val task = client.lastLocation
            task.addOnSuccessListener {
                long = it.longitude
                lat = it.latitude
            }
        }
        val myLocation = LatLng(lat, long)
        map.addMarker(MarkerOptions().position(customerLocation).title("Customer Location"))
        map.addMarker(MarkerOptions().position(myLocation).title("My Location"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(customerLocation, 10f))
    }
}