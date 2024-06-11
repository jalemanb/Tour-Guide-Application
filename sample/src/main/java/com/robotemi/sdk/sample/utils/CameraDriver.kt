package com.robotemi.sdk.sample.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import androidx.core.app.ActivityCompat

class CameraDriver(private val context: Context, websocket: WebSocketCom) {

    private var ws: WebSocketCom
    private lateinit var contextExt: Context
    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequest: CaptureRequest
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var capReq: CaptureRequest.Builder
    private lateinit var imageReader: ImageReader

    init {
        this.ws = websocket
        this.contextExt = context
    }

    fun start_camera() {

        cameraManager = contextExt.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        imageReader = ImageReader.newInstance(480, 640, ImageFormat.JPEG, 1).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireNextImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                image.close()
                if (ws.isConnected())
                {
                    ws.getWebSocket().sendBinary(bytes) // Send image data over WebSocket
                }
            }, handler)
        }

        open_camera()
    }

    @SuppressLint("MissingPermission")
    private fun open_camera() {
        try {
            if(ActivityCompat.checkSelfPermission(contextExt, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                // Handle permission not granted situation
                return
            }
            cameraManager.openCamera(this.cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
                override fun onOpened(p0: CameraDevice) {
                    cameraDevice = p0
                    createCameraSession()
                }

                override fun onClosed(camera: CameraDevice) {
                }

                override fun onDisconnected(p0: CameraDevice) {
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                }
            }, handler)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    fun stopCamera() {
        cameraCaptureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        handlerThread?.quitSafely()
    }

    private fun createCameraSession() {
        try {
            capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            capReq.addTarget(imageReader.surface)

            cameraDevice.createCaptureSession(listOf(imageReader.surface), object: CameraCaptureSession.StateCallback(){
                override fun onConfigured(p0: CameraCaptureSession) {
                    cameraCaptureSession = p0
                    cameraCaptureSession.setRepeatingRequest(capReq.build(), null, handler)
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                }
            }, handler)

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
    }

}