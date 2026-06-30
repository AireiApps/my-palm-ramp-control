package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VlcRtspPlayerHelper(
    context: Context
) {

    companion object {

        private const val TAG = "VlcRtspPlayerHelper"

        private const val NETWORK_CACHE = 500

        private const val LIVE_CACHE = 300

        private const val FILE_CACHE = 300

        private const val MAX_RETRY_COUNT = 3

        private const val RETRY_DELAY_MS = 2000L

        private const val PLAY_TIMEOUT_MS = 8000L
    }

    sealed class VlcPlayStatus {

        object Opening : VlcPlayStatus()

        data class Buffering(
            val percent: Int
        ) : VlcPlayStatus()

        object Playing : VlcPlayStatus()

        object Paused : VlcPlayStatus()

        data class Stopped(
            val reason: String
        ) : VlcPlayStatus()

        data class NotPlaying(
            val reason: String,
            val retryCount: Int,
            val maxRetryCount: Int
        ) : VlcPlayStatus()

        data class Error(
            val reason: String,
            val throwable: Throwable? = null,
            val retryCount: Int = 0,
            val maxRetryCount: Int = 0
        ) : VlcPlayStatus()
    }

    private val appContext = context.applicationContext

    private val handler = Handler(
        Looper.getMainLooper()
    )

    private val initLock = Any()

    private var libVLC: LibVLC? = null

    private var mediaPlayer: MediaPlayer? = null

    private var videoLayout: VLCVideoLayout? = null

    private var rtspUrl: String? = null

    private var retryRunnable: Runnable? = null

    private var timeoutRunnable: Runnable? = null

    private var callback: ((VlcPlayStatus) -> Unit)? = null

    private var retryCount = 0

    private var lastBuffer = -1

    private var isReleased = false

    private var isVlcPlaying = false

    private fun log(
        msg: String
    ) {
        AppLogger.log(
            tag = TAG,
            message = msg
        )
    }

    fun setStatusCallback(
        cb: (VlcPlayStatus) -> Unit
    ) {
        callback = cb
    }

    private fun notify(
        status: VlcPlayStatus
    ) {

        if (isReleased) return

        handler.post {

            if (!isReleased) {

                callback?.invoke(
                    status
                )
            }
        }
    }

    fun warmup() {

        createLibVlcIfNeeded {}
    }

    private fun createLibVlcIfNeeded(
        ready: () -> Unit
    ) {

        if (libVLC != null) {

            ready()

            return
        }

        Thread {

            synchronized(
                initLock
            ) {

                if (libVLC == null) {

                    log(
                        "Initializing VLC"
                    )

                    val options = arrayListOf(

                        "--network-caching=$NETWORK_CACHE",

                        "--live-caching=$LIVE_CACHE",

                        "--file-caching=$FILE_CACHE",

                        "--drop-late-frames",

                        "--skip-frames",

                        "--avcodec-fast",

                        "--avcodec-hw=any",

                        "--rtsp-tcp",

                        "--no-video-title-show",

                        "--no-audio",

                        "--stats"
                    )

                    libVLC = LibVLC(
                        appContext,
                        options
                    )

                    log(
                        "VLC Initialized"
                    )
                }
            }

            handler.post {

                if (!isReleased) {

                    ready()
                }
            }

        }.start()
    }

    fun play(
        layout: VLCVideoLayout,
        url: String
    ) {

        log(
            "play -> $url"
        )

        stopInternal(
            false,
            false
        )

        retryCount = 0

        videoLayout = layout

        rtspUrl = url.trim()

        start(
            rtspUrl
        )
    }

    private fun start(
        url: String?
    ) {

        createLibVlcIfNeeded {

            try {

                val vlc =
                    libVLC ?: return@createLibVlcIfNeeded

                val layout =
                    videoLayout ?: return@createLibVlcIfNeeded

                val finalUrl =
                    url ?: return@createLibVlcIfNeeded

                mediaPlayer?.release()

                mediaPlayer = MediaPlayer(
                    vlc
                ).apply {

                    attachViews(
                        layout,
                        null,
                        false,
                        false
                    )

                    setEventListener {

                        when (
                            it.type
                        ) {

                            MediaPlayer.Event.Opening -> {

                                notify(
                                    VlcPlayStatus.Opening
                                )
                            }

                            MediaPlayer.Event.Buffering -> {

                                val percent =
                                    it.buffering.toInt()

                                if (
                                    percent != lastBuffer
                                ) {

                                    lastBuffer =
                                        percent

                                    notify(
                                        VlcPlayStatus.Buffering(
                                            percent
                                        )
                                    )
                                }
                            }

                            MediaPlayer.Event.Playing -> {

                                log(
                                    "PLAYING"
                                )

                                cancelTimeout()

                                retryCount = 0

                                isVlcPlaying = true

                                notify(
                                    VlcPlayStatus.Playing
                                )
                            }

                            MediaPlayer.Event.Paused -> {

                                isVlcPlaying = false

                                notify(
                                    VlcPlayStatus.Paused
                                )
                            }

                            MediaPlayer.Event.Stopped -> {

                                isVlcPlaying = false

                                notify(
                                    VlcPlayStatus.Stopped(
                                        "Stopped"
                                    )
                                )
                            }

                            MediaPlayer.Event.EndReached,
                            MediaPlayer.Event.EncounteredError -> {

                                isVlcPlaying = false

                                val reason =
                                    getFailureReason()

                                notify(
                                    VlcPlayStatus.Error(
                                        reason,
                                        null,
                                        retryCount,
                                        MAX_RETRY_COUNT
                                    )
                                )

                                retry(
                                    reason
                                )
                            }
                        }
                    }

                    val media = Media(
                        vlc,
                        Uri.parse(
                            finalUrl
                        )
                    )

                    media.setHWDecoderEnabled(
                        true,
                        true
                    )

                    media.addOption(
                        ":network-caching=$NETWORK_CACHE"
                    )

                    media.addOption(
                        ":live-caching=$LIVE_CACHE"
                    )

                    media.addOption(
                        ":drop-late-frames"
                    )

                    media.addOption(
                        ":skip-frames"
                    )

                    media.addOption(
                        ":rtsp-tcp"
                    )

                    media.addOption(
                        ":clock-jitter=0"
                    )

                    media.addOption(
                        ":clock-synchro=0"
                    )

                    media.addOption(
                        ":no-audio"
                    )

                    this.media =
                        media

                    media.release()
                }

                startTimeout()

                mediaPlayer?.play()

            } catch (
                e: Exception
            ) {

                retry(
                    e.message
                        ?: "Play Failed"
                )
            }
        }
    }

    private fun getFailureReason(): String {

        return when {

            lastBuffer <= 0 -> {

                "No Data"
            }

            lastBuffer < 100 -> {

                "Buffer Stuck $lastBuffer%"
            }

            else -> {

                "Playback Error"
            }
        }
    }

    private fun startTimeout() {

        cancelTimeout()

        timeoutRunnable = Runnable {

            if (
                isReleased ||
                isVlcPlaying
            ) return@Runnable

            retry(
                getFailureReason()
            )
        }

        handler.postDelayed(
            timeoutRunnable!!,
            PLAY_TIMEOUT_MS
        )
    }

    private fun cancelTimeout() {

        timeoutRunnable?.let {

            handler.removeCallbacks(
                it
            )
        }

        timeoutRunnable = null
    }

    private fun retry(
        reason: String
    ) {

        if (
            retryCount >= MAX_RETRY_COUNT
        ) {

            notify(
                VlcPlayStatus.NotPlaying(
                    reason,
                    retryCount,
                    MAX_RETRY_COUNT
                )
            )

            return
        }

        retryCount++

        retryRunnable?.let {

            handler.removeCallbacks(
                it
            )
        }

        retryRunnable = Runnable {

            if (
                isReleased
            ) return@Runnable

            mediaPlayer?.stop()

            mediaPlayer?.play()
        }

        handler.postDelayed(
            retryRunnable!!,
            RETRY_DELAY_MS
        )
    }

    fun pause() {

        mediaPlayer?.pause()
    }

    fun resume() {

        mediaPlayer?.play()
    }

    fun stop() {

        stopInternal(
            true,
            true
        )
    }

    private fun stopInternal(
        clearData: Boolean,
        notifyStopped: Boolean
    ) {

        cancelTimeout()

        mediaPlayer?.let {

            runCatching {

                it.setEventListener(
                    null
                )

                it.stop()

                it.detachViews()

                it.release()
            }
        }

        mediaPlayer = null

        isVlcPlaying = false

        lastBuffer = -1

        if (
            clearData
        ) {

            videoLayout = null

            rtspUrl = null
        }

        if (
            notifyStopped &&
            !isReleased
        ) {

            notify(
                VlcPlayStatus.Stopped(
                    "Stopped"
                )
            )
        }
    }

    fun release() {

        isReleased = true

        stopInternal(
            true,
            false
        )

        retryRunnable?.let {

            handler.removeCallbacks(
                it
            )
        }

        handler.removeCallbacksAndMessages(
            null
        )

        libVLC?.release()

        libVLC = null

        mediaPlayer = null

        callback = null

        log(
            "Released"
        )
    }
}

//class VlcRtspPlayerHelper(context: Context)
//{
//
//    companion object {
//        private const val TAG = "VlcRtspPlayerHelper"
//        private const val NETWORK_CACHE = 3000
//        private const val LIVE_CACHE = 3000
//        private const val FILE_CACHE = 1500
//        private const val MAX_RETRY_COUNT = 3
//        private const val RETRY_DELAY_MS = 2000L
//        private const val PLAY_TIMEOUT_MS = 12000L
//    }
//
//    sealed class VlcPlayStatus {
//        object Opening : VlcPlayStatus()
//        data class Buffering(val percent: Int) : VlcPlayStatus()
//        object Playing : VlcPlayStatus()
//        object Paused : VlcPlayStatus()
//        data class Stopped(val reason: String) : VlcPlayStatus()
//        data class NotPlaying(val reason: String, val retryCount: Int, val maxRetryCount: Int) : VlcPlayStatus()
//        data class Error(val reason: String, val throwable: Throwable? = null, val retryCount: Int = 0, val maxRetryCount: Int = 0) : VlcPlayStatus()
//    }
//
//    private val appContext = context.applicationContext
//    private val handler = Handler(Looper.getMainLooper())
//    private val initLock = Any()
//
//    private var libVLC: LibVLC? = null
//    private var mediaPlayer: MediaPlayer? = null
//    private var videoLayout: VLCVideoLayout? = null
//    private var rtspUrl: String? = null
//    private var retryRunnable: Runnable? = null
//    private var timeoutRunnable: Runnable? = null
//    private var callback: ((VlcPlayStatus) -> Unit)? = null
//
//    private var retryCount = 0
//    private var lastBuffer = -1
//    private var isReleased = false
//    private var isVlcPlaying = false
//
//    private fun log(msg: String) { AppLogger.log(tag = TAG, message = msg) }
//
//    fun setStatusCallback(cb: (VlcPlayStatus) -> Unit) { callback = cb }
//
//    private fun notify(status: VlcPlayStatus) {
//        if (isReleased) return
//        handler.post { if (!isReleased) callback?.invoke(status) }
//    }
//
//    fun warmup() { createLibVlcIfNeeded {} }
//
//    private fun createLibVlcIfNeeded(ready: () -> Unit) {
//        if (libVLC != null) { ready(); return }
//        Thread {
//            synchronized(initLock) {
//                if (libVLC == null) {
//                    log("Initializing VLC")
//                    val options = arrayListOf("--network-caching=$NETWORK_CACHE","--live-caching=$LIVE_CACHE","--file-caching=$FILE_CACHE","--rtsp-frame-buffer-size=2000000","--drop-late-frames","--skip-frames","--avcodec-fast","--avcodec-hw=any","--clock-jitter=5000","--clock-synchro=1","--stats","--no-video-title-show")
//                    libVLC = LibVLC(appContext, options)
//                    log("VLC Initialized")
//                }
//            }
//            handler.post { if (!isReleased) ready() }
//        }.start()
//    }
//
//    fun play(layout: VLCVideoLayout, url: String) {
//        log("play url=$url")
//        stopInternal(false, false)
//        retryCount = 0
//        videoLayout = layout
//        rtspUrl = url.trim()
//        start(rtspUrl)
//    }
//
//    private fun start(url: String?) {
//        createLibVlcIfNeeded {
//            try {
//                val vlc = libVLC ?: return@createLibVlcIfNeeded
//                val layout = videoLayout ?: return@createLibVlcIfNeeded
//                val finalUrl = url ?: return@createLibVlcIfNeeded
//
//                mediaPlayer?.release()
//
//                mediaPlayer = MediaPlayer(vlc).apply {
//                    attachViews(layout, null, true, false)
//                    setEventListener {
//                        when (it.type) {
//                            MediaPlayer.Event.Opening -> notify(VlcPlayStatus.Opening)
//                            MediaPlayer.Event.Buffering -> {
//                                val percent = it.buffering.toInt()
//                                if (percent != lastBuffer) {
//                                    lastBuffer = percent
//                                    notify(VlcPlayStatus.Buffering(percent))
//                                }
//                            }
//                            MediaPlayer.Event.Playing -> {
//                                log("PLAYING")
//                                cancelTimeout()
//                                retryCount = 0
//                                isVlcPlaying = true
//                                notify(VlcPlayStatus.Playing)
//                            }
//                            MediaPlayer.Event.Paused -> {
//                                isVlcPlaying = false
//                                notify(VlcPlayStatus.Paused)
//                            }
//                            MediaPlayer.Event.Stopped -> {
//                                isVlcPlaying = false
//                                notify(VlcPlayStatus.Stopped("Stopped"))
//                            }
//                            MediaPlayer.Event.EndReached, MediaPlayer.Event.EncounteredError -> {
//                                isVlcPlaying = false
//                                val reason = getFailureReason()
//                                log("ERROR $reason")
//                                notify(VlcPlayStatus.Error(reason, null, retryCount, MAX_RETRY_COUNT))
//                                retry(reason)
//                            }
//                        }
//                    }
//
//                    val media = Media(vlc, Uri.parse(finalUrl))
//                    media.setHWDecoderEnabled(true, false)
//                    media.addOption(":network-caching=$NETWORK_CACHE")
//                    media.addOption(":live-caching=$LIVE_CACHE")
//                    media.addOption(":drop-late-frames")
//                    media.addOption(":skip-frames")
//                    media.addOption(":clock-jitter=5000")
//                    media.addOption(":clock-synchro=1")
//                    media.addOption(":rtsp-tcp")
//                    media.addOption(":rtsp-frame-buffer-size=2000000")
//                    media.addOption(":codec=all")
//                    this.media = media
//                    media.release()
//                }
//
//                startTimeout()
//                mediaPlayer?.play()
//
//            } catch (e: Exception) {
//                log("start failed ${e.message}")
//                retry(e.message ?: "Start Failed")
//            }
//        }
//    }
//
//    private fun getFailureReason(): String = when {
//        lastBuffer <= 0 -> "No Data Received"
//        lastBuffer < 100 -> "Buffer Stuck $lastBuffer%"
//        else -> "Playback Error"
//    }
//
//    private fun startTimeout() {
//        cancelTimeout()
//        timeoutRunnable = Runnable {
//            if (isReleased || isVlcPlaying) return@Runnable
//            val reason = getFailureReason()
//            log("TIMEOUT $reason")
//            retry(reason)
//        }
//        handler.postDelayed(timeoutRunnable!!, PLAY_TIMEOUT_MS)
//    }
//
//    private fun cancelTimeout() {
//        timeoutRunnable?.let { handler.removeCallbacks(it) }
//        timeoutRunnable = null
//    }
//
//    private fun retry(reason: String) {
//        if (retryCount >= MAX_RETRY_COUNT) {
//            notify(VlcPlayStatus.NotPlaying(reason, retryCount, MAX_RETRY_COUNT))
//            return
//        }
//
//        retryCount++
//        log("retry=$retryCount reason=$reason")
//        stopInternal(false, false)
//
//        retryRunnable?.let { handler.removeCallbacks(it) }
//
//        retryRunnable = Runnable {
//            if (!isReleased) start(rtspUrl)
//        }
//
//        handler.postDelayed(retryRunnable!!, RETRY_DELAY_MS)
//    }
//
//    fun pause() { mediaPlayer?.pause() }
//
//    fun resume() { mediaPlayer?.play() }
//
//    fun stop() { stopInternal(true, true) }
//
//    private fun stopInternal(clearData: Boolean, notifyStopped: Boolean) {
//        cancelTimeout()
//
//        mediaPlayer?.let {
//            runCatching {
//                it.setEventListener(null)
//                it.stop()
//                it.detachViews()
//                it.release()
//            }
//        }
//
//        mediaPlayer = null
//        isVlcPlaying = false
//        lastBuffer = -1
//
//        if (clearData) {
//            videoLayout = null
//            rtspUrl = null
//        }
//
//        if (notifyStopped && !isReleased) notify(VlcPlayStatus.Stopped("Stopped"))
//    }
//
//    fun release() {
//        isReleased = true
//        stopInternal(true, false)
//
//        retryRunnable?.let { handler.removeCallbacks(it) }
//        handler.removeCallbacksAndMessages(null)
//
//        mediaPlayer = null
//        libVLC?.release()
//        libVLC = null
//        callback = null
//
//        log("Released")
//    }
//}

data class PlayerStatusViews(
    val overlay: View,
    val progress: View,
    val status: TextView,
    val reason: TextView,
    val retry: View
)
