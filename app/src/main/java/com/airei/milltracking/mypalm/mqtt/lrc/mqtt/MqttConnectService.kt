package com.airei.milltracking.mypalm.mqtt.lrc.mqtt

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.airei.milltracking.mypalm.mqtt.lrc.utils.BROADCAST_MAG
import com.airei.milltracking.mypalm.mqtt.lrc.utils.BROADCAST_TOPIC
import com.airei.milltracking.mypalm.mqtt.lrc.utils.ACTION_BROADCAST_MQTT_CONN

class MqttConnectService : Service() {

    private val TAG = "MqttConnectService"

    private lateinit var handler :Handler
    private lateinit var taskRunnable: Runnable

    private var mqttHandler: MqttHandler? = null


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        handler = Handler(mainLooper)
        taskRunnable = object : Runnable {
            override fun run() {
                performBackgroundTask()
                // Repeat the task every 30 seconds (30000ms)
                handler.postDelayed(this, 30000)
            }
        }
        handler.postDelayed(taskRunnable,10000)
        // Return START_STICKY if you want the service to be restarted if killed by the system
        return START_STICKY
    }

    private fun performBackgroundTask() {
        Log.d(TAG, "Background task running")
        sendLocalBroadcast()
    }

    private fun sendLocalBroadcast() {
        val intent = Intent(ACTION_BROADCAST_MQTT_CONN)
        intent.putExtra(BROADCAST_TOPIC, BROADCAST_MAG)
        sendBroadcast(intent) // Send the broadcast
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}