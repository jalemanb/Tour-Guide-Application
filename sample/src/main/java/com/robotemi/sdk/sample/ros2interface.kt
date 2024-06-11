package com.robotemi.sdk.sample

import android.util.Log
import com.google.gson.Gson
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.navigation.model.Position
import com.robotemi.sdk.navigation.model.SpeedLevel
import com.robotemi.sdk.sample.jsonmsgs.TemiCommand
import com.robotemi.sdk.sample.jsonmsgs.TemiHumanDetection
import com.robotemi.sdk.sample.jsonmsgs.TemiMap
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking


class ros2interface(serverIP: String, bot: Robot) {

    lateinit var robot: Robot

    // Websocket client
    lateinit var ws_getloc: WebSocketCom
    lateinit var ws_speak: WebSocketCom
    lateinit var ws_goto: WebSocketCom
    lateinit var ws_turnby: WebSocketCom
    lateinit var ws_tiltangle: WebSocketCom
    lateinit var ws_follow: WebSocketCom
    lateinit var ws_tree: WebSocketCom
    lateinit var ws_hd: WebSocketCom
    lateinit var ws_rosbridge: WebSocketCom
    lateinit var ws_mapserver: WebSocketCom

    val gson = Gson()

    private val mutex = Mutex()
    private val positionMutex = Mutex()
    private val mapMutex = Mutex()
    private var temiPosition: Position = Position(0F, 0F, 0F, 0)
    private val positionExecutor = Executors.newSingleThreadScheduledExecutor()
    private val mapExecutor = Executors.newSingleThreadScheduledExecutor()
    private lateinit var future_map: ScheduledFuture<*>
    private lateinit var future_position: ScheduledFuture<*>

    init {

        robot = bot

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
                    val temiMapJson = gson.toJson(
                        TemiMap(currentMap!!.mapInfo.height, currentMap.mapInfo.width,
                            currentMap.mapInfo.originX,  currentMap.mapInfo.originY,
                            currentMap.mapInfo.resolution, currentMap.mapImage.data)
                    )
                    ws_mapserver.getWebSocket().sendText(temiMapJson)
                }
            }
        }, 0, 1, TimeUnit.SECONDS)
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
        ws_goto.close()
        ws_turnby.close()
        ws_tiltangle.close()
        ws_follow.close()
        ws_hd.close()
        ws_tree.close()

        ws_rosbridge.close()
        ws_mapserver.close()

        future_position.cancel(true)
        future_map.cancel(true)
        positionExecutor.shutdown()
    }



    private fun follow_cmd(cmd: TemiCommand) {
        robot.beWithMe()
    }

    private fun stop_cmd(cmd: TemiCommand)
    {
        robot.stopMovement()
    }
    private fun speak_cmd(cmd: TemiCommand) {
        robot.speak(TtsRequest.create(cmd.text!!, language = TtsRequest.Language.EN_US, isShowOnConversationLayer = false))
    }
    private fun goto_cmd(cmd: TemiCommand) {
        robot.goTo(cmd.text!!, backwards = false, noBypass = false, SpeedLevel.MEDIUM)
    }
    private fun gotopos_cmd(cmd: TemiCommand) {
        val position: Position = Position(x = cmd.x!!, y = cmd.y!!, yaw = cmd.angle!!)
        robot.goToPosition( position, backwards = cmd.flag1, noBypass = false, speedLevel = SpeedLevel.MEDIUM)
    }
    private fun joy_cmd(cmd: TemiCommand) {
        robot.skidJoy(cmd.x!!,cmd.y!!, false)
    }
    private fun turnby_cmd(cmd: TemiCommand) {
        robot.turnBy(cmd.angle!!.toInt(), 1.0F)
    }
    private fun getloc_cmd(cmd: TemiCommand) {
        val locations = robot.locations.toMutableList()
        // The getloc Action is Labeled as 0
        val statusList = listOf("pending", "started", "processing", "complete")
        for (statusString in statusList) {
            getLocSendStatus(TemiStatus(0,statusString, locations))
            Thread.sleep(100)
        }
    }

    private fun tiltangle_cmd(cmd: TemiCommand) {
        robot.tiltAngle(cmd.angle!!.toInt().coerceIn(-25, 55), 0.7F)
        val statusList = listOf("pending", "started", "processing", "complete")
        val templist = mutableListOf<String>()
        for (statusString in statusList) {
            tiltAngleSendStatus(TemiStatus(4,statusString, templist))
            Thread.sleep(50)
        }
    }

}