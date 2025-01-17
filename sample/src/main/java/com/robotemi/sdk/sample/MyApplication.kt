package com.robotemi.sdk.sample

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log

class MyApplication : Application() {

    companion object {
        private var currentActivity: Activity? = null
        private lateinit var instance: MyApplication

        fun getAppContext() = instance.applicationContext

        fun getCurrentActivity() = currentActivity
    }

    override fun onCreate() {
        super.onCreate()
        instance = this


        // Track activity lifecycle
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }
        })
    }
}
