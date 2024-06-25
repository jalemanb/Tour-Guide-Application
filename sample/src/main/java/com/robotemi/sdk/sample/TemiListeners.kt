package com.robotemi.sdk.sample

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.annotation.CheckResult
import androidx.core.app.ActivityCompat
import com.robotemi.sdk.NlpResult
import com.robotemi.sdk.Robot
import com.robotemi.sdk.SttLanguage
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.activitystream.ActivityStreamPublishMessage
import com.robotemi.sdk.constants.SdkConstants
import com.robotemi.sdk.exception.OnSdkExceptionListener
import com.robotemi.sdk.exception.SdkException
import com.robotemi.sdk.face.ContactModel
import com.robotemi.sdk.face.OnContinuousFaceRecognizedListener
import com.robotemi.sdk.face.OnFaceRecognizedListener
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener
import com.robotemi.sdk.listeners.OnConstraintBeWithStatusChangedListener
import com.robotemi.sdk.listeners.OnConversationStatusChangedListener
import com.robotemi.sdk.listeners.OnDetectionDataChangedListener
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener
import com.robotemi.sdk.listeners.OnDisabledFeatureListUpdatedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnGreetModeStateChangedListener
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener
import com.robotemi.sdk.listeners.OnMovementVelocityChangedListener
import com.robotemi.sdk.listeners.OnRobotDragStateChangedListener
import com.robotemi.sdk.listeners.OnRobotLiftedListener
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.listeners.OnTelepresenceEventChangedListener
import com.robotemi.sdk.listeners.OnTtsVisualizerFftDataChangedListener
import com.robotemi.sdk.listeners.OnTtsVisualizerWaveFormDataChangedListener
import com.robotemi.sdk.listeners.OnUserInteractionChangedListener
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
import com.robotemi.sdk.sample.jsonmsgs.TemiHumanDetection
import com.robotemi.sdk.sample.jsonmsgs.TemiStatus
import com.robotemi.sdk.sequence.OnSequencePlayStatusChangedListener
import com.robotemi.sdk.voice.ITtsService

class TemiListeners : Service(), Robot.NlpListener, OnRobotReadyListener,
    Robot.ConversationViewAttachesListener, Robot.WakeupWordListener,
    Robot.ActivityStreamPublishListener,
    Robot.TtsListener, OnBeWithMeStatusChangedListener, OnGoToLocationStatusChangedListener,
    OnLocationsUpdatedListener, OnConstraintBeWithStatusChangedListener,
    OnDetectionStateChangedListener, Robot.AsrListener, OnTelepresenceEventChangedListener,
    OnRequestPermissionResultListener, OnDistanceToLocationChangedListener,
    OnCurrentPositionChangedListener, OnSequencePlayStatusChangedListener, OnRobotLiftedListener,
    OnDetectionDataChangedListener, OnUserInteractionChangedListener, OnFaceRecognizedListener,
    OnConversationStatusChangedListener, OnTtsVisualizerWaveFormDataChangedListener,
    OnTtsVisualizerFftDataChangedListener, OnReposeStatusChangedListener,
    OnLoadMapStatusChangedListener, OnDisabledFeatureListUpdatedListener,
    OnMovementVelocityChangedListener, OnMovementStatusChangedListener,
    OnContinuousFaceRecognizedListener, ITtsService, OnGreetModeStateChangedListener,
    TextToSpeech.OnInitListener, OnLoadFloorStatusChangedListener,
    OnDistanceToDestinationChangedListener, OnSdkExceptionListener,
    OnRobotDragStateChangedListener {

    private var tts: TextToSpeech? = null
    private var created = false

    fun isCreated(): Boolean
    {
        return created
    }

    override fun onCreate() {
        super.onCreate()

        // Robot Class Setup
        Temi.robot.addOnRequestPermissionResultListener(this)
        Temi.robot.addOnTelepresenceEventChangedListener(this)
        Temi.robot.addOnFaceRecognizedListener(this)
        Temi.robot.addOnContinuousFaceRecognizedListener(this)
        Temi.robot.addOnLoadMapStatusChangedListener(this)
        Temi.robot.addOnDisabledFeatureListUpdatedListener(this)
        Temi.robot.addOnSdkExceptionListener(this)
        Temi.robot.addOnMovementStatusChangedListener(this)
        Temi.robot.addOnGreetModeStateChangedListener(this)
        Temi.robot.addOnLoadFloorStatusChangedListener(this)
        startDetectionWithDistance()

        // Initialization Stuff
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            tts = TextToSpeech(this, this)
            Temi.robot.setTtsService(this)
        }

        created = true

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Temi.robot.addOnRobotReadyListener(this)
        Temi.robot.addNlpListener(this)
        Temi.robot.addOnBeWithMeStatusChangedListener(this)
        Temi.robot.addOnGoToLocationStatusChangedListener(this)
        Temi.robot.addConversationViewAttachesListener(this)
        Temi.robot.addWakeupWordListener(this)
        Temi.robot.addTtsListener(this)
        Temi.robot.addOnLocationsUpdatedListener(this)
        Temi.robot.addOnConstraintBeWithStatusChangedListener(this)
        Temi.robot.addOnDetectionStateChangedListener(this)
        Temi.robot.addAsrListener(this)
        Temi.robot.addOnDistanceToLocationChangedListener(this)
        Temi.robot.addOnCurrentPositionChangedListener(this)
        Temi.robot.addOnSequencePlayStatusChangedListener(this)
        Temi.robot.addOnRobotLiftedListener(this)
        Temi.robot.addOnDetectionDataChangedListener(this)
        Temi.robot.addOnUserInteractionChangedListener(this)
        Temi.robot.addOnConversationStatusChangedListener(this)
        Temi.robot.addOnTtsVisualizerWaveFormDataChangedListener(this)
        Temi.robot.addOnTtsVisualizerFftDataChangedListener(this)
        Temi.robot.addOnReposeStatusChangedListener(this)
        Temi.robot.addOnMovementVelocityChangedListener(this)
        Temi.robot.setActivityStreamPublishListener(this)
        Temi.robot.addOnDistanceToDestinationChangedListener(this)
        Temi.robot.addOnRobotDragStateChangedListener(this)
        Temi.robot.showTopBar()



        // Get General Permissions Permisssions

        return START_STICKY
    }


    override fun onDestroy() {

        Temi.robot.removeOnRequestPermissionResultListener(this)
        Temi.robot.removeOnTelepresenceEventChangedListener(this)
        Temi.robot.removeOnFaceRecognizedListener(this)
        Temi.robot.removeOnContinuousFaceRecognizedListener(this)
        Temi.robot.removeOnSdkExceptionListener(this)
        Temi.robot.removeOnLoadMapStatusChangedListener(this)
        Temi.robot.removeOnDisabledFeatureListUpdatedListener(this)
        Temi.robot.removeOnMovementStatusChangedListener(this)
        Temi.robot.removeOnGreetModeStateChangedListener(this)
        Temi.robot.removeOnLoadFloorStatusChangedListener(this)

        Temi.robot.removeOnRobotReadyListener(this)
        Temi.robot.removeNlpListener(this)
        Temi.robot.removeOnBeWithMeStatusChangedListener(this)
        Temi.robot.removeOnGoToLocationStatusChangedListener(this)
        Temi.robot.removeConversationViewAttachesListener(this)
        Temi.robot.removeWakeupWordListener(this)
        Temi.robot.removeTtsListener(this)
        Temi.robot.removeOnLocationsUpdateListener(this)
        Temi.robot.removeOnDetectionStateChangedListener(this)
        Temi.robot.removeAsrListener(this)
        Temi.robot.removeOnDistanceToLocationChangedListener(this)
        Temi.robot.removeOnCurrentPositionChangedListener(this)
        Temi.robot.removeOnSequencePlayStatusChangedListener(this)
        Temi.robot.removeOnRobotLiftedListener(this)
        Temi.robot.removeOnDetectionDataChangedListener(this)
        Temi.robot.addOnUserInteractionChangedListener(this)
        Temi.robot.stopMovement()
        if (Temi.robot.checkSelfPermission(Permission.FACE_RECOGNITION) == Permission.GRANTED) {
            Temi.robot.stopFaceRecognition()
        }
        Temi.robot.removeOnConversationStatusChangedListener(this)
        Temi.robot.removeOnTtsVisualizerWaveFormDataChangedListener(this)
        Temi.robot.removeOnTtsVisualizerFftDataChangedListener(this)
        Temi.robot.removeOnReposeStatusChangedListener(this)
        Temi.robot.removeOnMovementVelocityChangedListener(this)
        Temi.robot.setActivityStreamPublishListener(null)
        Temi.robot.removeOnDistanceToDestinationChangedListener(this)
        Temi.robot.removeOnRobotDragStateChangedListener(this)

        tts?.shutdown()
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        if (appInfo.metaData != null
            && appInfo.metaData.getBoolean(SdkConstants.METADATA_OVERRIDE_TTS, false)
        ) {
            tts = null
            Temi.robot.setTtsService(null)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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


    /**
     * Removing the event listeners upon leaving the app.
     */

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

        ros2interface.sendHumanDetection(
            TemiHumanDetection(detectionData.angle,
            detectionData.distance,
            detectionData.isDetected)
        )

        HumanDectection.angle = detectionData.angle.toFloat()
        HumanDectection.distance = detectionData.distance.toFloat()
        HumanDectection.is_detection = detectionData.isDetected
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
        var distanceStr = "1.7"
        if (distanceStr.isEmpty()) distanceStr = "0"
        try {
            val distance = distanceStr.toFloat()
            Temi.robot.setDetectionModeOn(true, distance)
            Log.d("detection", "Start detection mode with distance: $distance")
        } catch (e: Exception) {
            Log.d("detection", "startDetectionModeWithDistance")
        }
    }
    @CheckResult
    private fun requestPermissionIfNeeded(permission: Permission, requestCode: Int): Boolean {
        if (Temi.robot.checkSelfPermission(permission) == Permission.GRANTED) {
            return false
        }
        Temi.robot.requestPermissions(listOf(permission), requestCode)
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