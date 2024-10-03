package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorSelectAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentDoorsBinding
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class DoorsFragment : Fragment() {
    private var _binding: FragmentDoorsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: DoorSelectAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoorsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
        observeData()
        setButton()
    }

    private fun setButton() {
        with(binding) {
            btnSelectAll.setOnClickListener {
                Log.d(TAG, "setButton: btnSelect")
                adapter.selectAll()
            }
            btnClear.setOnClickListener {
                Log.d(TAG, "setButton: btnClear")
                adapter.clearAll()
            }
            btnSave.setOnClickListener {
                val selectedDoors = adapter.getSelectedDoors().map { it.doorName }
                Log.d(TAG, "setButton: btnSave ${selectedDoors.joinToString(",")}")
                if (selectedDoors.isNotEmpty()) {
                    updateAiMode(selectedDoors.joinToString(","))
                    AppPreferences.availableDoorsData = selectedDoors.joinToString(",")
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please select at least one door.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeData() {
        viewModel.doorsLiveData.observe(viewLifecycleOwner) {
            viewModel.doorsLiveData.observe(viewLifecycleOwner) {
                if (!it.isNullOrEmpty()) {
                    val doorList = it.map { door -> door.toDoorData() }
                    setDoorsAdapterList(doorList)
                } else {
                    saveDoorList(doorList)
                }
            }
        }
    }

    data class AvailableDoorsData(
        @SerializedName("MOBILE")
        val mobile: String = 0.toString(),
        @SerializedName("AVAILABLE_DOORS")
        val availableDoors: String
    )

    private fun updateAiMode(availableDoors: String) {
        val mobileData = AvailableDoorsData(availableDoors = availableDoors)
        val jsonString = Gson().toJson(mobileData)
        viewModel.updateAiModeData.postValue(jsonString)
    }


    private fun setDoorsAdapterList(doorList: List<DoorData>) {
        adapter = DoorSelectAdapter(doorList, object : DoorSelectAdapter.ActionClickListener {
            override fun onActionClick(data: DoorData) {
                Log.d(TAG, "onActionClick: $data")
            }
        })
        val availableDoors = AppPreferences.availableDoorsData
        binding.rvDoors.adapter = adapter
        adapter.selectDoors(availableDoors)
    }

    private fun saveDoorList(doorList: List<DoorData>) {
        val doorTable = doorList.map { it.toDoorTable() }
        viewModel.insertAllDoors(doorTable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "DoorsFragment"
    }
}