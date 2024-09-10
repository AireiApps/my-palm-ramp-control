package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.iot.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentHomeBinding
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectDoor: DoorData? = null
    private var doorState: Boolean = false
    private var isHold: Boolean = false

    private val viewModel: AppViewModel by activityViewModels()
    lateinit var adapter: DoorAdapter

    private var player: ExoPlayer? = null
    private var playView: Boolean = false

    private lateinit var handler: Handler
    private val retryDelayMillis: Long = 1000

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

        observeDoorList()
        setupUI()
        doorActionBtn()
        handler = Handler(Looper.getMainLooper())
    }

    private fun setupUI() {
        binding.rtspLayout.visibility = View.GONE

        binding.fabConfig.setOnClickListener {
            findNavController().navigate(R.id.mqttConfigFragment)
        }

        binding.imgClose.setOnClickListener {
            stopPlayer()
        }

        binding.tgMotor.setOnClickListener {
            updateMotor(state = binding.tgMotor.isChecked)
        }
    }

    private fun observeDoorList() {
        viewModel.doorsLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                val doorList = it.map { door -> door.toDoorData() }
                setConveyorList(doorList)
            } else {
                saveDoorList(doorList)
            }
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
        binding.rvConveyor.adapter = adapter
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doorActionBtn() {
        fun handleButtonTouch(doorStateValue: Boolean) = View.OnTouchListener { _, event ->
            if ((activity as MainActivity).mqttConnectionCheck()) {
                selectDoor?.let {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isHold = true
                            doorState = doorStateValue
                            val cmd = if (doorStateValue) AppPreferences.doorOpenCmd else AppPreferences.doorCloseCmd
                            generateMsg(cmd, 1)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isHold = false
                            val cmd = if (doorStateValue) AppPreferences.doorOpenCmd else AppPreferences.doorCloseCmd
                            generateMsg(cmd, 0)
                            true
                        }
                        else -> false
                    }
                } ?: run {
                    if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_CANCEL) {
                        showToast("Please select a door.")
                    }
                    true
                }
            } else {
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_CANCEL) {
                    showToast("Mqtt connection not available. Please check mqtt connection.")
                }
                true
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

        binding.tvDoorId.text = doorId
        binding.rtspLayout.visibility = View.VISIBLE

        // Initialize player
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10000, 30000, 1000, 2000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player?.release() // Release any previous player
        player = ExoPlayer.Builder(requireContext()).setLoadControl(loadControl).build()

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
        player?.apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(rtspConfig)))
            prepare()
            playWhenReady = true
        }
    }

    private fun stopPlayer() {
        playView = false
        binding.rtspLayout.visibility = View.GONE
        player?.apply {
            stop()
            release()
        }
        handler.removeCallbacksAndMessages(null)
    }

    private fun retryPlay(rtspConfig: String, doorId: String) {
        handler.postDelayed({
            showToast(message = "Retrying to connect...")
            playExoPlayer(rtspConfig, doorId)
        }, retryDelayMillis)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPlayer()
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
