package com.robotemi.sdk.sample

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.robotemi.sdk.*
import com.robotemi.sdk.Robot.*
import com.robotemi.sdk.Robot.Companion.getInstance

object Temi {

    lateinit var robot: Robot

    init {
        robot = getInstance()
    }

}