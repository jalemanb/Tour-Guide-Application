package com.robotemi.sdk.sample.guidebehavior

import android.os.Bundle
import android.provider.ContactsContract.Data
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.robotemi.sdk.sample.R

class GuideActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList:ArrayList<DataClass>
    lateinit var imageList:Array<Int>
    lateinit var titleList: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        titleList = intent.getStringArrayExtra("locations")!!

        imageList = arrayOf(
            R.drawable.citec,
            R.drawable.citec,
            R.drawable.citec
            )


        recyclerView = findViewById(R.id.recycler_view_location)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        dataList = arrayListOf<DataClass>()
        getData()

    }

    private fun getData() {
        for (i in imageList.indices) {
            val dataClass = DataClass(imageList[0], titleList[i])
            dataList.add(dataClass)
        }
        recyclerView.adapter = AdapterClass(dataList)
    }
}