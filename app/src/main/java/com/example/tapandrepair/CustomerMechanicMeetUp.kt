package com.example.tapandrepair

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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