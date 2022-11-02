package com.example.tapandrepair

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException

class MechanicArrival : FragmentActivity(), OnMapReadyCallback{
    private lateinit var map: GoogleMap
    private var mechanicMarker: Marker? = null
    private var myLocationMarker: Marker? = null
    private var shopMechanicId = 0
    private var shopMechanicName = ""
    private lateinit var jsonObject: JSONObject
    private lateinit var request: RequestBody
    private var zoom = false
    private var token = ""
    private lateinit var time: TextView
    private lateinit var distance: TextView
    private var rating = 1
    private var bookingId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mechanic_arrival)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        mapFragment.getMapAsync(this)
        shopMechanicId = intent.getIntExtra("shop_mechanic_id", 0)
        shopMechanicName = intent.getStringExtra("name").toString()
        bookingId = intent.getIntExtra("booking_id", 0)
        Log.e("booking id", bookingId.toString())
        val mechanicName = findViewById<TextView>(R.id.mechanicName)
        val chat = findViewById<Button>(R.id.chat)
        val progress = Progress(this)
        val alerts = Alerts(this)
        val db = TokenDB(this)
        val report = findViewById<LinearLayout>(R.id.report)
        val cancel = findViewById<Button>(R.id.cancel)
        time = findViewById<TextView>(R.id.time)
        distance = findViewById<TextView>(R.id.distance)
        token = db.getToken()


        cancel.setOnClickListener {
            val confirmCancelAlert = AlertDialog.Builder(this)
                .setTitle("Cancel")
                .setMessage("Cancel Booking?")
                .setPositiveButton("YES"){_,_->
                    progress.showProgress("Please Wait...")
                    val cancelBookingJson = JSONObject()
                    cancelBookingJson.put("booking_id", bookingId)
                    val cancelBookingRequest = cancelBookingJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    CoroutineScope(Dispatchers.IO).launch {
                        val cancelResponse = try{ RetrofitInstance.retro.cancelBooking("Bearer $token", cancelBookingRequest) }
                        catch(e: SocketTimeoutException){
                            withContext(Dispatchers.Main){
                                progress.dismiss()
                                alerts.socketTimeOut()
                            }
                            return@launch
                        }catch(e: Exception){
                            withContext(Dispatchers.Main){
                                progress.dismiss()
                                alerts.socketTimeOut()
                            }
                            return@launch
                        }

                        withContext(Dispatchers.Main){
                            progress.dismiss()
                            if(cancelResponse.status == "cancelled by the customer"){
                                val intent = Intent(this@MechanicArrival, Navigation::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                        }
                    }
                }.setNegativeButton("NO", null)
            val showConfirmCancelAlert = confirmCancelAlert.show()
        }


        report.setOnClickListener{
            val reportAlert = AlertDialog.Builder(this)
            val reportAlertView = LayoutInflater.from(this).inflate(R.layout.report_form, null)

            reportAlert.setView(reportAlertView)
            val showReportAlert = reportAlert.show()

            val violation = reportAlertView.findViewById<TextInputEditText>(R.id.violation)
            val submitReport = reportAlertView.findViewById<Button>(R.id.submitViolation)

            submitReport.setOnClickListener {
                if(violation.text.toString().isEmpty()){
                    violation.error = "Please fill out this field"
                }else{
                    progress.showProgress("Please Wait...")
                    val reportJson = JSONObject()
                    reportJson.put("user_id", shopMechanicId)
                    reportJson.put("violation", violation.text.toString())
                    val reportRequest = reportJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    CoroutineScope(Dispatchers.IO).launch {
                        val submitReportResponse = try{ RetrofitInstance.retro.submitViolation("Bearer $token", reportRequest) }
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
                            showReportAlert.dismiss()
                            if(submitReportResponse.isSuccessful){
                                AlertDialog.Builder(this@MechanicArrival)
                                    .setTitle("Submitted")
                                    .setMessage("Violation report has been submitted.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }else{
                                alerts.wentWrong()
                            }
                        }
                    }
                }
            }
        }

        chat.setOnClickListener {
            val chatBottomSheet = BottomSheetDialog(this)
            val chatBottomSheetView = LayoutInflater.from(this).inflate(R.layout.conversation_layout, null)
            chatBottomSheet.setContentView(chatBottomSheetView)
            chatBottomSheet.show()

            val name = chatBottomSheetView.findViewById<TextView>(R.id.name)
            val userType = chatBottomSheetView.findViewById<TextView>(R.id.userType)
            val messageRecycler = chatBottomSheetView.findViewById<RecyclerView>(R.id.messageRecycler)
            val writeMessage = chatBottomSheetView.findViewById<EditText>(R.id.writeMessage)

            val messageAdapter = MessageAdapter(mutableListOf())
            messageRecycler.adapter = messageAdapter
            messageRecycler.layoutManager = LinearLayoutManager(this)
            val send = chatBottomSheetView.findViewById<Button>(R.id.send)
            send.setOnClickListener {
                if(writeMessage.text.toString().isEmpty()){
                    writeMessage.error = "Please fill out this field"
                }else{
                    val sendMessageJson = JSONObject()
                    sendMessageJson.put("message", writeMessage.text.toString())
                    sendMessageJson.put("receiver_id", shopMechanicId)
                    val sendMessageRequest = sendMessageJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    CoroutineScope(Dispatchers.IO).launch {
                        val sendMessageResponse = try{ RetrofitInstance.retro.sendMessage("Bearer $token", sendMessageRequest) }
                        catch(e: SocketTimeoutException){
                            withContext(Dispatchers.Main){
                                alerts.socketTimeOut()
                            }
                            return@launch
                        }catch(e: Exception){
                            withContext(Dispatchers.Main){
                                alerts.error(e.toString())
                            }
                            return@launch
                        }

                        withContext(Dispatchers.Main){
                            writeMessage.text.clear()
                            messageAdapter.add(sendMessageResponse)
                        }
                    }
                }
            }


            progress.showProgress("Loading...")
            val conversationJsonObject = JSONObject()
            conversationJsonObject.put("receiver_id", shopMechanicId)
            val conversationRequest = conversationJsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
            CoroutineScope(Dispatchers.IO).launch {

                val conversation = try{ RetrofitInstance.retro.conversation("Bearer $token", conversationRequest) }
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
                    for(i in conversation.conversation.indices){
                        messageAdapter.add(conversation.conversation[i])
                    }
                    name.text = "${conversation.receiver.first_name} ${conversation.receiver.last_name}"
                    userType.text = conversation.receiver.user_type
                }


            }
        }
        mechanicName.text = shopMechanicName
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        val db = TokenDB(this)
        val token = db.getToken()
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.isTrafficEnabled = true


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED

        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }else{
            implementMap()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            implementMap()
        }
    }

    @SuppressLint("MissingPermission")
    private fun implementMap(){
        val progress = Progress(this)
        val alerts = Alerts(this)
        val client = LocationServices.getFusedLocationProviderClient(this)
        val task = client.lastLocation
        task.addOnSuccessListener {
            val myLat = it.latitude
            val myLong = it.longitude
            val myLocation = LatLng(myLat, myLong)

            jsonObject = JSONObject()
            jsonObject.put("lat", myLat)
            jsonObject.put("long", myLong)
            jsonObject.put("mechanic_id", shopMechanicId)
            jsonObject.put("booking_id", bookingId)
            request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

            CoroutineScope(Dispatchers.IO).launch {
                while(true){
                    val mechanicLocationResponse = try{ RetrofitInstance.retro.mechanicLocation("Bearer $token", request) }
                    catch(e: Exception){
                        Log.e("MechanicArrival", e.toString())
                        return@launch
                    }
                    withContext(Dispatchers.Main){
                        val mechanicLocation = LatLng(mechanicLocationResponse.mechanic.lat.toDouble(), mechanicLocationResponse.mechanic.long.toDouble())
                        myLocationMarker?.remove()
                        mechanicMarker?.remove()

                        myLocationMarker = map.addMarker(MarkerOptions().position(myLocation).title(mechanicLocationResponse.me.name))!!
                        mechanicMarker = map.addMarker(MarkerOptions().position(mechanicLocation).title(mechanicLocationResponse.mechanic.name))!!
                        if(!zoom){
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(mechanicLocation, 10f))
                            zoom = true
                        }

                        time.text = "${mechanicLocationResponse.travel.time} min."
                        distance.text = "${mechanicLocationResponse.travel.distance} km."

                    }

                    if(mechanicLocationResponse.booking_status == "done"){
                        withContext(Dispatchers.Main){
                            AlertDialog.Builder(this@MechanicArrival)
                                .setTitle("Information")
                                .setMessage("Your vehicle has been fixed by the mechanic.")
                                .setPositiveButton("Rate"){_,_->
                                    val rateDialog = AlertDialog.Builder(this@MechanicArrival)
                                    val rateDialogView = LayoutInflater.from(this@MechanicArrival).inflate(R.layout.rate, null)

                                    rateDialog.setView(rateDialogView)
                                    rateDialog.setCancelable(false)
                                    val showRateDialog = rateDialog.show()

                                    val one = rateDialogView.findViewById<ImageView>(R.id.one)
                                    val two = rateDialogView.findViewById<ImageView>(R.id.two)
                                    val three = rateDialogView.findViewById<ImageView>(R.id.three)
                                    val four = rateDialogView.findViewById<ImageView>(R.id.four)
                                    val five = rateDialogView.findViewById<ImageView>(R.id.five)
                                    val confirm = rateDialogView.findViewById<Button>(R.id.confirm)

                                    val ratingsArray = ArrayList<ImageView>()
                                    ratingsArray.add(one)
                                    ratingsArray.add(two)
                                    ratingsArray.add(three)
                                    ratingsArray.add(four)
                                    ratingsArray.add(five)


                                    renderStars(ratingsArray)


                                    one.setOnClickListener{
                                        rating = 1
                                        renderStars(ratingsArray)
                                    }
                                    two.setOnClickListener{
                                        rating = 2
                                        renderStars(ratingsArray)
                                    }
                                    three.setOnClickListener{
                                        rating = 3
                                        renderStars(ratingsArray)
                                    }
                                    four.setOnClickListener{
                                        rating = 4
                                        renderStars(ratingsArray)
                                    }
                                    five.setOnClickListener{
                                        rating = 5
                                        renderStars(ratingsArray)
                                    }

                                    confirm.setOnClickListener {
                                        progress.showProgress("Please Wait...")
                                        val rateJson = JSONObject()
                                        rateJson.put("mechanic_shop_id", shopMechanicId)
                                        rateJson.put("rating", rating)
                                        val rateRequest = rateJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val rateResponse = try{ RetrofitInstance.retro.rate("Bearer $token", rateRequest) }
                                            catch(e: SocketTimeoutException){
                                                withContext(Dispatchers.Main){
                                                    progress.dismiss()
                                                    alerts.socketTimeOut()
                                                }
                                                return@launch
                                            }
                                            catch(e: Exception){
                                                withContext(Dispatchers.Main){
                                                    progress.dismiss()
                                                    alerts.error(e.toString())
                                                }
                                                return@launch
                                            }

                                            withContext(Dispatchers.Main){
                                                progress.dismiss()
                                                if(rateResponse.isSuccessful){
                                                    val intent = Intent(this@MechanicArrival, Navigation::class.java)
                                                    startActivity(intent)
                                                    finishAffinity()
                                                }
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton("OK", null)
                                .show()
                        }
                        break
                    }
                    delay(3000)
                }
            }
        }
    }

    private fun renderStars(ratingsArray: ArrayList<ImageView>) {
        for(i in ratingsArray.indices){
            if(i + 1 <= rating){
                ratingsArray[i].setBackgroundResource(R.drawable.ic_baseline_star_24)
            }else{
                ratingsArray[i].setBackgroundResource(R.drawable.ic_baseline_star_border_24)
            }
        }
    }
}