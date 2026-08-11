package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.app.AlertDialog
import android.os.Bundle
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.MessageListener
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.StuckDoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorStatusData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentStuckDoorBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_DOOR_SRUCK
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StuckDoorFragment : Fragment(), MessageListener {

    private var _binding: FragmentStuckDoorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    private lateinit var adapter: StuckDoorAdapter

    private val allDoors = mutableListOf<DoorData>()
    private val stuckDoorList = MutableLiveData<MutableList<String>>(mutableListOf())

    private var stuckDialog: AlertDialog? = null

    private var clearDoorDialog: AlertDialog? = null

    companion object {

        private const val TAG = "StuckDoorFragment"

        @JvmStatic
        fun newInstance() = StuckDoorFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentStuckDoorBinding.inflate(
            inflater, container, false
        )

        AppLogger.log(TAG, "StuckDoorFragment onCreateView")

        return binding.root
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view, savedInstanceState
        )

        AppLogger.log(TAG, "StuckDoorFragment onViewCreated")

        setupRecyclerView()

        observeDoors()
    }

    // =====================================================
    // OBSERVE
    // =====================================================

    private fun observeDoors() {

        stuckDoorList.observe(viewLifecycleOwner) { list ->
            AppLogger.log(TAG, "Stuck door list updated in UI")
            adapter.updateData(
                allDoors, list
            )
        }

        viewModel.doorsLiveData.observe(
            viewLifecycleOwner
        ) { list ->

            AppLogger.log(
                TAG, "Doors size: ${list?.size ?: 0}"
            )

            allDoors.apply {
                clear()
                addAll(list?.map {
                    it.toDoorData()
                } ?: emptyList())
            }

            updateAdapter()
        }

    }

    private fun updateAdapter() {

        adapter.updateData(
            allDoors, AppPreferences.stuckDoorsData.split(",")
        )

        AppLogger.log(
            TAG, "Adapter updated"
        )
    }

    // =====================================================
    // RECYCLER VIEW
    // =====================================================

    private fun setupRecyclerView() {

        adapter = StuckDoorAdapter(
            mutableListOf(),
            mutableListOf()
        ) { door, isStuck ->

            AppLogger.log(
                TAG,
                "Door clicked: ${door.doorName}, current stuck status: $isStuck"
            )
            // Already stuck -> ask confirmation to clear
            if (isStuck){
                // Check already showing
                if (clearDoorDialog != null &&
                    clearDoorDialog?.isShowing == true
                ) {
                    clearDoorDialog?.dismiss()
                }

                clearDoorDialog =
                    AlertDialog.Builder(requireContext())
                        .setTitle("Clear Door Alert")
                        .setMessage(
                            "Are you sure you want to clear the stuck alert for '${door.doorName}'?"
                        )
                        .setCancelable(false)
                        .setPositiveButton("Yes") { dialog, _ ->

                            sendDoorStatus(
                                door = door,
                                stuckStatus = 0
                            )

                            dialog.dismiss()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()

                clearDoorDialog?.show()
            }else{
                /*sendDoorStatus(
                    door = door,
                    stuckStatus = 1
                )*/
            }

            }

        val spanCount =
            if (resources.displayMetrics.heightPixels / resources.displayMetrics.density < 700) 6 else 8

        binding.rvDoors.apply {

            layoutManager = GridLayoutManager(
                requireContext(), spanCount
            )

            adapter = this@StuckDoorFragment.adapter
        }

        AppLogger.log(
            TAG, "Span Count: $spanCount"
        )
    }

    // =====================================================
    // SEND MQTT
    // =====================================================

    private fun sendDoorStatus(
        door: DoorData, stuckStatus: Int
    ) {

        val currentTime = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
        ).format(Date())

        val data = DoorStatusData(
            currentTime, door.doorId.toInt(), stuckStatus
        )

        val json = Gson().toJson(data)

        AppLogger.log(
            TAG, "Sending door status MQTT: $json"
        )

        viewModel.updateDoorPmc.postValue(
            Pair(
                MQTT_DOOR_SRUCK, json
            )
        )
    }

    // =====================================================
    // MQTT CALLBACKS
    // =====================================================
    override fun onReceiveMessage(
        topic: String, message: String
    ) {

        try {

            val stuckDoorData = Gson().fromJson(
                message, DoorStatusData::class.java
            )

            val doorId = stuckDoorData.door.toString()

            val alreadyStuckDoors =
                AppPreferences.stuckDoorsData.split(",").filter { it.isNotEmpty() }.toMutableList()

            AppLogger.log(
                TAG, "Already stuck doors: $alreadyStuckDoors"
            )

            if (stuckDoorData.stuck == 1) {

                // Add if not already exists
                if (!alreadyStuckDoors.contains(doorId)) {

                    alreadyStuckDoors.add(doorId)

                    AppLogger.log(
                        TAG, "Added stuck door: $doorId"
                    )
                }

            } else if (stuckDoorData.stuck == 0) {

                // Remove if exists
                alreadyStuckDoors.remove(doorId)

                AppLogger.log(
                    TAG, "Removed stuck door: $doorId"
                )
            }

            AppPreferences.stuckDoorsData = alreadyStuckDoors.joinToString(",")

            AppLogger.log(
                TAG, "Final stuck doors: ${AppPreferences.stuckDoorsData}"
            )

            activity?.runOnUiThread {

                updateAdapter()
            }

        } catch (e: Exception) {

            AppLogger.logError(
                TAG, e,
            )
        }
    }

    override fun onResume() {

        super.onResume()

        (requireActivity() as MainActivity).setMqttListener(this)
    }

    override fun onPause() {

        super.onPause()
        if (clearDoorDialog != null &&
            clearDoorDialog?.isShowing == true
        ) {
            clearDoorDialog?.dismiss()
        }
        (requireActivity() as MainActivity).removeMqttListener()
    }


    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroyView() {

        stuckDialog?.dismiss()
        stuckDialog = null

        _binding = null

        super.onDestroyView()

        AppLogger.log(TAG, "StuckDoorFragment onDestroyView")
    }
}
