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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mechanic_arrival)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        mapFragment.getMapAsync(this)
        shopMechanicId = intent.getIntExtra("shop_mechanic_id", 0)
        shopMechanicName = intent.getStringExtra("name").toString()

        val mechanicName = findViewById<TextView>(R.id.mechanicName)
        val chat = findViewById<Button>(R.id.chat)
        val progress = Progress(this)
        val alerts = Alerts(this)
        val db = TokenDB(this)
        val token = db.getToken()

        chat.setOnClickListener {
            val chatBottomSheet = BottomSheetDialog(this)
            val chatBottomSheetView = LayoutInflater.from(this).inflate(R.layout.conversation_layout, null)
            chatBottomSheet.setContentView(chatBottomSheetView)
            chatBottomSheet.show()

            val name = chatBottomSheetView.findViewById<TextView>(R.id.name)
            val userType = chatBottomSheetView.findViewById<TextView>(R.id.userType)
            val messageRecycler = chatBottomSheetView.findViewById<RecyclerView>(R.id.messageRecycler)
            val writeMessage = chatBottomSheetView.findViewById<EditText>(R.id.writeMessage)
            val send = chatBottomSheetView.findViewById<Button>(R.id.send)
            val messageAdapter = MessageAdapter(mutableListOf())
            messageRecycler.adapter = messageAdapter
            messageRecycler.layoutManager = LinearLayoutManager(this)

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
        val client = LocationServices.getFusedLocationProviderClient(this)

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
            val task = client.lastLocation
            task.addOnSuccessListener {
                val myLat = it.latitude
                val myLong = it.longitude
                val myLocation = LatLng(myLat, myLong)

                jsonObject = JSONObject()
                jsonObject.put("lat", myLat)
                jsonObject.put("long", myLong)
                jsonObject.put("mechanic_id", shopMechanicId)
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
                        }
                        delay(3000)



                    }

                }
            }
        }



    }
}