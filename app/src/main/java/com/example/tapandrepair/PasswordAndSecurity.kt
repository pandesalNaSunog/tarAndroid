package com.example.tapandrepair

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import retrofit2.HttpException
import retrofit2.Retrofit
import java.net.SocketTimeoutException

class PasswordAndSecurity : AppCompatActivity() {
    private lateinit var showAlert: AlertDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_and_security)


        val password = findViewById<TextInputEditText>(R.id.password)
        val confirmPassword = findViewById<TextInputEditText>(R.id.confirmPassword)
        val next = findViewById<Button>(R.id.next)
        val streetName = intent.getStringExtra("street_name")
        val barangay = intent.getStringExtra("barangay")
        val shopType = intent.getStringExtra("shop_type")
        val municipality = intent.getStringExtra("municipality")
        val postalCode = intent.getStringExtra("postal_code")
        val certification = intent.getStringExtra("certification")
        val validId = intent.getStringExtra("valid_id")
        val firstName = intent.getStringExtra("first_name")
        val lastName = intent.getStringExtra("last_name")
        val contact = intent.getStringExtra("contact")
        val email = intent.getStringExtra("email")
        val userType = intent.getStringExtra("user_type")
        val shopName = intent.getStringExtra("shop_name")
        val latitude = intent.getDoubleExtra("lat", 0.0)
        val longitude = intent.getDoubleExtra("long", 0.0)
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
                val dataPolicy = alertView.findViewById<Button>(R.id.dataPolicy)

                dataPolicy.setOnClickListener {
                    val dataPolicyAlert = AlertDialog.Builder(this)
                    val dataPolicyAlertView = LayoutInflater.from(this).inflate(R.layout.data_privacy_policy, null)

                    dataPolicyAlert.setView(dataPolicyAlertView)
                    val showDataPolicy = dataPolicyAlert.show()
                    val ok = dataPolicyAlertView.findViewById<Button>(R.id.ok)
                    ok.setOnClickListener {
                        showDataPolicy.dismiss()
                    }
                }


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
                        jsonObject.put("user_type", userType)
                        jsonObject.put("valid_id", validId)
                        jsonObject.put("shop_name", shopName)
                        jsonObject.put("certification", certification)
                        jsonObject.put("shop_type", shopType)
                        jsonObject.put("lat", latitude)
                        jsonObject.put("long", longitude)
                        jsonObject.put("street_name", streetName)
                        jsonObject.put("barangay", barangay)
                        jsonObject.put("municipality", municipality)
                        jsonObject.put("postal_code", postalCode)
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
                            }catch (e: HttpException) {
                                withContext(Dispatchers.Main) {
                                    progress.dismiss()
                                    if(e.code() == 401){
                                        AlertDialog.Builder(this@PasswordAndSecurity)
                                            .setTitle("Error")
                                            .setMessage("Contact Number or Email already exists.")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }else{
                                        AlertDialog.Builder(this@PasswordAndSecurity)
                                            .setTitle("Error")
                                            .setMessage("Something Went Wrong.")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
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

                                val intent = Intent(this@PasswordAndSecurity, OneTimePassword::class.java)
                                intent.putExtra("token", registerResponse.token)
                                startActivity(intent)
                                finishAffinity()

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