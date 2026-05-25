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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CommandData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.PmcDoorCommand
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.VlcRtspPlayerHelper
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

    private var isLeftPlaying = false
    private var isRightPlaying = false

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: DoorAdapter

    private var vlcHelper: VlcRtspPlayerHelper? = null
    private var playView: Boolean = false

    private var clickListener =
        MutableLiveData<Pair<Boolean, String>>(
            Pair(false, "")
        )

    private var TAG_OPEN_DOOR: String = ""
    private var TAG_CLOSE_DOOR: String = ""

    private var aiButtonDisable: Boolean = false

    private var previousMypalmStatus: String? = null
    private var previousLrStarterStatus: String? = null

    private lateinit var msgBuilder: AlertDialog.Builder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHomeBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        AppLogger.log(
            tag = TAG,
            message = "HomeFragment onViewCreated"
        )

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        AppLogger.log(
                            tag = TAG,
                            message = "Back Pressed : Finish Activity"
                        )

                        requireActivity().finish()
                    }
                }
            )

        binding.layoutBtns.visibility =
            View.INVISIBLE

        binding.rtspLayout.visibility =
            View.GONE

        aiButtonDisable =
            false

        msgBuilder =
            AlertDialog.Builder(
                requireContext()
            )

        observeData()

        setupUI()

        doorActionBtnPmc()

        AppLogger.log(
            tag = TAG,
            message = "AI Listening Mode : ${AppPreferences.aiListeningMode}"
        )
    }

    private fun setupUI() {

        binding.rtspLayout.visibility =
            View.GONE

        hideRtspStatusUi()

        binding.imgClose.setOnClickListener {

            AppLogger.log(
                tag = TAG,
                message = "RTSP Close Clicked"
            )

            stopRtspView()
        }

        binding.imgLog.setOnClickListener {

            AppLogger.log(
                tag = TAG,
                message = "Log Button Clicked"
            )

            AppLogger.showLogFilesDialog(
                requireActivity()
            )
        }

        binding.tgAiMode.isChecked =
            false

        viewModel.aiStatus.postValue(
            AppPreferences.aiMode
        )

        binding.tgMotor.setOnClickListener {

            if ((activity as MainActivity).mqttConnectionCheck()) {

                updateMotor(
                    state = binding.tgMotor.isChecked
                )

            } else {

                binding.tgMotor.isChecked =
                    !binding.tgMotor.isChecked

                showToast(
                    "Mqtt connection not available. Please check mqtt connection."
                )
            }
        }

        binding.tgAiMode.setOnClickListener {

            if (aiButtonDisable) {

                binding.tgAiMode.isChecked =
                    !binding.tgAiMode.isChecked

                showToast(
                    "Processing... Please wait until the operation completes."
                )

            } else {

                if ((activity as MainActivity).mqttConnectionCheck()) {

                    val aiState =
                        binding.tgAiMode.isChecked

                    AppPreferences.aiMode =
                        if (aiState) 1 else 0

                    if (AppPreferences.availableDoorsData.isNotEmpty()) {

                        updateAiMode(
                            aiState
                        )

                    } else {

                        showToast(
                            "Please select at least one door."
                        )

                        findNavController()
                            .navigate(
                                R.id.doorsFragment
                            )
                    }

                } else {

                    binding.tgAiMode.isChecked =
                        !binding.tgAiMode.isChecked

                    showToast(
                        "Mqtt connection not available. Please check mqtt connection."
                    )
                }
            }
        }

        viewModel.aiStatus.postValue(
            AppPreferences.aiMode
        )
    }

    private fun setupVlcStatusCallback() {

        vlcHelper?.setStatusCallback { status ->

            if (_binding == null) {
                return@setStatusCallback
            }

            requireActivity().runOnUiThread {

                when (status) {

                    is VlcRtspPlayerHelper.VlcPlayStatus.Opening -> {

                        when (status.side) {

                            VlcRtspPlayerHelper.PlayerSide.LEFT -> {
                                isLeftPlaying = false
                            }

                            VlcRtspPlayerHelper.PlayerSide.RIGHT -> {
                                isRightPlaying = false
                            }
                        }

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = true,
                            showProgress = true,
                            title = "Opening stream...",
                            reason = "Connecting to RTSP camera"
                        )
                    }

                    is VlcRtspPlayerHelper.VlcPlayStatus.Buffering -> {

                        val alreadyPlaying =
                            when (status.side) {

                                VlcRtspPlayerHelper.PlayerSide.LEFT -> {
                                    isLeftPlaying
                                }

                                VlcRtspPlayerHelper.PlayerSide.RIGHT -> {
                                    isRightPlaying
                                }
                            }

                        /*
                        * Ignore buffering after playing started
                        * */
                        if (alreadyPlaying) {
                            return@runOnUiThread
                        }

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = true,
                            showProgress = true,
                            title = "Loading ${status.percent}%",
                            reason = "Please wait..."
                        )
                    }

                    is VlcRtspPlayerHelper.VlcPlayStatus.Playing -> {

                        when (status.side) {

                            VlcRtspPlayerHelper.PlayerSide.LEFT -> {
                                isLeftPlaying = true
                            }

                            VlcRtspPlayerHelper.PlayerSide.RIGHT -> {
                                isRightPlaying = true
                            }
                        }

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = false,
                            showProgress = false,
                            title = "",
                            reason = ""
                        )
                    }

                    is VlcRtspPlayerHelper.VlcPlayStatus.Paused -> {

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = true,
                            showProgress = false,
                            title = "Paused",
                            reason = "Player paused"
                        )
                    }

                    is VlcRtspPlayerHelper.VlcPlayStatus.Stopped -> {

                        when (status.side) {

                            VlcRtspPlayerHelper.PlayerSide.LEFT -> {
                                isLeftPlaying = false
                            }

                            VlcRtspPlayerHelper.PlayerSide.RIGHT -> {
                                isRightPlaying = false
                            }
                        }

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = true,
                            showProgress = false,
                            title = "Stopped",
                            reason = status.reason
                        )
                    }

                    is VlcRtspPlayerHelper.VlcPlayStatus.NotPlaying -> {

                        when (status.side) {

                            VlcRtspPlayerHelper.PlayerSide.LEFT -> {
                                isLeftPlaying = false
                            }

                            VlcRtspPlayerHelper.PlayerSide.RIGHT -> {
                                isRightPlaying = false
                            }
                        }

                        val retryText =
                            if (status.maxRetryCount > 0) {
                                "Retry ${status.retryCount}/${status.maxRetryCount}"
                            } else {
                                ""
                            }

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = true,
                            showProgress = status.retryCount < status.maxRetryCount,
                            title = "Not playing",
                            reason = "${status.reason}\n$retryText".trim()
                        )
                    }

                    is VlcRtspPlayerHelper.VlcPlayStatus.Error -> {

                        when (status.side) {

                            VlcRtspPlayerHelper.PlayerSide.LEFT -> {
                                isLeftPlaying = false
                            }

                            VlcRtspPlayerHelper.PlayerSide.RIGHT -> {
                                isRightPlaying = false
                            }
                        }

                        val retryText =
                            if (status.maxRetryCount > 0) {
                                "Retry ${status.retryCount}/${status.maxRetryCount}"
                            } else {
                                ""
                            }

                        updatePlayerStatus(
                            side = status.side,
                            showOverlay = true,
                            showProgress = status.retryCount < status.maxRetryCount,
                            title = "Playback error",
                            reason = "${status.reason}\n$retryText".trim()
                        )
                    }
                }
            }
        }
    }

    private fun updatePlayerStatus(
        side: VlcRtspPlayerHelper.PlayerSide,
        showOverlay: Boolean,
        showProgress: Boolean,
        title: String,
        reason: String
    ) {

        if (_binding == null) {
            return
        }

        when (side) {

            VlcRtspPlayerHelper.PlayerSide.LEFT -> {

                binding.layoutLeftStatus.visibility =
                    if (showOverlay) View.VISIBLE else View.GONE

                binding.progressLeft.visibility =
                    if (showProgress) View.VISIBLE else View.GONE

                binding.tvLeftStatus.text =
                    title

                binding.tvLeftReason.text =
                    reason
            }

            VlcRtspPlayerHelper.PlayerSide.RIGHT -> {

                binding.layoutRightStatus.visibility =
                    if (showOverlay) View.VISIBLE else View.GONE

                binding.progressRight.visibility =
                    if (showProgress) View.VISIBLE else View.GONE

                binding.tvRightStatus.text =
                    title

                binding.tvRightReason.text =
                    reason
            }
        }
    }

    private fun resetRtspStatusUi() {

        if (_binding == null) {
            return
        }

        binding.layoutLeftStatus.visibility =
            View.VISIBLE

        binding.progressLeft.visibility =
            View.VISIBLE

        binding.tvLeftStatus.text =
            "Loading..."

        binding.tvLeftReason.text =
            "Waiting for ramp stream"

        binding.layoutRightStatus.visibility =
            View.VISIBLE

        binding.progressRight.visibility =
            View.VISIBLE

        binding.tvRightStatus.text =
            "Loading..."

        binding.tvRightReason.text =
            "Waiting for cage stream"
    }

    private fun hideRtspStatusUi() {

        if (_binding == null) {
            return
        }

        binding.layoutLeftStatus.visibility =
            View.GONE

        binding.layoutRightStatus.visibility =
            View.GONE
    }

    private fun updateAiMode(
        state: Boolean,
        availableDoors: String = AppPreferences.availableDoorsData
    ) {

        val mobileData =
            AvailableDoorsData(
                availableDoors = availableDoors,
                mobile = if (state) "1" else "0"
            )

        val jsonString =
            Gson().toJson(
                mobileData
            )

        AppLogger.log(
            tag = TAG,
            message = "Update AI Mode : $jsonString"
        )

        viewModel.updateAiModeData.postValue(
            jsonString
        )

        if (!AppPreferences.aiListeningMode) {

            showSendDataDialog(
                AppPreferences.availableDoorsData
            )
        }
    }

    private fun showSendDataDialog(
        availableDoors: String,
        builder: AlertDialog.Builder = msgBuilder
    ) {

        builder.setTitle(
            "Send Data"
        )

        builder.setMessage(
            "Available doors: $availableDoors"
        )

        builder.setPositiveButton(
            "OK"
        ) { dialog, _ ->

            dialog.dismiss()
        }

        val dialog =
            builder.create()

        dialog.show()

        android.os.Handler(
            android.os.Looper.getMainLooper()
        ).postDelayed(
            {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            },
            10000
        )
    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {

        viewModel.aiCountdown.observe(
            viewLifecycleOwner
        ) {

            if (it == 0L) {

                if (binding.tvAiCount.visibility == View.VISIBLE) {
                    binding.tvAiCount.visibility =
                        View.GONE
                }

                if (viewModel.aiStatus.value == -1) {
                    viewModel.aiStatus.postValue(
                        0
                    )
                }

            } else {

                if (binding.tvAiCount.visibility == View.GONE) {
                    binding.tvAiCount.visibility =
                        View.VISIBLE
                }

                binding.tvAiCount.text =
                    "$it"
            }
        }

        viewModel.aiStatus.observe(
            viewLifecycleOwner
        ) {

            AppPreferences.aiMode =
                it

            when (it) {

                1 -> {
                    binding.tgAiMode.isChecked =
                        true

                    aiButtonDisable =
                        false

                    binding.btnAiMode.text =
                        getString(
                            R.string.ai_mode_turn_on
                        )
                }

                0 -> {
                    binding.tgAiMode.isChecked =
                        false

                    aiButtonDisable =
                        false

                    binding.btnAiMode.text =
                        getString(
                            R.string.ai_mode_turn_off
                        )
                }

                -1 -> {
                    binding.tgAiMode.isChecked =
                        false

                    aiButtonDisable =
                        true

                    binding.btnAiMode.text =
                        getString(
                            R.string.initialized_to_turn_off
                        )
                }
            }
        }

        viewModel.doorsLiveData.observe(
            viewLifecycleOwner
        ) {

            if (!it.isNullOrEmpty()) {

                val doorList =
                    it.map { door ->
                        door.toDoorData()
                    }

                setConveyorList(
                    doorList
                )

            } else {

                saveDoorList(
                    doorList
                )
            }
        }

        viewModel.commendData.observe(
            viewLifecycleOwner
        ) {

            if (it != null) {

                TAG_OPEN_DOOR =
                    it.rampDoorOpen

                TAG_CLOSE_DOOR =
                    it.rampDoorClose

            } else {

                (activity as MainActivity)
                    .updateCommend()
            }
        }

        viewModel.statusData.observe(
            viewLifecycleOwner
        ) {

            if (it != null) {

                val newMypalmStatus =
                    it.data.mypalmStatus

                val newLrStarterStatus =
                    it.data.lrStarter

                if (previousMypalmStatus != newMypalmStatus) {

                    binding.btnDoorStatus.text =
                        when (newMypalmStatus) {

                            "0" -> getString(
                                R.string.scada_mode
                            )

                            "1" -> getString(
                                R.string.my_palm_mode
                            )

                            else -> getString(
                                R.string.manual_mode
                            )
                        }

                    previousMypalmStatus =
                        newMypalmStatus
                }

                if (previousLrStarterStatus != newLrStarterStatus) {

                    binding.tgMotor.isChecked =
                        newLrStarterStatus == "1"

                    previousLrStarterStatus =
                        newLrStarterStatus
                }

                binding.tvFfb1.visibility =
                    View.VISIBLE

                binding.tvFfb1.text =
                    "FFB 1: ${it.data.ffb1Ma} A"

            } else {

                binding.btnDoorStatus.text =
                    "--"

                binding.btnRampStatus.text =
                    "--"

                previousMypalmStatus =
                    null

                previousLrStarterStatus =
                    null

                binding.tvFfb1.visibility =
                    View.GONE
            }
        }
    }

    private fun saveDoorList(
        doorList: List<DoorData>
    ) {

        val doorTable =
            doorList.map {
                it.toDoorTable()
            }

        viewModel.insertAllDoors(
            doorTable
        )
    }

    private fun setConveyorList(
        list: List<DoorData>
    ) {

        selectDoor =
            null

        adapter =
            DoorAdapter(
                list,
                object : DoorAdapter.ActionClickListener {

                    override fun onActionClick(
                        data: DoorData
                    ) {

                        val newSelected =
                            !data.selected

                        val temp =
                            adapter.getList()

                        temp.forEach {
                            it.selected =
                                it.doorId == data.doorId &&
                                        newSelected
                        }

                        if (newSelected) {

                            val selectedData =
                                temp.firstOrNull {
                                    it.doorId == data.doorId
                                } ?: data

                            AppLogger.log(
                                tag = TAG,
                                message = "Door Selected : ${selectedData.doorId}"
                            )

                            startRtspView(
                                selectedData
                            )

                            selectDoor =
                                selectedData

                        } else {

                            AppLogger.log(
                                tag = TAG,
                                message = "Door Unselected : ${data.doorId}"
                            )

                            stopRtspView()

                            selectDoor =
                                null
                        }

                        adapter.updateDoor(
                            temp
                        )
                    }
                }
            )

        val displayMetrics =
            resources.displayMetrics

        val screenHeightDp =
            displayMetrics.heightPixels /
                    displayMetrics.density

        AppLogger.log(
            tag = TAG,
            message = "Screen Height DP : $screenHeightDp"
        )

        val spanCount =
            if (screenHeightDp < 700) {
                6
            } else {
                8
            }

        val gridLayoutManager =
            GridLayoutManager(
                requireContext(),
                spanCount
            )

        binding.rvDoors.layoutManager =
            gridLayoutManager

        binding.rvDoors.adapter =
            adapter

        binding.layoutBtns.visibility =
            View.VISIBLE
    }
    private fun startRtspView(
        doorData: DoorData
    ) {

        try {
            //"rtsp://9627b0bf2a7b.entrypoint.cloud.wowza.com:1935/app-p5260J38/66abe4b9_stream1"
            val rampRtsp = doorData.rampDoorRtsp.trim().replace(
                Regex("subtype=\\d+"),
                "subtype=1"
            )
            val cageRtsp = doorData.cageFillRtsp.trim().replace(
                Regex("subtype=\\d+"),
                "subtype=1"
            )

            if (
                rampRtsp.isBlank() ||
                cageRtsp.isBlank()
            ) {

                AppLogger.log(
                    tag = TAG,
                    message = "RTSP URL Missing For Door : ${doorData.doorId}"
                )

                showToast(
                    "RTSP URL missing for selected door"
                )

                binding.rtspLayout.visibility =
                    View.VISIBLE

                resetRtspStatusUi()

                updatePlayerStatus(
                    side = VlcRtspPlayerHelper.PlayerSide.LEFT,
                    showOverlay = true,
                    showProgress = false,
                    title = "URL missing",
                    reason = "Ramp RTSP URL is empty"
                )

                updatePlayerStatus(
                    side = VlcRtspPlayerHelper.PlayerSide.RIGHT,
                    showOverlay = true,
                    showProgress = false,
                    title = "URL missing",
                    reason = "Cage RTSP URL is empty"
                )

                return
            }

            AppLogger.log(
                tag = TAG,
                message = "Start RTSP Door : ${doorData.doorId}"
            )

            binding.rtspLayout.visibility =
                View.VISIBLE

            binding.tvDoor1.text =
                "Door ${doorData.doorId} - Ramp"

            binding.tvDoor2.text =
                "Door ${doorData.doorId} - Cage"

            resetRtspStatusUi()

            if (vlcHelper == null) {

                vlcHelper =
                    VlcRtspPlayerHelper(
                        requireContext()
                    )
            }

            setupVlcStatusCallback()
            isLeftPlaying = false
            isRightPlaying = false

            vlcHelper?.playBoth(
                leftVideoLayout = binding.vlcView1,
                leftRtspUrl = rampRtsp,
                rightVideoLayout = binding.vlcView2,
                rightRtspUrl = cageRtsp
            )

            playView =
                true

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            showToast(
                e.message ?: "RTSP play failed"
            )

            updatePlayerStatus(
                side = VlcRtspPlayerHelper.PlayerSide.LEFT,
                showOverlay = true,
                showProgress = false,
                title = "Start failed",
                reason = e.message ?: "Unknown error"
            )

            updatePlayerStatus(
                side = VlcRtspPlayerHelper.PlayerSide.RIGHT,
                showOverlay = true,
                showProgress = false,
                title = "Start failed",
                reason = e.message ?: "Unknown error"
            )
        }
    }

    private fun stopRtspView() {

        try {

            AppLogger.log(
                tag = TAG,
                message = "Stop RTSP View"
            )

            playView = false

            isLeftPlaying = false
            isRightPlaying = false

            binding.rtspLayout.visibility =
                View.GONE

            hideRtspStatusUi()

            Thread {

                try {

                    vlcHelper?.stopAll()

                } catch (e: Exception) {

                    AppLogger.logError(
                        tag = TAG,
                        exception = e
                    )
                }

            }.start()

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )
        }
    }

    private fun doorActionBtnPmc() {

        fun animatePress(
            view: View,
            isPressed: Boolean
        ) {

            view.animate()
                .scaleX(
                    if (isPressed) 0.92f else 1f
                )
                .scaleY(
                    if (isPressed) 0.92f else 1f
                )
                .alpha(
                    if (isPressed) 0.8f else 1f
                )
                .setDuration(
                    120
                )
                .start()
        }

        binding.btnOpen.setOnTouchListener { view, event ->

            AppLogger.log(
                tag = TAG,
                message = "Open Touch : selectDoor=$selectDoor"
            )

            val doorId =
                selectDoor
                    ?.doorId
                    ?.toIntOrNull()
                    ?.toString()

            if (doorId.isNullOrEmpty()) {

                AppLogger.log(
                    tag = TAG,
                    message = "Open Failed : DoorId Null Or Invalid"
                )

                return@setOnTouchListener true
            }

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    animatePress(
                        view,
                        true
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "Open Button Pressed"
                    )

                    mqttTopicAndMsg(
                        doorNo = doorId,
                        command = "open"
                    )
                }

                MotionEvent.ACTION_UP -> {

                    view.performClick()

                    animatePress(
                        view,
                        false
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "Open Button Released"
                    )

                    mqttTopicAndMsg(
                        doorNo = doorId,
                        command = "stop"
                    )
                }

                MotionEvent.ACTION_CANCEL -> {

                    animatePress(
                        view,
                        false
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "Open Button Cancelled"
                    )

                    mqttTopicAndMsg(
                        doorNo = doorId,
                        command = "stop"
                    )
                }
            }

            true
        }

        binding.btnClose.setOnTouchListener { view, event ->

            AppLogger.log(
                tag = TAG,
                message = "Close Touch : selectDoor=$selectDoor"
            )

            val doorId =
                selectDoor
                    ?.doorId
                    ?.toIntOrNull()
                    ?.toString()

            if (doorId.isNullOrEmpty()) {

                AppLogger.log(
                    tag = TAG,
                    message = "Close Failed : DoorId Null Or Invalid"
                )

                return@setOnTouchListener true
            }

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    animatePress(
                        view,
                        true
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "Close Button Pressed"
                    )

                    mqttTopicAndMsg(
                        doorNo = doorId,
                        command = "close"
                    )
                }

                MotionEvent.ACTION_UP -> {

                    view.performClick()

                    animatePress(
                        view,
                        false
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "Close Button Released"
                    )

                    mqttTopicAndMsg(
                        doorNo = doorId,
                        command = "stop"
                    )
                }

                MotionEvent.ACTION_CANCEL -> {

                    animatePress(
                        view,
                        false
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "Close Button Cancelled"
                    )

                    mqttTopicAndMsg(
                        doorNo = doorId,
                        command = "stop"
                    )
                }
            }

            true
        }
    }

    fun mqttTopicAndMsg(
        doorNo: String,
        command: String
    ) {

        try {

            val gson =
                Gson()

            val commandData =
                PmcDoorCommand(
                    door = doorNo,
                    mode = "ai",
                    command = command
                )

            val topic =
                "mill/loading_ramp/$doorNo/command"

            val json =
                gson.toJson(
                    commandData
                )

            AppLogger.log(
                tag = TAG,
                message = "MQTT Topic : $topic"
            )

            AppLogger.log(
                tag = TAG,
                message = "MQTT Data : $json"
            )

            viewModel.updateDoorPmc.postValue(
                Pair(
                    topic,
                    json
                )
            )

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )
        }
    }

    private fun generateMsg(
        tag: String,
        value: Int
    ) {

        selectDoor?.let {

            val modifyTag =
                tag.replace(
                    "[DOOR_X]",
                    it.doorId
                )

            val data =
                WData(
                    w = listOf(
                        TagData(
                            tag = modifyTag,
                            value = value
                        )
                    )
                )

            val json =
                Gson().toJson(
                    data
                )

            AppLogger.log(
                tag = TAG,
                message = "Generate Msg : $json"
            )

            viewModel.updateDoor.postValue(
                json
            )
        }
    }

    private fun updateMotor(
        tag: String = "LoadingRamp:LRStarter_Cmd",
        state: Boolean
    ) {

        val data =
            WData(
                w = listOf(
                    TagData(
                        tag = tag,
                        value = if (state) 1 else 0
                    )
                )
            )

        val json =
            Gson().toJson(
                data
            )

        AppLogger.log(
            tag = TAG,
            message = "Update Motor : $json"
        )

        viewModel.updateStarter.postValue(
            json
        )
    }

    private fun showToast(
        message: String
    ) {

        try {

            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )
        }
    }

    override fun onPause() {
        super.onPause()

        AppLogger.log(
            tag = TAG,
            message = "HomeFragment onPause"
        )

        vlcHelper?.pauseAll()
    }

    override fun onResume() {
        super.onResume()

        AppLogger.log(
            tag = TAG,
            message = "HomeFragment onResume"
        )

        if (playView && selectDoor != null) {

            startRtspView(
                selectDoor!!
            )
        }
    }

    override fun onDestroyView() {

        AppLogger.log(
            tag = TAG,
            message = "HomeFragment onDestroyView"
        )

        vlcHelper?.release()
        vlcHelper =
            null

        _binding =
            null

        super.onDestroyView()
    }

    companion object {
        const val TAG =
            "HomeFragment"
    }
}
