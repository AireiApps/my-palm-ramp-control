package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentHomeBinding
import com.airei.milltracking.mypalm.mqtt.lrc.ui.DoorsFragment.AvailableDoorsData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectDoor: DoorData? = null
    private var doorState: Boolean = false
    private var isHold: Boolean = false

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: DoorAdapter

    private var player: ExoPlayer? = null
    private var playView: Boolean = false

    private lateinit var handler: Handler
    private val retryDelayMillis: Long = 1000

    private var clickListener = MutableLiveData<Pair<Boolean,String>>(Pair(false,""))

    private var TAG_OPEN_DOOR :String = ""
    private var TAG_CLOSE_DOOR :String = ""

    private var aiButtonDisable: Boolean = false

    // Declare variables to store previous values
    private var previousMypalmStatus: String? = null
    private var previousLrStarterStatus: String? = null

    private lateinit var aiModeHandler: Handler
    private var aiModeDelay: Long = 30000L
    private val aiModeRunnable = Runnable {
        // Disable the toggle button after 30 seconds
        Log.d(TAG, "aiModeRunnable: ")
        aiButtonDisable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })

        binding.layoutBtns.visibility = View.INVISIBLE
        aiButtonDisable = false
        observeData()
        setupUI()
        doorActionBtn()
        handler = Handler(Looper.getMainLooper())
        aiModeHandler = Handler(Looper.getMainLooper())
    }

    private fun setupUI() {
        binding.rtspLayout.visibility = View.GONE

        binding.imgClose.setOnClickListener {
            stopPlayer()
        }

        binding.tgAiMode.isChecked = AppPreferences.aiMode.split(":")[0] == "true"

        viewModel.aiModeUpdate.postValue(AppPreferences.aiMode)

        binding.tgMotor.setOnClickListener {
            if ((activity as MainActivity).mqttConnectionCheck()) {
                updateMotor(state = binding.tgMotor.isChecked)
            } else {
                binding.tgMotor.isChecked = !binding.tgMotor.isChecked
                showToast("Mqtt connection not available. Please check mqtt connection.")
            }
        }

        binding.tgAiMode.setOnClickListener {
            if (aiButtonDisable) {
                binding.tgAiMode.isChecked = !binding.tgAiMode.isChecked
                showToast("Processing... Please wait until the operation completes.")
            } else {
                if ((activity as MainActivity).mqttConnectionCheck()) {
                    val aiState = binding.tgAiMode.isChecked

                    if (AppPreferences.availableDoorsData.isNotEmpty()) {
                        viewModel.aiModeUpdate.postValue("${aiState}:${System.currentTimeMillis()}")
                        updateAiMode(aiState)
                    } else {
                        showToast("Please select at least one door.")
                        findNavController().navigate(R.id.doorsFragment)
                    }
                }else{
                    binding.tgAiMode.isChecked = !binding.tgAiMode.isChecked
                    showToast("Mqtt connection not available. Please check mqtt connection.")
                }
            }
        }
    }

    private fun updateAiMode(
        state: Boolean, availableDoors: String = AppPreferences.availableDoorsData
    ) {
        val mobileData =
            AvailableDoorsData(availableDoors = availableDoors, mobile = if (state) "1" else "0")
        val jsonString = Gson().toJson(mobileData)
        viewModel.updateAiModeData.postValue(jsonString)
    }

    private fun observeData() {

        viewModel.doorsLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                val doorList = it.map { door -> door.toDoorData() }
                setConveyorList(doorList)
            } else {
                saveDoorList(doorList)
            }
        }

        viewModel.commendData.observe(viewLifecycleOwner){
            if (it != null){
                TAG_OPEN_DOOR = it.rampDoorOpen
                TAG_CLOSE_DOOR = it.rampDoorClose
            }else{
                (activity as MainActivity).updateCommend()
            }
        }

        viewModel.statusData.observe(viewLifecycleOwner) {
            if (it != null) {
                val newMypalmStatus = it.data.mypalmStatus
                val newLrStarterStatus = it.data.lrStarter

                // Update btnDoorStatus only if the mypalmStatus has changed
                if (previousMypalmStatus != newMypalmStatus) {
                    binding.btnDoorStatus.text = when (newMypalmStatus) {
                        "0" -> getString(R.string.scada_mode)
                        "1" -> getString(R.string.my_palm_mode)
                        else -> getString(R.string.manual_mode)
                    }
                    previousMypalmStatus = newMypalmStatus
                }

                // Update tgMotor isChecked only if lrStarter has changed
                if (previousLrStarterStatus != newLrStarterStatus) {
                    binding.tgMotor.isChecked = newLrStarterStatus == "1"
                    previousLrStarterStatus = newLrStarterStatus
                }

                // Uncomment and use this when rampStatus is available
                /*if (previousRampStatus != newRampStatus) {
                    binding.btnRampStatus.text = when (newRampStatus) {
                        "0" -> "Auto"
                        "1" -> "Manual"
                        else -> "Manual"
                    }
                    previousRampStatus = newRampStatus
                }*/

            } else {
                // If data is null, reset the UI and previous values
                binding.btnDoorStatus.text = "--"
                binding.btnRampStatus.text = "--"
                previousMypalmStatus = null
                previousLrStarterStatus = null
            }
        }

        viewModel.aiModeUpdate.observe(viewLifecycleOwner) { lastUpdate ->
            if (lastUpdate.isNotEmpty()) {
                val aiStateData = lastUpdate.split(":")
                AppPreferences.aiMode = lastUpdate
                if (aiStateData.first() == "true") {
                    binding.tgAiMode.isChecked = true
                } else {
                    binding.tgAiMode.isChecked = false
                    if (aiStateData.last().toLong() != 0L) {
                        aiModeDelay = checkTimeDifference(aiStateData.last().toLong())
                        Log.d(TAG, "observeData: aiModeUpdate ${(aiModeDelay)}")

                        if (aiModeDelay != 0L) {
                            aiButtonDisable = true
                            aiModeHandler.postDelayed(aiModeRunnable, aiModeDelay)
                        }
                    }
                }
            }
        }
    }

    private fun checkTimeDifference(timestamp: Long): Long {
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        // Calculate the time difference in milliseconds
        val timeDifference = currentTime - timestamp

        // Check if the difference is less than or equal to 30 seconds
        Log.i(TAG, "checkTimeDifference = $timeDifference")
        return if (timeDifference <= 30000) {
            30000 - timeDifference
        } else {
            0L
        }
    }


    private fun saveDoorList(doorList: List<DoorData>) {
        val doorTable = doorList.map { it.toDoorTable() }
        viewModel.insertAllDoors(doorTable)
    }

    private fun setConveyorList(list: List<DoorData>) {
        selectDoor = null
        adapter = DoorAdapter(
            list,
            object : DoorAdapter.ActionClickListener {
                override fun onActionClick(data: DoorData) {
                    val temp = adapter.getList()
                    temp.forEach { it.selected = it.doorId == data.doorId && !data.selected }
                    if (data.selected) {
                        playView = true
                        playExoPlayer(data.rtspConfig, doorId = data.doorId)
                    } else {
                        stopPlayer()
                    }
                    selectDoor = if (selectDoor == data) null else data
                    adapter.updateDoor(temp)
                }
            }
        )
        val displayMetrics = resources.displayMetrics
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        Log.i(TAG, "setConveyorList: $screenHeightDp / ${displayMetrics.heightPixels}")
        val spanCount = if (screenHeightDp < 700) 6 else 8
        val gridLayoutManager = GridLayoutManager(requireContext(), spanCount)

        binding.rvDoors.layoutManager = gridLayoutManager

        binding.rvDoors.adapter = adapter

        binding.layoutBtns.visibility = View.VISIBLE
    }


    @kotlin.OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("ClickableViewAccessibility")
    private fun doorActionBtn() {
        fun handleButtonTouch(doorStateValue: Boolean) = View.OnTouchListener { v, event ->
            if ((activity as MainActivity).mqttConnectionCheck()) {

                Log.i(TAG, "handleButtonTouch: ${event.action}")
                selectDoor?.let {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {

                            isHold = true
                            doorState = doorStateValue
                            GlobalScope.launch {
                                runBlocking {
                                    val perCmd =
                                        if (!doorStateValue) AppPreferences.doorOpenCmd else AppPreferences.doorCloseCmd
                                    generateMsg(perCmd, 0)
                                }
                                runBlocking {
                                    val cmd =
                                        if (doorStateValue) AppPreferences.doorOpenCmd else AppPreferences.doorCloseCmd
                                    generateMsg(cmd, 1)
                                }
                            }

                            false
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                            clickListener.postValue(Pair(false,""))
                            isHold = false
                            val cmd = if (doorStateValue) AppPreferences.doorOpenCmd else AppPreferences.doorCloseCmd
                            generateMsg(cmd, 0)

                            false
                        }
                        else -> false
                    }
                } ?: run {
                    if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_CANCEL) {
                        showToast("Please select a door.")
                    }
                    false
                }
            } else {
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_CANCEL) {
                    showToast("Mqtt connection not available. Please check mqtt connection.")
                }
                false
            }
        }
        binding.btnClose.setOnTouchListener(handleButtonTouch(false))
        binding.btnOpen.setOnTouchListener(handleButtonTouch(true))
    }

    private fun generateMsg(tag: String, value: Int) {
        selectDoor?.let {
            val modifyTag = tag.replace("[DOOR_X]", it.doorId)
            val data = WData(w = listOf(TagData(tag = modifyTag, value = value)))
            viewModel.updateDoor.postValue(Gson().toJson(data))
        }
    }

    private fun updateMotor(tag: String = "LoadingRamp:LRStarter_Cmd", state: Boolean) {
        val data = WData(w = listOf(TagData(tag = tag, value = if (state) 1 else 0)))
        viewModel.updateStarter.postValue(Gson().toJson(data))
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("ClickableViewAccessibility")
    fun playExoPlayer(rtspConfig: String, doorId: String) {
        if (!playView) return
        try {
            binding.tvDoorId.text = doorId
            binding.rtspLayout.visibility = View.VISIBLE

            // Initialize load control
            val loadControl = DefaultLoadControl.Builder()
                //.setBufferDurationsMs(10000, 30000, 1000, 2000)
                .setPrioritizeTimeOverSizeThresholds(true).build()

            player?.release() // Release any previous player
            player = ExoPlayer.Builder(requireContext())
                //.setTrackSelector(trackSelector)
                .setLoadControl(loadControl).build()
            player?.videoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT
            binding.playerView.apply {
                player = this@HomeFragment.player
                useController = false
                setOnTouchListener { _, _ -> true }
            }

            player?.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    showToast("Failed to connect to RTSP stream.")
                    Log.i(TAG, "onPlaybackStateChanged: Failed to connect to RTSP stream.")
                    retryPlay(rtspConfig, doorId)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            Log.i(TAG, "onPlaybackStateChanged: STATE_BUFFERING")
                            showToast("Buffering...")
                        }

                        Player.STATE_ENDED -> {
                            Log.i(TAG, "onPlaybackStateChanged: STATE_ENDED")
                            showToast("Stream ended.")
                        }

                        Player.STATE_IDLE -> {
                            Log.i(TAG, "onPlaybackStateChanged: STATE_IDLE")
                        }

                        Player.STATE_READY -> {
                            Log.i(TAG, "onPlaybackStateChanged: STATE_READY")
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    showToast(if (isPlaying) "RTSP stream is playing." else "RTSP stream is paused.")
                    if (!isPlaying) retryPlay(rtspConfig, doorId)
                }
            })

            val mediaSource = RtspMediaSource.Factory().setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(rtspConfig))
            //"rtsp://admin:L2ACBEC1@192.168.1.13:554/cam/realmonitor?channel=1&subtype=0"));

            //val mediaSource = RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri("rtsp://admin:L2ACBEC1@192.168.1.13:554/cam/realmonitor?channel=1&subtype=0"))

            player?.apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
        } catch (ex: Exception) {
            Log.e(TAG, "playExoPlayer: Error -> ", ex)
        }
    }

    private fun stopPlayer() {
        try {
            playView = false
            binding.rtspLayout.visibility = View.GONE
            player?.apply {
                stop()
                release()
            }
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e(TAG, "stopPlayer: Error -> ", e)
        }

    }

    private fun retryPlay(rtspConfig: String, doorId: String) {
        handler.postDelayed({
            showToast(message = "Retrying to connect...")
            playExoPlayer(rtspConfig, doorId)
        }, retryDelayMillis)
    }

    private fun showToast(message: String) {
        try {
            kotlin.runCatching {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "showToast: ", e)
        }

    }

    override fun onPause() {
        super.onPause()
        stopPlayer()
        aiModeHandler.removeCallbacksAndMessages(aiModeRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        player?.release()
        player = null
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}
