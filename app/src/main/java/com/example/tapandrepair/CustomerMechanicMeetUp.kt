package com.example.tapandrepair

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException

class CustomerMechanicMeetUp : FragmentActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private var customerId = 0
    private lateinit var token: String
    private var myLocationMarker: Marker? = null
    private var mechanicMarker: Marker? = null
    var zoom = false
    private lateinit var time: TextView
    private lateinit var distance: TextView
    private var bookingId = 0
    private lateinit var arrivedAlert: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_mechanic_meet_up)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val chat = findViewById<LinearLayout>(R.id.chat)
        val cancel = findViewById<LinearLayout>(R.id.cancel)
        val progress = Progress(this)
        val alerts = Alerts(this)
        val db = TokenDB(this)
        time = findViewById(R.id.time)
        distance = findViewById(R.id.distance)
        token = db.getToken()
        customerId = intent.getIntExtra("customer_id", 0)
        bookingId = intent.getIntExtra("booking_id",0)
        Log.e("customer_id", customerId.toString())
        chat.setOnClickListener{
            val chatBottomSheet = BottomSheetDialog(this)
            val chatView = LayoutInflater.from(this).inflate(R.layout.conversation_layout, null)
            chatBottomSheet.setContentView(chatView)
            chatBottomSheet.show()

            val name = chatView.findViewById<TextView>(R.id.name)
            val userType = chatView.findViewById<TextView>(R.id.userType)


            val send = chatView.findViewById<Button>(R.id.send)
            val writeMessage = chatView.findViewById<EditText>(R.id.writeMessage)
            val messageRecycler = chatView.findViewById<RecyclerView>(R.id.messageRecycler)
            val messageAdapter = MessageAdapter(mutableListOf())
            messageRecycler.adapter = messageAdapter
            messageRecycler.layoutManager = LinearLayoutManager(this)

            progress.showProgress("Loading...")
            val chatJson = JSONObject()
            chatJson.put("receiver_id", customerId)
            val chatRequest = chatJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
            CoroutineScope(Dispatchers.IO).launch {
                val conversation = try{ RetrofitInstance.retro.conversation("Bearer $token", chatRequest) }
                catch(e: SocketTimeoutException) {
                    withContext(Dispatchers.Main) {
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
                    name.text = "${conversation.receiver.first_name} ${conversation.receiver.last_name}"
                    userType.text = conversation.receiver.user_type
                    for(i in conversation.conversation.indices){
                        messageAdapter.add(conversation.conversation[i])
                    }
                }
            }


            send.setOnClickListener {
                if(writeMessage.text.toString().isEmpty()){
                    writeMessage.error = "Please fill out this field"
                }else{
                    val sendMessageJson = JSONObject()
                    sendMessageJson.put("message", writeMessage.text.toString())
                    sendMessageJson.put("receiver_id", customerId)
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
                            messageAdapter.add(sendMessageResponse)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.isTrafficEnabled = true



        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
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

    @SuppressLint("MissingPermission")
    private fun implementMap(){
        val client = LocationServices.getFusedLocationProviderClient(this)
        val task = client.lastLocation
        task.addOnSuccessListener {
            val myLat = it.latitude
            val myLong = it.longitude
            val myLocation = LatLng(myLat, myLong)

            val jsonObject = JSONObject()
            jsonObject.put("lat", myLat)
            jsonObject.put("long", myLong)
            jsonObject.put("mechanic_id", customerId)
            val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

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

                        distance.text = "${mechanicLocationResponse.travel.distance} km."
                        time.text = "${mechanicLocationResponse.travel.time} min."

                        Log.e("Travel Distance", mechanicLocationResponse.travel.distance.toString())
                    }
                    if(mechanicLocationResponse.travel.distance < 1.0){

                        withContext(Dispatchers.Main){
                            arrivedAlert = AlertDialog.Builder(this@CustomerMechanicMeetUp)
                                .setTitle("Arrived")
                                .setMessage("Your have arrived to your customer's location")
                                .setCancelable(false)
                                .setPositiveButton("Start Fixing"){_,_->
                                    val progress = Progress(this@CustomerMechanicMeetUp)
                                    val alerts = Alerts(this@CustomerMechanicMeetUp)

                                    progress.showProgress("Please Wait...")
                                    val fixRequestJson = JSONObject()
                                    fixRequestJson.put("booking_id", bookingId)
                                    val fixRequest = fixRequestJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val fixResponse = try{ RetrofitInstance.retro.fix("Bearer $token", fixRequest) }
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
                                            if(fixResponse.isSuccessful){
                                                arrivedAlert.dismiss()
                                                AlertDialog.Builder(this@CustomerMechanicMeetUp)
                                                    .setTitle("Ongoing")
                                                    .setMessage("Currently servicing customer's vehicle")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Done"){_,_->
                                                        val doneAlert = AlertDialog.Builder(this@CustomerMechanicMeetUp)
                                                        val doneAlertView = LayoutInflater.from(this@CustomerMechanicMeetUp).inflate(R.layout.amount_charged_layout, null)

                                                        doneAlert.setView(doneAlertView)
                                                        val showDoneAlert = doneAlert.show()

                                                        val amount = doneAlertView.findViewById<TextInputEditText>(R.id.amountCharged)
                                                        val confirm = doneAlertView.findViewById<Button>(R.id.confirm)

                                                        confirm.setOnClickListener {
                                                            if(amount.text.toString().isEmpty()){
                                                                amount.error = "Please fill out this field"
                                                            }else{
                                                                progress.showProgress("Please Wait...")
                                                                val doneJson = JSONObject()
                                                                doneJson.put("booking_id", bookingId)
                                                                doneJson.put("amount", amount.text.toString())
                                                                val doneRequest = doneJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                                                CoroutineScope(Dispatchers.IO).launch {
                                                                    val doneResponse = try{ RetrofitInstance.retro.done("Bearer $token", doneRequest) }
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
                                                                        val wellDoneAlert = AlertDialog.Builder(this@CustomerMechanicMeetUp)
                                                                        val wellDoneAlertView = LayoutInflater.from(this@CustomerMechanicMeetUp).inflate(R.layout.done_response, null)

                                                                        wellDoneAlert.setView(wellDoneAlertView)
                                                                        wellDoneAlert.setCancelable(false)
                                                                        val showWellDoneAlert = wellDoneAlert.show()

                                                                        val customerName = wellDoneAlertView.findViewById<TextView>(R.id.customerName)
                                                                        val service = wellDoneAlertView.findViewById<TextView>(R.id.service)
                                                                        val vehicleType = wellDoneAlertView.findViewById<TextView>(R.id.vehicleType)
                                                                        val redirect = wellDoneAlertView.findViewById<Button>(R.id.redirect)

                                                                        customerName.text = doneResponse.customer_name
                                                                        service.text = doneResponse.service
                                                                        vehicleType.text = doneResponse.vehicle_type

                                                                        redirect.setOnClickListener {

                                                                            progress.showProgress("Please Wait...")
                                                                            val paidJson = JSONObject()
                                                                            paidJson.put("transaction_id", doneResponse.transaction_id)
                                                                            val paidRequest = paidJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                                                                            CoroutineScope(Dispatchers.IO).launch {
                                                                                val paidResponse = try{ RetrofitInstance.retro.markAsPaid("Bearer $token", paidRequest) }
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
                                                                                    if(paidResponse.isSuccessful){
                                                                                        val intent = Intent(this@CustomerMechanicMeetUp, ShopOrMechanicHome::class.java)
                                                                                        startActivity(intent)
                                                                                        finishAffinity()
                                                                                    }
                                                                                }
                                                                            }

                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .show()
                                            }else{
                                                Log.e("Fix Response", fixResponse.errorBody().toString())
                                            }
                                        }
                                    }
                                }
                                .show()
                        }
                        break
                    }
                    delay(3000)
                }
            }
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
}