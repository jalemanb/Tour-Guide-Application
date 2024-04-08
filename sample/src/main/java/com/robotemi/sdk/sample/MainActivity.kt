package com.robotemi.sdk.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.RemoteException
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.robotemi.sdk.*
import com.robotemi.sdk.Robot.*
import com.robotemi.sdk.Robot.Companion.getInstance
import com.robotemi.sdk.TtsRequest.Companion.create
import com.robotemi.sdk.activitystream.ActivityStreamObject
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage
import com.robotemi.sdk.constants.*
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.*
import com.robotemi.sdk.map.Floor
import com.robotemi.sdk.map.MapModel
import com.robotemi.sdk.map.OnLoadFloorStatusChangedListener
import com.robotemi.sdk.map.OnLoadMapStatusChangedListener
import com.robotemi.sdk.model.CallEventModel
import com.robotemi.sdk.model.DetectionData
import com.robotemi.sdk.navigation.listener.OnCurrentPositionChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToDestinationChangedListener
import com.robotemi.sdk.navigation.listener.OnDistanceToLocationChangedListener
import com.robotemi.sdk.navigation.listener.OnReposeStatusChangedListener
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SafetyLevel
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import com.robotemi.sdk.sequence.SequenceModel
import com.robotemi.sdk.telepresence.CallState
import com.robotemi.sdk.telepresence.LinkBasedMeeting
import com.robotemi.sdk.telepresence.Participant
import com.robotemi.sdk.tourguide.TourModel
import com.robotemi.sdk.voice.ITtsService
import com.robotemi.sdk.voice.model.TtsVoice
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.group_app_and_permission.*
import kotlinx.android.synthetic.main.group_buttons.*
import kotlinx.android.synthetic.main.group_map_and_movement.*
import kotlinx.android.synthetic.main.group_resources.*
import kotlinx.android.synthetic.main.group_settings_and_status.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import javax.net.ssl.SSLEngineResult.Status
import kotlin.concurrent.thread


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

    private val executorService = Executors.newSingleThreadExecutor()

    private var tts: TextToSpeech? = null

    private var goto_status: String? = null
    private var speak_status: TtsRequest.Status? = null

    private var debugReceiver: TemiBroadcastReceiver? = null

    private val assistantReceiver = AssistantChangeReceiver()

    val lock = Object()

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

        // Initialization Stuff
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            tts = TextToSpeech(this, this)
            robot.setTtsService(this)
        }

        // Debugging Stuff
        debugReceiver = TemiBroadcastReceiver()
        registerReceiver(debugReceiver, IntentFilter(TemiBroadcastReceiver.ACTION_DEBUG))
        registerReceiver(assistantReceiver, IntentFilter(AssistantChangeReceiver.ACTION_ASSISTANT_SELECTION))
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
        if (!executorService.isShutdown) {
            executorService.shutdownNow()
        }
        tts?.shutdown()
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            tts = null
            robot.setTtsService(null)
        }
        if (debugReceiver != null) {
            unregisterReceiver(debugReceiver)
        }

        unregisterReceiver(assistantReceiver)
        super.onDestroy()
    }

    private fun initOnClickListener() {

        button_start_tour.setOnClickListener { startTour() }
        button_stop_tour.setOnClickListener { stopTour() }
    }

    private fun startTour() {
        val locations = robot.locations.toMutableList()

        val startMsg: String = "Tour \nStarting"

        this.button_start_tour.isEnabled = false
        this.button_start_tour.isClickable = false

        Thread(Runnable {
            runOnUiThread{updateView(startMsg)}
            Thread.sleep(2000)
            for (location in locations) {

                // Avoid going to home base
                if (location == "home base") {
                    continue
                }

                runOnUiThread{updateView("Going to: "+ location)}
                speakText("On my way to: " + location)

                robot.goTo(location, backwards = false, noBypass = false, SpeedLevel.MEDIUM)
                waitGoTo()

                runOnUiThread{updateView("Arrived to: "+ location)}
                speakText("Arrived to: " + location)

                if (location == "stefan")
                {
                    speakText("Hallo Stefan, bald kann ich Bier mitbringen", lan = TtsRequest.Language.DE_DE)
                }
            }
            runOnUiThread{updateView("Tour Completed")}
            robot.goTo("office", backwards = false, noBypass = false, SpeedLevel.MEDIUM)
            waitGoTo()
            speakText("Tour Completed I was Specifically designed for this application, this is my life purpose hope you enjoyed this unique experience")

        }).start()
    }


    private fun stopTour() {
        this.button_start_tour.isEnabled = true
        this.button_start_tour.isClickable = true
        robot.stopMovement()
    }

    private fun updateView(text:String) {
        text_view_goal.text = text
    }

    private fun speakText(text:String, lan: TtsRequest.Language =  TtsRequest.Language.EN_US) {
        robot.speak(TtsRequest.create(text, language = lan))
        waitSpeak()
    }

    private fun waitSpeak() {
        while(speak_status != TtsRequest.Status.COMPLETED)
        {
            Thread.sleep(100)
        }
        speak_status = TtsRequest.Status.PENDING
    }

    private fun waitGoTo() {
        while(goto_status != "complete")
        {
            Thread.sleep(100)
        }
        goto_status = ""
    }

    override fun onUserInteraction(isInteracting: Boolean) {}
    override fun onNlpCompleted(nlpResult: NlpResult) {}
    override fun onRobotReady(isReady: Boolean) {}
    override fun onConversationAttaches(isAttached: Boolean) {}
    override fun onWakeupWord(wakeupWord: String, direction: Int) {}
    override fun onPublish(message: ActivityStreamPublishMessage) {}
    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {

        speak_status = ttsRequest.status
    }
    override fun onBeWithMeStatusChanged(status: String) {}
    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        goto_status = status
    }
    override fun onLocationsUpdated(locations: List<String>) {}
    override fun onConstraintBeWithStatusChanged(isConstraint: Boolean) {}
    override fun onDetectionStateChanged(state: Int) {}
    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {}
    override fun onTelepresenceEventChanged(callEventModel: CallEventModel) {}
    override fun onRequestPermissionResult(permission: Permission, grantResult: Int, requestCode: Int) {}
    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {}
    override fun onCurrentPositionChanged(position: Position) {}
    override fun onSequencePlayStatusChanged(status: Int) {}
    override fun onRobotLifted(isLifted: Boolean, reason: String) {}
    override fun onDetectionDataChanged(detectionData: DetectionData) {}
    override fun onFaceRecognized(contactModelList: List<ContactModel>) {}
    override fun onConversationStatusChanged(status: Int, text: String) {}
    override fun onTtsVisualizerWaveFormDataChanged(waveForm: ByteArray) {}
    override fun onTtsVisualizerFftDataChanged(fft: ByteArray) {}
    override fun onReposeStatusChanged(status: Int, description: String) {}
    override fun onLoadMapStatusChanged(status: Int, requestId: String) {}
    override fun onDisabledFeatureListUpdated(disabledFeatureList: List<String>) {}
    override fun onMovementVelocityChanged(velocity: Float) {}
    override fun onMovementStatusChanged(type: String, status: String) {}
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

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


}