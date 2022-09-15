package com.example.tapandrepair

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MechanicArrival : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mechanic_arrival)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}