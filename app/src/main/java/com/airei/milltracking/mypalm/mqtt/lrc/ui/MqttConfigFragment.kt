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
import com.airei.milltracking.mypalm.iot.adapter.DoorAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.DoorIpAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.DoorData
import com.airei.milltracking.mypalm.mqtt.lrc.commons.MqttConfig
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentMqttConfigBinding
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorData
import com.airei.milltracking.mypalm.mqtt.lrc.utils.toDoorTable
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import com.google.gson.Gson

class MqttConfigFragment : Fragment() {

    private var _binding: FragmentMqttConfigBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    lateinit var adapter: DoorIpAdapter

    var dbDoorList =  listOf<DoorData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMqttConfigBinding.inflate(layoutInflater, container, false)
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

        binding.etClientId.setText("")
        binding.etHost.setText("")
        binding.etPort.setText("")
        binding.etUserName.setText("")
        binding.repeatField.visibility = View.GONE
        binding.svMqttConfig.visibility = View.VISIBLE
        binding.svDoorIp.visibility = View.GONE

        if (!AppPreferences.mqttConfig.isNullOrEmpty()){
            val config = Gson().fromJson(AppPreferences.mqttConfig, MqttConfig::class.java)
            binding.etClientId.setText(AppPreferences.mqttClientId)
            binding.etHost.setText(config.host)
            binding.etPort.setText(config.port.toString())
            binding.etUserName.setText(config.username)
            binding.etPassword.setText(config.password)
        }else{
            binding.etHost.setText("airei.net")
            binding.etPort.setText("1883")
            binding.etUserName.setText("airei")
            binding.etPassword.setText("4rEpepi#OsaYoPUGewRI")
        }

        binding.etRepeat.setText(AppPreferences.repeatCnt.toString())

        binding.tvGoToMqtt.setOnClickListener {
            binding.svMqttConfig.visibility = View.VISIBLE
            binding.svDoorIp.visibility = View.GONE
        }
        binding.tvGoToRtsp.setOnClickListener {
            binding.svMqttConfig.visibility = View.GONE
            binding.svDoorIp.visibility = View.VISIBLE
        }

        viewModel.doorsLiveData.observe(viewLifecycleOwner){
            if (!it.isNullOrEmpty()){
                val temp = it.map { it.toDoorData() }
                dbDoorList = temp
                setDoorIp(dbDoorList)
            }
        }

        binding.btnSaveIp.setOnClickListener {
            val temp = adapter.getList()
            val differentDoorsByIp = temp.filter { tempDoor ->
                val dbDoor = dbDoorList.find { it.doorId == tempDoor.doorId }
                dbDoor != null && tempDoor.rtspConfig != dbDoor.rtspConfig
            }
            val doorTable = temp.map { it.toDoorTable() }
            viewModel.updateAllDoors(doorTable)
            findNavController().navigate(R.id.homeFragment)
        }

        binding.btnConfig.setOnClickListener {
            // Clear any previous errors
            binding.hostField.error = null
            binding.postField.error = null
            binding.userNameField.error = null
            binding.passwordField.error = null
            binding.repeatField.error = null

            // Retrieve text from the input fields
            val clientId = binding.etClientId.text.toString().trim()
            val host = binding.etHost.text.toString().trim()
            val port = binding.etPort.text.toString().trim()
            val user = binding.etUserName.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            val repeatCnt = binding.etRepeat.text.toString().trim()

            var isValid = true

            // Check if fields are empty and set errors
            if (host.isEmpty()) {
                binding.hostField.error = "Hostname cannot be empty"
                isValid = false
            }
            if (clientId.isEmpty()) {
                binding.clientIdField.error = "Client Id cannot be empty"
                isValid = false
            }

            if (port.isEmpty()) {
                binding.postField.error = "Port cannot be empty"
                isValid = false
            }

            if (user.isEmpty()) {
                binding.userNameField.error = "Username cannot be empty"
                isValid = false
            }

            if (pass.isEmpty()) {
                binding.passwordField.error = "Password cannot be empty"
                isValid = false
            }
            if (repeatCnt.isEmpty()) {
                binding.repeatField.error = "Repeat Count cannot be empty"
                isValid = false
            }

            // If all fields are valid, save the data
            if (isValid) {
                // Convert the port to an integer
                val portInt = port.toIntOrNull()

                if (portInt != null) {
                    // Save the configuration
                    val mqttConfig = MqttConfig(
                        host = host,
                        port = portInt,
                        username = user,
                        password = pass
                    )
                    AppPreferences.mqttClientId = clientId
                    AppPreferences.mqttConfig = Gson().toJson(mqttConfig)
                    AppPreferences.repeatCnt = repeatCnt.toInt()
                    Toast.makeText(requireContext(), "Configuration saved!", Toast.LENGTH_SHORT).show()
                    viewModel.startMqtt.postValue(true)
                    findNavController().navigate(R.id.homeFragment)
                } else {
                    binding.postField.error = "Port must be a valid number"
                }
            }
        }
    }

    private fun setDoorIp(temp: List<DoorData>) {
        adapter = DoorIpAdapter(temp)
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