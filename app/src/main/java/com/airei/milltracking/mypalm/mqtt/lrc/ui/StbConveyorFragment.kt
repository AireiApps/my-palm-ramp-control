package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.SfbRunningStatus
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
        viewModel.sfbLastStatus.observe(viewLifecycleOwner) {
            with(binding) {
                if (it != null) {
                    val conveyors = listOf(
                        Triple(
                            it.sfb1Run,
                            imgGearConveyor1,
                            R.raw.gear_conveyor_3_sfb1 to R.drawable.gear_conveyor_3_sfb1
                        ),
                        Triple(
                            it.sfb2Run,
                            imgGearConveyor2,
                            R.raw.gear_conveyor_3_sfb2 to R.drawable.gear_conveyor_3_sfb2
                        ),
                        Triple(
                            it.sfb3Run,
                            imgGearConveyor3,
                            R.raw.gear_conveyor_3_sfb3 to R.drawable.gear_conveyor_3_sfb3
                        )
                    )

                    conveyors.forEach { (runStatus, imgView, resources) ->
                        val (gifResource, staticResource) = resources
                        sfbConveyorGif(
                            imgView,
                            if (runStatus == "1") gifResource else staticResource
                        )
                    }
                } else {
                    val imageViews = listOf(
                        imgGearConveyor1 to R.drawable.gear_conveyor_3_sfb1,
                        imgGearConveyor2 to R.drawable.gear_conveyor_3_sfb2,
                        imgGearConveyor3 to R.drawable.gear_conveyor_3_sfb3
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
                    // Set button status based on mypalmStatus
                    btnDoorStatus.text = when (it.data.mypalmStatus) {
                        "0" -> getString(R.string.my_palm_mode)
                        "1" -> getString(R.string.scada_mode)
                        else -> getString(R.string.manual_mode)
                    }

                    val sfb = SfbRunningStatus(
                        sfb1Run = it.data.sfb1Run,
                        sfb2Run = it.data.sfb2Run,
                        sfb3Run = it.data.sfb3Run
                    )

                    viewModel.sfbLastStatus.postValue(sfb)

                    // Update the current values
                    tvSfbSpeed1.text = "${it.data.sfb1Ma} A"
                    tvSfbSpeed2.text = "${it.data.sfb2Ma} A"
                    tvSfbSpeed3.text = "${it.data.sfb3Ma} A"

                    // Set status and background for each SFB TextView
                    setSfbStatus(layoutSfb1, tvSfbStatus1, it.data.sfb1Run, it.data.sfb1Estop)
                    setSfbStatus(layoutSfb2, tvSfbStatus2, it.data.sfb2Run, it.data.sfb2Estop)
                    setSfbStatus(layoutSfb3, tvSfbStatus3, it.data.sfb3Run, it.data.sfb3Estop)
                }
            } else {
                with(binding) {
                    btnDoorStatus.text = "--"
                    tvSfbSpeed1.text = "0.0 A"
                    tvSfbSpeed2.text = "0.0 A"
                    tvSfbSpeed3.text = "0.0 A"

                    // Reset status and background for each SFB TextView
                    setSfbStatus(layoutSfb1, tvSfbStatus1, "--", "0")
                    setSfbStatus(layoutSfb2, tvSfbStatus2, "--", "0")
                    setSfbStatus(layoutSfb3, tvSfbStatus3, "--", "0")
                }
            }
        }

    }

    private fun sfbConveyorGif(imageView: ImageView, gifResId: Int) {
        Glide.with(this).clear(imageView)
        Glide.with(this)
            .asGif()
            .load(gifResId)
            .error(gifResId)
            .into(imageView)
    }

    // Helper function to set status and background tint for SFB
    private fun setSfbStatus(
        layout: ConstraintLayout,
        tvStatus: TextView,
        runStatus: String,
        estopStatus: String
    ) {
        val finalStatus = if (estopStatus == "1") {
            tvStatus.context.getString(R.string.e_stop)
        } else {
            if (runStatus == "1") tvStatus.context.getString(R.string.run)
            else if (runStatus == "0") tvStatus.context.getString(R.string.stop)
            else ("--")
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
                Log.i(TAG, "handleButtonTouch: actionTag = $actionTag")
                false // Return true to indicate the event was handled
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                generateMsg(actionTag, 0)
                false // Return true to indicate the event was handled
            }

            else -> false // Return false for other actions to allow default behavior
        }
        } else {
            showToast("Mqtt connection not available. Please check mqtt connection.")
            false
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "SfbConveyorFragment"
    }
}