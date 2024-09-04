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
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.RtspConfig
import com.airei.milltracking.mypalm.mqtt.lrc.roomdb.DoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.roomdb.Rtsp

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

// Extension functions for conversion between DoorTable and DoorData
fun DoorTable.toDoorData(): DoorData {
    return DoorData(
        doorId = this.doorId,
        doorName = this.doorName,
        openStatus = this.openStatus,
        selected = false, // Set to a default value or modify as needed
        rtspConfig = this.rtsp
    )
}

fun DoorData.toDoorTable(): DoorTable {
    return DoorTable(
        doorId = this.doorId,
        doorName = this.doorName,
        openStatus = this.openStatus,
        rtsp = this.rtspConfig
    )
}

// Extension functions for conversion between Rtsp and RtspConfig
fun Rtsp.toRtspConfig(): RtspConfig {
    return RtspConfig(
        channel = this.channel,
        subtype = this.subtype,
        ip = this.ip,
        username = this.username,
        password = this.password
    )
}

fun RtspConfig.toRtsp(): Rtsp {
    return Rtsp(
        channel = this.channel,
        subtype = this.subtype,
        ip = this.ip,
        username = this.username,
        password = this.password
    )
}
