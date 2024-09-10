package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorIpAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.doorList
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentMqttConfigBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_HOST
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PASS
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PORT
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_USER
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson

class MqttConfigFragment : Fragment() {

    private var _binding: FragmentMqttConfigBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    private lateinit var adapter: DoorIpAdapter
    private var dbDoorList = listOf<DoorData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMqttConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackPressHandler()
        initializeUI()
        setupMqttConfigFields()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
    }

    private fun initializeUI() {
        with(binding) {
            etClientId.setText("")
            etHost.setText("")
            etPort.setText("")
            etUserName.setText("")
            repeatField.visibility = View.GONE
            svMqttConfig.visibility = View.VISIBLE
            svDoorIp.visibility = View.GONE
        }
    }

    private fun setupMqttConfigFields() {
        val mqttConfig = AppPreferences.mqttConfig?.let { Gson().fromJson(it, MqttConfig::class.java) }
        with(binding) {
            etClientId.setText(AppPreferences.mqttClientId ?: "")
            etHost.setText(mqttConfig?.host ?: MQTT_HOST)
            etPort.setText(mqttConfig?.port?.toString() ?: MQTT_PORT)
            etUserName.setText(mqttConfig?.username ?: MQTT_USER)
            etPassword.setText(mqttConfig?.password ?: MQTT_PASS)
            etRepeat.setText(AppPreferences.repeatCnt.toString())
        }
    }

    private fun observeViewModel() {
        viewModel.doorsLiveData.observe(viewLifecycleOwner) { doors ->
            doors?.map { it.toDoorData() }?.let { doorData ->
                dbDoorList = doorData
                setDoorIp(doorData)
            } ?: saveDoorList(doorList)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            tvGoToMqtt.setOnClickListener {
                svMqttConfig.visibility = View.VISIBLE
                svDoorIp.visibility = View.GONE
            }

            tvGoToRtsp.setOnClickListener {
                svMqttConfig.visibility = View.GONE
                svDoorIp.visibility = View.VISIBLE
            }

            btnSaveIp.setOnClickListener {
                val updatedDoors = adapter.getList().filter { newDoor ->
                    dbDoorList.find { it.doorId == newDoor.doorId }?.rtspConfig != newDoor.rtspConfig
                }
                viewModel.updateAllDoors(adapter.getList().map { it.toDoorTable() })
                findNavController().navigate(R.id.homeFragment)
            }

            btnConfig.setOnClickListener {
                if (validateInputs()) {
                    saveMqttConfig()
                    viewModel.startMqtt.postValue(true)
                    findNavController().navigate(R.id.homeFragment)
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        binding.apply {
            hostField.error = null
            postField.error = null
            userNameField.error = null
            passwordField.error = null
            repeatField.error = null

            if (etHost.text.isNullOrBlank()) {
                hostField.error = "Hostname cannot be empty"
                isValid = false
            }
            if (etClientId.text.isNullOrBlank()) {
                clientIdField.error = "Client Id cannot be empty"
                isValid = false
            }
            if (etPort.text.isNullOrBlank()) {
                postField.error = "Port cannot be empty"
                isValid = false
            }
            if (etUserName.text.isNullOrBlank()) {
                userNameField.error = "Username cannot be empty"
                isValid = false
            }
            if (etPassword.text.isNullOrBlank()) {
                passwordField.error = "Password cannot be empty"
                isValid = false
            }
            if (etRepeat.text.isNullOrBlank()) {
                repeatField.error = "Repeat Count cannot be empty"
                isValid = false
            }
        }

        return isValid
    }

    private fun saveMqttConfig() {
        val portInt = binding.etPort.text.toString().toIntOrNull()
        if (portInt == null) {
            binding.postField.error = "Port must be a valid number"
            return
        }

        val mqttConfig = MqttConfig(
            host = binding.etHost.text.toString().trim(),
            port = portInt,
            username = binding.etUserName.text.toString().trim(),
            password = binding.etPassword.text.toString().trim()
        )

        AppPreferences.mqttClientId = binding.etClientId.text.toString().trim()
        AppPreferences.mqttConfig = Gson().toJson(mqttConfig)
        AppPreferences.repeatCnt = binding.etRepeat.text.toString().toInt()
        Toast.makeText(requireContext(), "Configuration saved!", Toast.LENGTH_SHORT).show()
    }

    private fun saveDoorList(doorList: List<DoorData>) {
        viewModel.insertAllDoors(doorList.map { it.toDoorTable() })
    }

    private fun setDoorIp(doorList: List<DoorData>) {
        adapter = DoorIpAdapter(doorList)
        binding.rvRtspConfig.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "MqttConfigFragment"
    }
}
