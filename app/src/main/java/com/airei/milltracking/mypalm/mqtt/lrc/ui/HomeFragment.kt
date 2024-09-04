package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
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
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.iot.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.MyPalmApp
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.RtspConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentHomeBinding
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectDoor: DoorData? = null

    private var doorState : Boolean = false

    private var isHold : Boolean = false

    private val viewModel: AppViewModel by activityViewModels()

    private var repeat : Int = 0

    lateinit var adapter: DoorAdapter

    var libVlc: LibVLC? = null
    var mediaPlayer: MediaPlayer? = null

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
        libVlc = LibVLC(MyPalmApp.instance)
        binding.rtspLayout.visibility = View.GONE
        binding.fabConfig.setOnClickListener {
            findNavController().navigate(R.id.mqttConfigFragment)
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
                            data.rtspConfig?.let { openRtspView(it, data.doorId) }
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
        if (mediaPlayer != null) {
            try {
                mediaPlayer!!.stop()
                mediaPlayer!!.detachViews()
                mediaPlayer!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::adapter.isInitialized){
            selectDoor = adapter.getList().find { it.selected }
        }
        Log.d(TAG, "onResume: ")
    }

    fun openRtspView(rtspConfig: RtspConfig, doorId: String) {
        binding.rtspLayout.visibility = View.VISIBLE
        binding.tvDoorId.text = doorId
        try {
            val url: String =
                "rtsp://${rtspConfig.username}:${rtspConfig.password}@${rtspConfig.ip}:554/cam/realmonitor?channel=${rtspConfig.channel}&subtype=${rtspConfig.subtype}"

            Log.i(TAG, "showPopupWindow: url = $url")

            // Initialize VLC components using view binding for the video layout

            mediaPlayer = MediaPlayer(libVlc)
            val videoLayout = binding.surfaceView

            if (mediaPlayer != null) {
                mediaPlayer!!.attachViews(videoLayout, null, false, false)

                // Set video scaling mode to fit the view
                mediaPlayer!!.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT

                val media = Media(libVlc, Uri.parse(url))
                media.setHWDecoderEnabled(true, false)
                media.addOption(":network-caching=600")

                // Set up close button to stop the media and release resources
                binding.imgClose.setOnClickListener {
                    binding.rtspLayout.visibility = View.GONE
                    mediaPlayer!!.stop()
                    mediaPlayer!!.release()
                }

                mediaPlayer!!.media = media
                media.release()
                mediaPlayer!!.play()
            }
        } catch (e: Exception) {
            Log.d(TAG, "showPopupWindow: ", e)
            binding.rtspLayout.visibility = View.GONE
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "HomeFragment"
    }
}