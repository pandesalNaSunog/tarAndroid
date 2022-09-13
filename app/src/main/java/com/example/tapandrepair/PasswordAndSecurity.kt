package com.example.tapandrepair

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import java.net.SocketTimeoutException

class PasswordAndSecurity : AppCompatActivity() {
    private lateinit var showAlert: AlertDialog
    private lateinit var showSuccessAlert: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_and_security)

        val firstName = intent.getStringExtra("first_name")
        val lastName = intent.getStringExtra("last_name")
        val contact = intent.getStringExtra("contact")
        val email = intent.getStringExtra("email")
        val password = findViewById<TextInputEditText>(R.id.password)
        val confirmPassword = findViewById<TextInputEditText>(R.id.confirmPassword)
        val next = findViewById<Button>(R.id.next)


        val progress = Progress(this)
        val alerts = Alerts(this)

        next.setOnClickListener {
            if(password.text.toString().isEmpty()){
                password.error = "Please fill out this field"
            }else if (confirmPassword.text.toString().isEmpty()){
                confirmPassword.error = "Please fill out this field"
            }else if(password.text.toString() != confirmPassword.text.toString()){
                password.error = "Password Mismatch"
            }else {


                val alert = AlertDialog.Builder(this)
                val alertView = LayoutInflater.from(this).inflate(R.layout.agree_to_terms, null)
                val agree = alertView.findViewById<CheckBox>(R.id.agree)
                val signup = alertView.findViewById<Button>(R.id.signup)


                signup.setOnClickListener {
                    if (!agree.isChecked) {
                        AlertDialog.Builder(this)
                            .setTitle("Warning")
                            .setMessage("You have to agree to the Terms and Conditions to proceed with signing up.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        showAlert.dismiss()
                        progress.showProgress("Signing Up...")
                        val jsonObject = JSONObject()
                        jsonObject.put("first_name", firstName)
                        jsonObject.put("last_name", lastName)
                        jsonObject.put("contact_number", contact)
                        jsonObject.put("password", password.text.toString())
                        jsonObject.put("email", email)
                        val request = jsonObject.toString()
                            .toRequestBody("application/json".toMediaTypeOrNull())
                        CoroutineScope(Dispatchers.IO).launch {
                            val registerResponse = try {
                                RetrofitInstance.retro.register(request)
                            } catch (e: SocketTimeoutException) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    alerts.socketTimeOut()
                                }
                                return@launch
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    alerts.error(e.toString())
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                progress.dismiss()
                                if (registerResponse.isSuccessful) {
                                    val successAlert = AlertDialog.Builder(this@PasswordAndSecurity)
                                    val successALertView =
                                        LayoutInflater.from(this@PasswordAndSecurity)
                                            .inflate(R.layout.register_success, null)

                                    val proceedToLogin =
                                        successALertView.findViewById<Button>(R.id.proceedToLogin)

                                    proceedToLogin.setOnClickListener {
                                        val intent =
                                            Intent(this@PasswordAndSecurity, Login::class.java)
                                        startActivity(intent)
                                        finishAffinity()
                                    }
                                    successAlert.setView(successALertView)
                                    showSuccessAlert = successAlert.show()
                                }
                            }
                        }
                    }
                }
                alert.setView(alertView)
                showAlert = alert.show()
            }
        }
    }
}