package com.airei.milltracking.mypalm.mqtt.lrc.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
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

fun hideKeyboard(activity: Activity) {
    // Get the current focused view
    val view = activity.currentFocus
    if (view != null) {
        // Clear focus from the view
        //view.clearFocus()

        // Hide the keyboard
        val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    } else {
        // If no view is focused, try to hide the keyboard anyway
        val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }
}

fun Window.hideKeyboard() {
    try {
        (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        if ((context as Activity).currentFocus != null && (context as Activity).currentFocus!!
                .windowToken != null
        ) {
            currentFocus?.clearFocus()
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                (context as Activity).currentFocus!!.windowToken, 0
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
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
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    // Fallback for older versions
    decorView.systemUiVisibility =
        decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and
                View.SYSTEM_UI_FLAG_FULLSCREEN.inv()
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



