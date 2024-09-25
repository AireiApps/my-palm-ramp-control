package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.AlartFfbBinding
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
        setImages()
    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {
        viewModel.commendData.observe(viewLifecycleOwner){
            if (it != null){
                FFB_START_TAG = it.FFB.start
                FFB_STOP_TAG = it.FFB.stop
                FFB_EME_STOP_TAG = it.FFB.emergencyStop
                binding.btnStart.setOnTouchListener(handleButtonTouch(CMD_FFB_START))
                binding.btnStop.setOnTouchListener(handleButtonTouch(CMD_FFB_STOP))
                binding.btnEmergencyStop.setOnTouchListener(handleButtonTouch(CMD_FFB_EME_STOP))
            }else{
                (activity as MainActivity).updateCommend()
            }
        }
        viewModel.statusData.observe(viewLifecycleOwner){
            if (it != null){
                with(binding){
                    btnDoorStatus.text = when(it.data.mypalmStatus){
                        "0" -> getString(R.string.my_palm_mode)
                        "1" -> getString(R.string.scada_mode)
                        else -> getString(R.string.manual_mode)
                    }
                    tvFfbSpeed1.text = "${it.data.ffb1Ma} A"
                    tvFfbSpeed2.text = "${it.data.ffb2Ma} A"
                    tvFfbSpeed3.text = "${it.data.ffb3Ma} A"
                    tvFfbSpeed4.text = "${it.data.ffb4Ma} A"
                    tvFfbSpeed5.text = "${it.data.ffb5Ma} A"
                    tvFfbStatus1.text = if (it.data.ffb1Run == "1") (getString(R.string.run)) else (getString(R.string.stop))
                    tvFfbStatus2.text = if (it.data.ffb2Run == "1") (getString(R.string.run)) else (getString(R.string.stop))
                    tvFfbStatus3.text = if (it.data.ffb3Run == "1") (getString(R.string.run)) else (getString(R.string.stop))
                    tvFfbStatus4.text = if (it.data.ffb4Run == "1") (getString(R.string.run)) else (getString(R.string.stop))
                    tvFfbStatus5.text = if (it.data.ffb5Run == "1") (getString(R.string.run)) else (getString(R.string.stop))
                    tvFfbStatus1.text = if (it.data.ffb1Estop == "1") (getString(R.string.e_stop)) else tvFfbStatus1.text.toString()
                    tvFfbStatus2.text = if (it.data.ffb2Estop == "1") (getString(R.string.e_stop)) else tvFfbStatus2.text.toString()
                    tvFfbStatus3.text = if (it.data.ffb3Estop == "1") (getString(R.string.e_stop)) else tvFfbStatus3.text.toString()
                    tvFfbStatus4.text = if (it.data.ffb4Estop == "1") (getString(R.string.e_stop)) else tvFfbStatus4.text.toString()
                    tvFfbStatus5.text = if (it.data.ffb5Estop == "1") (getString(R.string.e_stop)) else tvFfbStatus5.text.toString()

                }
            }else {
                with(binding) {
                    btnDoorStatus.text = "--"
                    tvFfbSpeed1.text = "0.0 A"
                    tvFfbSpeed2.text = "0.0 A"
                    tvFfbSpeed3.text = "0.0 A"
                    tvFfbSpeed4.text = "0.0 A"
                    tvFfbSpeed5.text = "0.0 A"
                    tvFfbStatus1.text = "--"
                    tvFfbStatus2.text = "--"
                    tvFfbStatus3.text = "--"
                    tvFfbStatus4.text = "--"
                    tvFfbStatus5.text = "--"
                }
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
                    when (actionTag) {
                        FFB_START_TAG -> {
                            setImages(true)
                        }

                        FFB_STOP_TAG -> {
                            setImages(false)
                        }

                        FFB_EME_STOP_TAG -> {
                            setImages(false)
                        }
                    }
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

    private fun setImages(isSetGif: Boolean = false) {
        try {
            // Clear previous images and stop any ongoing GIFs
            Glide.with(this).clear(binding.imgGearConveyor1)
            Glide.with(this).clear(binding.imgGearConveyor2)
            Glide.with(this).clear(binding.imgGearConveyor3)
            Glide.with(this).clear(binding.imgGearConveyor4)
            Glide.with(this).clear(binding.imgGearConveyor5)

            if (isSetGif) {
                // Load GIFs from raw resources
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_ffb1)
                    .error(R.drawable.gear_conveyor_3_ffb1)
                    .into(binding.imgGearConveyor1)
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_ffb2) // Adjusted to load a different GIF
                    .error(R.drawable.gear_conveyor_3_ffb2)
                    .into(binding.imgGearConveyor2)
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_ffb3) // Adjusted to load a different GIF
                    .error(R.drawable.gear_conveyor_3_ffb3)
                    .into(binding.imgGearConveyor3)
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_ffb4)
                    .error(R.drawable.gear_conveyor_3_ffb4)
                    .into(binding.imgGearConveyor4)
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_ffb5)
                    .error(R.drawable.gear_conveyor_3_ffb5)
                    .into(binding.imgGearConveyor5)
            } else {
                // Load drawables
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_ffb1)
                    .error(R.drawable.gear_conveyor_3_ffb1)
                    .into(binding.imgGearConveyor1)
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_ffb2) // Adjusted to load a different drawable
                    .error(R.drawable.gear_conveyor_3_ffb2)
                    .into(binding.imgGearConveyor2)
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_ffb3) // Adjusted to load a different drawable
                    .error(R.drawable.gear_conveyor_3_ffb3)
                    .into(binding.imgGearConveyor3)
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_ffb4)
                    .error(R.drawable.gear_conveyor_3_ffb4)
                    .into(binding.imgGearConveyor4)
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_ffb5)
                    .error(R.drawable.gear_conveyor_3_ffb5)
                    .into(binding.imgGearConveyor5)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Handle any exceptions and set default images in case of failure
            Glide.with(this).clear(binding.imgGearConveyor1)
            Glide.with(this).clear(binding.imgGearConveyor2)
            Glide.with(this).clear(binding.imgGearConveyor3)
            Glide.with(this).clear(binding.imgGearConveyor4)
            Glide.with(this).clear(binding.imgGearConveyor5)
            binding.imgGearConveyor1.setImageResource(R.drawable.gear_conveyor_3_ffb1)
            binding.imgGearConveyor2.setImageResource(R.drawable.gear_conveyor_3_ffb2)
            binding.imgGearConveyor3.setImageResource(R.drawable.gear_conveyor_3_ffb3)
            binding.imgGearConveyor4.setImageResource(R.drawable.gear_conveyor_3_ffb4)
            binding.imgGearConveyor4.setImageResource(R.drawable.gear_conveyor_3_ffb5)
        }
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