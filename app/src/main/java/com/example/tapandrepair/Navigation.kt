package com.example.tapandrepair

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class Navigation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val navigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val controller = findNavController(R.id.fragmentContainerView)
        navigation.setupWithNavController(controller)
    }
}