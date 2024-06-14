package com.robotemi.sdk.sample.guidebehavior

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.sample.R

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
            Log.d("LOC", "WELCOME : "+it.dataTitle)
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