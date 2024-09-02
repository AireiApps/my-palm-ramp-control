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
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.iot.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentHomeBinding
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import kotlin.math.log


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectDoor: DoorData? = null

    private var doorState : Boolean = false

    private var isHold : Boolean = false

    private val viewModel: AppViewModel by activityViewModels()

    private var repeat : Int = 0

    lateinit var adapter: DoorAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        if (isHold){
            runFunction()
        }
    }

    private fun runFunction() {
        if (isHold){
            updateDoor(doorState)
            repeat += 1
            Log.i(TAG, "btn : $doorState -> $repeat")
            if (selectDoor != null && repeat < AppPreferences.repeatCnt) {
                handler.postDelayed(runnable, 500)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
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
        (activity as MainActivity).setMqttService()
        setConveyorList()
        doorActionbtn()
        binding.fabConfig.setOnClickListener {
            findNavController().navigate(R.id.mqttConfigFragment)
        }
    }

    private fun setConveyorList(list: List<DoorData> = doorList) {
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
    private fun handleSingleClick(doorStateValue: Boolean) {
        updateDoor(doorStateValue)
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun doorActionbtn() {

        // Common function to handle touch events for both buttons
        fun handleButtonTouch(doorStateValue: Boolean) = View.OnTouchListener { _, event ->
            Log.i(TAG, "doorActionbtn: ------------> ${event.action}")
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.i(TAG, "ACTION_DOWN")
                    isHold = true
                    doorState = doorStateValue
                    handler.postDelayed(runnable, 500) // 500 milliseconds for long press
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.i(TAG, "ACTION_UP")
                    repeat = 0
                    isHold = false
                    handler.removeCallbacks(runnable)
                    Log.i(TAG, "handleButtonTouch: ${event.eventTime - event.downTime}")
                    Log.i(TAG, "handleButtonTouch: ${event.rawX} / ${ event.rawY}")
                    if (event.eventTime - event.downTime < 100 && isWholeNumber(event.rawX) && isWholeNumber(event.rawY)) {
                        handleSingleClick(doorStateValue)
                    }
                    true
                }
                else -> false
            }
        }

        // Set the touch listener for each button using the common function
        binding.btnClose.setOnTouchListener(handleButtonTouch(false)) // Close action
        binding.btnOpen.setOnTouchListener(handleButtonTouch(true))   // Open action
    }

    fun isWholeNumber(value: Float): Boolean {
        return value == value.toInt().toFloat()
    }


    private fun updateDoor(state: Boolean){
        if (selectDoor != null) {
            viewModel.publishedMsg.postValue((state to selectDoor!!))
        } else {
            Toast.makeText(requireContext(), "Door not selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        selectDoor = null
    }

    override fun onResume() {
        super.onResume()
        if (this::adapter.isInitialized){
            selectDoor = adapter.getList().find { it.selected }
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