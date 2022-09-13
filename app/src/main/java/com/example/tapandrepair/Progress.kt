package com.example.tapandrepair

import android.app.ProgressDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView

class Progress(private val context: Context) {
    private lateinit var progress: ProgressDialog
    fun showProgress(progressText: String){
        progress = ProgressDialog(context)
        val progressView = LayoutInflater.from(context).inflate(R.layout.loading_screen, null)
        val text = progressView.findViewById<TextView>(R.id.loadingText)
        text.text = progressText
        progress.show()
        progress.setContentView(progressView)
        progress.setCancelable(false)
        progress.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun dismiss(){
        progress.dismiss()
    }
}