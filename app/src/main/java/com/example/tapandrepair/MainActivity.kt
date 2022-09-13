package com.example.tapandrepair

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)


        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                delay(5000)
                val intent = Intent(this@MainActivity, Home::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}