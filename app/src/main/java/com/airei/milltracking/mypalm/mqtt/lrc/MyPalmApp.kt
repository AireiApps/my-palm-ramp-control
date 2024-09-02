package com.airei.milltracking.mypalm.mqtt.lrc

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyPalmApp : Application() {
    
    companion object {
        lateinit var instance: MyPalmApp
            private set
        private const val TAG = "MyPalmIOTApp"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        instance = this
        AppPreferences.init(this)
    }
}