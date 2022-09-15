package com.example.tapandrepair

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Navigation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        val navigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val controller = findNavController(R.id.fragmentContainerView)
        navigation.setupWithNavController(controller)

        val db = TokenDB(this)
        val token = db.getToken()

        checkHasBooking(token)
    }

    private fun checkHasBooking(token: String){
        CoroutineScope(Dispatchers.IO).launch {
            val hasBooking = try{RetrofitInstance.retro.hasBooking("Bearer $token")}
            catch(e: Exception){
                withContext(Dispatchers.Main){
                    AlertDialog.Builder(this@Navigation)
                        .setTitle("Error")
                        .setMessage("No Internet Connection. Please Try Again")
                        .setPositiveButton("OK"){_,_->
                            checkHasBooking(token)
                        }
                        .show()
                }
                return@launch
            }

            withContext(Dispatchers.Main){
                if(hasBooking.message == "has booking"){
                    val intent = Intent(this@Navigation, MechanicArrival::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }
    }
}