package com.robotemi.sdk.sample

import WebSocketCom
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
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
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import com.robotemi.sdk.voice.ITtsService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

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

    private var debugReceiver: TemiBroadcastReceiver? = null

    private val assistantReceiver = AssistantChangeReceiver()

    private var tour_job: Job? = null

    val scope = CoroutineScope(Dispatchers.Default + CoroutineName("MyScope"))

    val onGoToChannel = Channel<Int>()

    val onSpeakChannel = Channel<Int>()

    val mutex = Mutex()

    val gson = Gson()

    private val positionMutex = Mutex()
    private val mapMutex = Mutex()


    private var temiPosition:Position = Position(0F, 0F, 0F, 0)

    val positionExecutor = Executors.newSingleThreadScheduledExecutor()
    val mapExecutor = Executors.newSingleThreadScheduledExecutor()

    private lateinit var future_map:ScheduledFuture<*>
    private lateinit var future_position:ScheduledFuture<*>

    // Websocket client
    private lateinit var ws_getloc: WebSocketCom
    private lateinit var ws_speak: WebSocketCom
    private lateinit var ws_goto: WebSocketCom
    private lateinit var ws_turnby: WebSocketCom
    private lateinit var ws_tiltangle: WebSocketCom
    private lateinit var ws_follow: WebSocketCom

    private lateinit var ws_hd: WebSocketCom
    private lateinit var ws_rosbridge: WebSocketCom
    private lateinit var ws_mapserver: WebSocketCom

//    private lateinit var cameraService: CameraDriver

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

        // Debugging Stuff
        debugReceiver = TemiBroadcastReceiver()
        registerReceiver(debugReceiver, IntentFilter(TemiBroadcastReceiver.ACTION_DEBUG))
        registerReceiver(assistantReceiver, IntentFilter(AssistantChangeReceiver.ACTION_ASSISTANT_SELECTION))

        // Get General Permissions Permisssions
        get_permissions()

        // Enable start tour button assuming the robot start on idle state
        // Disable stop tour button assuming the root is already stopped at the beginning
        button_speakEn.isEnabled = true
        button_speakEn.isClickable = true
        button_speakDe.isEnabled = true
        button_speakDe.isClickable = true

        val serverIP = "10.42.0.1";
//        val serverIP = "10.0.0.1";

        // Websocket configuration
        ws_getloc = object : WebSocketCom("ws://$serverIP:8760", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command Getloc Received")
                        // Implement Switch stamente for each Robot Capability
                        when (commandObj.command) {
                            0 -> getloc_cmd(commandObj)
                            else -> {
                                Log.d("WebSocket", "Command Sent is: ${commandObj.command}")
                            }
                        }
                    }
                }
            }
        }


        ws_speak = object : WebSocketCom("ws://$serverIP:8761", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command Speak Received")
                        // Implement Switch stamente for each Robot Capability
                        when (commandObj.command) {
                            1 -> speak_cmd(commandObj)
                            else -> {
                                Log.d("WebSocket", "Command Sent is: ${commandObj.command}")
                            }
                        }
                    }
                }
            }
        }

        ws_goto = object : WebSocketCom("ws://$serverIP:8762", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command Goto Received")
                        // Implement Switch stamente for each Robot Capability
                        if(!commandObj.flag3)
                        {
                            if(commandObj.flag0)
                            {
                                gotopos_cmd(commandObj)
                            }
                            else
                            {
                                goto_cmd(commandObj)
                            }
                        }
                        else
                        {
                            stop_cmd(commandObj)
                        }

                    }
                }
            }
        }

        ws_turnby = object : WebSocketCom("ws://$serverIP:8763", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("TurnBy", "Command TurnBy Received")
                        // Implement Switch stamente for each Robot Capability
                        turnby_cmd(commandObj)
                    }
                }
            }
        }

        ws_tiltangle = object : WebSocketCom("ws://$serverIP:8764", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command TiltAngle Received")
                        tiltangle_cmd(commandObj)
                    }
                }
            }
        }

        ws_follow = object : WebSocketCom("ws://$serverIP:8765", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command Follow Received")

                        if(!commandObj.flag3)
                        {
                            follow_cmd(commandObj)
                        }
                        else
                        {
                            stop_cmd(commandObj)
                        }
                    }
                }
            }
        }


        ws_hd =  object : WebSocketCom("ws://$serverIP:8767", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                    }
                }
            }
        }

        ws_rosbridge = object : WebSocketCom("ws://$serverIP:8768", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command Joy_cmd received")
                        joy_cmd(commandObj)
                    }
                }
            }
        }

        ws_mapserver = object : WebSocketCom("ws://$serverIP:8769", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                    }
                }
            }
        }

        val positionScope = CoroutineScope(Dispatchers.IO) // Assuming IO-bound tasks
        val mapScope = CoroutineScope(Dispatchers.IO)

        future_position = positionExecutor.scheduleAtFixedRate({
            positionScope.launch {
                positionMutex.withLock {
                    Log.d("PERIODIC", "Pose is: ${temiPosition.x}, ${temiPosition.y}, ${temiPosition.yaw}")
                    // Get current robot pose and send it to the temi_bridge
                    val temiPositionJson = gson.toJson(temiPosition)
                    ws_rosbridge.getWebSocket().sendText(temiPositionJson)
                }
            }
        }, 0, 30, TimeUnit.MILLISECONDS)

        future_map = mapExecutor.scheduleAtFixedRate({
            mapScope.launch {
                mapMutex.withLock {
                    // Get the current map data and send it to the temi_bridge
                    val currentMap = robot.getMapData()
                    val temiMapJson = gson.toJson(TemiMap(currentMap!!.mapInfo.height, currentMap.mapInfo.width,
                        currentMap.mapInfo.originX,  currentMap.mapInfo.originY,
                        currentMap.mapInfo.resolution, currentMap.mapImage.data))
                    ws_mapserver.getWebSocket().sendText(temiMapJson)
                }
            }
        }, 0, 1, TimeUnit.SECONDS)

        // Start Camera Streaming
//        cameraService = CameraDriver(this, ws)
//        cameraService.start_camera()
    }
    private fun getloc_cmd(cmd:TemiCommand) {
        val locations = robot.locations.toMutableList()
        // The getloc Action is Labeled as 0
        val statusList = listOf("pending", "started", "processing", "complete")
        for (statusString in statusList) {
            val jsonString = gson.toJson(TemiStatus(0,statusString, locations))
            ws_getloc.getWebSocket().sendText(jsonString)
            Thread.sleep(100)
        }
    }
    private fun speak_cmd(cmd:TemiCommand) {
        robot.speak(TtsRequest.create(cmd.text!!, language = TtsRequest.Language.EN_US, isShowOnConversationLayer = false))
    }
    private fun goto_cmd(cmd:TemiCommand) {
        robot.goTo(cmd.text!!, backwards = false, noBypass = false, SpeedLevel.MEDIUM)
    }
    private fun gotopos_cmd(cmd:TemiCommand) {
        val position: Position = Position(x = cmd.x!!, y = cmd.y!!, yaw = cmd.angle!!)
        robot.goToPosition( position, backwards = cmd.flag1, noBypass = false, speedLevel = SpeedLevel.MEDIUM)
    }
    private fun joy_cmd(cmd:TemiCommand) {
        robot.skidJoy(cmd.x!!,cmd.y!!, false)
    }
    private fun turnby_cmd(cmd:TemiCommand) {
        robot.turnBy(cmd.angle!!.toInt(), 1.0F)
    }
    private fun tiltangle_cmd(cmd:TemiCommand) {
        robot.tiltAngle(cmd.angle!!.toInt().coerceIn(-25, 55), 0.7F)
        val statusList = listOf("pending", "started", "processing", "complete")
        val templist = mutableListOf<String>()
        for (statusString in statusList) {
            val jsonString = gson.toJson(TemiStatus(4,statusString, templist))
            ws_tiltangle.getWebSocket().sendText(jsonString)
            Thread.sleep(50)
        }
    }
    private fun follow_cmd(cmd:TemiCommand) {
        robot.beWithMe()
    }

    private fun stop_cmd(cmd:TemiCommand)
    {
        robot.stopMovement()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

        // Stop Camera Service
        // cameraService.stopCamera()
        // Stop web sockets
        ws_speak.close()
        ws_getloc.close()
        ws_goto.close()
        ws_turnby.close()
        ws_tiltangle.close()
        ws_follow.close()
        ws_hd.close()

        ws_rosbridge.close()
        ws_mapserver.close()

        future_position.cancel(true)
        future_map.cancel(true)
        positionExecutor.shutdown()
    }

    private fun initOnClickListener() {

        button_speakDe.setOnClickListener { speakDe() }
        button_speakEn.setOnClickListener { speakEn() }
    }

    private fun speakDe() {
        robot.speak(TtsRequest.create("Willkommen, mein Name ist TEMI. Ich freue mich, heute hier zu sein und Teil des DiBami-Projekts zu sein. Herzlich willkommen!", false,  TtsRequest.Language.DE_DE))

    }

    private fun speakEn() {
        robot.speak(TtsRequest.create("Welcome My Name is TEMI Im happy to be here today and be part of the DiBami Project, Welcome everybody", false, TtsRequest.Language.EN_US))

    }

    private fun speakText(text:String, lan: TtsRequest.Language =  TtsRequest.Language.EN_US) {
        robot.speak(TtsRequest.create(text, language = lan, isShowOnConversationLayer = false))
        runBlocking { onSpeakChannel.receive() }
    }

    private fun goToLocation(location: String) {
        robot.goTo(location, backwards = false, noBypass = false, SpeedLevel.MEDIUM)
        runBlocking { onGoToChannel.receive() }
    }

    override fun onUserInteraction(isInteracting: Boolean) {}
    override fun onNlpCompleted(nlpResult: NlpResult) {}
    override fun onRobotReady(isReady: Boolean) {}
    override fun onConversationAttaches(isAttached: Boolean) {}
    override fun onWakeupWord(wakeupWord: String, direction: Int) {}
    override fun onPublish(message: ActivityStreamPublishMessage) {}
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
        val jsonString = gson.toJson(TemiStatus(1,statusString, templist))
        ws_speak.getWebSocket().sendText(jsonString)

    }
    override fun onBeWithMeStatusChanged(status: String) {
        val templist = mutableListOf<String>()
        val jsonString = gson.toJson(TemiStatus(5,status, templist))
        ws_follow.getWebSocket().sendText(jsonString)
    }
    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        val templist = mutableListOf<String>()
        val jsonString = gson.toJson(TemiStatus(2,status, templist))
        ws_goto.getWebSocket().sendText(jsonString)

    }
    override fun onLocationsUpdated(locations: List<String>) {}
    override fun onConstraintBeWithStatusChanged(isConstraint: Boolean) {}
    override fun onDetectionStateChanged(state: Int) {}
    override fun onAsrResult(asrResult: String, sttLanguage: SttLanguage) {}
    override fun onTelepresenceEventChanged(callEventModel: CallEventModel) {}
    override fun onRequestPermissionResult(permission: Permission, grantResult: Int, requestCode: Int) {}
    override fun onDistanceToLocationChanged(distances: Map<String, Float>) {}
    override fun onCurrentPositionChanged(position: Position) {
        runBlocking {
            positionMutex.withLock {
                temiPosition = position
            }
        }
    }
    override fun onSequencePlayStatusChanged(status: Int) {}
    override fun onRobotLifted(isLifted: Boolean, reason: String) {}

    private fun startDetectionWithDistance() {
        hideKeyboard()
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

    override fun onDetectionDataChanged(detectionData: DetectionData) {

        val angle = detectionData.angle // angle from  the camera to the detected person
        val distance = detectionData.distance // Distance to the closest detected person
        val isDetected = detectionData.isDetected // Boolean flag to check if a person is being detected
        Log.d("detection", angle.toString() +" "+distance.toString()+" "+isDetected.toString())

        val temiHumanDetectionJson = gson.toJson(TemiHumanDetection(angle, distance, isDetected))
        ws_hd.getWebSocket().sendText(temiHumanDetectionJson)
    }
    override fun onFaceRecognized(contactModelList: List<ContactModel>) {}
    override fun onConversationStatusChanged(status: Int, text: String) {}
    override fun onTtsVisualizerWaveFormDataChanged(waveForm: ByteArray) {}
    override fun onTtsVisualizerFftDataChanged(fft: ByteArray) {}
    override fun onReposeStatusChanged(status: Int, description: String) {}
    override fun onLoadMapStatusChanged(status: Int, requestId: String) {}
    override fun onDisabledFeatureListUpdated(disabledFeatureList: List<String>) {}
    override fun onMovementVelocityChanged(velocity: Float) {}
    override fun onMovementStatusChanged(type: String, status: String) {
        val templist = mutableListOf<String>()
        val jsonString = gson.toJson(TemiStatus(3,status, templist))
        ws_turnby.getWebSocket().sendText(jsonString)

    }
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