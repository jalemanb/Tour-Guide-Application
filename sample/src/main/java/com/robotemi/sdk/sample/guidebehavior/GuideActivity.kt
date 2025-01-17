package com.robotemi.sdk.sample.guidebehavior

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.sample.HumanDectection
import com.robotemi.sdk.sample.MainActivity
import com.robotemi.sdk.sample.MapActivity
import com.robotemi.sdk.sample.R
import com.robotemi.sdk.sample.Temi
import com.robotemi.sdk.sample.jsonmsgs.TemiTree
import com.robotemi.sdk.sample.ros2interface
//import kotlinx.android.synthetic.main.activity_guide.person_detected_image_view

class GuideActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList:ArrayList<DataClass>
    lateinit var imageList: Array<Int>
    lateinit var titleList: Array<String>
    lateinit var locationAdapter: AdapterClass
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isHumanDetected:Boolean = false
    private var speak_:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        // Get the Location Names from the Previous Activity
        titleList = intent.getStringArrayExtra("locations")!!

        // Get the flag to speak or not depending from where is the intent to this activity is coming
        speak_ = intent.getBooleanExtra("should_speak", true)

        if (speak_) {
            // Speak The Initial Phrase to ask the person to select a location where he/she wants to navigate
            Temi.robot.speak(TtsRequest.create("bitte wählen sie aus, wenn ich sie fuhren soll, und ich werde sofort mit der anleitung beginnen", language = TtsRequest.Language.DE_DE, isShowOnConversationLayer = false))
        }
        else {
            Temi.robot.cancelAllTtsRequests()
        }

        // Boilerplate for Setting up a RecyclerView
        setupRecyclerView()

        // Setting Up the Functionality for when a Button fro the RecyclerView is Pressed
        buttonFunctionality()


        // Button to go back
        backBtn()

        // Visualize if a person is detected accoding to the inner Temi model
        setupRunnable()

        // Keep Temi's head tilted to allow the user easy access to the touchscreen
        Temi.robot.tiltAngle(45, 0.7F)

    }

    private fun buttonFunctionality() {
        locationAdapter.onItemClicked = {
            // Get The Desired Goal Location
            val goal_location: String = it.dataTitle
            ros2interface.treeSelect(TemiTree(0, goal_location.lowercase(), "Bitte folgen sie mir, das naechste Ziel ist: "+ it.dataTitle, false, false, false, false))
            // Visualize the map (robot, goal and person)
            val mapActivityIntent = Intent(this, MapActivity::class.java)
            mapActivityIntent.putExtra("origin", "guide")
            startActivity(mapActivityIntent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        }
    }

    private fun setupRecyclerView() {
        // Get the Img IDs from the corresponding locations
        imageList = titleList.map { imageName ->
            val formattedName = imageName.replace(" ", "_")
            resources.getIdentifier(formattedName, "drawable", packageName)
        }.toTypedArray()


        recyclerView = findViewById(R.id.recycler_view_location)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        dataList = arrayListOf<DataClass>()
        getData()
    }

    private fun backBtn() {
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val backActivityIntent = Intent(this, MainActivity::class.java)
            backActivityIntent.putExtra("should_speak", false)
            startActivity(backActivityIntent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun getData() {
        for (i in titleList.indices) {
            val dataClass = DataClass(imageList[i], titleList[i].uppercase())
            dataList.add(dataClass)
        }
        locationAdapter = AdapterClass(dataList)
        recyclerView.adapter = locationAdapter
    }

    private fun setupRunnable() {
        // Define the runnable
        runnable = Runnable {
            isHumanDetected = HumanDectection.is_detection
//            if(isHumanDetected) {
//                person_detected_image_view.setImageResource(R.drawable.yespersondetectedde_drawio)
//            }
//            else {
//                person_detected_image_view.setImageResource(R.drawable.nopersondetectedde_drawio)
//            }
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