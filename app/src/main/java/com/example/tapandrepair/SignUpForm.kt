package com.example.tapandrepair

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.ByteArrayOutputStream

class SignUpForm : AppCompatActivity() {
    private lateinit var certificationImage: ImageView
    private lateinit var validIdImage: ImageView
    private lateinit var image: String
    private lateinit var certificate: String
    private val selectValidIdCode = 3
    private val selectCertificationCode = 4
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
        val streetName = findViewById<TextInputEditText>(R.id.streetName)
        val streetNameInput = findViewById<TextInputLayout>(R.id.streetNameInput)
        val barangayInput = findViewById<TextInputLayout>(R.id.barangayInput)
        val barangay = findViewById<TextInputEditText>(R.id.barangay)
        val municipalityInput = findViewById<TextInputLayout>(R.id.municipalityInput)
        val municipality = findViewById<TextInputEditText>(R.id.municipality)
        val postalCodeInput = findViewById<TextInputLayout>(R.id.postalCodeInput)
        val postalCode = findViewById<TextInputEditText>(R.id.postalCode)
        val shopNameInput = findViewById<TextInputLayout>(R.id.shopNameInput)
        val certification = findViewById<Button>(R.id.certification)
        val serviceType = findViewById<Button>(R.id.shopType)
        validIdImage = findViewById(R.id.image)
        certificationImage = findViewById(R.id.certificationImage)
        validId.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),selectValidIdCode)
            }else{
                selectValidId()
            }
        }
        val userType = intent.getStringExtra("user_type")


        certification.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),selectCertificationCode)
            }else{
                selectCertification()
            }

        }

        if(userType == "user"){
            shopNameInput.isVisible = false
            streetNameInput.isVisible = false
            barangayInput.isVisible = false
            municipalityInput.isVisible = false
            postalCodeInput.isVisible = false
            validId.text = "Upload Driver's License)"
            certification.isVisible = false
            serviceType.isVisible = false
        }else if(userType == "mechanic"){
            shopNameInput.isVisible = false
            streetNameInput.isVisible = false
            barangayInput.isVisible = false
            municipalityInput.isVisible = false
            postalCodeInput.isVisible = false
            validId.text = "Upload Driver's License"
            certification.text = "Upload Mechanic Certificate"
        }else{
            shopNameInput.isVisible = true
            streetNameInput.isVisible = true
            barangayInput.isVisible = true
            municipalityInput.isVisible = true
            postalCodeInput.isVisible = true
            validId.text = "Upload Driver's License"
            certification.text = "Upload Business Permit"
        }

        serviceType.setOnClickListener {
            val serviceBottomSheet = BottomSheetDialog(this)
            val serviceView = LayoutInflater.from(this).inflate(R.layout.check_shop_type, null)
            serviceBottomSheet.setContentView(serviceView)
            serviceBottomSheet.show()

            val bike = serviceView.findViewById<CheckBox>(R.id.bike)
            val motorbike = serviceView.findViewById<CheckBox>(R.id.motorbike)
            val car = serviceView.findViewById<CheckBox>(R.id.car)
            val all = serviceView.findViewById<CheckBox>(R.id.all)
            val confirm = serviceView.findViewById<Button>(R.id.confirm)


            val checkArray = ArrayList<CheckBox>()
            checkArray.add(bike)
            checkArray.add(motorbike)
            checkArray.add(car)

            var shopTypeString = ""

            all.setOnClickListener {
                if(all.isChecked){
                    for(i in checkArray.indices){
                        checkArray[i].isChecked = true
                    }
                }else{
                    for(i in checkArray.indices){
                        checkArray[i].isChecked = false
                    }
                }

            }
            confirm.setOnClickListener {
                var hasCheckedItem = false
                for(i in checkArray.indices){
                    if(checkArray[i].isChecked){
                        hasCheckedItem = true
                        break
                    }
                }

                val checkedArray = ArrayList<CheckBox>()

                if(hasCheckedItem){
                    for(i in checkArray.indices){
                        if(checkArray[i].isChecked){
                            checkedArray.add(checkArray[i])
                        }
                    }

                    for(i in checkedArray.indices){
                        if(i + 1 != checkedArray.size){
                            shopTypeString += "${checkedArray[i].text}/"
                        }else{
                            shopTypeString += checkedArray[i].text
                        }
                    }


                    serviceType.text = shopTypeString
                    serviceBottomSheet.dismiss()
                }else{
                    confirm.error = "Choose at least one."
                }

            }

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
            }else if(streetName.text.toString().isEmpty() && userType == "owner"){
                streetName.error = "Please fill out this field"
            }else if(barangay.text.toString().isEmpty() && userType == "owner"){
                barangay.error = "Please fill out this field"
            }else if(municipality.text.toString().isEmpty() && userType == "owner"){
                municipality.error = "Please fill out this field"
            }else if(postalCode.text.toString().isEmpty() && userType == "owner"){
                postalCode.error = "Please fill out this field"
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
                thisIntent.putExtra("street_name", streetName.text.toString())
                thisIntent.putExtra("barangay", barangay.text.toString())
                thisIntent.putExtra("municipality", municipality.text.toString())
                thisIntent.putExtra("postal_code", postalCode.text.toString())
                thisIntent.putExtra("certification", certificate)
                thisIntent.putExtra("shop_type", serviceType.text.toString())
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == selectValidIdCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            selectValidId()
        }else if(requestCode == selectCertificationCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            selectCertification()
        }
    }

    private fun selectValidId(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,1)
    }

    private fun selectCertification(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,2)
    }
}