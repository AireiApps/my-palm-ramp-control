package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity
import com.airei.milltracking.mypalm.mqtt.lrc.MainActivity.Companion
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.commons.fadeInByObject
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentSplashBinding
import com.airei.milltracking.mypalm.mqtt.lrc.utils.getAppVersion
import com.airei.milltracking.mypalm.mqtt.lrc.utils.getScreenSizeInInches
import com.airei.milltracking.mypalm.mqtt.lrc.viewmodel.AppViewModel
import org.eclipse.paho.android.service.BuildConfig

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.tvAppVersion.visibility = View.VISIBLE
            binding.tvScreenInch.visibility = View.VISIBLE
            binding.tvAppVersion.text = getAppVersion(requireContext())
            val displayMetrics = resources.displayMetrics
            val density = displayMetrics.density
            val densityDpi = displayMetrics.densityDpi

            binding.tvScreenInch.text = "Screen: I ${getScreenSizeInInches(requireContext())} | D ${densityDpi}"
        }else{
            binding.tvAppVersion.visibility = View.INVISIBLE
            binding.tvScreenInch.visibility = View.INVISIBLE
        }
    }

    private val timer: CountDownTimer = object : CountDownTimer(DELAY, 1000) {

        override fun onTick(millisUntilFinished: Long) {
            Log.i(TAG, "onTick: ")
        }

        override fun onFinish() {
            binding.root.visibility = View.INVISIBLE
            if (AppPreferences.guideStatus){
                if (!AppPreferences.mqttConfig.isNullOrEmpty() && !AppPreferences.mqttClientId.isNullOrEmpty()) {
                    findNavController().navigate(R.id.homeFragment)
                } else {
                    findNavController().navigate(R.id.mqttConfigFragment)
                }
            }else{
                findNavController().navigate(R.id.guideFragment)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        fadeInByObject(binding.imgLogo)
        timer.start()
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "SplashFragment"
        const val DELAY: Long = 4000
    }
}