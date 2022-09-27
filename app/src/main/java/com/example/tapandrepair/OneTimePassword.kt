package com.example.tapandrepair

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException

class OneTimePassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_time_password)

        val token = intent.getStringExtra("token")
        val otp = findViewById<TextInputEditText>(R.id.otp)
        val confirm = findViewById<Button>(R.id.confirm)
        val progress = Progress(this)
        val alerts = Alerts(this)



        val timer = object: CountDownTimer(100000, 1000){
            override fun onTick(p0: Long) {
                Log.e("OneTimePassword", p0.toString())
            }

            override fun onFinish() {

                val intent = Intent(this@OneTimePassword, Home::class.java)
                startActivity(intent)
                finishAffinity()
            }
        }.start()


        confirm.setOnClickListener {

            if(otp.text.toString().isEmpty()){
                otp.error = "Please fill out this field"
            }else{
                progress.showProgress("Please Wait...")
                val jsonObject = JSONObject()
                jsonObject.put("otp", otp.text.toString())
                val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
                CoroutineScope(Dispatchers.IO).launch {
                    val sendOtpResponse = try{ RetrofitInstance.retro.sendOTP("Bearer $token", request) }
                    catch(e: SocketTimeoutException){
                        withContext(Dispatchers.Main){
                            progress.dismiss()
                            alerts.socketTimeOut()
                        }
                        return@launch
                    }catch(e: HttpException){
                        withContext(Dispatchers.Main){
                            progress.dismiss()
                            AlertDialog.Builder(this@OneTimePassword)
                                .setTitle("Error")
                                .setMessage("Invalid OTP")
                                .show()
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
                        timer.cancel()
                        if(sendOtpResponse.isSuccessful){
                            val successAlert = AlertDialog.Builder(this@OneTimePassword)
                            val successALertView =
                                LayoutInflater.from(this@OneTimePassword)
                                    .inflate(R.layout.register_success, null)

                            val proceedToLogin =
                                successALertView.findViewById<Button>(R.id.proceedToLogin)

                            proceedToLogin.setOnClickListener {
                                val intent =
                                    Intent(this@OneTimePassword, Login::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                            successAlert.setView(successALertView)
                            successAlert.show()
                        }
                    }
                }
            }

        }
    }
}