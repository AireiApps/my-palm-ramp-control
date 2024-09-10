package com.airei.milltracking.mypalm.mqtt.lrc

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ActivityMainBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_LR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_STR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttHandler
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttMessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.utils.setStatusBar
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttMessage


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MqttMessageListener {

    private var mqttHandler: MqttHandler? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: AppViewModel by viewModels()

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isActivityLaunched()) { return }
        setupView()
        setupDebugMode()
        observeViewModel()
        Log.i(TAG, "onCreate: ")
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Handler(Looper.getMainLooper()).postDelayed({ setMqttService() }, 200)
            navigateBasedOnMqttConfig()
        }
    }

    private fun isActivityLaunched(): Boolean {
        return if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            finish()
            true
        } else {
            false
        }
    }

    private fun setupView() {
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

        navController = (supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment).navController
    }

    private fun setupDebugMode() {
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }
    }

    private fun navigateBasedOnMqttConfig() {
        if (!AppPreferences.mqttConfig.isNullOrEmpty() && !AppPreferences.mqttClientId.isNullOrEmpty()) {
            navController.navigate(R.id.homeFragment)
        } else {
            navController.navigate(R.id.mqttConfigFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.updateDoor.observe(this) {
            if (!it.isNullOrEmpty()){
                publishMessage(topic = MQTT_PUBLISH_TOPIC_LR, message = it)
            }
        }
        viewModel.updateStarter.observe(this) {
            if (!it.isNullOrEmpty()){
                publishMessage(topic = MQTT_PUBLISH_TOPIC_STR, message = it)
            }
        }

        viewModel.startMqtt.observe(this) {
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({ setMqttService() }, 300)
                viewModel.startMqtt.postValue(false)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setMqttService() {
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d(TAG, "setMqttService: Starting MQTT Service")
            mqttHandler = MqttHandler().apply { setListener(this@MainActivity) }
            val clientId = AppPreferences.mqttClientId

            try {
                val config = Gson().fromJson(AppPreferences.mqttConfig, MqttConfig::class.java)
                clientId?.let {
                    mqttHandler?.connect(
                        "tcp://${config.host}:${config.port}",
                        it,
                        config.username,
                        config.password
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "setMqttService: Error connecting to MQTT", e)
            }
        }
    }

    private fun publishMessage(topic: String, message: String) {
        mqttHandler?.publish(topic, message, 0)
    }

    override fun onResume() {
        super.onResume()
        acquireWakeLock()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        mqttHandler?.disconnect()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MyApp::MyWakeLockTag"
        ).apply { acquire(10 * 60 * 1000L /* 10 minutes */) }
    }

    private fun releaseWakeLock() {
        wakeLock?.release()
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    // MQTT Callbacks
    override fun onConnection(isConnect: Boolean) {
        Log.i(TAG, "onConnection: isConnect = $isConnect")
        runOnUiThread {
            val message = if (isConnect) "Mqtt Connected" else "Mqtt Connection Failed"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onReceiveMessage(topic: String, message: String) {
        Log.i(TAG, "onReceiveMessage: $topic: $message")
    }

    override fun onDeliveryComplete(id: Int, message: MqttMessage, complete: Boolean) {
        Log.i(TAG, "onDeliveryComplete: id = $id: message = ${message.toString()}")
        runOnUiThread {
            val status = if (complete) "Delivery Complete" else "Delivery Failed"
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        }
    }

    override fun isConnectionLost(error: Throwable) {
        Log.e(TAG, "isConnectionLost: ", error)
    }

    fun mqttConnectionCheck(): Boolean {
        return mqttHandler?.isConnected() ?: false
    }
}
