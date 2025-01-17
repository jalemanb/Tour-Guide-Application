package com.robotemi.sdk.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.nightonke.boommenu.BoomButtons.OnBMClickListener
import com.nightonke.boommenu.BoomButtons.TextInsideCircleButton
import com.nightonke.boommenu.BoomMenuButton
import com.nightonke.boommenu.OnBoomListenerAdapter
import com.nightonke.boommenu.Util
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.sample.guidebehavior.GuideActivity
import kotlinx.android.synthetic.main.activity_main.temi_talk1
import kotlinx.android.synthetic.main.activity_main.boom_relative_layout
import kotlinx.android.synthetic.main.activity_main.unibi_img
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity :  AppCompatActivity() {

    private var speak_: Boolean = true

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the flag to speak or not depending from where is the intent to this activity is coming
        speak_ = intent.getBooleanExtra("should_speak", true)


        if (speak_) {
            Temi.robot.speak(TtsRequest.create("Wie kann ich Ihnen heute helfen", language = TtsRequest.Language.DE_DE, isShowOnConversationLayer = false))
        }
        else {
            Temi.robot.cancelAllTtsRequests()
        }

        // Specify an explicit soft input mode to use for the window
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Get General Permissions Permisssions
        verifyStoragePermissions(this)
        get_permissions()

        val bmb = findViewById<BoomMenuButton>(R.id.boommenu_button)

        val items = listOf("Führung", "Folgen", "Karte")

        val functionList: List<() -> Unit> = listOf(::locations_menu, ::following_mode, ::info_mode)

        val icons = listOf(
            R.drawable.speak_icon,
            R.drawable.follow_icon,
            R.drawable.info_icon
        )

        for (i in 0 until bmb.piecePlaceEnum.pieceNumber()) {
            val builder = TextInsideCircleButton.Builder()
                .buttonRadius(Util.dp2px(120F))
                .normalImageRes(icons[i]) // Replace with your icon
                .normalText(items[i])
                .imageRect(Rect(Util.dp2px(10F), Util.dp2px(-10F), Util.dp2px(230F), Util.dp2px(200F)))
                .normalText(items[i])
                .textSize(30)
                .textGravity(Gravity.CENTER)
                .textRect(Rect(Util.dp2px(60F), Util.dp2px(180F), Util.dp2px(180F), Util.dp2px(220F)))
                .listener(object: OnBMClickListener {
                    override fun onBoomButtonClick(p0: Int) {
                        functionList[i]()
                    }
                })

            bmb.addBuilder(builder)
        }

        bmb.onBoomListener = object : OnBoomListenerAdapter() {

            override fun onBoomDidHide() {
                val mSlideLeft = Slide()
                mSlideLeft.slideEdge = Gravity.START
                TransitionManager.beginDelayedTransition(boom_relative_layout, mSlideLeft)
                temi_talk1.visibility = View.VISIBLE
            }

            override fun onBoomWillShow() {
                temi_talk1.visibility = View.INVISIBLE
            }

        }


        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            // Handle the back button click here
            // For example, finish the activity
            val backActivityIntent = Intent(this, StartActivity::class.java)
            startActivity(backActivityIntent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        bmb.postDelayed({
            bmb.boom()
        }, 650)

        // Keep Temi's head tilted to allow the user easy access to the touchscreen
        Temi.robot.tiltAngle(45, 0.7F)

    }

    private fun locations_menu() {

        val guideActivityIntent = Intent(this, GuideActivity::class.java)
        guideActivityIntent.putExtra("locations", Temi.robot.locations.toTypedArray())
        guideActivityIntent.putExtra("should_speak", true)
        startActivity(guideActivityIntent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

    }

    private fun following_mode() {

        val followActivityIntent = Intent(this, FollowActivity::class.java)

        startActivity(followActivityIntent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

    }

    private fun info_mode() {

        val infoActivityIntent = Intent(this, MapActivity::class.java)
        infoActivityIntent.putExtra("origin", "main")
        startActivity(infoActivityIntent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

    }


    /**
     * Setting up all the event listeners
     */
    override fun onStart() {
        super.onStart()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /**
     * Removing the event listeners upon leaving the app.
     */
    override fun onStop() {
        super.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
//        temiros2.onDestroy()
    }

    private fun get_permissions() {
        var permissionsLst = mutableListOf<String>()

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionsLst.add(android.Manifest.permission.CAMERA)
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionsLst.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionsLst.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (checkSelfPermission(android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) permissionsLst.add(android.Manifest.permission.INTERNET)

        if(permissionsLst.size > 0)
        {
            requestPermissions(permissionsLst.toTypedArray(), 101)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                get_permissions()
            }
        }
    }

    companion object {
        const val ACTION_HOME_WELCOME = "home.welcome"
        const val ACTION_HOME_DANCE = "home.dance"
        const val ACTION_HOME_SLEEP = "home.sleep"
        const val HOME_BASE_LOCATION = "home base"

        // Storage Permissions
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private const val REQUEST_CODE_NORMAL = 0
        private const val REQUEST_CODE_FACE_START = 1
        private const val REQUEST_CODE_FACE_STOP = 2
        private const val REQUEST_CODE_MAP = 3
        private const val REQUEST_CODE_SEQUENCE_FETCH_ALL = 4
        private const val REQUEST_CODE_SEQUENCE_PLAY = 5
        private const val REQUEST_CODE_START_DETECTION_WITH_DISTANCE = 6
        private const val REQUEST_CODE_SEQUENCE_PLAY_WITHOUT_PLAYER = 7
        private const val REQUEST_CODE_GET_MAP_LIST = 8
        private const val REQUEST_CODE_GET_ALL_FLOORS = 9
        private val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        /**
         * Checks if the app has permission to write to device storage
         * If the app does not has permission then the user will be prompted to grant permissions
         */
        fun verifyStoragePermissions(activity: Activity?) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
                )
            }
        }
    }
}