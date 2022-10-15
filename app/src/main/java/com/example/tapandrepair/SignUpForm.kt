package com.example.tapandrepair

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.ByteArrayOutputStream

class SignUpForm : AppCompatActivity() {
    private lateinit var certificationImage: ImageView
    private lateinit var validIdImage: ImageView
    private lateinit var image: String
    private lateinit var certificate: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_form)
        image = ""
        certificate = ""
        val firstName = findViewById<TextInputEditText>(R.id.firstName)
        val lastName = findViewById<TextInputEditText>(R.id.lastName)
        val contact = findViewById<TextInputEditText>(R.id.contact)
        val email = findViewById<TextInputEditText>(R.id.email)
        val next = findViewById<Button>(R.id.next)
        val validId = findViewById<Button>(R.id.validId)
        val shopName = findViewById<TextInputEditText>(R.id.shopName)
        val shopAddress = findViewById<TextInputEditText>(R.id.shopAddress)
        val shopNameInput = findViewById<TextInputLayout>(R.id.shopNameInput)
        val shopAddressInput = findViewById<TextInputLayout>(R.id.shopAddressInput)
        val certification = findViewById<Button>(R.id.certification)
        validIdImage = findViewById(R.id.image)
        certificationImage = findViewById(R.id.certificationImage)
        validId.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,1)
        }
        val userType = intent.getStringExtra("user_type")


        certification.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,2)
        }

        if(userType == "user"){
            shopNameInput.isVisible = false
            shopAddressInput.isVisible = false
            validId.text = "Upload Driver's License)"
            certification.isVisible = false
        }else if(userType == "mechanic"){
            shopNameInput.isVisible = false
            shopAddressInput.isVisible = false
            validId.text = "Upload Driver's License"
            certification.text = "Upload Mechanic Certificate"
        }else{
            shopNameInput.isVisible = true
            shopAddressInput.isVisible = true
            validId.text = "Upload Driver's License"
            certification.text = "Upload Business Permit"
        }

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
            }else if(image.isEmpty()){
                email.error = "Please submit a valid ID."
            }else if(shopAddress.text.toString().isEmpty() && userType == "owner"){
                shopAddress.error = "Please fill out this field"
            }else if(shopName.text.toString().isEmpty() && userType == "owner"){
                shopName.error = "Please fill out this field"
            }else if(certificate.isEmpty() && userType == "mechanic"){
                email.error = "Please submit your mechanic certificate"
            }else if(certificate.isEmpty() && userType == "owner"){
                email.error = "Please submit your business permit"
            }else{
                var thisIntent: Intent = if(userType != "owner"){
                    Intent(this, PasswordAndSecurity::class.java)
                }else{
                    Intent(this, ShopLocation::class.java)
                }

                thisIntent.putExtra("first_name", firstName.text.toString())
                thisIntent.putExtra("last_name", lastName.text.toString())
                thisIntent.putExtra("contact", contact.text.toString())
                thisIntent.putExtra("email", email.text.toString())
                thisIntent.putExtra("user_type", userType)
                thisIntent.putExtra("valid_id", image)
                thisIntent.putExtra("shop_name", shopName.text.toString())
                thisIntent.putExtra("shop_address", shopAddress.text.toString())
                thisIntent.putExtra("certification", certificate)
                thisIntent.putExtra("lat", 0.0)
                thisIntent.putExtra("long", 0.0)
                startActivity(thisIntent)
                finish()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == RESULT_OK && data != null){
            val uri = data.data!!
            validIdImage.setImageURI(uri)
            val bitmap =
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val bytes: ByteArray = stream.toByteArray()

            image = Base64.encodeToString(bytes, Base64.DEFAULT)
        }else if(requestCode == 2 && resultCode == RESULT_OK && data != null){
            val uri = data.data!!
            certificationImage.setImageURI(uri)
            val bitmap =
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
            val bytes: ByteArray = stream.toByteArray()

            certificate = Base64.encodeToString(bytes, Base64.DEFAULT)
        }
    }
}