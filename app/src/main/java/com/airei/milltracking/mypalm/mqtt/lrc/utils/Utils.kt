package com.airei.milltracking.mypalm.mqtt.lrc.utils

import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R

fun isOnline(): Boolean {
    try {
        val p1 =
            Runtime.getRuntime().exec("ping -c 1 www.google.com")
        val returnVal = p1.waitFor()
        return returnVal == 0
    } catch (e: Exception) {
        Log.e("isOnline", "isOnline: ", e)
    }
    return false
}

fun Window.setStatusBar(color: Int = R.color.black) {
    // Change status bar color
    statusBarColor = getColor(MyPalmApp.instance, color)

    // Change status bar text color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = WindowCompat.getInsetsController(this, decorView)
        controller.let {
            it.isAppearanceLightStatusBars = false
            it.isAppearanceLightNavigationBars = false
            //it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    } else
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
}