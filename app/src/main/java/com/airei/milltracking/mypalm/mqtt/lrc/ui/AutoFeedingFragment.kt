package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentAutoFeedingBinding
import com.airei.milltracking.mypalm.mqtt.lrc.utils.getAverageLevel
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
        with(binding) {
            tvLeave1.text = "--"
            tvLeave2.text = "--"
            tvSpeed1.text = "--"
            tvSpeed2.text = "--"
            tvDigesterLeave1.text = "--"
            tvDigesterLeave2.text = "--"
            btnDoorStatus.text = "--"
        }

        viewModelObserve()
    }

    @SuppressLint("SetTextI18n")
    private fun viewModelObserve() {
        viewModel.statusData.observe(viewLifecycleOwner){
            if (it != null) {
                with(binding) {
                    btnDoorStatus.text = when (it.data.mypalmStatus) {
                        "1" -> getString(R.string.my_palm_mode)
                        "0" -> getString(R.string.scada_mode)
                        else -> getString(R.string.manual_mode)
                    }
                }
            }
        }

        viewModel.autoFeedingData1.observe(viewLifecycleOwner) {
            with(binding) {
                if (it != null) {
                    tvLeave1.text = "${it.getAverageLevel()} %"
                } else {
                    tvLeave1.text = "--"
                }
            }
        }
        viewModel.autoFeedingData2.observe(viewLifecycleOwner) {
            with(binding) {
                if (it != null) {
                    tvLeave2.text = "${it.getAverageLevel()} %"
                } else {
                    tvLeave2.text = "--"
                }
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