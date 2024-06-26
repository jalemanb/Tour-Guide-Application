package com.robotemi.sdk.sample.guidebehavior

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.sample.MainActivity
import com.robotemi.sdk.sample.MapActivity
import com.robotemi.sdk.sample.R
import com.robotemi.sdk.sample.Temi
import com.robotemi.sdk.sample.jsonmsgs.TemiTree
import com.robotemi.sdk.sample.ros2interface
import kotlinx.android.synthetic.main.activity_guide.guide_relative_layout
import kotlinx.android.synthetic.main.activity_guide.temi_talk_guide

class GuideActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList:ArrayList<DataClass>
    lateinit var imageList: Array<Int>
    lateinit var titleList: Array<String>
    lateinit var locationAdapter: AdapterClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)


        // Get the Location Names from the Previous Activity
        titleList = intent.getStringArrayExtra("locations")!!

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

        locationAdapter.onItemClicked = {
            val goal_location: String = it.dataTitle
            ros2interface.treeSelect(TemiTree(0, goal_location.lowercase(), "Please Follow Me, I'll take you to the: "+ it.dataTitle+" location", false, false, false, false))
            val mapActivityIntent = Intent(this, MapActivity::class.java)
            startActivity(mapActivityIntent)
        }

        Temi.robot.speak(TtsRequest.create("bitte wählen sie aus, wenn ich sie fuhren soll, und ich werde sofort mit der anleitung beginnen", language = TtsRequest.Language.DE_DE, isShowOnConversationLayer = false))


        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val backActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(backActivityIntent)
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
}