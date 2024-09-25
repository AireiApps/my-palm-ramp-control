package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.StatusData
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentAutoFeedingBinding
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel

class AutoFeedingFragment : Fragment() {

    private var _binding: FragmentAutoFeedingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAutoFeedingBinding.inflate(layoutInflater, container, false)
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

        viewModelObserve()
    }

    private fun viewModelObserve() {
        viewModel.statusData.observe(viewLifecycleOwner){

                updateView(it)

        }
    }

    private fun updateView(it: StatusData?) {
        if (it == null){
            binding.btnDoorStatus.text = "--"
        }else{
            binding.btnDoorStatus.text = when(it.data.mypalmStatus){
                "0" -> getString(R.string.my_palm_mode)
                "1" -> getString(R.string.scada_mode)
                else -> getString(R.string.manual_mode)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "AutoFeedingFragment"
    }
}