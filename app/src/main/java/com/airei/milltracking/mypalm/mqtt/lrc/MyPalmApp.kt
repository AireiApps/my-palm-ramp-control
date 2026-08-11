package com.airei.milltracking.mypalm.mqtt.lrc

import android.app.Application
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyPalmApp : Application() {
    
    companion object {
        lateinit var instance: MyPalmApp
            private set
        private const val TAG = "MyPalmIOTApp"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppPreferences.init(this)
        AppLogger.init(this)
    }
}