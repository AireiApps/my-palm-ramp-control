package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.airei.milltracking.mypalm.mqtt.lrc.R
import com.airei.milltracking.mypalm.mqtt.lrc.adapter.SliderAdapter
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppPreferences
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentGuideBinding
import kotlin.math.abs


class GuideFragment : Fragment() {
    private var _binding: FragmentGuideBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuideBinding.inflate(layoutInflater, container, false)
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

        val images = listOf(
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
            R.drawable.image4,
            R.drawable.image5,
            R.drawable.image6,
            R.drawable.image7
        )

        // Set up the ViewPager2 with the adapter
        val adapter = SliderAdapter(images)
        binding.viewPager.adapter = adapter

        // Attach TabLayout with ViewPager2
        // TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        binding.tabLayout.setDotCount(images.size-1)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tabLayout.setSelectedPosition(position)
            }
        })

        // Set page transformer for sliding animation
        binding.viewPager.setPageTransformer { page, position ->
            page.translationX = -position * page.width
            page.alpha = 1 - abs(position)
        }

        // Button Click listeners
        binding.btnNext.setOnClickListener {
            Log.d(TAG, "onViewCreated: ${binding.viewPager.currentItem} | ${images.size}")
            if (binding.viewPager.currentItem < images.size - 1) {
                binding.viewPager.currentItem += 1
            }
            if (binding.viewPager.currentItem == images.size-1) {
                binding.mainRoot.visibility = View.INVISIBLE
                if (!AppPreferences.mqttConfig.isNullOrEmpty() && !AppPreferences.mqttClientId.isNullOrEmpty()) {
                    findNavController().navigate(R.id.homeFragment)
                } else {
                    findNavController().navigate(R.id.mqttConfigFragment)
                }
            }
        }

        binding.btnSkip.setOnClickListener {
            binding.mainRoot.visibility = View.INVISIBLE
            if (!AppPreferences.mqttConfig.isNullOrEmpty() && !AppPreferences.mqttClientId.isNullOrEmpty()) {
                findNavController().navigate(R.id.homeFragment)
            } else {
                findNavController().navigate(R.id.mqttConfigFragment)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "GuideFragment"
    }
}