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
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FfbRunningStatus
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
                    val conveyors = listOf(
                        Triple(it.ffb1Run, imgGearConveyor1, R.raw.gear_conveyor_3_ffb1 to R.drawable.gear_conveyor_3_ffb1),
                        Triple(it.ffb2Run, imgGearConveyor2, R.raw.gear_conveyor_3_ffb2 to R.drawable.gear_conveyor_3_ffb2),
                        Triple(it.ffb3Run, imgGearConveyor3, R.raw.gear_conveyor_3_ffb3 to R.drawable.gear_conveyor_3_ffb3),
                        Triple(it.ffb4Run, imgGearConveyor4, R.raw.gear_conveyor_3_ffb4 to R.drawable.gear_conveyor_3_ffb4),
                        Triple(it.ffb5Run, imgGearConveyor5, R.raw.gear_conveyor_3_ffb5 to R.drawable.gear_conveyor_3_ffb5)
                    )

                    conveyors.forEach { (runStatus, imgView, resources) ->
                        val (gifResource, staticResource) = resources
                        ffbConveyorGif(imgView, if (runStatus == "1") gifResource else staticResource)
                    }
                } else {
                    val imageViews = listOf(
                        imgGearConveyor1 to R.drawable.gear_conveyor_3_ffb1,
                        imgGearConveyor2 to R.drawable.gear_conveyor_3_ffb2,
                        imgGearConveyor3 to R.drawable.gear_conveyor_3_ffb3,
                        imgGearConveyor4 to R.drawable.gear_conveyor_3_ffb4,
                        imgGearConveyor5 to R.drawable.gear_conveyor_3_ffb5
                    )

                    imageViews.forEach { (imgView, staticResource) ->
                        Glide.with(requireContext()).clear(imgView)
                        imgView.setImageResource(staticResource)
                    }
                }
            }
        }


        viewModel.statusData.observe(viewLifecycleOwner) {
            if (it != null) {
                with(binding) {
                    btnDoorStatus.text = when (it.data.mypalmStatus) {
                        "0" -> getString(R.string.my_palm_mode)
                        "1" -> getString(R.string.scada_mode)
                        else -> getString(R.string.manual_mode)
                    }

                    val ffb = FfbRunningStatus(
                        ffb1Run = it.data.ffb1Run,
                        ffb2Run = it.data.ffb2Run,
                        ffb3Run = it.data.ffb3Run,
                        ffb4Run = it.data.ffb4Run,
                        ffb5Run = it.data.ffb5Run
                    )

                    viewModel.ffbLastStatus.postValue(ffb)

                    // Update the current values
                    tvFfbSpeed1.text = "${it.data.ffb1Ma} A"
                    tvFfbSpeed2.text = "${it.data.ffb2Ma} A"
                    tvFfbSpeed3.text = "${it.data.ffb3Ma} A"
                    tvFfbSpeed4.text = "${it.data.ffb4Ma} A"
                    tvFfbSpeed5.text = "${it.data.ffb5Ma} A"

                    // Set status and background for each FFB TextView
                    setFfbStatus(layoutFfb1, tvFfbStatus1, it.data.ffb1Run, it.data.ffb1Estop)
                    setFfbStatus(layoutFfb2, tvFfbStatus2, it.data.ffb2Run, it.data.ffb2Estop)
                    setFfbStatus(layoutFfb3, tvFfbStatus3, it.data.ffb3Run, it.data.ffb3Estop)
                    setFfbStatus(layoutFfb4, tvFfbStatus4, it.data.ffb4Run, it.data.ffb4Estop)
                    setFfbStatus(layoutFfb5, tvFfbStatus5, it.data.ffb5Run, it.data.ffb5Estop)
                }
            } else {
                with(binding) {
                    btnDoorStatus.text = "--"
                    tvFfbSpeed1.text = "0.0 A"
                    tvFfbSpeed2.text = "0.0 A"
                    tvFfbSpeed3.text = "0.0 A"
                    tvFfbSpeed4.text = "0.0 A"
                    tvFfbSpeed5.text = "0.0 A"

                    // Reset status and background for each FFB TextView
                    setFfbStatus(layoutFfb1, tvFfbStatus1, "--", "0")
                    setFfbStatus(layoutFfb2, tvFfbStatus2, "--", "0")
                    setFfbStatus(layoutFfb3, tvFfbStatus3, "--", "0")
                    setFfbStatus(layoutFfb4, tvFfbStatus4, "--", "0")
                    setFfbStatus(layoutFfb5, tvFfbStatus5, "--", "0")
                }
            }
        }
    }

    private fun setFfbStatus(
        layout: LinearLayout,
        tvStatus: TextView,
        status: String,
        estop: String
    ) {
        val finalStatus = if (estop == "1") {
            tvStatus.context.getString(R.string.e_stop)
        } else {
            if (status == "1") tvStatus.context.getString(R.string.run)
            else if (status == "0") tvStatus.context.getString(R.string.stop)
            else "--"
        }

        tvStatus.text = finalStatus

        // Set background tint based on the status
        when (finalStatus) {
            tvStatus.context.getString(R.string.run) -> {
                layout.setBackgroundTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            tvStatus.context,
                            R.color.japanese_laurel
                        )
                    )
                )
            }

            "--" -> {
                layout.setBackgroundTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            tvStatus.context,
                            R.color.color_background_1
                        )
                    )
                )
            }

            tvStatus.context.getString(R.string.e_stop) -> {
                layout.setBackgroundTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            tvStatus.context,
                            R.color.flamingo
                        )
                    )
                )
            }

            else -> {
                layout.setBackgroundTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            tvStatus.context,
                            R.color.flamingo
                        )
                    )
                )
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