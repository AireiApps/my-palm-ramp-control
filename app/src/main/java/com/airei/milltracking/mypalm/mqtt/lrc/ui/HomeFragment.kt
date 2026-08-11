package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.PmcDoorCommand
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.VlcRtspPlayerHelper
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentHomeBinding
import com.airei.milltracking.mypalm.mqtt.lrc.ui.DoorsFragment.AvailableDoorsData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.isNetworkAvailable
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import androidx.core.view.isVisible
import com.airei.milltracking.mypalm.mqtt.lrc.commons.PlayerStatusViews
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectDoor: DoorData? = null

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: DoorAdapter

    private var vlcHelperL: VlcRtspPlayerHelper? = null
    private var vlcHelperR: VlcRtspPlayerHelper? = null

    private var playView = false

    private var leftRtsp:String?=null
    private var rightRtsp:String?=null

    private lateinit var leftStatusViews: PlayerStatusViews
    private lateinit var rightStatusViews: PlayerStatusViews


    private var clickListener = MutableLiveData<Pair<Boolean, String>>(
        Pair(false, "")
    )

    private var TAG_OPEN_DOOR: String = ""
    private var TAG_CLOSE_DOOR: String = ""

    private var aiButtonDisable: Boolean = false

    private var previousMypalmStatus: String? = null
    private var previousLrStarterStatus: String? = null

    private lateinit var msgBuilder: AlertDialog.Builder

    private var doorActionJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(
            inflater, container, false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view, savedInstanceState
        )

        AppLogger.log(
            tag = TAG, message = "HomeFragment onViewCreated"
        )

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    AppLogger.log(
                        tag = TAG, message = "Back Pressed : Finish Activity"
                    )

                    requireActivity().finish()
                }
            })

        binding.layoutBtns.visibility = View.INVISIBLE
        binding.rtspLayout.visibility = View.GONE

        aiButtonDisable = false

        msgBuilder = AlertDialog.Builder(requireContext())

        initVlcHelpers()

        setupRecyclerView()

        observeData()

        setupUI()

        doorActionBtnPmc()


        AppLogger.log(
            tag = TAG, message = "AI Listening Mode : ${AppPreferences.aiListeningMode}"
        )
    }

    private fun initVlcHelpers() {

        vlcHelperL = VlcRtspPlayerHelper(requireContext())
        vlcHelperR = VlcRtspPlayerHelper(requireContext())

        setLeftVlcCallback()
        setRightVlcCallback()
    }

    private fun handleBufferUi(
        views: PlayerStatusViews,
        percent: Int
    ) {

        updatePlayerUi(
            views,
            percent < 100,
            percent < 100,
            if (percent >= 100) "Playing" else "Buffer $percent%"
        )
    }

    private fun setRightVlcCallback() {

        vlcHelperR?.setStatusCallback { status ->

            when(status){

                is VlcRtspPlayerHelper.VlcPlayStatus.Opening -> {

                    AppLogger.log(TAG,"RIGHT Opening")

                    updatePlayerUi(
                        rightStatusViews,
                        true,
                        true,
                        "Opening Camera"
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.Buffering -> {

                    AppLogger.log(
                        TAG,
                        "RIGHT Buffer ${status.percent}"
                    )

                    handleBufferUi(
                        rightStatusViews,
                        status.percent
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.Playing -> {

                    AppLogger.log(
                        TAG,
                        "RIGHT Playing"
                    )

                    updatePlayerUi(
                        rightStatusViews,
                        false,
                        false,
                        "Playing"
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.Error -> {

                    AppLogger.log(
                        TAG,
                        "RIGHT Error ${status.reason}"
                    )

                    updatePlayerUi(
                        rightStatusViews,
                        true,
                        false,
                        "Error",
                        status.reason,
                        true
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.NotPlaying -> {

                    updatePlayerUi(
                        rightStatusViews,
                        true,
                        false,
                        "Disconnected",
                        status.reason,
                        true
                    )
                }

                else -> {}
            }
        }
    }

    private fun setLeftVlcCallback() {

        vlcHelperL?.setStatusCallback { status ->

            when(status){

                is VlcRtspPlayerHelper.VlcPlayStatus.Opening -> {

                    AppLogger.log(
                        TAG,
                        "LEFT Opening"
                    )

                    updatePlayerUi(
                        leftStatusViews,
                        true,
                        true,
                        "Opening Camera"
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.Buffering -> {

                    AppLogger.log(
                        TAG,
                        "LEFT Buffer ${status.percent}"
                    )

                    handleBufferUi(
                        leftStatusViews,
                        status.percent
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.Playing -> {

                    AppLogger.log(
                        TAG,
                        "LEFT Playing"
                    )

                    updatePlayerUi(
                        leftStatusViews,
                        false,
                        false,
                        "Playing"
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.Error -> {

                    AppLogger.log(
                        TAG,
                        "LEFT Error ${status.reason}"
                    )

                    updatePlayerUi(
                        leftStatusViews,
                        true,
                        false,
                        "Error",
                        status.reason,
                        true
                    )
                }

                is VlcRtspPlayerHelper.VlcPlayStatus.NotPlaying -> {

                    updatePlayerUi(
                        leftStatusViews,
                        true,
                        false,
                        "Disconnected",
                        status.reason,
                        true
                    )
                }

                else -> {}
            }
        }
    }

    private fun setupUI() {

        leftStatusViews = PlayerStatusViews(
            overlay = binding.layoutLeftStatus,
            progress = binding.progressLeft,
            status = binding.tvLeftStatus,
            reason = binding.tvLeftReason,
            retry = binding.btnRetryLeft
        )

        rightStatusViews = PlayerStatusViews(
            overlay = binding.layoutRightStatus,
            progress = binding.progressRight,
            status = binding.tvRightStatus,
            reason = binding.tvRightReason,
            retry = binding.btnRetryRight
        )

        binding.rtspLayout.visibility = View.GONE

        binding.imgClose.setOnClickListener {

            AppLogger.log(
                TAG,
                "Close Player"
            )

            playView=false

            binding.rtspLayout.isVisible=false

            vlcHelperL?.stop()
            vlcHelperR?.stop()

            leftRtsp=null
            rightRtsp=null
        }

        binding.btnRetryLeft.setOnClickListener {
            if (!isInternetAvailable()) return@setOnClickListener
            leftRtsp?.let {

                AppLogger.log(
                    TAG,
                    "Retry Left"
                )

                vlcHelperL?.play(
                    binding.vlcViewL,
                    it
                )
            }
        }

        binding.btnRetryRight.setOnClickListener {
            if (!isInternetAvailable()) return@setOnClickListener
            rightRtsp?.let {

                AppLogger.log(
                    TAG,
                    "Retry Right"
                )

                vlcHelperR?.play(
                    binding.vlcViewR,
                    it
                )
            }
        }

        binding.imgLog.setOnClickListener {
            AppLogger.log(tag = TAG, message = "Log Button Clicked")
            AppLogger.showLogFilesDialog(
                requireActivity()
            )
        }

        binding.tgAiMode.isChecked = false

        viewModel.aiStatus.postValue(AppPreferences.aiMode)

        binding.tgMotor.setOnClickListener {
            if (!isInternetAvailable()) {
                binding.tgMotor.isChecked = !binding.tgMotor.isChecked
                return@setOnClickListener
            }
            if ((activity as MainActivity).mqttConnectionCheck()) {
                updateMotor(state = binding.tgMotor.isChecked)
            } else {
                binding.tgMotor.isChecked = !binding.tgMotor.isChecked
                showToast("Mqtt connection not available. Please check mqtt connection.")
            }
        }

        binding.tgAiMode.setOnClickListener {

            if (!isInternetAvailable()) {
                binding.tgAiMode.isChecked = !binding.tgAiMode.isChecked
                return@setOnClickListener
            }

            if (aiButtonDisable) {
                binding.tgAiMode.isChecked = !binding.tgAiMode.isChecked
                showToast("Processing... Please wait until the operation completes.")
            } else {

                if ((activity as MainActivity).mqttConnectionCheck()) {
                    val aiState = binding.tgAiMode.isChecked

                    AppPreferences.aiMode = if (aiState) 1 else 0
                    if (AppPreferences.availableDoorsData.isNotEmpty()) {
                        updateAiMode(aiState)
                    } else {
                        showToast("Please select at least one door.")
                        findNavController().navigate(R.id.doorsFragment)
                    }

                } else {
                    binding.tgAiMode.isChecked = !binding.tgAiMode.isChecked
                    showToast("Mqtt connection not available. Please check mqtt connection.")
                }
            }
        }

        viewModel.aiStatus.postValue(AppPreferences.aiMode)
    }

    private fun updatePlayerUi(
        views: PlayerStatusViews,
        show:Boolean,
        loading:Boolean,
        status:String,
        reason:String="",
        retry:Boolean=false
    ){

        views.overlay.isVisible = show

        views.progress.isVisible = loading

        views.status.text = status

        views.reason.text = reason

        views.reason.isVisible =
            reason.isNotEmpty()

        views.retry.isVisible = retry
    }

    private fun updateAiMode(
        state: Boolean, availableDoors: String = AppPreferences.availableDoorsData
    ) {

        val mobileData =
            AvailableDoorsData(availableDoors = availableDoors, mobile = if (state) "1" else "0")
        val jsonString = Gson().toJson(mobileData)

        AppLogger.log(tag = TAG, message = "Update AI Mode : $jsonString")

        viewModel.updateAiModeData.postValue(jsonString)

        if (!AppPreferences.aiListeningMode) {
            showSendDataDialog(
                AppPreferences.availableDoorsData
            )
        }
    }

    private fun showSendDataDialog(
        availableDoors: String, builder: AlertDialog.Builder = msgBuilder
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

        val dialog = builder.create()

        dialog.show()

        Handler(
            Looper.getMainLooper()
        ).postDelayed(
            {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }, 10000
        )
    }

    @SuppressLint("SetTextI18n", "UseKtx")
    private fun observeData() {

        viewModel.aiCountdown.observe(
            viewLifecycleOwner
        ) {

            if (it == 0L) {

                if (binding.tvAiCount.isVisible) {
                    binding.tvAiCount.visibility = View.GONE
                }

                if (viewModel.aiStatus.value == -1) {
                    viewModel.aiStatus.postValue(
                        0
                    )
                }

            } else {

                if (binding.tvAiCount.visibility == View.GONE) {
                    binding.tvAiCount.visibility = View.VISIBLE
                }

                binding.tvAiCount.text = "$it"
            }
        }

        viewModel.aiStatus.observe(
            viewLifecycleOwner
        ) {

            AppPreferences.aiMode = it

            when (it) {

                1 -> {
                    binding.tgAiMode.isChecked = true

                    aiButtonDisable = false

                    binding.btnAiMode.text = getString(
                        R.string.ai_mode_turn_on
                    )
                }

                0 -> {
                    binding.tgAiMode.isChecked = false

                    aiButtonDisable = false

                    binding.btnAiMode.text = getString(
                        R.string.ai_mode_turn_off
                    )
                }

                -1,-2 -> {
                    binding.tgAiMode.isChecked = false

                    aiButtonDisable = true

                    binding.btnAiMode.text = getString(
                        R.string.initialized_to_turn_off
                    )
                }
            }
        }

        viewModel.doorsLiveData.observe(
            viewLifecycleOwner
        ) {

            if (!it.isNullOrEmpty()) {

                val doorList = it.map { door ->
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
                TAG_OPEN_DOOR = it.rampDoorOpen
                TAG_CLOSE_DOOR = it.rampDoorClose
            } else {
                (activity as MainActivity).updateCommend()
            }
        }

        viewModel.statusData.observe(
            viewLifecycleOwner
        ) {

            if (it != null) {

                val newMyPalmStatus = it.data.mypalmStatus
                val newLrStarterStatus = it.data.lrStarter
                if (previousMypalmStatus != newMyPalmStatus) {

                    binding.btnDoorStatus.text = when (newMyPalmStatus) {
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
                    previousMypalmStatus = newMyPalmStatus
                }

                if (previousLrStarterStatus != newLrStarterStatus) {

                    binding.tgMotor.isChecked = newLrStarterStatus == "1"
                    previousLrStarterStatus = newLrStarterStatus
                }

                val ffbText = "FFB 1: ${it.data.ffb1Ma} A"
                if (binding.tvFfb1.text.toString() != ffbText) {
                    binding.tvFfb1.text = ffbText
                }
                binding.tvFfb1.visibility = View.VISIBLE

            } else {

                binding.btnDoorStatus.text = "--"
                binding.btnRampStatus.text = "--"
                previousMypalmStatus = null
                previousLrStarterStatus = null
                binding.tvFfb1.visibility = View.GONE
            }
        }

        viewModel.cageFillData.observe(viewLifecycleOwner) { cageData ->
            if (cageData != null && ::adapter.isInitialized) {
                val currentList = adapter.currentList
                var hasChanged = false
                val newList = currentList.map { door ->
                    val fillValue = when (door.doorId) {
                        "1" -> cageData.rd1Fill
                        "2" -> cageData.rd2Fill
                        "3" -> cageData.rd3Fill
                        "4" -> cageData.rd4Fill
                        "5" -> cageData.rd5Fill
                        "6" -> cageData.rd6Fill
                        "7" -> cageData.rd7Fill
                        "8" -> cageData.rd8Fill
                        "9" -> cageData.rd9Fill
                        "10" -> cageData.rd10Fill
                        "11" -> cageData.rd11Fill
                        "12" -> cageData.rd12Fill
                        "13" -> cageData.rd13Fill
                        "14" -> cageData.rd14Fill
                        "15" -> cageData.rd15Fill
                        "16" -> cageData.rd16Fill
                        "17" -> cageData.rd17Fill
                        "18" -> cageData.rd18Fill
                        else -> null
                    }

                    val isFull = (fillValue?.toDoubleOrNull() ?: 0.0) > 90.0
                    if (door.isFull != isFull) {
                        hasChanged = true
                        door.copy(isFull = isFull)
                    } else {
                        door
                    }
                }
                if (hasChanged) {
                    adapter.submitList(newList)
                }
            }
        }
    }

    private fun saveDoorList(
        doorList: List<DoorData>
    ) {

        val doorTable = doorList.map { it.toDoorTable() }

        viewModel.insertAllDoors(doorTable)
    }

    private fun setupRecyclerView() {
        adapter = DoorAdapter(object : DoorAdapter.ActionClickListener {
            override fun onActionClick(data: DoorData) {
                if (!isInternetAvailable() || playView) return

                val newSelected = !data.selected
                val currentList = adapter.currentList
                
                val newList = currentList.map { 
                    it.copy(selected = it.doorId == data.doorId && newSelected)
                }

                if (newSelected) {
                    val selectedData = newList.firstOrNull { it.doorId == data.doorId } ?: data
                    AppLogger.log(tag = TAG, message = "Door Selected : ${selectedData.doorId}")
                    startRtspView(selectedData)
                    selectDoor = selectedData
                } else {
                    AppLogger.log(tag = TAG, message = "Door Unselected : ${data.doorId}")
                    selectDoor = null
                }

                adapter.submitList(newList)
            }
        })

        val displayMetrics = resources.displayMetrics
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        AppLogger.log(tag = TAG, message = "Screen Height DP : $screenHeightDp")

        val spanCount = if (screenHeightDp < 700) 6 else 8
        binding.rvDoors.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.rvDoors.adapter = adapter
    }

    private fun setConveyorList(
        list: List<DoorData>
    ) {
        val currentSelectedId = selectDoor?.doorId
        val newList = list.map { door ->
            door.copy(selected = door.doorId == currentSelectedId)
        }
        
        adapter.submitList(newList)
        binding.layoutBtns.visibility = View.VISIBLE
    }

    private fun startRtspView(
        doorData: DoorData
    ){

        try{

            leftRtsp =
                doorData.rampDoorRtsp
                    .trim()
                    .replace(
                        Regex("subtype=\\d+"),
                        "subtype=1"
                    )

            rightRtsp =
                doorData.cageFillRtsp
                    .trim()
                    .replace(
                        Regex("subtype=\\d+"),
                        "subtype=1"
                    )

            AppLogger.log(
                TAG,
                "Start RTSP\nL=$leftRtsp\nR=$rightRtsp"
            )

            playView=true

            binding.rtspLayout.isVisible=true

            binding.tvDoor1.text =
                "Door ${doorData.doorId}"

            binding.tvDoor2.text =
                "Door ${doorData.doorId}"

            //val dome = "rtsp://10.135.230.202:8080/h264_ulaw.sdp"
            //val dome = "rtsp://9627b0bf2a7b.entrypoint.cloud.wowza.com:1935/app-p5260J38/66abe4b9_stream1"

            leftRtsp?.let {

                vlcHelperL?.play(
                    binding.vlcViewL,
                    it
                )
            }

            rightRtsp?.let {
                vlcHelperR?.play(
                    binding.vlcViewR,
                    it
                )
            }

        }catch (e:Exception){

            AppLogger.logError(
                TAG,
                e,
            )
        }
    }

    private fun doorActionBtnPmc() {

        fun animatePress(
            view: View, isPressed: Boolean
        ) {

            view.animate().scaleX(
                if (isPressed) 0.92f else 1f
            ).scaleY(
                if (isPressed) 0.92f else 1f
            ).alpha(
                if (isPressed) 0.8f else 1f
            ).setDuration(
                120
            ).start()
        }

        binding.btnOpen.setOnTouchListener { view, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!isInternetAvailable()) return@setOnTouchListener true
                if (!(activity as MainActivity).mqttConnectionCheck()) {
                    showToast("Mqtt connection not available. Please check mqtt connection.")
                    return@setOnTouchListener true
                }
            }

            AppLogger.log(
                tag = TAG, message = "Open Touch : selectDoor=$selectDoor"
            )

            val doorId = selectDoor?.doorId?.toIntOrNull()?.toString()

            if (doorId.isNullOrEmpty()) {

                AppLogger.log(
                    tag = TAG, message = "Open Failed : DoorId Null Or Invalid"
                )

                return@setOnTouchListener true
            }

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    animatePress(
                        view, true
                    )

                    AppLogger.log(
                        tag = TAG, message = "Open Button Pressed"
                    )

                    doorActionJob?.cancel()
                    doorActionJob = lifecycleScope.launch {
                        mqttTopicAndMsg(
                            doorNo = doorId, command = "stop"
                        )
                        delay(300.milliseconds)
                        mqttTopicAndMsg(
                            doorNo = doorId, command = "open"
                        )
                    }
                }

                MotionEvent.ACTION_UP -> {

                    view.performClick()

                    animatePress(
                        view, false
                    )

                    AppLogger.log(
                        tag = TAG, message = "Open Button Released"
                    )

                    doorActionJob?.cancel()
                    mqttTopicAndMsg(
                        doorNo = doorId, command = "stop"
                    )
                }

                MotionEvent.ACTION_CANCEL -> {

                    animatePress(
                        view, false
                    )

                    AppLogger.log(
                        tag = TAG, message = "Open Button Cancelled"
                    )

                    doorActionJob?.cancel()
                    mqttTopicAndMsg(
                        doorNo = doorId, command = "stop"
                    )
                }
            }

            true
        }

        binding.btnClose.setOnTouchListener { view, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!isInternetAvailable()) return@setOnTouchListener true
                if (!(activity as MainActivity).mqttConnectionCheck()) {
                    showToast("Mqtt connection not available. Please check mqtt connection.")
                    return@setOnTouchListener true
                }
            }

            AppLogger.log(
                tag = TAG, message = "Close Touch : selectDoor=$selectDoor"
            )

            val doorId = selectDoor?.doorId?.toIntOrNull()?.toString()

            if (doorId.isNullOrEmpty()) {

                AppLogger.log(
                    tag = TAG, message = "Close Failed : DoorId Null Or Invalid"
                )

                return@setOnTouchListener true
            }

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    animatePress(
                        view, true
                    )

                    AppLogger.log(
                        tag = TAG, message = "Close Button Pressed"
                    )

                    doorActionJob?.cancel()
                    doorActionJob = lifecycleScope.launch {
                        mqttTopicAndMsg(
                            doorNo = doorId, command = "stop"
                        )
                        delay(300.milliseconds)
                        mqttTopicAndMsg(
                            doorNo = doorId, command = "close"
                        )
                    }
                }

                MotionEvent.ACTION_UP -> {

                    view.performClick()

                    animatePress(
                        view, false
                    )

                    AppLogger.log(
                        tag = TAG, message = "Close Button Released"
                    )

                    doorActionJob?.cancel()
                    mqttTopicAndMsg(
                        doorNo = doorId, command = "stop"
                    )
                }

                MotionEvent.ACTION_CANCEL -> {

                    animatePress(
                        view, false
                    )

                    AppLogger.log(
                        tag = TAG, message = "Close Button Cancelled"
                    )

                    doorActionJob?.cancel()
                    mqttTopicAndMsg(
                        doorNo = doorId, command = "stop"
                    )
                }
            }

            true
        }
    }

    fun mqttTopicAndMsg(
        doorNo: String, command: String
    ) {

        try {

            val gson = Gson()

            val commandData = PmcDoorCommand(
                door = doorNo, mode = "ai", command = command
            )

            val topic = "mill/loading_ramp/$doorNo/command"

            val json = gson.toJson(
                commandData
            )

            AppLogger.log(
                tag = TAG, message = "MQTT Topic : $topic"
            )

            AppLogger.log(
                tag = TAG, message = "MQTT Data : $json"
            )

            viewModel.updateDoorPmc.postValue(
                Pair(
                    topic, json
                )
            )

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG, exception = e,
                throwable = TODO()
            )
        }
    }

    private fun generateMsg(
        tag: String, value: Int
    ) {

        selectDoor?.let {

            val modifyTag = tag.replace("[DOOR_X]", it.doorId)
            val data = WData(w = listOf(TagData(tag = modifyTag, value = value)))
            val json = Gson().toJson(data)
            AppLogger.log(tag = TAG, message = "Generate Msg : $json")
            viewModel.updateDoor.postValue(json)
        }
    }

    private fun updateMotor(
        tag: String = "LoadingRamp:LRStarter_Cmd", state: Boolean
    ) {
        val data = WData(w = listOf(TagData(tag = tag, value = if (state) 1 else 0)))
        val json = Gson().toJson(data)
        AppLogger.log(tag = TAG, message = "Update Motor : $json")
        viewModel.updateStarter.postValue(json)
    }

    private fun showToast(
        message: String
    ) {
        try {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            AppLogger.logError(tag = TAG, exception = e)
        }
    }

    private fun isInternetAvailable(): Boolean {
        return if (isNetworkAvailable(requireContext())) {
            true
        } else {
            showNoInternetDialog()
            false
        }
    }

    private fun showNoInternetDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection and try again.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }, 3000)
    }

    override fun onPause() {
        super.onPause()

        AppLogger.log(tag = TAG, message = "HomeFragment onPause")
        vlcHelperL?.pause()
        vlcHelperR?.pause()
    }

    override fun onResume() {
        super.onResume()

        AppLogger.log(tag = TAG, message = "HomeFragment onResume")

        if(playView){
            vlcHelperL?.resume()
            vlcHelperR?.resume()
        }
    }

    override fun onDestroyView() {
        AppLogger.log(
            tag = TAG, message = "HomeFragment onDestroyView"
        )
        _binding = null
        vlcHelperL?.release()
        vlcHelperR?.release()

        vlcHelperL = null
        vlcHelperR = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "HomeFragment"
    }
}
