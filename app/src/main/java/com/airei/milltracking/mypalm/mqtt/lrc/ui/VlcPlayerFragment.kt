package com.airei.milltracking.mypalm.mqtt.lrc.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airei.milltracking.mypalm.mqtt.lrc.commons.AppLogger
import com.airei.milltracking.mypalm.mqtt.lrc.databinding.FragmentVlcPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerFragment : Fragment() {

    private var _binding: FragmentVlcPlayerBinding? = null
    private val binding get() = _binding!!

    private var libVLC: LibVLC? = null
    private var vlcMediaPlayer: MediaPlayer? = null

    private var rtspUrl: String? = null

    private var initJob: Job? = null
    private var retryJob: Job? = null
    private var timeoutJob: Job? = null

    private var retryCount = 0
    private var isPlayingStarted = false

    companion object {
        private const val TAG = "VlcPlayerFragment"

        const val ARG_RTSP_URL = "rtsp_url"

        private const val MAX_RETRY_COUNT = 1
        private const val STREAM_START_TIMEOUT_MS = 15_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rtspUrl = arguments?.getString(ARG_RTSP_URL)

        AppLogger.log(TAG, "onCreate: Fragment created")
        AppLogger.log(TAG, "onCreate: rtspUrl = $rtspUrl")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        AppLogger.log(TAG, "onCreateView: Inflating binding")

        _binding = FragmentVlcPlayerBinding.inflate(
            inflater,
            container,
            false
        )

        AppLogger.log(TAG, "onCreateView: Binding initialized")

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        AppLogger.log(TAG, "onViewCreated: View created")

        binding.btnRetry.setOnClickListener {
            AppLogger.log(TAG, "Retry button clicked")
            retryCount = 0
            restartPlayer()
        }

        val url = rtspUrl

        if (url.isNullOrBlank()) {
            showErrorUi(
                reason = "RTSP URL is empty. Please check camera URL.",
                url = ""
            )
            return
        }

        showLoadingUi("Preparing VLC player...")

        initializeVlcVideoPlayer(url)
    }

    private fun initializeVlcVideoPlayer(videoUrl: String) {
        AppLogger.log(TAG, "initializeVlcVideoPlayer: Started")
        AppLogger.log(TAG, "initializeVlcVideoPlayer: videoUrl = $videoUrl")

        val currentBinding = _binding ?: run {
            AppLogger.log(TAG, "initializeVlcVideoPlayer: Binding is null")
            return
        }

        initJob?.cancel()
        timeoutJob?.cancel()

        isPlayingStarted = false

        showLoadingUi("Connecting to camera stream...")

        startStreamTimeout(videoUrl)

        initJob = viewLifecycleOwner.lifecycleScope.launch {

            try {
                val args = arrayListOf(
                    "--file-caching=150",
                    "--network-caching=300",
                    "--clock-jitter=0",
                    "--live-caching=300",
                    "--drop-late-frames",
                    "--skip-frames",
                    "--vout=android-display",
                    "--rtsp-tcp",
                    "--no-audio"
                )

                AppLogger.log(TAG, "initializeVlcVideoPlayer: VLC args = $args")

                val createdLibVlc = withContext(Dispatchers.IO) {
                    AppLogger.log(TAG, "initializeVlcVideoPlayer: Creating LibVLC in IO thread")
                    LibVLC(requireContext().applicationContext, args)
                }

                if (_binding == null) {
                    AppLogger.log(TAG, "initializeVlcVideoPlayer: Binding became null after LibVLC creation")
                    createdLibVlc.release()
                    return@launch
                }

                libVLC = createdLibVlc

                val createdPlayer = MediaPlayer(createdLibVlc)
                vlcMediaPlayer = createdPlayer

                AppLogger.log(TAG, "initializeVlcVideoPlayer: Attaching VLC views")

                createdPlayer.attachViews(
                    currentBinding.vlcVideoLayout,
                    null,
                    false,
                    false
                )

                createdPlayer.setEventListener { event ->
                    handleVlcEvents(event)
                }

                currentBinding.vlcVideoLayout.visibility = View.VISIBLE

                AppLogger.log(TAG, "initializeVlcVideoPlayer: Setting VLC media")

                setVlcMedia(videoUrl)

            } catch (e: Exception) {
                AppLogger.log(TAG, "initializeVlcVideoPlayer: Exception | "+e.localizedMessage)

                showErrorUi(
                    reason = "VLC initialization failed: ${e.message ?: "Unknown error"}",
                    url = videoUrl
                )
            }
        }
    }

    private fun setVlcMedia(videoUrl: String) {
        AppLogger.log(TAG, "setVlcMedia: Started")
        AppLogger.log(TAG, "setVlcMedia: url = $videoUrl")

        val currentLibVlc = libVLC ?: run {
            AppLogger.log(TAG, "setVlcMedia: libVLC is null")
            showErrorUi(
                reason = "VLC engine is not initialized.",
                url = videoUrl
            )
            return
        }

        val currentPlayer = vlcMediaPlayer ?: run {
            AppLogger.log(TAG, "setVlcMedia: vlcMediaPlayer is null")
            showErrorUi(
                reason = "VLC media player is not initialized.",
                url = videoUrl
            )
            return
        }

        try {
            val media = Media(currentLibVlc, Uri.parse(videoUrl)).apply {
                setHWDecoderEnabled(true, false)

                addOption(":network-caching=300")
                addOption(":live-caching=300")
                addOption(":clock-jitter=0")
                addOption(":rtsp-tcp")
                addOption(":no-audio")
            }

            currentPlayer.media = media

            AppLogger.log(TAG, "setVlcMedia: Starting playback")

            currentPlayer.play()

            media.release()

            AppLogger.log(TAG, "setVlcMedia: Media released after attach")

        } catch (e: Exception) {
            AppLogger.log(TAG, "setVlcMedia: Exception"+e.localizedMessage)

            showErrorUi(
                reason = "Failed to start media: ${e.message ?: "Unknown error"}",
                url = videoUrl
            )
        }
    }

    private fun handleVlcEvents(event: MediaPlayer.Event) {
        when (event.type) {

            MediaPlayer.Event.Opening -> {
                AppLogger.log(TAG, "VLC Event: Opening")
                showLoadingUi("Opening camera stream...")
            }

            MediaPlayer.Event.Buffering -> {
                AppLogger.log(TAG, "VLC Event: Buffering")
                showLoadingUi("Buffering stream...")
            }

            MediaPlayer.Event.Playing -> {
                AppLogger.log(TAG, "VLC Event: Playing")

                isPlayingStarted = true
                retryCount = 0
                timeoutJob?.cancel()

                showPlayingUi()
            }

            MediaPlayer.Event.Paused -> {
                AppLogger.log(TAG, "VLC Event: Paused")
                showLoadingUi("Stream paused")
            }

            MediaPlayer.Event.Stopped -> {
                AppLogger.log(TAG, "VLC Event: Stopped")

                if (!isPlayingStarted) {
                    showErrorUi(
                        reason = buildStopReason(),
                        url = rtspUrl.orEmpty()
                    )
                }
            }

            MediaPlayer.Event.EncounteredError -> {
                AppLogger.log(TAG, "VLC Event: EncounteredError")

                if (retryCount < MAX_RETRY_COUNT) {
                    retryPlayback("VLC error occurred. Retrying...")
                } else {
                    showErrorUi(
                        reason = buildErrorReason(),
                        url = rtspUrl.orEmpty()
                    )
                }
            }

            MediaPlayer.Event.EndReached -> {
                AppLogger.log(TAG, "VLC Event: EndReached")

                if (retryCount < MAX_RETRY_COUNT) {
                    retryPlayback("Stream ended. Retrying...")
                } else {
                    showErrorUi(
                        reason = "Stream ended and could not reconnect.",
                        url = rtspUrl.orEmpty()
                    )
                }
            }

            MediaPlayer.Event.TimeChanged -> {
                AppLogger.log(TAG, "VLC Event: TimeChanged")
            }

            MediaPlayer.Event.PositionChanged -> {
                AppLogger.log(TAG, "VLC Event: PositionChanged")
            }

            MediaPlayer.Event.Vout -> {
                AppLogger.log(TAG, "VLC Event: Video Output")
                showPlayingUi()
            }

            else -> {
                AppLogger.log(TAG, "VLC Event Other: ${event.type}")
            }
        }
    }

    private fun retryPlayback(message: String) {
        val url = rtspUrl

        if (url.isNullOrBlank()) {
            showErrorUi(
                reason = "Cannot retry. RTSP URL is empty.",
                url = ""
            )
            return
        }

        retryCount++

        AppLogger.log(TAG, "retryPlayback: Retry count = $retryCount")

        showLoadingUi(message)

        retryJob?.cancel()

        retryJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(1_000)

            AppLogger.log(TAG, "retryPlayback: Restarting media")

            clearVlcPlayerMedia()
            setVlcMedia(url)
        }
    }

    private fun restartPlayer() {
        val url = rtspUrl

        if (url.isNullOrBlank()) {
            showErrorUi(
                reason = "RTSP URL is empty. Cannot restart.",
                url = ""
            )
            return
        }

        AppLogger.log(TAG, "restartPlayer: Restarting VLC player")

        releaseVlcPlayer()

        showLoadingUi("Restarting stream...")

        initializeVlcVideoPlayer(url)
    }

    private fun startStreamTimeout(videoUrl: String) {
        timeoutJob?.cancel()

        timeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(STREAM_START_TIMEOUT_MS)

            if (!isPlayingStarted) {
                AppLogger.log(TAG, "startStreamTimeout: Stream start timeout")

                showErrorUi(
                    reason = buildTimeoutReason(),
                    url = videoUrl
                )

                clearVlcPlayerMedia()
            }
        }
    }

    private fun showLoadingUi(message: String) {
        val currentBinding = _binding ?: return

        currentBinding.layoutLoading.visibility = View.VISIBLE
        currentBinding.layoutError.visibility = View.GONE
        currentBinding.vlcVideoLayout.visibility = View.VISIBLE

        currentBinding.tvLoadingMessage.text = message

        AppLogger.log(TAG, "showLoadingUi: $message")
    }

    private fun showPlayingUi() {
        val currentBinding = _binding ?: return

        currentBinding.layoutLoading.visibility = View.GONE
        currentBinding.layoutError.visibility = View.GONE
        currentBinding.vlcVideoLayout.visibility = View.VISIBLE

        AppLogger.log(TAG, "showPlayingUi: Video playing")
    }

    private fun showErrorUi(
        reason: String,
        url: String,
    ) {
        val currentBinding = _binding ?: return

        timeoutJob?.cancel()

        currentBinding.layoutLoading.visibility = View.GONE
        currentBinding.layoutError.visibility = View.VISIBLE
        currentBinding.vlcVideoLayout.visibility = View.GONE

        currentBinding.tvErrorReason.text = reason
        currentBinding.tvErrorUrl.text = maskRtspUrl(url)

        AppLogger.log(TAG, "showErrorUi: reason = $reason")
        AppLogger.log(TAG, "showErrorUi: url = ${maskRtspUrl(url)}")
    }

    private fun buildErrorReason(): String {
        return """
            VLC could not play this RTSP stream.

            Possible reasons:
            • Camera IP is not reachable from this device/network
            • Username or password is wrong
            • Password is not URL encoded correctly
            • RTSP path/channel/subtype is invalid
            • Camera does not allow RTSP TCP
            • Stream codec is not supported
        """.trimIndent()
    }

    private fun buildStopReason(): String {
        return """
            Stream stopped before playback started.

            Please check:
            • Camera is online
            • RTSP URL is correct
            • Device is connected to the same network
            • Channel and subtype values are valid
        """.trimIndent()
    }

    private fun buildTimeoutReason(): String {
        return """
            Stream loading timeout.

            VLC waited ${STREAM_START_TIMEOUT_MS / 1000} seconds but video did not start.

            Please check:
            • Camera IP address
            • RTSP username/password
            • Network connection
            • RTSP port 554
            • Camera stream subtype
        """.trimIndent()
    }

    private fun maskRtspUrl(url: String): String {
        if (url.isBlank()) return ""

        return try {
            url.replace(
                Regex("rtsp://([^:]+):([^@]+)@"),
                "rtsp://$1:****@"
            )
        } catch (e: Exception) {
            AppLogger.log(TAG, "maskRtspUrl: Exception"+e.localizedMessage)
            url
        }
    }

    private fun clearVlcPlayerMedia() {
        AppLogger.log(TAG, "clearVlcPlayerMedia: Started")

        try {
            vlcMediaPlayer?.stop()
            vlcMediaPlayer?.media?.release()

            AppLogger.log(TAG, "clearVlcPlayerMedia: Media cleared")

        } catch (e: Exception) {
            AppLogger.log(TAG, "clearVlcPlayerMedia: Exception"+e.localizedMessage)
        }
    }

    private fun releaseVlcPlayer() {
        AppLogger.log(TAG, "releaseVlcPlayer: Started")

        try {
            retryJob?.cancel()
            initJob?.cancel()
            timeoutJob?.cancel()

            vlcMediaPlayer?.setEventListener(null)

            vlcMediaPlayer?.stop()
            vlcMediaPlayer?.detachViews()
            vlcMediaPlayer?.media?.release()
            vlcMediaPlayer?.release()

            libVLC?.release()

            vlcMediaPlayer = null
            libVLC = null
            isPlayingStarted = false

            AppLogger.log(TAG, "releaseVlcPlayer: Released successfully")

        } catch (e: Exception) {
            AppLogger.log(TAG, "releaseVlcPlayer: Exception"+e.localizedMessage)
        }
    }

    override fun onPause() {
        AppLogger.log(TAG, "onPause: Fragment paused")
        super.onPause()
    }

    override fun onStop() {
        AppLogger.log(TAG, "onStop: Fragment stopped")
        super.onStop()
    }

    override fun onDestroyView() {
        AppLogger.log(TAG, "onDestroyView: Started")

        releaseVlcPlayer()

        _binding = null

        AppLogger.log(TAG, "onDestroyView: Binding cleared")

        super.onDestroyView()
    }

    override fun onDestroy() {
        AppLogger.log(TAG, "onDestroy: Fragment destroyed")
        super.onDestroy()
    }
}