package com.robotemi.sdk.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.robotemi.sdk.sample.guidebehavior.GuideActivity
import kotlinx.android.synthetic.main.activity_guide.person_detected_image_view


class MapActivity : AppCompatActivity() {

    lateinit var myWebView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isHumanDetected:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Visualize given url on Temi Screen
        setupWebView("http://10.0.0.1:5000")
        // setupWebView("http://10.42.0.1:5000")

        // Button to halt the robot motion and go back to the previous activity
        stopBtn()

        // Visualize if a person is detected accoding to the inner Temi model
        setupRunnable()

    }

    private fun setupWebView(url:String) {

        myWebView = findViewById(R.id.webview)
        myWebView.getSettings().setJavaScriptEnabled(true)
        // Enable JavaScript
        myWebView.settings.javaScriptEnabled = true
        // Enable DOM storage
        myWebView.settings.domStorageEnabled = true
        myWebView.loadUrl(url)
    }

    private fun stopBtn() {
        val stopButton: ImageButton = findViewById(R.id.stopButton)
        stopButton.setOnClickListener {
            // Handle the back button click here
            // For example, finish the activity
            val backActivityIntent = Intent(this, GuideActivity::class.java)
            startActivity(backActivityIntent)
        }
    }

    private fun setupRunnable() {
        // Define the runnable
        runnable = Runnable {
            isHumanDetected = HumanDectection.is_detection
            if(isHumanDetected) {
                person_detected_image_view.setImageResource(R.drawable.yespersondetectedde_drawio)
            }
            else {
                person_detected_image_view.setImageResource(R.drawable.nopersondetectedde_drawio)
            }
            handler.postDelayed(runnable, 400)
        }

    }

    override fun onStart() {
        super.onStart()
        handler.post(runnable) // Start the runnable when the activity is started
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable) // Stop the runnable when the activity is stopped
    }
}