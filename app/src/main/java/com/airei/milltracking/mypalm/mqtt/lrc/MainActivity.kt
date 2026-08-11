package com.airei.milltracking.mypalm.mqtt.lrc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AiStatusData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppBroadcastReceiver
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AutoFeedingData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CageFillData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.BroadcastListener
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CommandData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorStatusData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbRunningStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.HumanDetectionData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.PmcStatusData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.applyBounceAnimation
import com.airei.milltracking.mypalm.mqtt.lrc.commons.applyDismissAnimation
import com.airei.milltracking.mypalm.mqtt.lrc.commons.toStatusData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.ActivityMainBinding
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.AlartFfbBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_DOOR_SRUCK
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_AI
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_AI_NOTIFY
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_LR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PUBLISH_TOPIC_STR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_AI_NOTIFY
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_AI_STATUS
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_AUTO_FEED_1
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_AUTO_FEED_2
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_CAGE_FILL
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_HUMAN_DETECTION
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_TOPIC_LR
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_SUBSCRIBE_TOPIC_PMC
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttConnectService
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttHandler
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MqttMessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.ui.DoorsFragment.AvailableDoorsData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.ACTION_BROADCAST_MQTT_CONN
import com.airei.milltracking.mypalm.mqtt.lrc.utils.hideKeyboard
import com.airei.milltracking.mypalm.mqtt.lrc.utils.setStatusBar
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import org.eclipse.paho.client.mqttv3.MqttMessage

interface MessageListener {

    fun onReceiveMessage(
        topic: String, message: String
    )
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MqttMessageListener, BroadcastListener {

    private var mqttHandler: MqttHandler? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: AppViewModel by viewModels()

    private var wakeLock: PowerManager.WakeLock? = null
    private val tenDaysInMillis = 1 * 24 * 60 * 60 * 1000L
    private lateinit var alertDialog: AlertDialog

    private lateinit var serviceIntent: Intent
    private lateinit var broadcastReceiver: AppBroadcastReceiver

    private lateinit var timer: CountDownTimer
    private val startTimeInMillis: Long = 20000

    private var localHumanDetectionData: MutableList<String> = CopyOnWriteArrayList()

    private var stuckDoorDialog: AlertDialog? = null

    private var lastAiStatusMessageTime: Long = 0
    private val aiMonitoringHandler = Handler(Looper.getMainLooper())
    private val aiMonitoringRunnable = object : Runnable {
        override fun run() {
            checkAiStatusTimeout()
            aiMonitoringHandler.postDelayed(this, 10000) // Check every 10 seconds
        }
    }

    //----------------------
    private var messageListener: MessageListener? = null

    fun setMqttListener(
        listener: MessageListener
    ) {

        messageListener = listener
    }

    fun removeMqttListener() {

        messageListener = null
    }

    private fun mqttCallback(
        topic: String, message: String
    ) {
        messageListener?.onReceiveMessage(
            topic, message
        )
    }
    //-----------------------


    private var lastStatus: String = ""

    private fun showStoragePermissionDialog() {

        AlertDialog.Builder(this).setTitle("Storage Permission Required").setMessage(
            "This app needs storage permission to access files. Please allow permission."
        ).setCancelable(false)

            .setPositiveButton("Allow") { _, _ ->

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    // Android 11+
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        "package:$packageName".toUri()
                    )
                    startActivity(intent)

                } else {

                    // Android 10 and below
                    ActivityCompat.requestPermissions(
                        this, arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ), 1001
                    )
                }
            }

            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }

            .show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isActivityLaunched()) {
            return
        }

        if (!hasStoragePermission()) {
            showStoragePermissionDialog()
        }

        setupView()
        setupDebugMode()
        observeViewModel()
        updateCommend()

        serviceIntent = Intent(this, MqttConnectService::class.java)
        broadcastReceiver = AppBroadcastReceiver(this)

        AppLogger.log(TAG, "MainActivity onCreate")

        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            navController.navigate(R.id.splashFragment)
        }
    }

    private fun hasStoragePermission(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == 1001) {

            val granted = grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }

            if (granted) {
                recreate()
            } else {
                Toast.makeText(
                    this, "Storage permission denied", Toast.LENGTH_SHORT
                ).show()
            }
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
            nextFragment(R.id.homeFragment)
        }
        binding.btnStuckDoor.setOnClickListener {
            nextFragment(R.id.stuckDoorFragment)
        }
        binding.btnFfb.setOnClickListener {
            nextFragment(R.id.ffbConveyorFragment)
        }
        binding.btnSfb.setOnClickListener {
            nextFragment(R.id.sfbConveyorFragment)
        }
        binding.btnAutoFeeding.setOnClickListener {
            nextFragment(R.id.autoFeedingFragment)
        }
        binding.btnDoor.setOnClickListener {
            nextFragment(R.id.doorsFragment)
        }
        binding.btnConfig.setOnClickListener {
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
        val lastStatus = button.text.toString()
        runOnUiThread {
            if (isConnected && lastStatus != getString(R.string.connect)) {
                // Update to connected status
                button.text = getString(R.string.connect)
                button.setIconResource(R.drawable.ic_circle) // Set icon to circle
                button.setIconTintResource(R.color.japanese_laurel) // Icon color to japanese_laurel
                button.setTextColor(
                    ContextCompat.getColor(
                        button.context, R.color.japanese_laurel
                    )
                ) // Text color
            } else if (!isConnected && lastStatus != getString(R.string.reconnect)) {
                // Update to disconnected status
                button.text = getString(R.string.reconnect)
                button.setIconResource(R.drawable.ic_refresh) // Set icon to retry
                button.setIconTintResource(R.color.flamingo) // Icon color to flamingo
                button.setTextColor(
                    ContextCompat.getColor(
                        button.context, R.color.flamingo
                    )
                ) // Text color
            }
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
            binding.btnStuckDoor,
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
                    binding.btnHome -> getString(R.string.home)
                    binding.btnStuckDoor -> getString(R.string.stuck_door)
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
            binding.layoutTtb.visibility = View.GONE
        }

    }


    private fun setupDebugMode() {
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
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


        viewModel.updateDoorPmc.observe(this) {
            AppLogger.log(TAG, "observeViewModel (updateDoorPmc): $it")
            if (it.first.isNotEmpty() && it.second.isNotEmpty()) {
                publishMessage(topic = it.first, message = it.second)
            }
        }

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
        viewModel.updateAiModeData.observe(this) {
            AppLogger.log(TAG, "observeViewModel (updateAiModeData): $it")
            if (!it.isNullOrEmpty()) {
                try {
                    val data = Gson().fromJson(it, AvailableDoorsData::class.java)
                    if (data.mobile == "1") {
                        lastAiStatusMessageTime = System.currentTimeMillis()
                        startAiMonitoring()
                    } else {
                        stopAiMonitoring()
                    }
                } catch (e: Exception) {
                    AppLogger.logError(TAG, e)
                }

                if (AppPreferences.aiListeningMode) {
                    publishMessage(topic = MQTT_PUBLISH_AI_NOTIFY, message = it)
                } else {
                    publishMessage(topic = MQTT_PUBLISH_AI, message = it)
                }
            }
        }

        viewModel.startMqtt.observe(this) {
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({ setMqttService() }, 300)
                viewModel.startMqtt.postValue(false)
            }
        }

        viewModel.screenWaiting.observe(this) {
            if (it) {
                showAlertWaiting()
            } else {
                if (this::alertDialog.isInitialized) {
                    if (alertDialog.isShowing) {
                        alertDialog.dismiss()
                    }
                }

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
                        AppLogger.log(TAG, "showAlert: msgString = $msgString")
                        showAlert(msgString.joinToString(", "), (msgString.size != 1))
                    }
                } else {
                    val msgString: ArrayList<String> = arrayListOf()
                    if (ffb.ffb1Run == "1") msgString.add("FFB1")
                    //if (ffb.ffb2Run == "1") msgString.add("FFB2")
                    //if (ffb.ffb3Run == "1") msgString.add("FFB3")
                    //if (ffb.ffb4Run == "1") msgString.add("FFB4")
                    //if (ffb.ffb5Run == "1") msgString.add("FFB5")
                    showAlert(msgString.joinToString(", "), (msgString.size != 1))
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setMqttService() {
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                AppLogger.log(TAG, "setMqttService: Starting MQTT Service")
                if (mqttHandler != null) {
                    if (mqttHandler?.isConnected() == true) {
                        mqttHandler!!.disconnect()
                    }

                }

                mqttHandler = MqttHandler().apply { setListener(this@MainActivity) }
                val clientId = AppPreferences.mqttClientId
                val configStr = AppPreferences.mqttConfig

                if (configStr.isNullOrEmpty()) {
                    AppLogger.log(TAG, "setMqttService: MQTT config is empty")
                    return@launch
                }

                val config = Gson().fromJson(configStr, MqttConfig::class.java)
                if (config == null || clientId.isNullOrEmpty()) {
                    AppLogger.log(TAG, "setMqttService: MQTT config or client ID is invalid")
                    return@launch
                }

                mqttHandler?.connect(
                    "tcp://${config.host}:${config.port}", clientId, config.username, config.password
                )
            } catch (e: Exception) {
                AppLogger.logError(TAG, e,)
            }
        }
    }

    private fun publishMessage(topic: String, message: String) {
        AppLogger.log(TAG, "publishMessage: topic=$topic | message=${message}")
        mqttHandler?.publish(topic, message, 0)
    }

    override fun onPause() {
        super.onPause()
        stopAiMonitoring()
        if (::serviceIntent.isInitialized) {
            stopService(serviceIntent)
        }

        if (::broadcastReceiver.isInitialized) {
            try {
                unregisterReceiver(broadcastReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        acquireWakeLock()

        if (::serviceIntent.isInitialized) {
            startService(serviceIntent)
        }

        if (AppPreferences.aiMode == 1) {
            lastAiStatusMessageTime = System.currentTimeMillis()
            startAiMonitoring()
        }

        if (::broadcastReceiver.isInitialized) {
            val intentFilter = IntentFilter(ACTION_BROADCAST_MQTT_CONN)

            ContextCompat.registerReceiver(
                this, broadcastReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        mqttHandler?.disconnect()
        if (this::alertDialog.isInitialized) {
            if (alertDialog.isShowing) {
                alertDialog.dismiss()
            }
        }
        if (this::timer.isInitialized) {
            timer.cancel()
        }
    }

    private fun acquireWakeLock() = wakeLock?.acquire(tenDaysInMillis) //fun acquireWakeLock

    private fun releaseWakeLock() {
        wakeLock?.release()
    }

    @SuppressLint("RestrictedApi")
    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            val destinationFragment = destination.label
            AppLogger.log(TAG, "destinationChangedListener destinationFragment: $destinationFragment")
            setBottomView(destinationFragment)
            runOnUiThread {
                when (destinationFragment) {
                    navController.findDestination(R.id.mqttConfigFragment)?.label -> {
                        binding.bnNavigation.visibility = View.GONE
                        binding.navHostFragment.visibility = View.VISIBLE
                    }

                    navController.findDestination(R.id.splashFragment)?.label, navController.findDestination(
                        R.id.guideFragment
                    )?.label -> {
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
            if (isFinishing || isDestroyed) return@runOnUiThread
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
                val rootView =
                    alertDialog.window?.decorView?.findViewById<View>(android.R.id.content)
                rootView?.let {
                    applyBounceAnimation(it)
                }
            }
        }
    }


    private fun showAlertMsg(
        topMsg: String = getString(R.string.ai_mode_alert_title),
        msgString: String,
        animation: Int = R.raw.alart,
        isMultiple: Boolean = false
    ) {
        try {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
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
                    val message = msgString
                    binding.tvTopic.text = topMsg
                    binding.tvMsg.text = message
                    binding.lottieAnimationView.setAnimation(animation)
                    alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    alertDialog.setCanceledOnTouchOutside(false)
                    binding.btnConfirm.text = "Ok"
                    binding.btnConfirm.setOnClickListener {
                        applyDismissAnimation(alertDialog.window?.decorView?.findViewById(android.R.id.content)) {
                            alertDialog.dismiss() // Dismiss after animation
                        }
                    }

                    alertDialog.show()

                    // Apply the bounce-in animation
                    val rootView =
                        alertDialog.window?.decorView?.findViewById<View>(android.R.id.content)
                    rootView?.let {
                        applyBounceAnimation(it)
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.logError(TAG, e,)
        }

    }

    @SuppressLint("SetTextI18n")
    private fun showAlertWaiting(
        topMsg: String = getString(R.string.processing),
        msgString: String = "",
        animation: Int = R.raw.loading_dots,
        isMultiple: Boolean = false
    ) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
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
            binding.tvTopic.text = topMsg
            binding.tvMsg.text = ""
            binding.lottieAnimationView.setAnimation(animation)
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.setCanceledOnTouchOutside(false)
            binding.btnConfirm.text = "Ok"
            binding.btnConfirm.visibility = View.GONE
            binding.tvMsg.visibility = View.GONE
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


    private fun setBottomView(destinationFragment: CharSequence?) {
        when (destinationFragment) {
            "fragment_home" -> {
                selectButton(binding.btnHome)
            }

            "fragment_ffb_conveyor" -> {
                selectButton(binding.btnFfb)
            }

            "fragment_stuck_door" -> {
                selectButton(binding.btnStuckDoor)
            }

            "fragment_sfb_conveyor" -> {
                selectButton(binding.btnSfb)
            }

            "fragment_doors" -> {
                selectButton(binding.btnDoor)
            }

            "fragment_auto_feeding" -> {
                selectButton(binding.btnAutoFeeding)
            }

            "fragment_mqtt_config" -> {
                selectButton(binding.btnConfig)
            }
        }
    }
    private fun startAiMonitoring() {
        AppLogger.log(TAG, "Starting AI Status Monitoring")
        aiMonitoringHandler.removeCallbacks(aiMonitoringRunnable)
        aiMonitoringHandler.post(aiMonitoringRunnable)
    }

    private fun stopAiMonitoring() {
        AppLogger.log(TAG, "Stopping AI Status Monitoring")
        aiMonitoringHandler.removeCallbacks(aiMonitoringRunnable)
    }

    private fun checkAiStatusTimeout() {
        if (AppPreferences.aiMode == 1) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAiStatusMessageTime > 120000) { // 2 minutes timeout
                if (mqttHandler?.isConnected() == true) {
                    AppLogger.log(TAG, "AI Status timeout: turning AI mode OFF")
                    turnOffAiMode()
                } else {
                    AppLogger.log(TAG, "AI Status timeout: MQTT not connected, waiting...")
                }
            }
        } else {
            stopAiMonitoring()
        }
    }

    private fun turnOffAiMode() {
        val availableDoors = AppPreferences.availableDoorsData
        val mobileData = AvailableDoorsData(availableDoors = availableDoors, mobile = "0")
        val jsonString = Gson().toJson(mobileData)

        viewModel.updateAiModeData.postValue(jsonString)
        viewModel.aiStatus.postValue(0)
        AppPreferences.aiMode = 0

        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            Toast.makeText(this, "AI mode turned off due to inactivity", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    // MQTT Callbacks
    override fun onConnection(isConnect: Boolean) {
        AppLogger.log(
            TAG,
            "onConnection: isConnect = $isConnect ,minister mode = ${AppPreferences.aiListeningMode}"
        )
        if (isConnect) {
            mqttSubscribe()
        }
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val message = if (isConnect) "Mqtt Connected" else "Mqtt Connection Failed"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            updateMqttButton(isConnect, binding.btnMqttStatus)
        }
    }

    fun mqttSubscribe() {
        mqttHandler?.subscribe(MQTT_SUBSCRIBE_TOPIC_PMC)

        mqttHandler?.subscribe(MQTT_SUBSCRIBE_TOPIC_LR)
        mqttHandler?.subscribe(MQTT_SUBSCRIBE_AUTO_FEED_1)
        mqttHandler?.subscribe(MQTT_SUBSCRIBE_AUTO_FEED_2)
        mqttHandler?.subscribe(MQTT_SUBSCRIBE_AI_STATUS)
        mqttHandler?.subscribe(MQTT_DOOR_SRUCK)
        MQTT_SUBSCRIBE_HUMAN_DETECTION.forEach {
            mqttHandler?.subscribe(it)
        }
        MQTT_SUBSCRIBE_CAGE_FILL.forEach {
            mqttHandler?.subscribe(it)
        }
        if (!AppPreferences.aiListeningMode) {
            mqttHandler?.subscribe(MQTT_SUBSCRIBE_AI_NOTIFY)
        }
    }


    override fun onReceiveMessage(topic: String, message: String) {
        AppLogger.log(TAG, "onReceiveMessage: $topic: $message")
        when (topic) {

            MQTT_SUBSCRIBE_TOPIC_PMC -> {
                try {
                    val data = Gson().fromJson(message, PmcStatusData::class.java)

                    val statusMessage = when (data.mobile) {
                        "1" -> "Doors Open: ${data.availableDoors}"
                        "0" -> "All Doors Closed"
                        else -> "Unknown Status"
                    }

                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        Toast.makeText(
                            this@MainActivity, statusMessage, Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_DOOR_SRUCK -> {

                mqttCallback(
                    topic = topic, message = message
                )

                try {
                    val stuckDoorData = Gson().fromJson(
                        message, DoorStatusData::class.java
                    )
                    val doorId = stuckDoorData.door.toString()
                    val currentList =
                        AppPreferences.stuckDoorsData.split(",").filter { it.isNotEmpty() }
                            .toMutableList()
                    if (stuckDoorData.stuck == 1) {

                        if (!currentList.contains(doorId)) {

                            currentList.add(doorId)

                            AppPreferences.stuckDoorsData =
                                currentList.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                                    .joinToString(",")
                        }

                    } else {

                        currentList.remove(doorId)

                        AppPreferences.stuckDoorsData =
                            currentList.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                                .joinToString(",")
                    }
                    runOnUiThread {

                        if (isFinishing || isDestroyed) return@runOnUiThread

                        if (
                            stuckDoorData.stuck == 1 &&
                            AppPreferences.stuckDoorsData.isNotEmpty()
                        ) {

                            // Close old dialog if already showing
                            if (stuckDoorDialog?.isShowing == true) {

                                stuckDoorDialog?.dismiss()

                                stuckDoorDialog = null
                            }

                            val doorList = AppPreferences.stuckDoorsData
                                .split(",")
                                .filter { it.isNotBlank() }
                                .joinToString(", ") {
                                    "Door $it"
                                }

                            val dialogView = layoutInflater.inflate(
                                R.layout.dialog_stuck_door,
                                null
                            )

                            dialogView.findViewById<TextView>(
                                R.id.txtDoors
                            ).text = doorList

                            stuckDoorDialog = MaterialAlertDialogBuilder(this)
                                .setView(dialogView)
                                .setCancelable(false)
                                .create()

                            stuckDoorDialog?.show()

                            dialogView.findViewById<Button>(
                                R.id.btnView
                            ).setOnClickListener {

                                selectButton(
                                    binding.btnStuckDoor
                                )

                                nextFragment(
                                    R.id.stuckDoorFragment
                                )

                                stuckDoorDialog?.dismiss()

                                stuckDoorDialog = null
                            }

                            dialogView.findViewById<Button>(
                                R.id.btnClose
                            ).setOnClickListener {

                                stuckDoorDialog?.dismiss()

                                stuckDoorDialog = null
                            }
                        }
                    }

                } catch (e: Exception) {

                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_SUBSCRIBE_TOPIC_LR -> {
                try {
                    val statusData = message.toStatusData()
                    AppLogger.log(TAG, "onReceiveMessage: statusData : $statusData")

                    statusData.data?.let { data ->
                        if (viewModel.statusData.value != statusData) {
                            viewModel.statusData.postValue(statusData)
                            if (lastStatus != data.lrStarter) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "starter motor: ${data.lrStarter}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                AppLogger.log(TAG, "onReceiveMessage: starter motor: ${data.lrStarter}")
                                lastStatus = data.lrStarter
                            }
                        }
                    }
                } catch (e: Exception) {
                    viewModel.statusData.postValue(null)
                    lastStatus = ""
                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_SUBSCRIBE_AUTO_FEED_1 -> {
                try {
                    val autoFeedingData = Gson().fromJson(message, AutoFeedingData::class.java)
                    if (viewModel.autoFeedingData1.value != autoFeedingData) {
                        AppLogger.log(TAG, "onReceiveMessage: AutoFeed1: $autoFeedingData ")
                        viewModel.autoFeedingData1.postValue(autoFeedingData)
                    }
                } catch (e: Exception) {
                    viewModel.autoFeedingData1.postValue(null)
                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_SUBSCRIBE_AUTO_FEED_2 -> {
                try {
                    val autoFeedingData = Gson().fromJson(message, AutoFeedingData::class.java)
                    if (viewModel.autoFeedingData2.value != autoFeedingData) {
                        AppLogger.log(TAG, "onReceiveMessage: AutoFeed2: $autoFeedingData ")
                        viewModel.autoFeedingData2.postValue(autoFeedingData)
                    }
                } catch (e: Exception) {
                    viewModel.autoFeedingData2.postValue(null)
                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_SUBSCRIBE_AI_STATUS -> {
                try {
                    AppLogger.log(TAG, "AI Status Raw Message : $message")
                    lastAiStatusMessageTime = System.currentTimeMillis()
                    val aiStatus = Gson().fromJson(message, AiStatusData::class.java)
                    val statusValue = aiStatus.w.first().value
                    AppLogger.log(TAG, "AI Status Received : $statusValue")
                    viewModel.aiStatus.postValue(statusValue)
                    if (statusValue == -1) {
                        AppLogger.log(TAG, "AI Status -> AI Processing Completed")
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            Toast.makeText(this, "AI Status Received", Toast.LENGTH_SHORT).show()
                            AppLogger.log(TAG, "Starting Timer : $startTimeInMillis")
                            startTimer(startTimeInMillis)
                        }
                    } else if (statusValue == -2) {
                        AppLogger.log(TAG, "AI Status -> No Cages Available")
                        runOnUiThread {
                            if (isFinishing || isDestroyed) return@runOnUiThread
                            Toast.makeText(this, "No Cages Available", Toast.LENGTH_LONG).show()
                            AppLogger.log(TAG, "Starting Timer : $startTimeInMillis")
                            startTimer(startTimeInMillis)
                        }
                    }
                } catch (e: Exception) {
                    viewModel.aiStatus.postValue(0)
                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_SUBSCRIBE_AI_NOTIFY -> {
                try {
                    val aiStatus = Gson().fromJson(message, AvailableDoorsData::class.java)
                    AppLogger.log(TAG, "onReceiveMessage (AI_NOTIFY): $aiStatus")
                    when (aiStatus.mobile) {
                        "0" -> {
                            showAlertMsg(
                                msgString = "AI Mode Off",
                                isMultiple = false,
                                animation = R.raw.alart_red
                            )
                        }

                        "1" -> {
                            showAlertMsg(
                                msgString = "AI Mode On",
                                isMultiple = false,
                                animation = R.raw.alart_green
                            )
                        }
                    }
                } catch (e: Exception) {
                    viewModel.aiStatus.postValue(0)
                    AppLogger.logError(TAG, e,)
                }
            }

            MQTT_SUBSCRIBE_HUMAN_DETECTION.find { it == topic } -> {

                val humanDetectionData = Gson().fromJson(message, HumanDetectionData::class.java)

                if (humanDetectionData != null) {
                    AppLogger.log(TAG, "onReceiveMessage (HUMAN_DETECTION): $humanDetectionData")
                    if (humanDetectionData.human == "1") {
                        if (!localHumanDetectionData.contains(topic)) {
                            localHumanDetectionData.add(topic)
                            showAlertMsg(
                                topMsg = getString(R.string.human_detected_alert_title),
                                msgString = getString(R.string.human_detected_alert_msg) + " ${
                                    topic.substringAfterLast(
                                        "AI/"
                                    )
                                }",
                                isMultiple = false,
                                animation = R.raw.alart_red
                            )
                        }
                    } else {
                        localHumanDetectionData.removeIf { it == topic }
                    }
                }
            }

            MQTT_SUBSCRIBE_CAGE_FILL.find { it == topic } -> {
                AppLogger.log(TAG, "Cage Fill Message Received from topic: $topic")
                AppLogger.log(TAG, "Cage Fill Data: $message")

                try {
                    val gson = Gson()
                    val currentData = viewModel.cageFillData.value

                    if (currentData == null) {
                        val cageData = gson.fromJson(message, CageFillData::class.java)
                        viewModel.cageFillData.postValue(cageData)
                        return
                    }

                    val currentJson = gson.toJsonTree(currentData).asJsonObject
                    val newJson = gson.fromJson(message, JsonObject::class.java)

                    var hasChanged = false

                    newJson.entrySet().forEach { (key, newValue) ->

                        // Skip null values
                        if (newValue == null || newValue.isJsonNull) return@forEach

                        val currentValue = currentJson.get(key)

                        // Skip if value is same
                        if (currentValue != null &&
                            !currentValue.isJsonNull &&
                            currentValue == newValue
                        ) {
                            return@forEach
                        }

                        // Update only when value changed
                        currentJson.add(key, newValue)
                        hasChanged = true

                        AppLogger.log(
                            TAG,
                            "Updated $key : ${currentValue ?: "null"} -> $newValue"
                        )
                    }

                    if (hasChanged) {
                        val mergedData = gson.fromJson(currentJson, CageFillData::class.java)
                        viewModel.cageFillData.postValue(mergedData)
                        AppLogger.log(TAG, "Cage Fill LiveData Updated")
                    } else {
                        AppLogger.log(TAG, "No changes detected")
                    }

                } catch (e: Exception) {
                    AppLogger.logError(TAG, e,)
                }
            }

            else -> {
                AppLogger.log(TAG, "onReceiveMessage (UNKNOWN): $topic: $message")
            }
        }
    }

    private fun showDoorStuckDialog(
        doorId: Int
    ) {

        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this@MainActivity).setTitle("Door Alert").setMessage(
            "Door $doorId is STUCK.\n\nDo you want to see?"
        ).setCancelable(false)

            // Open screen/class
            .setPositiveButton("Open") { _, _ ->
                selectButton(binding.btnStuckDoor)
                nextFragment(R.id.stuckDoorFragment)
            }

            .setNegativeButton("Cancel", null).show()
    }

    private fun startTimer(timeInMillis: Long) {
        AppLogger.log(TAG, "Timer Started : ${timeInMillis / 1000}s")
        timer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                AppLogger.log(TAG, "Timer Tick : ${secondsLeft}s left")
                viewModel.aiCountdown.postValue(secondsLeft)
            }
            override fun onFinish() {
                AppLogger.log(TAG, "Timer Finished")
            }
        }.start()
    }

    override fun onDeliveryComplete(id: Int, message: MqttMessage, complete: Boolean) {
        AppLogger.log(TAG, "onDeliveryComplete: id = $id: message = $message")
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val status = if (complete) "Delivery Complete" else "Delivery Failed"
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        }
    }

    override fun isConnectionLost(error: Throwable) {
        AppLogger.log(TAG, "isConnectionLost: ${error.message}")
        AppLogger.log(TAG, Log.getStackTraceString(error))
    }

    fun mqttConnectionCheck(): Boolean {
        return mqttHandler?.isConnected() ?: false
    }

    fun updateCommend() {
        val cmdData: CommandData = Gson().fromJson(AppPreferences.cmdJson, CommandData::class.java)
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
                    hideKeyboard(activity = this)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun checkMqttConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            val conn = mqttHandler?.isConnected()
            if (conn != null) {
                if (!conn) {
                    updateMqttButton(conn, binding.btnMqttStatus)
                    mqttHandler?.reconnect()
                }
            }
        }
    }

    override fun onBroadcastReceived() {
        checkMqttConnection()
    }

}
