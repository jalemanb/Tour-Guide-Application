package com.robotemi.sdk.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.nightonke.boommenu.BoomButtons.OnBMClickListener
import com.nightonke.boommenu.BoomButtons.TextInsideCircleButton
import com.nightonke.boommenu.BoomMenuButton
import com.nightonke.boommenu.Util
import com.robotemi.sdk.*
import com.robotemi.sdk.Robot.*
import com.robotemi.sdk.Robot.Companion.getInstance
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage
import com.robotemi.sdk.constants.*
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.*
import com.robotemi.sdk.map.OnLoadFloorStatusChangedListener
import com.robotemi.sdk.map.OnLoadMapStatusChangedListener
import com.robotemi.sdk.model.CallEventModel
import com.robotemi.sdk.model.DetectionData
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToDestinationChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.listener.OnReposeStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.sample.guidebehavior.GuideActivity
import com.robotemi.sdk.sample.jsonmsgs.TemiHumanDetection
import com.robotemi.sdk.sample.jsonmsgs.TemiStatus
import com.robotemi.sdk.sample.jsonmsgs.TemiTree
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import com.robotemi.sdk.voice.ITtsService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), NlpListener, OnRobotReadyListener,
    ConversationViewAttachesListener, WakeupWordListener, ActivityStreamPublishListener,
    TtsListener, OnBeWithMeStatusChangedListener, OnGoToLocationStatusChangedListener,
    OnLocationsUpdatedListener, OnConstraintBeWithStatusChangedListener,
    OnDetectionStateChangedListener, AsrListener, OnTelepresenceEventChangedListener,
    OnRequestPermissionResultListener, OnDistanceToLocationChangedListener,
    OnCurrentPositionChangedListener, OnSequencePlayStatusChangedListener, OnRobotLiftedListener,
    OnDetectionDataChangedListener, OnUserInteractionChangedListener, OnFaceRecognizedListener,
    OnConversationStatusChangedListener, OnTtsVisualizerWaveFormDataChangedListener,
    OnTtsVisualizerFftDataChangedListener, OnReposeStatusChangedListener,
    OnLoadMapStatusChangedListener, OnDisabledFeatureListUpdatedListener,
    OnMovementVelocityChangedListener, OnMovementStatusChangedListener,
    OnContinuousFaceRecognizedListener, ITtsService, OnGreetModeStateChangedListener,
    TextToSpeech.OnInitListener, OnLoadFloorStatusChangedListener,
    OnDistanceToDestinationChangedListener, OnSdkExceptionListener, OnRobotDragStateChangedListener {

    private lateinit var robot: Robot

    private var tts: TextToSpeech? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Specify an explicit soft input mode to use for the window
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Initialization Stuff
        verifyStoragePermissions(this)
        robot = getInstance()
        initOnClickListener()

        // Robot Class Setup
        robot.addOnRequestPermissionResultListener(this)
        robot.addOnTelepresenceEventChangedListener(this)
        robot.addOnFaceRecognizedListener(this)
        robot.addOnContinuousFaceRecognizedListener(this)
        robot.addOnLoadMapStatusChangedListener(this)
        robot.addOnDisabledFeatureListUpdatedListener(this)
        robot.addOnSdkExceptionListener(this)
        robot.addOnMovementStatusChangedListener(this)
        robot.addOnGreetModeStateChangedListener(this)
        robot.addOnLoadFloorStatusChangedListener(this)

        startDetectionWithDistance()

        // Initialization Stuff
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            tts = TextToSpeech(this, this)
            robot.setTtsService(this)
        }

        // Get General Permissions Permisssions
        get_permissions()

        val serverIP = "10.42.0.1"; // Control the temi robot from pc
//        val serverIP = "10.0.0.1"; // Control the temi robot from rpi

        if (!(ros2interface.isReady()))
        {
            ros2interface.setup(serverIP, robot)
        }

        val bmb = findViewById<BoomMenuButton>(R.id.boommenu_button)

        val items = listOf("Guide", "Speak", "Show Services")

        val functionList: List<() -> Unit> = listOf(::locations_menu, ::speakDe, ::speakDe)

        val icons = listOf(
            R.drawable.follow_icon,
            R.drawable.speak_icon,
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

        bmb.post {
            bmb.boom()
        }

    }
    private fun initOnClickListener() {

    }
    private fun speakDe() {
        robot.speak(TtsRequest.create("Willkommen, mein Name ist TEMI. Ich freue mich, heute hier zu sein und Teil des DiBami-Projekts zu sein. Herzlich willkommen!", false,  TtsRequest.Language.DE_DE))

    }
    private fun speakEn() {
        ros2interface.treeSelect(TemiTree(0, "robot", "My name is Teminator", false, false, false, false))
    }
    private fun locations_menu() {

        val guideActivityIntent = Intent(this, GuideActivity::class.java)
        guideActivityIntent.putExtra("locations", robot.locations.toTypedArray())
        startActivity(guideActivityIntent)
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        // Send The Status
        var statusString:String? = null
        when (ttsRequest.status) {
            TtsRequest.Status.COMPLETED -> statusString = "complete"
            TtsRequest.Status.PENDING -> statusString = "pending"
            TtsRequest.Status.PROCESSING -> statusString = "processing"
            TtsRequest.Status.STARTED -> statusString = "started"
            TtsRequest.Status.ERROR -> statusString = "error"
            TtsRequest.Status.NOT_ALLOWED -> statusString = "not_allowed"
            TtsRequest.Status.CANCELED -> statusString = "cancelled"
            else -> {
                Log.d("SpeakStatus", "Invalid Status")
            }
        }

        val templist = mutableListOf<String>()
        templist.add("BIELFELD")

        // The Speak Action is Labeled as 1
        ros2interface.speakSendStatus(TemiStatus(1,statusString, templist))

    }
    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        val templist = mutableListOf<String>()
        ros2interface.goToSendStatus(TemiStatus(2,status, templist))

    }
    override fun onMovementStatusChanged(type: String, status: String) {
        val templist = mutableListOf<String>()
        ros2interface.movementSendStatus(TemiStatus(3,status, templist))

    }
    override fun onBeWithMeStatusChanged(status: String) {
        val templist = mutableListOf<String>()
        ros2interface.followSendStatus(TemiStatus(5,status, templist))
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
    /**
     * Setting up all the event listeners
     */
    override fun onStart() {
        super.onStart()
        robot.addOnRobotReadyListener(this)
        robot.addNlpListener(this)
        robot.addOnBeWithMeStatusChangedListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addConversationViewAttachesListener(this)
        robot.addWakeupWordListener(this)
        robot.addTtsListener(this)
        robot.addOnLocationsUpdatedListener(this)
        robot.addOnConstraintBeWithStatusChangedListener(this)
        robot.addOnDetectionStateChangedListener(this)
        robot.addAsrListener(this)
        robot.addOnDistanceToLocationChangedListener(this)
        robot.addOnCurrentPositionChangedListener(this)
        robot.addOnSequencePlayStatusChangedListener(this)
        robot.addOnRobotLiftedListener(this)
        robot.addOnDetectionDataChangedListener(this)
        robot.addOnUserInteractionChangedListener(this)
        robot.addOnConversationStatusChangedListener(this)
        robot.addOnTtsVisualizerWaveFormDataChangedListener(this)
        robot.addOnTtsVisualizerFftDataChangedListener(this)
        robot.addOnReposeStatusChangedListener(this)
        robot.addOnMovementVelocityChangedListener(this)
        robot.setActivityStreamPublishListener(this)
        robot.addOnDistanceToDestinationChangedListener(this)
        robot.addOnRobotDragStateChangedListener(this)
        robot.showTopBar()
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
        robot.removeOnRobotReadyListener(this)
        robot.removeNlpListener(this)
        robot.removeOnBeWithMeStatusChangedListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeConversationViewAttachesListener(this)
        robot.removeWakeupWordListener(this)
        robot.removeTtsListener(this)
        robot.removeOnLocationsUpdateListener(this)
        robot.removeOnDetectionStateChangedListener(this)
        robot.removeAsrListener(this)
        robot.removeOnDistanceToLocationChangedListener(this)
        robot.removeOnCurrentPositionChangedListener(this)
        robot.removeOnSequencePlayStatusChangedListener(this)
        robot.removeOnRobotLiftedListener(this)
        robot.removeOnDetectionDataChangedListener(this)
        robot.addOnUserInteractionChangedListener(this)
        robot.stopMovement()
        if (robot.checkSelfPermission(Permission.FACE_RECOGNITION) == Permission.GRANTED) {
            robot.stopFaceRecognition()
        }
        robot.removeOnConversationStatusChangedListener(this)
        robot.removeOnTtsVisualizerWaveFormDataChangedListener(this)
        robot.removeOnTtsVisualizerFftDataChangedListener(this)
        robot.removeOnReposeStatusChangedListener(this)
        robot.removeOnMovementVelocityChangedListener(this)
        robot.setActivityStreamPublishListener(null)
        robot.removeOnDistanceToDestinationChangedListener(this)
        robot.removeOnRobotDragStateChangedListener(this)
        super.onStop()
    }
    override fun onDestroy() {
        robot.removeOnRequestPermissionResultListener(this)
        robot.removeOnTelepresenceEventChangedListener(this)
        robot.removeOnFaceRecognizedListener(this)
        robot.removeOnContinuousFaceRecognizedListener(this)
        robot.removeOnSdkExceptionListener(this)
        robot.removeOnLoadMapStatusChangedListener(this)
        robot.removeOnDisabledFeatureListUpdatedListener(this)
        robot.removeOnMovementStatusChangedListener(this)
        robot.removeOnGreetModeStateChangedListener(this)
        robot.removeOnLoadFloorStatusChangedListener(this)

        tts?.shutdown()
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            tts = null
            robot.setTtsService(null)
        }

        super.onDestroy()
//        temiros2.onDestroy()
    }
    override fun onUserInteraction(isInteracting: Boolean) {}
    override fun onNlpCompleted(nlpResult: NlpResult) {}
    override fun onRobotReady(isReady: Boolean) {}
    override fun onConversationAttaches(isAttached: Boolean) {}
    override fun onWakeupWord(wakeupWord: String, direction: Int) {}
    override fun onPublish(message: ActivityStreamPublishMessage) {}
    override fun onLocationsUpdated(locations: List<String>) {}
    override fun onConstraintBeWithStatusChanged(isConstraint: Boolean) {}
    override fun onDetectionStateChanged(state: Int) {}
    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {}
    override fun onTelepresenceEventChanged(callEventModel: CallEventModel) {}
    override fun onRequestPermissionResult(permission: Permission, grantResult: Int, requestCode: Int) {}
    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {}
    override fun onCurrentPositionChanged(position: Position) {
        ros2interface.updatePosition(position)
    }
    override fun onSequencePlayStatusChanged(status: Int) {}
    override fun onRobotLifted(isLifted: Boolean, reason: String) {}
    override fun onDetectionDataChanged(detectionData: DetectionData) {
        ros2interface.sendHumanDetection(TemiHumanDetection(detectionData.angle,
                                                       detectionData.distance,
                                                       detectionData.isDetected))
    }
    override fun onFaceRecognized(contactModelList: List<ContactModel>) {}
    override fun onConversationStatusChanged(status: Int, text: String) {}
    override fun onTtsVisualizerWaveFormDataChanged(waveForm: ByteArray) {}
    override fun onTtsVisualizerFftDataChanged(fft: ByteArray) {}
    override fun onReposeStatusChanged(status: Int, description: String) {}
    override fun onLoadMapStatusChanged(status: Int, requestId: String) {}
    override fun onDisabledFeatureListUpdated(disabledFeatureList: List<String>) {}
    override fun onMovementVelocityChanged(velocity: Float) {}
    override fun onContinuousFaceRecognized(contactModelList: List<ContactModel>) {}
    override fun cancel() {}
    override fun pause() {}
    override fun resume() {}
    override fun speak(ttsRequest: TtsRequest) {}
    override fun onGreetModeStateChanged(state: Int) {}
    override fun onInit(p0: Int) {}
    override fun onLoadFloorStatusChanged(status: Int) {}
    override fun onDistanceToDestinationChanged(location: String, distance: Float) {}
    override fun onSdkError(sdkException: SdkException) {}
    override fun onRobotDragStateChanged(isDragged: Boolean) {}
    private fun startDetectionWithDistance() {
        if (requestPermissionIfNeeded(
                Permission.SETTINGS,
                REQUEST_CODE_START_DETECTION_WITH_DISTANCE
            )
        ) {
            return
        }
        var distanceStr = "0.8"
        if (distanceStr.isEmpty()) distanceStr = "0"
        try {
            val distance = distanceStr.toFloat()
            robot.setDetectionModeOn(true, distance)
            Log.d("detection", "Start detection mode with distance: $distance")
        } catch (e: Exception) {
            Log.d("detection", "startDetectionModeWithDistance")
        }
    }
    @CheckResult
    private fun requestPermissionIfNeeded(permission: Permission, requestCode: Int): Boolean {
        if (robot.checkSelfPermission(permission) == Permission.GRANTED) {
            return false
        }
        robot.requestPermissions(listOf(permission), requestCode)
        return true
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