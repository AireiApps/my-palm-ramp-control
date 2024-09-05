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

    private var doorState : Boolean = false

    private var isHold : Boolean = false

    private val viewModel: AppViewModel by activityViewModels()

    private var repeat : Int = 0

    lateinit var adapter: DoorAdapter


    private var player: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
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
        doorActionbtn()
        Log.d(TAG, "onViewCreated: ")
        binding.rtspLayout.visibility = View.GONE

        binding.fabConfig.setOnClickListener {
            findNavController().navigate(R.id.mqttConfigFragment)
        }

        binding.imgClose.setOnClickListener {

            binding.rtspLayout.visibility = View.GONE
            if(player!=null){
                player?.stop()
                player?.release()
            }
        }

        binding.tgMotor.setOnClickListener {
            updateMotor(state = binding.tgMotor.isChecked)
        }
        //viewModel.startMqtt.postValue(true)
    }

    private fun observeDoorList() {
        viewModel.doorsLiveData.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                val doorList = it.map { it.toDoorData() }
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
            requireContext(),
            list,
            object : DoorAdapter.ActionClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onActionClick(data: DoorData) {
                    val temp = adapter.getList()

                    temp.map { it ->
                        if (data.doorId == it.doorId && !data.selected) {
                            playExoPlayer(data.rtspConfig,doorId =  data.doorId)
                            it.selected = true
                        } else {
                            it.selected = false
                        }
                    }

                    if (selectDoor == data) {
                        selectDoor = null
                    } else {
                        selectDoor = data
                    }

                    adapter.updateDoor(temp)
                }
            }
        )
        binding.rvConveyor.adapter = adapter
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doorActionbtn() {
        fun handleButtonTouch(doorStateValue: Boolean) = View.OnTouchListener { _, event ->
            val mqttConnection = (activity as MainActivity).mqttConnectionCheck()
            if (mqttConnection) {
                if (selectDoor != null) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isHold = true
                            doorState = doorStateValue
                            if (doorStateValue) {
                                generateMsg(AppPreferences.doorOpenCmd, 1)
                            } else {
                                generateMsg(AppPreferences.doorCloseCmd, 1)
                            }
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            repeat = 0
                            isHold = false
                            if (doorStateValue) {
                                generateMsg(AppPreferences.doorOpenCmd, 0)
                            } else {
                                generateMsg(AppPreferences.doorCloseCmd, 0)
                            }

                            true
                        }

                        else -> false
                    }
                } else {
                    if (event.action != MotionEvent.ACTION_DOWN || event.action != MotionEvent.ACTION_CANCEL) {
                        Toast.makeText(requireContext(), "Please select door.", Toast.LENGTH_SHORT)
                            .show()
                    }
                    true
                }
            } else {
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_CANCEL) {
                    Toast.makeText(
                        requireContext(),
                        "Mqtt connection not available. Please check mqtt connection.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
        }
        // Set the touch listener for each button using the common function
        binding.btnClose.setOnTouchListener(handleButtonTouch(false)) // Close action
        binding.btnOpen.setOnTouchListener(handleButtonTouch(true))   // Open action
    }

    private fun isWholeNumber(value: Float): Boolean {
        return value == value.toInt().toFloat()
    }

    private fun generateMsg(tag: String, value: Int) {

        val doorID = selectDoor!!.doorId
        val modifyTag = tag.replace("[DOOR_X]", doorID)
        val data = WData(
            w = listOf(
                TagData(
                    tag = modifyTag,
                    value = value
                )
            )
        )
        val msg = Gson().toJson(data)
        viewModel.updateDoor.postValue(msg)
    }

    private fun updateMotor(tag: String = "LoadingRamp:LRStarter_Cmd", state: Boolean) {
        val data = WData(
            w = listOf(
                TagData(
                    tag = tag,
                    value = if (state) 1 else 0
                )
            )
        )
        val jsonString = Gson().toJson(data)
        viewModel.updateStarter.postValue(jsonString)
    }

    override fun onPause() {
        super.onPause()
        selectDoor = null
        binding.rtspLayout.visibility = View.GONE

    }

    override fun onResume() {
        super.onResume()
        if (this::adapter.isInitialized){
            selectDoor = adapter.getList().find { it.selected }
        }
        Log.d(TAG, "onResume: ")
    }


    @OptIn(UnstableApi::class)
    @SuppressLint("ClickableViewAccessibility")
    fun playExoPlayer(
        rtspConfig: String = "rtsp://admin:L2ACBEC1@192.168.1.13:554/cam/realmonitor?channel=1&subtype=0",
        doorId: String
    ) {
        binding.tvDoorId.text = doorId
        binding.rtspLayout.visibility = View.VISIBLE



        try {
            player?.release()
            val loadControl: LoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    10000,  // Minimum buffer before playback starts (10 seconds)
                    30000,  // Maximum buffer during playback (30 seconds)
                    1500,   // Minimum buffer for a smooth start (1.5 seconds)
                    3000    // Minimum buffer after a rebuffer (3 seconds)
                )
                .build()
            val tabLoadControl: LoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    0,  // Minimum buffer before playback starts (10 seconds)
                    1000,  // Maximum buffer during playback (30 seconds)
                    0,   // Minimum buffer for a smooth start (1.5 seconds)
                    0    // Minimum buffer after a rebuffer (3 seconds)
                ).setPrioritizeTimeOverSizeThresholds(true)
                .build()

            player = ExoPlayer.Builder(requireContext())
                .setLoadControl(tabLoadControl)
                .build()

            binding.playerView.player = player
            binding.playerView.useController = false

            player?.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(requireContext(), "Failed to connect to RTSP stream.", Toast.LENGTH_LONG).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(requireContext(), "Attempting to reconnect...", Toast.LENGTH_SHORT).show()
                        playExoPlayer(rtspConfig, doorId = doorId)
                    }, 1000)

                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            // The player is connected and ready to stream
                            binding.rtspLayout.visibility = View.VISIBLE
                            Toast.makeText(requireContext(), "Connected to RTSP stream.", Toast.LENGTH_SHORT).show()
                         }
                        Player.STATE_BUFFERING -> {
                            Toast.makeText(requireContext(), "Buffering...", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_ENDED -> {
                            Toast.makeText(requireContext(), "Stream ended.", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_IDLE -> {
                            Toast.makeText(requireContext(), "Player idle, no stream.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        Toast.makeText(requireContext(), "RTSP stream is playing.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "RTSP stream is paused.", Toast.LENGTH_SHORT).show()
                        // Delay reconnection attempt
                        Handler(Looper.getMainLooper()).postDelayed({
                            Toast.makeText(requireContext(), "Attempting to reconnect...", Toast.LENGTH_SHORT).show()
                            playExoPlayer(rtspConfig, doorId = doorId)
                        }, 1000)
                    }
                }
            })

            binding.playerView.setOnTouchListener { _, _ -> true }

            val uri = Uri.parse(rtspConfig)
            val mediaItem = MediaItem.fromUri(uri)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Reconnection logic function
    private fun reconnectToRtspStream(rtspConfig: String) {
        try {
            player?.release() // Release the current player before reconnecting
            player = ExoPlayer.Builder(requireContext()).build()
            val uri = Uri.parse(rtspConfig)
            val mediaItem = MediaItem.fromUri(uri)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        player?.release()
        player = null
    }

    companion object {
        const val TAG: String = "HomeFragment"
    }
}