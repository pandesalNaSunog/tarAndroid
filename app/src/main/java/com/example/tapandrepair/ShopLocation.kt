package com.example.tapandrepair

import android.Manifest
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_location)



        var latitude = intent.getDoubleExtra("lat", 0.0)
        var longitude = intent.getDoubleExtra("long", 0.0)
        val client = LocationServices.getFusedLocationProviderClient(this)




        val proceed = findViewById<Button>(R.id.proceed)
        val certification = intent.getStringExtra("certification")
        val validId = intent.getStringExtra("valid_id")
        val firstName = intent.getStringExtra("first_name")
        val lastName = intent.getStringExtra("last_name")
        val contact = intent.getStringExtra("contact")
        val email = intent.getStringExtra("email")
        val userType = intent.getStringExtra("user_type")
        val shopName = intent.getStringExtra("shop_name")
        val shopAddress = intent.getStringExtra("shop_address")

        proceed.setOnClickListener {
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
                    intent.putExtra("shop_address", shopAddress)
                    intent.putExtra("certification", certification)
                    intent.putExtra("lat", latitude)
                    intent.putExtra("long", longitude)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }
    }
}