package com.example.tapandrepair

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tapandrepair.databinding.ActivityShopMapBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShopMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityShopMapBinding
    private lateinit var progress: Progress
    private var token = ""
    private lateinit var db: TokenDB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityShopMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        db = TokenDB(this)
        token = db.getToken()
        progress = Progress(this)
        getLocations()
        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun getLocations(){

        progress.showProgress("Loading...")
        CoroutineScope(Dispatchers.IO).launch {
            val shopLocationArray = try{ RetrofitInstance.retro.shopLocations("Bearer $token") }
            catch(e: Exception){
                progress.dismiss()
                getLocations()
                return@launch
            }

            withContext(Dispatchers.Main){
                progress.dismiss()
                for(i in shopLocationArray.indices){
                    mMap.addMarker(MarkerOptions().position(LatLng(shopLocationArray[i].lat, shopLocationArray[i].long)).title(shopLocationArray[i].shop_name))
                }
            }
        }
    }
}