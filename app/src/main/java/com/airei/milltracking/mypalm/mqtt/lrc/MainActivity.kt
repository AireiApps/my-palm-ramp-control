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
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ActivityMainBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttHandler
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttMessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.utils.setStatusBar
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.concurrent.atomic.AtomicInteger


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MqttMessageListener {

    private val TOPIC = "cmd/minsawi"

    private var mqttHandler: MqttHandler? = null

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    private val viewModel: AppViewModel by viewModels()

    private var wakeLock: PowerManager.WakeLock? = null

    private var activitiesLaunched: AtomicInteger = AtomicInteger(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (activitiesLaunched.incrementAndGet() > 1) { finish(); }
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
        Log.d("MainActivity", "onCreate:")
        // Your initialization code here
        val orientation = resources.configuration.orientation
        navController = (supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment).navController
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Handler(Looper.getMainLooper()).postDelayed({
                setMqttService()
            }, 200)
            if (!AppPreferences.mqttConfig.isNullOrEmpty() && !AppPreferences.mqttClientId.isNullOrEmpty()) {
                navController.navigate(R.id.homeFragment)
            } else {
                navController.navigate(R.id.mqttConfigFragment)
            }
        }
        dataObserve()

    }

    private fun dataObserve() {
        viewModel.updateDoor.observe(this) {
            if (!it.isNullOrEmpty()) {
                updateDoorStatus(it)
            }
        }
        viewModel.updateStarter.observe(this) {
            if (!it.isNullOrEmpty()) {
                updateDoorStatus(it)
            }
        }

        viewModel.startMqtt.observe(this) {
            if (it){
                Handler(Looper.getMainLooper()).postDelayed({
                    setMqttService()
                }, 300)

                viewModel.startMqtt.postValue(false)
            }
        }

    }



    @OptIn(DelicateCoroutinesApi::class)
    fun setMqttService() {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d(TAG, "setMqttService: ---------------> start")
            mqttHandler = MqttHandler()
            mqttHandler!!.setListener(this@MainActivity)
            val clientId = AppPreferences.mqttClientId

            try {
                val config: MqttConfig =
                    Gson().fromJson(AppPreferences.mqttConfig, MqttConfig::class.java)

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
    }

    private fun updateDoorStatus(msg: String) {
        publishMessage(TOPIC, msg)
    }

    fun mqttConnectionCheck(): Boolean {
        return mqttHandler!!.isConnected()
    }

    private fun publishMessage(topic: String = TOPIC, jsonString: String?) {
        if (jsonString != null) {
            mqttHandler!!.publish(topic, jsonString, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::MyWakeLockTag")
        wakeLock?.acquire(24*24*60*1000L /*10 minutes*/)
    }

    override fun onDestroy() {
        activitiesLaunched.getAndDecrement();
        super.onDestroy()
        if (wakeLock != null){
            wakeLock?.release()
        }
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