package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AppBroadcastReceiver(private val listener: BroadcastListener) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //Toast.makeText(context, "Intent Detected.", Toast.LENGTH_LONG).show()
        listener.onBroadcastReceived()
    }
}

interface BroadcastListener {
    fun onBroadcastReceived()
}