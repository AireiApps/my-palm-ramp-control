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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.TagData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.WData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentSfbConveyorBinding
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.bumptech.glide.Glide
import com.google.gson.Gson
import pl.droidsonroids.gif.GifImageView


class SfbConveyorFragment : Fragment() {
    private var _binding: FragmentSfbConveyorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    private var SFB_START_TAG = ""
    private var SFB_STOP_TAG = ""
    private var SFB_EME_STOP_TAG = ""

    lateinit var gifImageView: GifImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSfbConveyorBinding.inflate(layoutInflater, container, false)
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
        setImages(false)
    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {
        viewModel.commendData.observe(viewLifecycleOwner) {
            if (it != null) {
                SFB_START_TAG = it.SFB.start
                SFB_STOP_TAG = it.SFB.stop
                SFB_EME_STOP_TAG = it.SFB.emergencyStop

                Log.i(
                    TAG,
                    "observeData: SFB_START_TAG $SFB_START_TAG , SFB_STOP_TAG $SFB_STOP_TAG , SFB_EME_STOP_TAG $SFB_EME_STOP_TAG"
                )
                binding.btnStart.setOnTouchListener(handleButtonTouch(SFB_START_TAG))
                binding.btnStop.setOnTouchListener(handleButtonTouch(SFB_STOP_TAG))
                binding.btnEmergencyStop.setOnTouchListener(handleButtonTouch(SFB_EME_STOP_TAG))
            } else {
                (activity as MainActivity).updateCommend()
            }
        }
        viewModel.statusData.observe(viewLifecycleOwner) {
            if (it != null) {
                with(binding) {
                    btnDoorStatus.text = when (it.data.mypalmStatus) {
                        "0" -> "MyPalm Mode"
                        "1" -> "Sara Mode"
                        else -> "Manual Mode"
                    }
                    tvSfbSpeed1.text = "${it.data.sfb1Ma} A"
                    tvSfbSpeed2.text = "${it.data.sfb2Ma} A"
                    tvSfbSpeed3.text = "${it.data.sfb3Ma} A"
                    tvSfbStatus1.text =
                        if (it.data.sfb1Start == "0") (getString(R.string.run)) else (getString(R.string.stop))
                    tvSfbStatus2.text =
                        if (it.data.sfb2Start == "0") (getString(R.string.run)) else (getString(R.string.stop))
                    tvSfbStatus3.text =
                        if (it.data.sfb3Start == "0") (getString(R.string.run)) else (getString(R.string.stop))
                    tvSfbStatus1.text =
                        if (it.data.sfb1Estop == "1") (getString(R.string.e_stop)) else tvSfbStatus1.text.toString()
                    tvSfbStatus2.text =
                        if (it.data.sfb2Estop == "1") (getString(R.string.e_stop)) else tvSfbStatus2.text.toString()
                    tvSfbStatus3.text =
                        if (it.data.sfb3Estop == "1") (getString(R.string.e_stop)) else tvSfbStatus3.text.toString()
                }
            } else {
                with(binding) {
                    tvSfbSpeed1.text = "0.0 A"
                    tvSfbSpeed2.text = "0.0 A"
                    tvSfbSpeed3.text = "0.0 A"
                    tvSfbStatus1.text = "--"
                    tvSfbStatus2.text = "--"
                    tvSfbStatus3.text = "--"
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun handleButtonTouch(actionTag: String) = View.OnTouchListener { v, event ->
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                generateMsg(actionTag, 1)
                Log.i(TAG, "handleButtonTouch: actionTag = $actionTag")
                when (actionTag) {
                    SFB_START_TAG -> {
                        setImages(true)
                    }

                    SFB_STOP_TAG -> {
                        setImages(false)
                    }

                    SFB_EME_STOP_TAG -> {
                        setImages(false)
                    }
                }
                false // Return true to indicate the event was handled
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                generateMsg(actionTag, 0)
                false // Return true to indicate the event was handled
            }

            else -> false // Return false for other actions to allow default behavior
        }
    }

    private fun generateMsg(tagStr: String, value: Int) {
        val data = WData(w = listOf(TagData(tag = tagStr, value = value)))
        if ((activity as MainActivity).mqttConnectionCheck()) {
            viewModel.updateDoor.postValue(Gson().toJson(data))
        } else {
            if (value == 0) {
                showToast("Mqtt connection not available. Please check mqtt connection.")
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setImages(isSetGif: Boolean = false) {
        try {
            // Clear previous images and stop any ongoing GIFs
            Glide.with(this).clear(binding.imgGearConveyor1)
            Glide.with(this).clear(binding.imgGearConveyor2)
            Glide.with(this).clear(binding.imgGearConveyor3)

            if (isSetGif) {
                // Load GIFs from raw resources
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_sfb1)
                    .error(R.drawable.gear_conveyor_3_sfb1)
                    .into(binding.imgGearConveyor1)
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_sfb2) // Adjusted to load a different GIF
                    .error(R.drawable.gear_conveyor_3_sfb2)
                    .into(binding.imgGearConveyor2)
                Glide.with(this)
                    .asGif()
                    .load(R.raw.gear_conveyor_3_sfb3) // Adjusted to load a different GIF
                    .error(R.drawable.gear_conveyor_3_sfb3)
                    .into(binding.imgGearConveyor3)
            } else {
                // Load drawables
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_sfb1)
                    .error(R.drawable.gear_conveyor_3_sfb1)
                    .into(binding.imgGearConveyor1)
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_sfb2) // Adjusted to load a different drawable
                    .error(R.drawable.gear_conveyor_3_sfb2)
                    .into(binding.imgGearConveyor2)
                Glide.with(this)
                    .load(R.drawable.gear_conveyor_3_sfb3) // Adjusted to load a different drawable
                    .error(R.drawable.gear_conveyor_3_sfb3)
                    .into(binding.imgGearConveyor3)
                Glide.with(this)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Handle any exceptions and set default images in case of failure
            Glide.with(this).clear(binding.imgGearConveyor1)
            Glide.with(this).clear(binding.imgGearConveyor2)
            Glide.with(this).clear(binding.imgGearConveyor3)
            binding.imgGearConveyor1.setImageResource(R.drawable.gear_conveyor_3_sfb1)
            binding.imgGearConveyor2.setImageResource(R.drawable.gear_conveyor_3_sfb2)
            binding.imgGearConveyor3.setImageResource(R.drawable.gear_conveyor_3_sfb3)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "SfbConveyorFragment"
    }
}