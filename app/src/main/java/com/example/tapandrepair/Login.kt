package com.example.tapandrepair

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
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

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val contact = findViewById<TextInputEditText>(R.id.contact)
        val password = findViewById<TextInputEditText>(R.id.password)
        val login = findViewById<Button>(R.id.login)
        val progress = Progress(this)
        val alerts = Alerts(this)
        val db = TokenDB(this)
        login.setOnClickListener {
            if(contact.text.toString().isEmpty()){
                contact.error = "Please fill out this field"
            }else if(password.text.toString().isEmpty()){
                password.error = "Please fill out this field"
            }else{
                progress.showProgress("Logging in...")
                val jsonObject = JSONObject()
                jsonObject.put("contact_number", contact.text.toString())
                jsonObject.put("password", password.text.toString())
                val request = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

                CoroutineScope(Dispatchers.IO).launch {
                    val loginResponse = try{ RetrofitInstance.retro.login(request) }
                    catch(e: SocketTimeoutException){
                        withContext(Dispatchers.Main){
                            progress.dismiss()
                            alerts.socketTimeOut()
                        }
                        return@launch
                    }catch(e: HttpException){
                        withContext(Dispatchers.Main) {
                            progress.dismiss()

                            withContext(Dispatchers.Main) {
                                if(e.code() == 404){
                                    AlertDialog.Builder(this@Login)
                                        .setTitle("Error")
                                        .setMessage("Account Not Found")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }else if(e.code() == 400){
                                    AlertDialog.Builder(this@Login)
                                        .setTitle("Verification")
                                        .setMessage("You have failed to verify your account during your registration. Resending verification is still under development.")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }else{
                                    AlertDialog.Builder(this@Login)
                                        .setTitle("Error")
                                        .setMessage("Your Account has not yet approved by the administrator. You may receive an email notification regarding with your account approval.")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }

                            }
                        }
                        return@launch
                    }catch (e: Exception){
                        withContext(Dispatchers.Main){
                            progress.dismiss()
                            alerts.error(e.toString())
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main){
                        db.add(loginResponse)

                        val intent = if(loginResponse.type == "user"){
                            Intent(this@Login, Navigation::class.java)
                        }else{
                            Intent(this@Login, ShopOrMechanicHome::class.java)
                        }
                        startActivity(intent)
                        finishAffinity()
                    }
                }
            }
        }
    }
}