package com.robotemi.sdk.sample

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.robotemi.sdk.sample.guidebehavior.GuideActivity
import com.robotemi.sdk.sample.jsonmsgs.TemiCurrentAction
import com.robotemi.sdk.sample.jsonmsgs.TemiTree
import kotlinx.android.synthetic.main.activity_map.person_detected_image_view


class MapActivity : AppCompatActivity() {

    lateinit var myWebView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isHumanDetected:Boolean = false
    private var originActivity: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        originActivity = intent.getStringExtra("origin").toString()

        // Visualize given url on Temi Screen
//        setupWebView("http://10.42.0.1:5000") // Communicate with real computer
         setupWebView("http://10.0.0.1:5000") // Communicate with jetson

        // Button to halt the robot motion and go back to the previous activity
        stopBtn()

        // Visualize if a person is detected accoding to the inner Temi model
        setupRunnable()

    }

    private fun setupWebView(url:String) {

        myWebView = findViewById(R.id.webview)

        myWebView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        // Enable JavaScript and other settings
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        myWebView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        myWebView.settings.loadWithOverviewMode = true
        myWebView.settings.useWideViewPort = true
        myWebView.settings.loadsImagesAutomatically = true
        myWebView.settings.blockNetworkImage = false // Ensure images are loaded
        myWebView.settings.setSupportZoom(false) // Disable zoom support for better performance
        myWebView.settings.allowFileAccess = false
        myWebView.settings.allowContentAccess = false

        myWebView.loadUrl(url)
    }

    private fun stopBtn() {
        Temi.robot.stopMovement()
        val stopButton: ImageButton = findViewById(R.id.stopButton)

        stopButton.setOnClickListener {
            // Handle the back button click here
            // For example, finish the activity
            var backActivityIntent:Intent = Intent(this, GuideActivity::class.java)

            if (originActivity == "guide") {
                backActivityIntent = Intent(this, GuideActivity::class.java)
                backActivityIntent.putExtra("locations", Temi.robot.locations.toTypedArray())
            }
            else if (originActivity == "main") {
                 backActivityIntent = Intent(this, MainActivity::class.java)
            }
            else
            {
                backActivityIntent = Intent(this, MainActivity::class.java)
            }

            backActivityIntent.putExtra("should_speak", false)

            if (TemiCurrentAction.name == "goto") {
                // Execute a Behavior Tree for Emergency Stop
                ros2interface.treeSelect(TemiTree(1, "","", false, false, false, false))
//                stopButton.setBackgroundResource( R.drawable.circular_button)
//                stopButton.setImageResource(R.drawable.ic_arrow_back)
                TemiCurrentAction.name = "goto_completed"
            }

            startActivity(backActivityIntent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)

        }
    }

    override fun finish() {
        super.finish()
        myWebView = findViewById(R.id.webview)
        myWebView.clearCache(true)
        myWebView.clearHistory()
    }

    private fun setupRunnable() {
        // Define the runnable
        runnable = Runnable {

            val stopButton: ImageButton = findViewById(R.id.stopButton)

            isHumanDetected = HumanDectection.is_detection
            if(isHumanDetected) {
                person_detected_image_view.setImageResource(R.drawable.yespersondetectedde_drawio)
            }
            else {
                person_detected_image_view.setImageResource(R.drawable.nopersondetectedde_drawio)
            }

            if (TemiCurrentAction.name == "goto")
            {
                stopButton.setBackgroundResource( R.drawable.circular_button_red)
                stopButton.setImageResource(R.drawable.stopbtn)
            }
            else if (TemiCurrentAction.name == "goto_complete")
            {
                stopButton.setBackgroundResource( R.drawable.circular_button)
                stopButton.setImageResource(R.drawable.ic_arrow_back)
            }

            handler.postDelayed(runnable, 300)

        }

    }

    override fun onStart() {
        super.onStart()
        handler.post(runnable) // Start the runnable when the activity is started
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable) // Stop the runnable when the activity is stopped
        disconnectWebView()

    }

    override fun onPause() {
        super.onPause()
        myWebView.onPause()  // Pause WebView rendering
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up WebView to prevent memory leaks
        disconnectWebView()
    }

    private fun disconnectWebView() {
        myWebView.apply {
            // Stop any ongoing loading
            stopLoading()

            // Load a blank page to clear any active connection to the server
            loadUrl("about:blank")

            // Clear history and cache
            clearHistory()
            clearCache(true)

            // Remove the WebView from the view hierarchy
            removeAllViews()

            // Destroy the WebView instance
            destroy()
        }
    }

}