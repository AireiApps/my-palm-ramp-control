package com.airei.milltracking.mypalm.mqtt.lrc

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AutoFeedingData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CommandData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbRunningStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.applyBounceAnimation
import com.airei.milltracking.mypalm.mqtt.lrc.commons.applyDismissAnimation
import com.airei.milltracking.mypalm.mqtt.lrc.commons.toStatusData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ActivityMainBinding
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.AlartFfbBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_AI
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_LR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_STR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_AUTO_FEED_1
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_AUTO_FEED_2
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_TOPIC_LR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttHandler
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttMessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.ui.HomeFragment
import com.airei.milltracking.mypalm.mqtt.lrc.ui.HomeFragment.Companion
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
    private lateinit var alertDialog: AlertDialog

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
            navController.navigate(R.id.splashFragment)
            //navigateBasedOnMqttConfig()
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

        //val data : StatusData = Gson().fromJson(statusDataSample, StatusData::class.java)
        //viewModel.statusData.postValue(data)

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
        binding.btnDoor.setOnClickListener {
            selectButton(binding.btnDoor)
            nextFragment(R.id.doorsFragment)
        }
        binding.btnConfig.setOnClickListener {
            selectButton(binding.btnConfig)
            binding.navHostFragment.visibility = View.INVISIBLE
            nextFragment(R.id.mqttConfigFragment)
            //binding.bnNavigation.visibility = View.GONE
        }
        binding.btnMqttStatus.setOnClickListener {
            if (mqttHandler?.isConnected() != true) {
                setMqttService()
            }
        }
    }

    private fun updateMqttButton(isConnected: Boolean, button: MaterialButton) {
        if (isConnected) {
            // Update to connected status
            button.text = button.context.getString(R.string.connect)
            button.setIconResource(R.drawable.ic_circle) // Set icon to circle
            button.setIconTintResource(R.color.japanese_laurel) // Icon color to japanese_laurel
            button.setTextColor(
                ContextCompat.getColor(
                    button.context,
                    R.color.japanese_laurel
                )
            ) // Text color
        } else {
            // Update to disconnected status
            button.text = button.context.getString(R.string.reconnect)
            button.setIconResource(R.drawable.ic_refresh) // Set icon to retry
            button.setIconTintResource(R.color.flamingo) // Icon color to flamingo
            button.setTextColor(
                ContextCompat.getColor(
                    button.context,
                    R.color.flamingo
                )
            ) // Text color
        }

        // Disable button click (clickable set to false)
        button.isClickable = !isConnected
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
            binding.btnConfig,
            binding.btnDoor
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
                    binding.btnDoor -> getString(R.string.available_doors)
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

            Log.i(TAG, "observeViewModel: ")

            if (!it.isNullOrEmpty()) {
                publishMessage(topic = MQTT_PUBLISH_TOPIC_LR, message = it)
            }
        }
        viewModel.updateStarter.observe(this) {

            Log.i(TAG, "observeViewModel: ")
            if (!it.isNullOrEmpty()) {
                publishMessage(topic = MQTT_PUBLISH_TOPIC_STR, message = it)
            }
        }
        viewModel.updateAiModeData.observe(this) {
            Log.i(TAG, "observeViewModel: ")
            if (!it.isNullOrEmpty()) {
                publishMessage(topic = MQTT_PUBLISH_AI, message = it)
            }
        }

        viewModel.startMqtt.observe(this) {
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({ setMqttService() }, 300)
                viewModel.startMqtt.postValue(false)
            }
        }

        viewModel.statusData.observe(this) {
            if (it != null) {
                val lastFfb = viewModel.ffbLastStatus.value
                val ffb = FfbRunningStatus(
                    ffb1Run = it.data.ffb1Run,
                    ffb2Run = it.data.ffb2Run,
                    ffb3Run = it.data.ffb3Run,
                    ffb4Run = it.data.ffb4Run,
                    ffb5Run = it.data.ffb5Run
                )

                viewModel.ffbLastStatus.postValue(ffb)

                if (lastFfb != null) {
                    if (lastFfb == ffb) {
                        //viewModel.ffbLastStatus.postValue(ffb)
                    } else {
                        val msgString: ArrayList<String> = arrayListOf()
                        if (ffb.ffb1Run == "1" && lastFfb.ffb1Run != "1") msgString.add("FFB1")
                        //if (ffb.ffb2Run == "1" && lastFfb.ffb2Run != "1") msgString.add("FFB2")
                        //if (ffb.ffb3Run == "1" && lastFfb.ffb3Run != "1") msgString.add("FFB3")
                        //if (ffb.ffb4Run == "1" && lastFfb.ffb4Run != "1") msgString.add("FFB4")
                        //if (ffb.ffb5Run == "1" && lastFfb.ffb5Run != "1") msgString.add("FFB5")
                        //viewModel.ffbLastStatus = ffb
                        Log.i(TAG, "showAlert: msgString = $msgString")
                        showAlert(msgString.joinToString(", "),(msgString.size != 1))
                    }
                } else {
                    //viewModel.ffbLastStatus = ffb
                    val msgString: ArrayList<String> = arrayListOf()
                    if (ffb.ffb1Run == "1") msgString.add("FFB1")
                    //if (ffb.ffb2Run == "1") msgString.add("FFB2")
                    //if (ffb.ffb3Run == "1") msgString.add("FFB3")
                    //if (ffb.ffb4Run == "1") msgString.add("FFB4")
                    //if (ffb.ffb5Run == "1") msgString.add("FFB5")
                    showAlert(msgString.joinToString(", "),(msgString.size != 1))
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setMqttService() {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
            Log.d(TAG, "setMqttService: Starting MQTT Service")
                if (mqttHandler != null) {
                    if (mqttHandler?.isConnected() == true) {
                        mqttHandler!!.disconnect()
                    }

                }

            mqttHandler = MqttHandler().apply { setListener(this@MainActivity) }
            val clientId = AppPreferences.mqttClientId

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
                    navController.findDestination(R.id.mqttConfigFragment)?.label
                    -> {
                        binding.bnNavigation.visibility = View.GONE
                        binding.navHostFragment.visibility = View.VISIBLE
                    }

                    navController.findDestination(R.id.splashFragment)?.label,
                    navController.findDestination(R.id.guideFragment)?.label
                    -> {
                        binding.bnNavigation.visibility = View.GONE
                    }

                    else -> {
                        binding.bnNavigation.visibility = View.VISIBLE
                    }
                }
            }
        }



    private fun showAlert(msgString: String, isMultiple: Boolean = false) {
        runOnUiThread {
            if (msgString.isNotEmpty()) {
                if (this::alertDialog.isInitialized) {
                    if (alertDialog.isShowing) {
                        alertDialog.dismiss()
                    }
                }

                val binding = AlartFfbBinding.inflate(LayoutInflater.from(this))
                val builder = AlertDialog.Builder(this)
                builder.setView(binding.root)
                alertDialog = builder.create()
                // Update the message based on whether it's single or multiple FFBs
                val message = if (isMultiple) {
                    getString(R.string.ffb_alert_msg_multiple).replace("FFB", msgString)
                } else {
                    getString(R.string.ffb_alert_msg_single).replace("FFB", msgString)
                }
                binding.tvMsg.text = message
                alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                alertDialog.setCanceledOnTouchOutside(false)

                binding.btnConfirm.setOnClickListener {
                    applyDismissAnimation(alertDialog.window?.decorView?.findViewById(android.R.id.content)) {
                        alertDialog.dismiss() // Dismiss after animation
                    }
                }

                alertDialog.show()

                // Apply the bounce-in animation
                val rootView = alertDialog.window?.decorView?.findViewById<View>(android.R.id.content)
                rootView?.let {
                    applyBounceAnimation(it)
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
        if (isConnect) {
            mqttHandler?.subscribe(MQTT_SUBSCRIBE_TOPIC_LR)
            mqttHandler?.subscribe(MQTT_SUBSCRIBE_AUTO_FEED_1)
            mqttHandler?.subscribe(MQTT_SUBSCRIBE_AUTO_FEED_2)
        }
        runOnUiThread {
            val message = if (isConnect) "Mqtt Connected" else "Mqtt Connection Failed"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            updateMqttButton(isConnect, binding.btnMqttStatus)
        }
    }

    override fun onReceiveMessage(topic: String, message: String) {
        Log.i(TAG, "onReceiveMessage: $topic: $message")
        when (topic) {

            MQTT_SUBSCRIBE_TOPIC_LR -> {
                try {
                    val statusData = message.toStatusData()
                    viewModel.statusData.postValue(statusData)
                } catch (e: Exception) {
                    viewModel.statusData.postValue(null)
                    Log.e(TAG, "onReceiveMessage: ", e)
                }

            }

            MQTT_SUBSCRIBE_AUTO_FEED_1 -> {
                try {
                    val autoFeedingData = Gson().fromJson(message, AutoFeedingData::class.java)
                    Log.d(TAG, "onReceiveMessage: AutoFeed1: $autoFeedingData ")
                    viewModel.autoFeedingData1.postValue(autoFeedingData)
                }catch (e:Exception){
                    viewModel.autoFeedingData1.postValue(null)
                    Log.e(TAG, "onReceiveMessage: ", e)
                }
            }

            MQTT_SUBSCRIBE_AUTO_FEED_2 -> {
                try {
                    val autoFeedingData = Gson().fromJson(message, AutoFeedingData::class.java)
                    Log.d(TAG, "onReceiveMessage: AutoFeed2: $autoFeedingData ")
                    viewModel.autoFeedingData2.postValue(autoFeedingData)
                }catch (e:Exception){
                    viewModel.autoFeedingData2.postValue(null)
                    Log.e(TAG, "onReceiveMessage: ", e)
                }
            }

            else -> {
                Log.i(TAG, "onReceiveMessage: $topic: $message")
            }

        }

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
