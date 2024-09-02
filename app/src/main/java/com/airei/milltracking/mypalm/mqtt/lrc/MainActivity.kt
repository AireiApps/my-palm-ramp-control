package com.airei.milltracking.mypalm.mqtt.lrc

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ActivityMainBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttHandler
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttMessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.utils.setStatusBar
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.UUID

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MqttMessageListener {

    private val topic = "cmd/minsawi"

    private var mqttHandler: MqttHandler? = null

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.setStatusBar()
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            // Code to execute in debug mode
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }


        navController =
            (supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment).navController

        if (!AppPreferences.mqttConfig.isNullOrEmpty()) {
            navController.navigate(R.id.homeFragment)
        } else {
            navController.navigate(R.id.mqttConfigFragment)
        }

        //setMqttService()
        dataObserve()
    }

    private fun dataObserve() {
        viewModel.publishedMsg.observe(this) {
            if (it != null) {
                updateDoorStatus(it.first, it.second)
            }
        }
    }

    fun setMqttService() {
        var clientId = AppPreferences.mqttClientId
        if (clientId.isNullOrEmpty()) {
            AppPreferences.mqttClientId = UUID.randomUUID().toString()
            clientId = AppPreferences.mqttClientId
        }
        try {
            val config: MqttConfig =
                Gson().fromJson(AppPreferences.mqttConfig, MqttConfig::class.java)
            mqttHandler = MqttHandler()
            mqttHandler!!.setListener(this@MainActivity)
            if (clientId != null) {
                mqttHandler!!.connect(
                    "tcp://${config.host}:${config.port}",
                    clientId,
                    config.username,
                    config.password
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "setMqttService: ", e)
        }
    }


    private fun updateDoorStatus(b: Boolean, selectDoor: DoorData) {
        val data = WData(
            w = listOf(
                TagData(
                    tag = selectDoor!!.doorId,
                    value = if (b) 1 else 0
                )
            )
        )
        val jsonString = Gson().toJson(data)
        publishMessage(topic, jsonString)
    }

    private fun publishMessage(topic: String, jsonString: String?) {
        if (jsonString != null) {
            mqttHandler!!.publish(topic, jsonString, 0)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mqttHandler?.disconnect()
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onConnection(isConnect: Boolean) {
        Log.i(TAG, "onConnection: is connect $isConnect")
        runOnUiThread {
            if (isConnect) {
                Toast.makeText(this@MainActivity, "Mqtt Connected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Mqtt Connection Failed ", Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    override fun onReceiveMessage(topic: String, message: String) {
        Log.i(TAG, "onReceiveMessage: ${topic}: $message")
    }

    override fun onDeliveryComplete(id: Int, message: MqttMessage, complete: Boolean) {
        Log.i(TAG, "onDeliveryComplete: id $id: ${message.toString()}")
        runOnUiThread {
            if (complete) {
                Toast.makeText(this@MainActivity, "Delivery Complete", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Delivery Failed ", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun isConnectionLost(error: Throwable) {
        Log.e(TAG, "isConnectionLost: ", error)
    }

}