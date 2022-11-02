package com.example.tapandrepair

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices

class ShopLocation : AppCompatActivity() {

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_location)



        var latitude = intent.getDoubleExtra("lat", 0.0)
        var longitude = intent.getDoubleExtra("long", 0.0)
        val client = LocationServices.getFusedLocationProviderClient(this)



        val streetName = intent.getStringExtra("street_name")
        val barangay = intent.getStringExtra("barangay")
        val municipality = intent.getStringExtra("municipality")
        val postalCode = intent.getStringExtra("postal_code")
        val proceed = findViewById<Button>(R.id.proceed)
        val certification = intent.getStringExtra("certification")
        val validId = intent.getStringExtra("valid_id")
        val firstName = intent.getStringExtra("first_name")
        val lastName = intent.getStringExtra("last_name")
        val contact = intent.getStringExtra("contact")
        val email = intent.getStringExtra("email")
        val shopType = intent.getStringExtra("shop_type")
        val userType = intent.getStringExtra("user_type")
        val shopName = intent.getStringExtra("shop_name")

        proceed.setOnClickListener {
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
                val task = client.lastLocation
                task.addOnSuccessListener {
                    latitude = it.latitude
                    longitude = it.longitude
                    val intent = Intent(this, PasswordAndSecurity::class.java)
                    intent.putExtra("first_name", firstName)
                    intent.putExtra("last_name", lastName)
                    intent.putExtra("contact", contact)
                    intent.putExtra("email", email)
                    intent.putExtra("user_type", userType)
                    intent.putExtra("valid_id", validId)
                    intent.putExtra("shop_name", shopName)
                    intent.putExtra("certification", certification)
                    intent.putExtra("lat", latitude)
                    intent.putExtra("long", longitude)
                    intent.putExtra("shop_name", shopName)
                    intent.putExtra("street_name", streetName)
                    intent.putExtra("barangay", barangay)
                    intent.putExtra("shop_type", shopType)
                    intent.putExtra("municipality", municipality)
                    intent.putExtra("postal_code", postalCode)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }
    }
}