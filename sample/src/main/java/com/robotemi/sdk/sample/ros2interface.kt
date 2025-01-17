package com.robotemi.sdk.sample

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import com.google.gson.Gson
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.sample.jsonmsgs.Location
import com.robotemi.sdk.sample.jsonmsgs.TemiCommand
import com.robotemi.sdk.sample.jsonmsgs.TemiCurrentAction
import com.robotemi.sdk.sample.jsonmsgs.TemiHumanDetection
import com.robotemi.sdk.sample.jsonmsgs.TemiMap
import com.robotemi.sdk.sample.jsonmsgs.TemiState
import com.robotemi.sdk.sample.jsonmsgs.TemiStatus
import com.robotemi.sdk.sample.jsonmsgs.TemiTree
import com.robotemi.sdk.sample.utils.WebSocketCom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.sql.Timestamp

object ros2interface {

    // Websocket client
    var ws_getloc: WebSocketCom
    var ws_speak: WebSocketCom
    var ws_show: WebSocketCom
    var ws_goto: WebSocketCom
    var ws_turnby: WebSocketCom
    var ws_tiltangle: WebSocketCom
    var ws_follow: WebSocketCom
    var ws_tree: WebSocketCom
    var ws_hd: WebSocketCom
    var ws_rosbridge: WebSocketCom
    var ws_mapserver: WebSocketCom
    var ws_loggerserver: WebSocketCom

    val gson = Gson()

    private val mutex = Mutex()
    private val positionMutex = Mutex()
    private val mapMutex = Mutex()
    private val loggerMutex = Mutex()
    private var temiPosition: Position = Position(0F, 0F, 0F, 0)
    private var temiState: TemiState = TemiState("", 0, false, "")
    private val positionExecutor = Executors.newSingleThreadScheduledExecutor()
    private val mapExecutor = Executors.newSingleThreadScheduledExecutor()
    private val loggerExecutor = Executors.newSingleThreadScheduledExecutor()
    private var future_map: ScheduledFuture<*>
    private var future_position: ScheduledFuture<*>
    private var future_logger: ScheduledFuture<*>
    private val serverIP = "10.42.0.1"; // Control the temi robot from pc
//    private val serverIP = "10.0.0.1"; // Control the temi robot from edge device


    init {
        // setting up requied scopes for synchronous tasks
        val positionScope = CoroutineScope(Dispatchers.IO) // Assuming IO-bound tasks
        val mapScope = CoroutineScope(Dispatchers.IO)
        val loggerScope = CoroutineScope(Dispatchers.IO)

        // Initializing the temistate obeject to default initial values
        val timeStamp = Timestamp(System.currentTimeMillis())
        temiState.timestamp = timeStamp.toString()
        temiState.action = "none"


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

                        if(!commandObj.flag3)
                        {
                            speak_cmd(commandObj)
                        }
                        else
                        {
                            Temi.robot.cancelAllTtsRequests()
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

        ws_tree = object : WebSocketCom("ws://$serverIP:8766", 5000) {
            override fun onCommand(msg:String) { }
        }

        ws_hd =  object : WebSocketCom("ws://$serverIP:8767", 5000) {
            override fun onCommand(msg:String) { }
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
            override fun onCommand(msg:String) { }
        }

        ws_loggerserver = object : WebSocketCom("ws://$serverIP:8770", 5000) {
            override fun onCommand(msg:String) { }
        }

        ws_show = object : WebSocketCom("ws://$serverIP:8771", 5000) {
            override fun onCommand(msg:String) {
                GlobalScope.launch(Dispatchers.IO) {
                    mutex.withLock {
                        val commandObj = Gson().fromJson(msg, TemiCommand::class.java)
                        Log.d("WebSocket", "Command Show Received")
                        // Implement Switch stamente for each Robot Capability

                        if(commandObj.command == 11)
                        {
                            show_cmd(commandObj)
                        }
//                        show_cmd(commandObj)

                    }
                }
            }
        }

        future_position = positionExecutor.scheduleAtFixedRate({
            positionScope.launch {
                positionMutex.withLock {
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
                    val currentMap = Temi.robot.getMapData()
                    val mapLocations = mutableListOf<Location>()

                    for (location in currentMap!!.locations) {
                        mapLocations.add(Location(name = location.layerId, x = location.layerPoses!![0].x, y = location.layerPoses!![0].y, theta = location.layerPoses!![0].theta))
                    }

                    val temiMapJson = gson.toJson(
                        TemiMap(currentMap!!.mapInfo.height, currentMap.mapInfo.width,
                            currentMap.mapInfo.originX,  currentMap.mapInfo.originY,
                            currentMap.mapInfo.resolution, currentMap.mapImage.data, mapLocations)
                    )
                    ws_mapserver.getWebSocket().sendText(temiMapJson)
                }
            }
        }, 0, 1, TimeUnit.SECONDS)

        future_logger = loggerExecutor.scheduleAtFixedRate({
            loggerScope.launch {
                loggerMutex.withLock {

                    val timeStamp = Timestamp(System.currentTimeMillis())
                    temiState.timestamp = timeStamp.toString()
                    temiState.soc = Temi.robot.batteryData!!.level
                    temiState.isCharging = Temi.robot.batteryData!!.isCharging
                    temiState.action = TemiCurrentAction.name

                    // Get current robot pose and send it to the temi_bridge
                    val temiStateJson = gson.toJson(temiState)
//                    Log.d("Periodic", temiStateJson)

                    ws_loggerserver.getWebSocket().sendText(temiStateJson)
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS)

    }

    fun getLocSendStatus(status: TemiStatus)
    {
        val jsonString = gson.toJson(status)
        ws_getloc.getWebSocket().sendText(jsonString)
    }

    fun tiltAngleSendStatus(status: TemiStatus){
        val jsonString = gson.toJson(status)
        ws_tiltangle.getWebSocket().sendText(jsonString)
    }

    fun speakSendStatus(status: TemiStatus)
    {
        val jsonString = gson.toJson(status)
        ws_speak.getWebSocket().sendText(jsonString)
    }

    fun showSendStatus(status: TemiStatus)
    {
        val jsonString = gson.toJson(status)
        ws_show.getWebSocket().sendText(jsonString)
    }

    fun goToSendStatus(status: TemiStatus)
    {
        val jsonString = gson.toJson(status)
        ws_goto.getWebSocket().sendText(jsonString)
    }


    fun updatePosition(position: Position)
    {
        runBlocking {
            positionMutex.withLock {
                temiPosition = position
            }
        }
    }

    fun followSendStatus(status: TemiStatus)
    {
        val jsonString = gson.toJson(status)
        ws_follow.getWebSocket().sendText(jsonString)
    }

    fun sendHumanDetection(humanDetection: TemiHumanDetection)
    {
        val temiHumanDetectionJson = gson.toJson(humanDetection)
        ws_hd.getWebSocket().sendText(temiHumanDetectionJson)
    }
    fun movementSendStatus(status: TemiStatus)
    {
        val jsonString = gson.toJson(status)
        ws_turnby.getWebSocket().sendText(jsonString)
    }

    fun treeSelect(treeSel: TemiTree)
    {
        val temiTree = gson.toJson(treeSel)
        ws_tree.getWebSocket().sendText(temiTree)
    }

    fun onDestroy()
    {
        // Stop web sockets
        ws_speak.close()
        ws_getloc.close()
        ws_show.close()
        ws_goto.close()
        ws_turnby.close()
        ws_tiltangle.close()
        ws_follow.close()
        ws_hd.close()
        ws_tree.close()

        ws_rosbridge.close()
        ws_mapserver.close()
        ws_loggerserver.close()

        future_position.cancel(true)
        future_map.cancel(true)
        future_logger.cancel(true)
        positionExecutor.shutdown()
        mapExecutor.shutdown()
        loggerExecutor.shutdown()
    }

    private fun follow_cmd(cmd: TemiCommand) {
        Temi.robot.beWithMe()
    }

    private fun stop_cmd(cmd: TemiCommand)
    {
        Temi.robot.stopMovement()
    }

    private fun speak_cmd(cmd: TemiCommand) {
        Temi.robot.speak(TtsRequest.create(cmd.text!!, language = TtsRequest.Language.DE_DE, isShowOnConversationLayer = false))
    }
    private fun goto_cmd(cmd: TemiCommand) {
        Temi.robot.goTo(cmd.text!!, backwards = cmd.flag1, noBypass = false, SpeedLevel.SLOW)
    }
    private fun gotopos_cmd(cmd: TemiCommand) {
        val position: Position = Position(x = cmd.x!!, y = cmd.y!!, yaw = cmd.angle!!)
        Temi.robot.goToPosition( position, backwards = cmd.flag1, noBypass = false, speedLevel = SpeedLevel.SLOW)
    }
    private fun joy_cmd(cmd: TemiCommand) {
        Temi.robot.skidJoy(cmd.x!!,cmd.y!!, true)
    }
    private fun turnby_cmd(cmd: TemiCommand) {
        Temi.robot.turnBy(cmd.angle!!.toInt(), 1.0F)
    }

    private fun show_cmd(cmd: TemiCommand) {

        showSendStatus(TemiStatus(11, "started", mutableListOf<String>()))

        val baseActivity = MyApplication.getCurrentActivity()

        val baseActivityName = baseActivity!!.javaClass.simpleName

        Log.d("ACTIVITY_1", baseActivityName)

        // First go to the show message Activity to Display something
        val showActivityIntent = Intent(baseActivity, ShowActivity::class.java)
        showActivityIntent.putExtra("SHOW_MESSAGE", cmd.text)
        baseActivity.startActivity(showActivityIntent)

        for (i in 0 until cmd.x!!.toInt()) {
            Thread.sleep(1000L)
            showSendStatus(TemiStatus(11, "pending", mutableListOf<String>()))
        }

        // Then go back to display what was previously displayed
        val showActivity = MyApplication.getCurrentActivity()
        val baseActivityIntent = Intent(showActivity, baseActivity.javaClass)
        baseActivityIntent.putExtra("should_speak", false)
        if (baseActivityName == "GuideActivity")
        {
            baseActivityIntent.putExtra("locations", Temi.robot.locations.toTypedArray())
        }

        showActivity!!.startActivity(baseActivityIntent)
        showSendStatus(TemiStatus(11, "complete", mutableListOf<String>()))

    }

    private fun getloc_cmd(cmd: TemiCommand) {
        val locations = Temi.robot.locations.toMutableList()
        // The getloc Action is Labeled as 0
        val statusList = listOf("pending", "started", "processing", "complete")
        for (statusString in statusList) {
            getLocSendStatus(TemiStatus(0,statusString, locations))
            Thread.sleep(100)
        }
    }
    private fun tiltangle_cmd(cmd: TemiCommand) {
        Temi.robot.tiltAngle(cmd.angle!!.toInt().coerceIn(-25, 55), 0.7F)
        val statusList = listOf("pending", "started", "processing", "complete")
        val templist = mutableListOf<String>()
        for (statusString in statusList) {
            tiltAngleSendStatus(TemiStatus(4,statusString, templist))
            Thread.sleep(50)
        }
    }

}