package com.airei.milltracking.mypalm.mqtt.lrc

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StrictMode
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CommandData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.StatusData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.statusDataSample
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ActivityMainBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_LR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_STR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttHandler
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttMessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.utils.hideKeyboard
import com.airei.milltracking.mypalm.mqtt.lrc.utils.setStatusBar
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.android.material.button.MaterialButton
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
        if (isActivityLaunched()) {
            return
        }
        setupView()
        setupDebugMode()
        observeViewModel()
        updateCommend()
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

        navController =
            (supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment).navController
        navController.addOnDestinationChangedListener(destinationChangedListener)

        val data : StatusData = Gson().fromJson(statusDataSample, StatusData::class.java)
        viewModel.statusData.postValue(data)

        // Set default selection to "Home"
        selectButton(binding.btnHome)

        // Set up click listeners for each button
        binding.btnHome.setOnClickListener {
            selectButton(binding.btnHome)
            nextFragment(R.id.homeFragment)
        }
        binding.btnFfb.setOnClickListener {
            selectButton(binding.btnFfb)
            nextFragment(R.id.ffbConveyorFragment)
        }
        binding.btnSfb.setOnClickListener {
            selectButton(binding.btnSfb)
            nextFragment(R.id.sfbConveyorFragment)
        }
        binding.btnAutoFeeding.setOnClickListener {
            selectButton(binding.btnAutoFeeding)
            nextFragment(R.id.autoFeedingFragment)
        }
        binding.btnConfig.setOnClickListener {
            selectButton(binding.btnConfig)
            nextFragment(R.id.mqttConfigFragment)
        }
    }

    private fun nextFragment(navId: Int) {
        navController.navigate(navId)
    }

    private fun selectButton(selectedButton: MaterialButton) {
        val buttons = listOf(
            binding.btnHome,
            binding.btnFfb,
            binding.btnSfb,
            binding.btnAutoFeeding,
            binding.btnConfig
        )

        // Loop through all buttons and apply styles
        buttons.forEach { button ->
            if (button == selectedButton) {
                // Set background tint and text for selected button
                button.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.ic_launcher_background)
                button.text = when (button) {
                    binding.btnHome ->getString(R.string.home)
                    binding.btnFfb -> getString(R.string.ffb_conveyor)
                    binding.btnSfb -> getString(R.string.sfb_conveyor)
                    binding.btnAutoFeeding -> getString(R.string.auto_feeding)
                    binding.btnConfig -> getString(R.string.config)
                    else -> ""
                }
            } else {
                // Set background transparent and remove text for other buttons
                button.backgroundTintList =
                    ContextCompat.getColorStateList(this, android.R.color.transparent)
                button.text = ""
            }
        }
    }


    private fun setupDebugMode() {
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
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
            if (!it.isNullOrEmpty()) {
                publishMessage(topic = MQTT_PUBLISH_TOPIC_LR, message = it)
            }
        }
        viewModel.updateStarter.observe(this) {
            if (!it.isNullOrEmpty()) {
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

    @SuppressLint("RestrictedApi")
    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            val destinationFragment = destination.label
            //Log.d(TAG, "destinationChangedListener destinationFragment: $destinationFragment")
            setBottomView(destinationFragment)
            runOnUiThread {
                when (destinationFragment) {
                    navController.findDestination(R.id.mqttConfigFragment)?.label -> {
                        binding.bnNavigation.visibility = View.GONE
                    }

                    else -> {
                        binding.bnNavigation.visibility = View.VISIBLE
                    }
                }
            }
        }

    private fun setBottomView(destinationFragment: CharSequence?) {
        when (destinationFragment) {
            "fragment_home" -> {
                selectButton(binding.btnHome)
            }

            "fragment_ffb_conveyor" -> {
                selectButton(binding.btnFfb)
            }

            "fragment_sfb_conveyor" -> {
                selectButton(binding.btnSfb)
            }

            "fragment_auto_feeding" -> {
                selectButton(binding.btnAutoFeeding)
            }

            "fragment_mqtt_config" -> {
                selectButton(binding.btnConfig)
            }
        }
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

    fun updateCommend(){
        val cmdData : CommandData = Gson().fromJson(AppPreferences.cmdJson, CommandData::class.java)
        viewModel.commendData.postValue(cmdData)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    view.clearFocus()
                    hideKeyboard(activity = this )
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

}
