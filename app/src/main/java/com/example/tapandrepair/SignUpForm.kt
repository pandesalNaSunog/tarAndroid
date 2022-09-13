package com.example.tapandrepair

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText

class SignUpForm : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_form)

        val firstName = findViewById<TextInputEditText>(R.id.firstName)
        val lastName = findViewById<TextInputEditText>(R.id.lastName)
        val contact = findViewById<TextInputEditText>(R.id.contact)
        val email = findViewById<TextInputEditText>(R.id.email)
        val next = findViewById<Button>(R.id.next)

        next.setOnClickListener {
            if(firstName.text.toString().isEmpty()){
                firstName.error = "Please fill out this field"
            }else if(lastName.text.toString().isEmpty()){
                lastName.error = "Please fill out this field"
            }else if(contact.text.toString().isEmpty()){
                contact.error = "Please fill out this field"
            }else if((contact.text.toString()[0] != '0' || contact.text.toString()[1] != '9') && contact.text.toString().length != 11){
                contact.error = "Please enter a valid mobile number"
            }else if(email.text.toString().isEmpty()){
                email.error = "Please fill out this field"
            }else if(!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()){
                email.error = "Please enter a valid email address."
            }else{
                val intent = Intent(this, PasswordAndSecurity::class.java)
                intent.putExtra("first_name", firstName.text.toString())
                intent.putExtra("last_name", lastName.text.toString())
                intent.putExtra("contact", contact.text.toString())
                intent.putExtra("email", email.text.toString())
                startActivity(intent)
                finish()
            }

        }
    }
}