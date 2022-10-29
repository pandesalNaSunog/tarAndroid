package com.example.tapandrepair

import android.app.AlertDialog
import android.content.Context

class Alerts(private val context: Context) {
    fun socketTimeOut(){
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Connection Time Out.")
            .setPositiveButton("OK", null)
            .show()
    }

    fun error(error: String){
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(error)
            .setPositiveButton("OK", null)
            .show()
    }

    fun wentWrong(){
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Something Went Wrong")
            .setPositiveButton("OK", null)
            .show()
    }
}