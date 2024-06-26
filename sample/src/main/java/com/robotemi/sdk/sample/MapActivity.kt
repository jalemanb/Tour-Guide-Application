package com.robotemi.sdk.sample

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.robotemi.sdk.sample.guidebehavior.GuideActivity


class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val myWebView: WebView = findViewById(R.id.webview)
        myWebView.getSettings().setJavaScriptEnabled(true)

        // Enable JavaScript
        myWebView.settings.javaScriptEnabled = true

        // Enable DOM storage
        myWebView.settings.domStorageEnabled = true

//        myWebView.loadUrl("http://10.42.0.1:5000")
        myWebView.loadUrl("http://10.0.0.1:5000")

        val stopButton: ImageButton = findViewById(R.id.stopButton)
        stopButton.setOnClickListener {
            // Handle the back button click here
            // For example, finish the activity
            val backActivityIntent = Intent(this, GuideActivity::class.java)
            startActivity(backActivityIntent)
        }
    }
}