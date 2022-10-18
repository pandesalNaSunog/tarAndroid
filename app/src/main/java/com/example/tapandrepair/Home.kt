package com.example.tapandrepair

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.contextaware.withContextAvailable
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import kotlin.math.sign

class Home : AppCompatActivity() {
    private lateinit var uintent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        var userType = ""




        val login = findViewById<Button>(R.id.login)
        val signup = findViewById<Button>(R.id.signup)

        authenticate()
        login.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        signup.setOnClickListener {

            val userTypeAlert = AlertDialog.Builder(this)
            val userTypeAlertView = LayoutInflater.from(this).inflate(R.layout.choose_user_type, null)
            userTypeAlert.setView(userTypeAlertView)
            val showUserTypeAlert = userTypeAlert.show()


            val mechanic = userTypeAlertView.findViewById<Button>(R.id.mechanic)
            val customer = userTypeAlertView.findViewById<Button>(R.id.customer)
            val shopOwner = userTypeAlertView.findViewById<Button>(R.id.shopOwner)

            mechanic.setOnClickListener {
                userType = "mechanic"
                setUserType(userType)
            }

            customer.setOnClickListener {
                userType = "user"
                setUserType(userType)
            }

            shopOwner.setOnClickListener {
                userType = "owner"
                setUserType(userType)
            }

        }
    }

    private fun authenticate(){
        val progress = Progress(this)
        val db = TokenDB(this)
        val token = db.getToken()
        if(db.checkToken()){
            progress.showProgress("Please Wait...")
            CoroutineScope(Dispatchers.IO).launch {
                val userTypeResponse = try{ RetrofitInstance.retro.getUserType("Bearer $token") }
                catch(e: Exception){
                    withContext(Dispatchers.Main){
                        progress.dismiss()
                        Log.e(e.toString(), e.toString())
                        AlertDialog.Builder(this@Home)
                            .setTitle("Error")
                            .setCancelable(false)
                            .setMessage("No Internet Connection")
                            .setPositiveButton("Try Again"){_,_->
                                authenticate()
                            }
                            .show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main){
                    delay(2000)
                    progress.dismiss()
                    uintent = if(userTypeResponse.user_type == "user"){
                        Intent(this@Home, Navigation::class.java)
                    }else{
                        Intent(this@Home, ShopOrMechanicHome::class.java)
                    }
                    startActivity(uintent)
                    finishAffinity()
                }
            }
        }
    }

    private fun setUserType(type: String){
        val intent = Intent(this, SignUpForm::class.java)
        intent.putExtra("user_type", type)
        startActivity(intent)
    }
}