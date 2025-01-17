package com.robotemi.sdk.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ShowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show) // Ensure your XML layout file is named "activity_show.xml"
        val message = intent.getStringExtra("SHOW_MESSAGE")

        // Find the TextView by its ID
        val centeredTextView: TextView = findViewById(R.id.sampleTextView)

        // Set the text dynamically
        centeredTextView.text = message

        // Optionally adjust text size programmatically
        centeredTextView.textSize = 100f // Text size in SP

        // Additional setup if required
    }
}
