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
import android.widget.TextView
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
    private var previousMypalmStatus: String? = null
    private var previousFfbSpeeds = FfbSpeedStatus("", "", "", "", "")
    private var previousFfbStatuses = FfbRunningStatus("", "", "", "", "")


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

        viewModel.ffbLastStatus.observe(viewLifecycleOwner) {
            with(binding) {
                if (it != null) {
                    // Create a list of conveyor run statuses, image views, and resources
                    val conveyors = listOf(
                        Triple(it.ffb1Run, imgGearConveyor1, R.raw.gear_conveyor_3_ffb1 to R.drawable.gear_conveyor_3_ffb1),
                        Triple(it.ffb2Run, imgGearConveyor2, R.raw.gear_conveyor_3_ffb2 to R.drawable.gear_conveyor_3_ffb2),
                        Triple(it.ffb3Run, imgGearConveyor3, R.raw.gear_conveyor_3_ffb3 to R.drawable.gear_conveyor_3_ffb3),
                        Triple(it.ffb4Run, imgGearConveyor4, R.raw.gear_conveyor_3_ffb4 to R.drawable.gear_conveyor_3_ffb4),
                        Triple(it.ffb5Run, imgGearConveyor5, R.raw.gear_conveyor_3_ffb5 to R.drawable.gear_conveyor_3_ffb5)
                    )

                    conveyors.forEachIndexed { index, (runStatus, imgView, resources) ->
                        val (gifResource, staticResource) = resources
                        // Get the previous run status from the map
                        val prevStatus = previousRunStatus[index] ?: ""

                        // Only update the image if the status has changed
                        if (prevStatus != runStatus) {
                            ffbConveyorGif(
                                imgView,
                                if (runStatus == "1") gifResource else staticResource
                            )
                            // Update the map with the new status
                            previousRunStatus[index] = runStatus
                        }
                    }
                } else {
                    val imageViews = listOf(
                        imgGearConveyor1 to R.drawable.gear_conveyor_3_ffb1,
                        imgGearConveyor2 to R.drawable.gear_conveyor_3_ffb2,
                        imgGearConveyor3 to R.drawable.gear_conveyor_3_ffb3,
                        imgGearConveyor4 to R.drawable.gear_conveyor_3_ffb4,
                        imgGearConveyor5 to R.drawable.gear_conveyor_3_ffb5
                    )

                    imageViews.forEachIndexed { index, (imgView, staticResource) ->
                        Glide.with(requireContext()).clear(imgView)
                        imgView.setImageResource(staticResource)
                        // Clear previous run statuses when no data is available
                        previousRunStatus[index] = ""
                    }
                }
            }
        }

        viewModel.statusData.observe(viewLifecycleOwner) {
            if (it != null) {
                with(binding) {
                    // Check if mypalmStatus has changed before updating the UI
                    if (previousMypalmStatus != it.data.mypalmStatus) {
                        btnDoorStatus.text = when (it.data.mypalmStatus) {
                            "1" -> getString(R.string.my_palm_mode)
                            "0" -> getString(R.string.scada_mode)
                            else -> getString(R.string.manual_mode)
                        }
                        previousMypalmStatus = it.data.mypalmStatus
                    }

                    val ffb = FfbRunningStatus(
                        ffb1Run = it.data.ffb1Run,
                        ffb2Run = it.data.ffb2Run,
                        ffb3Run = it.data.ffb3Run,
                        ffb4Run = it.data.ffb4Run,
                        ffb5Run = it.data.ffb5Run
                    )
                    val ffbModes = FfbModeStatus(
                        ffb1Mode = it.data.ffb1Run,
                        ffb2Mode = it.data.ffb2Run,
                        ffb3Mode = it.data.ffb3Run,
                        ffb4Mode = it.data.ffb4Run,
                        ffb5Mode = it.data.ffb5Run
                    )



                    // Only post ffbLastStatus if it's different from the previous one
                    if (previousFfbStatuses != ffb) {
                        viewModel.ffbLastStatus.postValue(ffb)
                        previousFfbStatuses = ffb
                    }

                    // Update FFB speeds only if they have changed
                    val newFfbSpeeds = FfbSpeedStatus(
                        ffb1Ma = it.data.ffb1Ma,
                        ffb2Ma = it.data.ffb2Ma,
                        ffb3Ma = it.data.ffb3Ma,
                        ffb4Ma = it.data.ffb4Ma,
                        ffb5Ma = it.data.ffb5Ma
                    )

                    if (previousFfbSpeeds != newFfbSpeeds) {
                        tvFfbSpeed1.text = "${newFfbSpeeds.ffb1Ma} A"
                        tvFfbSpeed2.text = "${newFfbSpeeds.ffb2Ma} A"
                        tvFfbSpeed3.text = "${newFfbSpeeds.ffb3Ma} A"
                        tvFfbSpeed4.text = "${newFfbSpeeds.ffb4Ma} A"
                        tvFfbSpeed5.text = "${newFfbSpeeds.ffb5Ma} A"
                        previousFfbSpeeds = newFfbSpeeds
                    }

                    // Set status and background for each FFB TextView
                    setFfbStatus(
                        layoutFfb1,
                        viewFfb1,
                        tvFfbStatus1,
                        tvFfbMode1,
                        it.data.ffb1Run,
                        it.data.ffb1Estop,
                        it.data.ffb1Auto,
                        it.data.ffb1Manual
                    )
                    setFfbStatus(
                        layoutFfb2,
                        viewFfb2,
                        tvFfbStatus2,
                        tvFfbMode2,
                        it.data.ffb2Run,
                        it.data.ffb2Estop,
                        it.data.ffb2Auto,
                        it.data.ffb2Manual
                    )
                    setFfbStatus(
                        layoutFfb3,
                        viewFfb3,
                        tvFfbStatus3,
                        tvFfbMode3,
                        it.data.ffb3Run,
                        it.data.ffb3Estop,
                        it.data.ffb3Auto,
                        it.data.ffb3Manual
                    )
                    setFfbStatus(
                        layoutFfb4,
                        viewFfb4,
                        tvFfbStatus4,
                        tvFfbMode4,
                        it.data.ffb4Run,
                        it.data.ffb4Estop,
                        it.data.ffb4Auto,
                        it.data.ffb4Manual
                    )
                    setFfbStatus(
                        layoutFfb5,
                        viewFfb5,
                        tvFfbStatus5,
                        tvFfbMode5,
                        it.data.ffb5Run,
                        it.data.ffb5Estop,
                        it.data.ffb5Auto,
                        it.data.ffb5Manual
                    )




                }
            } else {
                with(binding) {
                    // Reset to default values
                    btnDoorStatus.text = "--"
                    tvFfbSpeed1.text = "0.0 A"
                    tvFfbSpeed2.text = "0.0 A"
                    tvFfbSpeed3.text = "0.0 A"
                    tvFfbSpeed4.text = "0.0 A"
                    tvFfbSpeed5.text = "0.0 A"

                    // Reset status and background for each FFB TextView
                    setFfbStatus(
                        layoutFfb1,
                        viewFfb1,
                        tvFfbStatus1,
                        tvFfbMode1,
                        "--",
                        "0",
                        "0",
                        "0"
                    )
                    setFfbStatus(
                        layoutFfb2,
                        viewFfb2,
                        tvFfbStatus2,
                        tvFfbMode2,
                        "--",
                        "0",
                        "0",
                        "0"
                    )
                    setFfbStatus(
                        layoutFfb3,
                        viewFfb3,
                        tvFfbStatus3,
                        tvFfbMode3,
                        "--",
                        "0",
                        "0",
                        "0"
                    )
                    setFfbStatus(
                        layoutFfb4,
                        viewFfb4,
                        tvFfbStatus4,
                        tvFfbMode4,
                        "--",
                        "0",
                        "0",
                        "0"
                    )
                    setFfbStatus(
                        layoutFfb5,
                        viewFfb5,
                        tvFfbStatus5,
                        tvFfbMode5,
                        "--",
                        "0",
                        "0",
                        "0"
                    )

                    // Clear previous values when data is null
                    previousMypalmStatus = null
                    previousFfbSpeeds = FfbSpeedStatus("", "", "", "", "")
                    previousFfbStatuses = FfbRunningStatus("", "", "", "", "")
                }
            }
        }
    }

    private fun setFfbStatus(
        layout: LinearLayout,
        lineView: View,
        tvStatus: TextView,
        tvMode: TextView,
        status: String,
        eStop: String,
        ffbAuto: String,
        ffbManual: String
    ) {
        // Determine the final status text
        val finalStatus = if (eStop == "1") {
            tvStatus.context.getString(R.string.e_stop)
        } else {
            when (status) {
                "1" -> tvStatus.context.getString(R.string.run)
                "0" -> tvStatus.context.getString(R.string.stop)
                else -> "--"
            }
        }

        // Set mode text and status text
        tvMode.text = if (ffbAuto == "1") {
            getString(R.string.auto)
        }else if (ffbManual == "1"){
            getString(R.string.manual)
        }else{
            "--"
        }
        tvStatus.text = finalStatus

        // Function to set the background tint
        fun setTint(colorRes: Int) {
            val color = ContextCompat.getColor(tvStatus.context, colorRes)
            layout.backgroundTintList = ColorStateList.valueOf(color)
            lineView.backgroundTintList = ColorStateList.valueOf(color)
        }

        // Apply the background tint based on finalStatus
        when (finalStatus) {
            tvStatus.context.getString(R.string.run) -> setTint(R.color.japanese_laurel)
            tvStatus.context.getString(R.string.e_stop) -> setTint(R.color.flamingo)
            "--" -> setTint(R.color.color_background_1)
            else -> setTint(R.color.flamingo)  // Default case for unexpected values
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
                    /*when (actionTag) {
                        FFB_START_TAG -> {
                            setImages(true)
                        }

                        FFB_STOP_TAG -> {
                            setImages(false)
                        }

                        FFB_EME_STOP_TAG -> {
                            setImages(false)
                        }
                    }*/
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