package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorIpAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.CommandData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.FFBCommands
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.commons.SFBCommands
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentMqttConfigBinding
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_HOST
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PASS
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_PORT
import com.airei.milltracking.mypalm.mqtt.lrc.mqtt.MQTT_USER
import com.airei.milltracking.mypalm.mqtt.lrc.utils.hideKeyboard
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson

class MqttConfigFragment : Fragment() {

    private var _binding: FragmentMqttConfigBinding? = null
    private val binding get() = _binding!!

    private var selectedLayout = 0
    /*
    * 0 - mqtt
    * 1 - door rdsp
    * 2 - cmd
    * */

    private lateinit var mqttConfigData: MqttConfig
    private lateinit var localCommandData: CommandData

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
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                }
            })
        getData()
    }

    private fun getData() {
        getMqttConfig()
        observeData()
        layoutControl()
        setupTitleClicks()
        setupButtonActions()
    }

    private fun setupButtonActions() {
        with(binding) {

            layoutMqtt.setOnClickListener {
                requireActivity().window.hideKeyboard()
            }
            layoutCmd.setOnClickListener {
                requireActivity().window.hideKeyboard()
            }
            layoutDoorTags.setOnClickListener {
                requireActivity().window.hideKeyboard()
            }

            btnReset.setOnClickListener {
                when (selectedLayout) {
                    0 -> {
                        setMqttConfig()
                        Toast.makeText(requireContext(), "Reset Mqtt data.", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        getDoorList()
                        Toast.makeText(requireContext(), "Reset Door data.", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        setCommandView()
                        Toast.makeText(requireContext(), "Reset Commands data.", Toast.LENGTH_SHORT).show()
                    }
                }

            }
            btnGoBack.setOnClickListener {
                if (!AppPreferences.mqttClientId.isNullOrBlank()) {
                    binding.cardMqttConfig.visibility = View.INVISIBLE
                    findNavController().navigate(R.id.homeFragment)
                    viewModel.startMqtt.postValue(true)
                }
            }
            btnSaveCmd.setOnClickListener {
                when (selectedLayout) {
                    0 -> if (validateInputs()) saveMqttConfig()
                    1 -> saveDoorList(adapter.getList())
                    2 -> saveCommandData()
                }
            }
        }
    }

    private fun saveCommandData() {
        with(binding) {
            clearCommandErrors()

            if (validateCommandFields()) {
                val cmdData = CommandData(
                    rampDoorOpen = etDoorOpen.text.toString().trim(),
                    rampDoorClose = etDoorClose.text.toString().trim(),
                    LRStarter = etStarterMotor.text.toString().trim(),
                    FFB = FFBCommands(
                        start = etFfbStart.text.toString().trim(),
                        stop = etFfbStop.text.toString().trim(),
                        emergencyStop = etFfbEmeStop.text.toString().trim()
                    ),
                    SFB = SFBCommands(
                        start = etSfbStart.text.toString().trim(),
                        stop = etSfbStop.text.toString().trim(),
                        emergencyStop = etSfbEmeStop.text.toString().trim()
                    )
                )

                AppPreferences.cmdJson = Gson().toJson(cmdData)
                (activity as MainActivity).updateCommend()
                Toast.makeText(requireContext(), "Commands data updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearCommandErrors() {
        with(binding) {
            doorOpenField.error = null
            doorCloseField.error = null
            starterField.error = null
            ffbStartField.error = null
            ffbStopField.error = null
            ffbEmeStopField.error = null
            sfbStartField.error = null
            sfbStopField.error = null
            sfbEmeStopField.error = null
        }
    }

    private fun validateCommandFields(): Boolean {
        with(binding) {
            return when {
                etDoorOpen.text.isNullOrEmpty() -> {
                    doorOpenField.error = "Door open command is required"; false
                }

                etDoorClose.text.isNullOrEmpty() -> {
                    doorCloseField.error = "Door close command is required"; false
                }

                etStarterMotor.text.isNullOrEmpty() -> {
                    starterField.error = "Starter motor command is required"; false
                }

                etFfbStart.text.isNullOrEmpty() -> {
                    ffbStartField.error = "FFB start command is required"; false
                }

                etFfbStop.text.isNullOrEmpty() -> {
                    ffbStopField.error = "FFB stop command is required"; false
                }

                etFfbEmeStop.text.isNullOrEmpty() -> {
                    ffbEmeStopField.error = "FFB emergency stop command is required"; false
                }

                etSfbStart.text.isNullOrEmpty() -> {
                    sfbStartField.error = "SFB start command is required"; false
                }

                etSfbStop.text.isNullOrEmpty() -> {
                    sfbStopField.error = "SFB stop command is required"; false
                }

                etSfbEmeStop.text.isNullOrEmpty() -> {
                    sfbEmeStopField.error = "SFB emergency stop command is required"; false
                }

                else -> true
            }
        }
    }

    private fun getDoorList() {
        if (dbDoorList.isEmpty()) {
            viewModel.doorsLiveData.value?.let {
                dbDoorList = it.map { door -> door.toDoorData() }
                saveDoorList(dbDoorList)
            } ?: setDoorIp()
        }
    }

    private fun setupTitleClicks() {
        with(binding) {
            tvMqtt.setOnClickListener { selectLayout(0) }
            tvDoorCameras.setOnClickListener { selectLayout(1) }
            tvCmdButton.setOnClickListener { selectLayout(2) }
        }
    }

    private fun selectLayout(layout: Int) {
        selectedLayout = layout
        layoutControl()
    }

    private fun observeData() {
        with(viewModel) {
            doorsLiveData.observe(viewLifecycleOwner) { doors ->
                if (doors.isNotEmpty()) {
                    dbDoorList = doors.map { it.toDoorData() }
                    setDoorIp()
                }
            }
            commendData.observe(viewLifecycleOwner) { data ->
                if (data != null) {
                    localCommandData = data
                } else {
                    (activity as MainActivity).updateCommend()
                }
            }

        }
    }

    private fun layoutControl() {
        with(binding) {
            resetLayoutState()

            when (selectedLayout) {
                0 -> {
                    tvMqtt.setSelectedLayoutState()
                    layoutMqtt.visibility = View.VISIBLE
                    setMqttConfig()

                }

                1 -> {
                    tvDoorCameras.setSelectedLayoutState()
                    layoutDoorTags.visibility = View.VISIBLE
                    setDoorIp()
                }

                2 -> {
                    tvCmdButton.setSelectedLayoutState()
                    layoutCmd.visibility = View.VISIBLE
                    setCommandView()
                }
            }
        }
    }

    private fun View.setSelectedLayoutState() {
        this.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.white)
    }

    private fun resetLayoutState() {
        with(binding) {
            tvMqtt.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
            tvDoorCameras.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
            tvCmdButton.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
            layoutMqtt.visibility = View.GONE
            layoutDoorTags.visibility = View.GONE
            layoutCmd.visibility = View.GONE
        }
    }

    private fun setMqttConfig() {
        with(binding) {
            etClientId.setText(AppPreferences.mqttClientId.orEmpty())
            etHost.setText(mqttConfigData.host.ifBlank { MQTT_HOST })
            etPort.setText(mqttConfigData.port.takeIf { it != 0 }?.toString() ?: MQTT_PORT)
            etUserName.setText(mqttConfigData.username.ifBlank { MQTT_USER })
            etPassword.setText(mqttConfigData.password.ifBlank { MQTT_PASS })
            etRepeat.visibility = View.GONE
            etRepeat.setText(AppPreferences.repeatCnt.toString().trim())
        }
    }

    private fun setCommandView(commandData: CommandData = localCommandData) {
        with(binding) {
            etDoorOpen.setText(commandData.rampDoorOpen)
            etDoorClose.setText(commandData.rampDoorClose)
            etStarterMotor.setText(commandData.LRStarter)
            etFfbStart.setText(commandData.FFB.start)
            etFfbStop.setText(commandData.FFB.stop)
            etFfbEmeStop.setText(commandData.FFB.emergencyStop)
            etSfbStart.setText(commandData.SFB.start)
            etSfbStop.setText(commandData.SFB.stop)
            etSfbEmeStop.setText(commandData.SFB.emergencyStop)
        }
    }

    private fun getMqttConfig() {
        mqttConfigData = AppPreferences.mqttConfig?.let {
            Gson().fromJson(it, MqttConfig::class.java)
        } ?: MqttConfig(
            host = MQTT_HOST,
            port = MQTT_PORT.toInt(),
            username = MQTT_USER,
            password = MQTT_PASS
        )

    }

    private fun setDoorIp(doorList: List<DoorData> = dbDoorList) {
        adapter = DoorIpAdapter(doorList)
        binding.rvRtspConfig.adapter = adapter
    }

    private fun saveDoorList(doorList: List<DoorData>) {
        viewModel.insertAllDoors(doorList.map { it.toDoorTable() })
        Toast.makeText(requireContext(), "Door config updated", Toast.LENGTH_SHORT).show()
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        with(binding) {
            if (etHost.text.isNullOrBlank()) {
                hostField.error = "Host is required"
                isValid = false
            }
            if (etPort.text.isNullOrBlank()) {
                postField.error = "Port is required"
                isValid = false
            }
            if (etUserName.text.isNullOrBlank()) {
                userNameField.error = "Username is required"
                isValid = false
            }
            if (etPassword.text.isNullOrBlank()) {
                passwordField.error = "Password is required"
                isValid = false
            }
            if (etClientId.text.isNullOrBlank()) {
                clientIdField.error = "Client ID is required"
                isValid = false
            }
        }
        return isValid
    }

    private fun saveMqttConfig() {
        with(binding) {
            mqttConfigData = MqttConfig(
                host = etHost.text.toString().trim(),
                port = etPort.text.toString().trim().toInt(),
                username = etUserName.text.toString().trim(),
                password = etPassword.text.toString().trim()
            )

            AppPreferences.mqttConfig = Gson().toJson(mqttConfigData)
            AppPreferences.mqttClientId = etClientId.text.toString().trim()
            AppPreferences.repeatCnt = etRepeat.text.toString().toInt()
            Toast.makeText(requireContext(), "Mqtt config updated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
