package com.example.tapandrepair

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlin.math.sign

class Home : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val db = TokenDB(this)

        if(db.checkToken()){
            val intent = Intent(this, Navigation::class.java)
            startActivity(intent)
            finishAffinity()
        }

        val login = findViewById<Button>(R.id.login)
        val signup = findViewById<Button>(R.id.signup)
        login.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        signup.setOnClickListener {
            val intent = Intent(this, SignUpForm::class.java)
            startActivity(intent)
        }
    }
}