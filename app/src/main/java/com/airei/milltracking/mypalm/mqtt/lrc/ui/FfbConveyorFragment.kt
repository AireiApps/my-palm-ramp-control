package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbModeStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbRunningStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbSpeedStatus
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentFfbConveyorBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.CMD_FFB_EME_STOP
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.CMD_FFB_START
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.CMD_FFB_STOP
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.bumptech.glide.Glide
import com.google.gson.Gson


class FfbConveyorFragment : Fragment() {
    private var _binding: FragmentFfbConveyorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    private var FFB_START_TAG :String = ""
    private var FFB_STOP_TAG :String = ""
    private var FFB_EME_STOP_TAG :String = ""

    private var previousRunStatus: MutableMap<Int, String> = mutableMapOf()

    // Variables to store previous values
    private var previousMyPalmStatus: String? = null
    private var previousFfbSpeed = FfbSpeedStatus("", "", "", "", "")
    private var previousFfbRunState = FfbRunningStatus("", "", "", "", "")
    private var previousSfbMode = FfbModeStatus("", "", "", "", "")



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFfbConveyorBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })
        observeData()
        //setImages()
    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {
        viewModel.commendData.observe(viewLifecycleOwner) {
            if (it != null) {
                FFB_START_TAG = it.FFB.start
                FFB_STOP_TAG = it.FFB.stop
                FFB_EME_STOP_TAG = it.FFB.emergencyStop
                binding.btnStart.setOnTouchListener(handleButtonTouch(CMD_FFB_START))
                binding.btnStop.setOnTouchListener(handleButtonTouch(CMD_FFB_STOP))
                binding.btnEmergencyStop.setOnTouchListener(handleButtonTouch(CMD_FFB_EME_STOP))
            } else {
                (activity as MainActivity).updateCommend()
            }
        }

        viewModel.statusData.observe(viewLifecycleOwner) {
            with(binding) {
                if (it != null) {
                    val status = when (it.data.mypalmStatus) {
                        "1" -> getString(R.string.my_palm_mode)
                        "0" -> getString(R.string.scada_mode)
                        else -> getString(R.string.manual_mode)
                    }
                    checkMyPalmStatus(status)
                    val ffbSpeeds = FfbSpeedStatus(
                        ffb1Ma = "${it.data.ffb1Ma} A",
                        ffb2Ma = "${it.data.ffb2Ma} A",
                        ffb3Ma = "${it.data.ffb3Ma} A",
                        ffb4Ma = "${it.data.ffb4Ma} A",
                        ffb5Ma = "${it.data.ffb5Ma} A"
                    )
                    checkFfbSpeed(ffbSpeeds)
                    var ffbRunStates = FfbRunningStatus(
                        ffb1Run = if (it.data.ffb1Run == "1") getString(R.string.run) else getString(
                            R.string.stop
                        ),
                        ffb2Run = if (it.data.ffb2Run == "1") getString(R.string.run) else getString(
                            R.string.stop
                        ),
                        ffb3Run = if (it.data.ffb3Run == "1") getString(R.string.run) else getString(
                            R.string.stop
                        ),
                        ffb4Run = if (it.data.ffb4Run == "1") getString(R.string.run) else getString(
                            R.string.stop
                        ),
                        ffb5Run = if (it.data.ffb5Run == "1") getString(R.string.run) else getString(
                            R.string.stop
                        )
                    )
                    ffbRunStates = FfbRunningStatus(
                        ffb1Run = if (it.data.ffb1Estop == "1") getString(R.string.e_stop) else ffbRunStates.ffb1Run,
                        ffb2Run = if (it.data.ffb2Estop == "1") getString(R.string.e_stop) else ffbRunStates.ffb2Run,
                        ffb3Run = if (it.data.ffb3Estop == "1") getString(R.string.e_stop) else ffbRunStates.ffb3Run,
                        ffb4Run = if (it.data.ffb4Estop == "1") getString(R.string.e_stop) else ffbRunStates.ffb4Run,
                        ffb5Run = if (it.data.ffb5Estop == "1") getString(R.string.e_stop) else ffbRunStates.ffb5Run
                    )
                    checkFfbRunStates(ffbRunStates)
                    val ffbModes = FfbModeStatus(
                        ffb1Mode = if (it.data.ffb1Auto == "1") getString(R.string.ffb_auto) else if (it.data.ffb1Manual == "1") getString(
                            R.string.ffb_manual
                        ) else "--",
                        ffb2Mode = if (it.data.ffb2Auto == "1") getString(R.string.ffb_auto) else if (it.data.ffb1Manual == "1") getString(
                            R.string.ffb_manual
                        ) else "--",
                        ffb3Mode = if (it.data.ffb3Auto == "1") getString(R.string.ffb_auto) else if (it.data.ffb1Manual == "1") getString(
                            R.string.ffb_manual
                        ) else "--",
                        ffb4Mode = if (it.data.ffb4Auto == "1") getString(R.string.ffb_auto) else if (it.data.ffb1Manual == "1") getString(
                            R.string.ffb_manual
                        ) else "--",
                        ffb5Mode = if (it.data.ffb5Auto == "1") getString(R.string.ffb_auto) else if (it.data.ffb1Manual == "1") getString(
                            R.string.ffb_manual
                        ) else "--"
                    )
                    checkFfbModes(ffbModes)
                } else {
                    checkMyPalmStatus("--")
                    checkFfbSpeed(FfbSpeedStatus())
                    checkFfbRunStates(FfbRunningStatus())
                    checkFfbModes(FfbModeStatus())
                }
            }
        }
    }

    private fun checkMyPalmStatus(status: String) {
        with(binding) {
            if (previousMyPalmStatus != status) {
                btnDoorStatus.text = status
                previousMyPalmStatus = status
            }
        }
    }

    private fun checkFfbModes(ffbModes: FfbModeStatus) {
        with(binding) {
            if (previousSfbMode != ffbModes) {
                if (previousSfbMode.ffb1Mode != ffbModes.ffb1Mode) {
                    tvFfbMode1.text = ffbModes.ffb1Mode
                }
                if (previousSfbMode.ffb2Mode != ffbModes.ffb2Mode) {
                    tvFfbMode2.text = ffbModes.ffb2Mode
                }
                if (previousSfbMode.ffb3Mode != ffbModes.ffb3Mode) {
                    tvFfbMode3.text = ffbModes.ffb3Mode
                }
                if (previousSfbMode.ffb4Mode != ffbModes.ffb4Mode) {
                    tvFfbMode4.text = ffbModes.ffb4Mode
                }
                if (previousSfbMode.ffb5Mode != ffbModes.ffb5Mode) {
                    tvFfbMode5.text = ffbModes.ffb5Mode
                }
                previousSfbMode = ffbModes
            }
        }
    }

    private fun checkFfbRunStates(ffbRunStates: FfbRunningStatus) {
        with(binding) {
            if (previousFfbRunState != ffbRunStates) {
                if (previousFfbRunState.ffb1Run != ffbRunStates.ffb1Run) {
                    tvFfbStatus1.text = ffbRunStates.ffb1Run
                    setTint(layoutFfb1,viewFfb1,ffbRunStates.ffb1Run)
                    val imgGearConveyor = Triple(ffbRunStates.ffb1Run, imgGearConveyor1, R.raw.gear_conveyor_3_ffb1 to R.drawable.gear_conveyor_3_ffb1)
                    setGifPlay(imgGearConveyor)
                }
                if (previousFfbRunState.ffb2Run != ffbRunStates.ffb2Run) {
                    tvFfbStatus2.text = ffbRunStates.ffb2Run
                    setTint(layoutFfb2,viewFfb2,ffbRunStates.ffb2Run)
                    val imgGearConveyor = Triple(ffbRunStates.ffb2Run, imgGearConveyor2, R.raw.gear_conveyor_3_ffb2 to R.drawable.gear_conveyor_3_ffb2)
                    setGifPlay(imgGearConveyor)
                }
                if (previousFfbRunState.ffb3Run != ffbRunStates.ffb3Run) {
                    tvFfbStatus3.text = ffbRunStates.ffb3Run
                    setTint(layoutFfb3,viewFfb3,ffbRunStates.ffb3Run)
                    val imgGearConveyor = Triple(ffbRunStates.ffb3Run, imgGearConveyor3, R.raw.gear_conveyor_3_ffb3 to R.drawable.gear_conveyor_3_ffb3)
                    setGifPlay(imgGearConveyor)
                }
                if (previousFfbRunState.ffb4Run != ffbRunStates.ffb4Run) {
                    tvFfbStatus4.text = ffbRunStates.ffb4Run
                    setTint(layoutFfb4,viewFfb4,ffbRunStates.ffb4Run)
                    val imgGearConveyor = Triple(ffbRunStates.ffb4Run, imgGearConveyor4, R.raw.gear_conveyor_3_ffb4 to R.drawable.gear_conveyor_3_ffb4)
                    setGifPlay(imgGearConveyor)
                }
                if (previousFfbRunState.ffb5Run != ffbRunStates.ffb5Run) {
                    tvFfbStatus5.text = ffbRunStates.ffb5Run
                    setTint(layoutFfb5,viewFfb5,ffbRunStates.ffb5Run)
                    val imgGearConveyor = Triple(ffbRunStates.ffb5Run, imgGearConveyor5, R.raw.gear_conveyor_3_ffb5 to R.drawable.gear_conveyor_3_ffb5)
                    setGifPlay(imgGearConveyor)
                }
                previousFfbRunState = ffbRunStates
            }
        }
    }

    private fun setTint(
        layout: LinearLayout,
        lineView: View,
        finalStatus: String
    ) {
        var colorRes = R.color.color_background_1
        colorRes = when (finalStatus) {
            getString(R.string.run) -> (R.color.japanese_laurel)
            getString(R.string.e_stop) -> (R.color.flamingo)
            "--" -> (R.color.color_background_1)
            else -> (R.color.flamingo)
        }
        val color = ContextCompat.getColor(requireContext(), colorRes)
        layout.backgroundTintList = ColorStateList.valueOf(color)
        lineView.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun setGifPlay(imgGearConveyor1: Triple<String, ImageView, Pair<Int, Int>>) {
        if (imgGearConveyor1.first == getString(R.string.run)){
            ffbConveyorGif(imgGearConveyor1.second,imgGearConveyor1.third.first)
        }else{
            ffbConveyorGif(imgGearConveyor1.second,imgGearConveyor1.third.second)
        }
    }

    private fun checkFfbSpeed(ffbSpeeds: FfbSpeedStatus) {
        with(binding) {
            if (previousFfbSpeed != ffbSpeeds) {
                if (previousFfbRunState.ffb1Run != ffbSpeeds.ffb1Ma) {
                    tvFfbSpeed1.text = ffbSpeeds.ffb1Ma
                }
                if (previousFfbRunState.ffb2Run != ffbSpeeds.ffb2Ma) {
                    tvFfbSpeed2.text = ffbSpeeds.ffb2Ma
                }
                if (previousFfbRunState.ffb3Run != ffbSpeeds.ffb3Ma) {
                    tvFfbSpeed3.text = ffbSpeeds.ffb3Ma
                }
                if (previousFfbRunState.ffb4Run != ffbSpeeds.ffb4Ma) {
                    tvFfbSpeed4.text = ffbSpeeds.ffb4Ma
                }
                if (previousFfbRunState.ffb5Run != ffbSpeeds.ffb5Ma) {
                    tvFfbSpeed5.text = ffbSpeeds.ffb5Ma
                }
                previousFfbSpeed = ffbSpeeds
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    fun handleButtonTouch(actionTag: String) = View.OnTouchListener { v, event ->

        if ((activity as MainActivity).mqttConnectionCheck()) {
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    generateMsg(actionTag, 1)
                    false // Return true to indicate the event was handled
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    generateMsg(actionTag, 0)
                    false // Return true to indicate the event was handled
                }

                else -> false // Return false for other actions to allow default behavior
            }
        }else{
            showToast("Mqtt connection not available. Please check mqtt connection.")
            false
        }

    }

    private fun generateMsg(tagStr: String, value: Int) {
        val data = WData(w = listOf(TagData(tag = tagStr, value = value)))
        if ((activity as MainActivity).mqttConnectionCheck()) {
            viewModel.updateDoor.postValue(Gson().toJson(data))
        }else{
            if (value == 0) {
                showToast("Mqtt connection not available. Please check mqtt connection.")
            }
        }
    }

    private fun ffbConveyorGif(imageView: ImageView, gifResId: Int) {
        Glide.with(this).clear(imageView)
        Glide.with(this)
            .asGif()
            .load(gifResId)
            .error(gifResId)
            .into(imageView)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "FfbConveyorFragment"
    }

}