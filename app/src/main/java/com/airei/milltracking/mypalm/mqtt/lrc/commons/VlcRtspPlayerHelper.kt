package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class VlcRtspPlayerHelper(
    context: Context
) {

    companion object {
        private const val TAG = "VLC_RTSP_PLAYER"

        private const val NETWORK_CACHE = 500
        private const val LIVE_CACHE = 500
        private const val FILE_CACHE = 500

        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 2000L

        private const val PLAY_TIMEOUT_MS = 15000L
    }

    enum class PlayerSide {
        LEFT,
        RIGHT
    }

    sealed class VlcPlayStatus {

        data class Opening(
            val side: PlayerSide
        ) : VlcPlayStatus()

        data class Buffering(
            val side: PlayerSide,
            val percent: Int
        ) : VlcPlayStatus()

        data class Playing(
            val side: PlayerSide
        ) : VlcPlayStatus()

        data class Paused(
            val side: PlayerSide
        ) : VlcPlayStatus()

        data class Stopped(
            val side: PlayerSide,
            val reason: String
        ) : VlcPlayStatus()

        data class NotPlaying(
            val side: PlayerSide,
            val reason: String,
            val retryCount: Int = 0,
            val maxRetryCount: Int = 0
        ) : VlcPlayStatus()

        data class Error(
            val side: PlayerSide,
            val reason: String,
            val throwable: Throwable? = null,
            val retryCount: Int = 0,
            val maxRetryCount: Int = 0
        ) : VlcPlayStatus()
    }

    private val appContext =
        context.applicationContext

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private var libVLC: LibVLC? = null

    private var leftPlayer: MediaPlayer? = null
    private var rightPlayer: MediaPlayer? = null

    private var leftLayout: VLCVideoLayout? = null
    private var rightLayout: VLCVideoLayout? = null

    private var leftUrl: String? = null
    private var rightUrl: String? = null

    private var leftRetryCount = 0
    private var rightRetryCount = 0

    private var isLeftPlaying = false
    private var isRightPlaying = false

    private var leftLastStatus: String = ""
    private var rightLastStatus: String = ""

    private var isReleased = false

    private var statusCallback: ((VlcPlayStatus) -> Unit)? = null

    fun setStatusCallback(
        callback: (VlcPlayStatus) -> Unit
    ) {
        statusCallback =
            callback
    }

    private fun notifyStatus(
        status: VlcPlayStatus
    ) {
        mainHandler.post {
            statusCallback?.invoke(
                status
            )
        }
    }

    private fun createLibVlcIfNeeded() {
        try {

            if (libVLC != null) {
                return
            }

            val options =
                arrayListOf(
                    "--rtsp-tcp",
                    "--network-caching=$NETWORK_CACHE",
                    "--live-caching=$LIVE_CACHE",
                    "--file-caching=$FILE_CACHE",
                    "--drop-late-frames",
                    "--skip-frames",
                    "--no-audio",
                    "--avcodec-hw=any",
                    "--verbose=2"
                )

            AppLogger.log(
                tag = TAG,
                message = "LibVLC Init Options : $options"
            )

            libVLC =
                LibVLC(
                    appContext,
                    options
                )

            AppLogger.log(
                tag = TAG,
                message = "LibVLC Initialized"
            )

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            val reason =
                e.message ?: "LibVLC initialization failed"

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.LEFT,
                    reason = reason,
                    throwable = e
                )
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.RIGHT,
                    reason = reason,
                    throwable = e
                )
            )
        }
    }

    fun playLeft(
        videoLayout: VLCVideoLayout,
        rtspUrl: String
    ) {

        AppLogger.log(
            tag = TAG,
            message = "Play Left Called : ${maskRtspUrl(rtspUrl)}"
        )

        isReleased =
            false

        leftLayout =
            videoLayout

        leftUrl =
            rtspUrl.trim()

        leftRetryCount =
            0

        isLeftPlaying =
            false

        leftLastStatus =
            "Play left requested"

        startLeft()
    }

    fun playRight(
        videoLayout: VLCVideoLayout,
        rtspUrl: String
    ) {

        AppLogger.log(
            tag = TAG,
            message = "Play Right Called : ${maskRtspUrl(rtspUrl)}"
        )

        isReleased =
            false

        rightLayout =
            videoLayout

        rightUrl =
            rtspUrl.trim()

        rightRetryCount =
            0

        isRightPlaying =
            false

        rightLastStatus =
            "Play right requested"

        startRight()
    }

    fun playBoth(
        leftVideoLayout: VLCVideoLayout,
        leftRtspUrl: String,
        rightVideoLayout: VLCVideoLayout,
        rightRtspUrl: String
    ) {

        AppLogger.log(
            tag = TAG,
            message = "Play Both Called"
        )

        playLeft(
            videoLayout = leftVideoLayout,
            rtspUrl = leftRtspUrl
        )

        mainHandler.postDelayed(
            {
                if (!isReleased) {
                    playRight(
                        videoLayout = rightVideoLayout,
                        rtspUrl = rightRtspUrl
                    )
                }
            },
            800L
        )
    }

    private fun startLeft() {
        try {

            createLibVlcIfNeeded()

            val vlc =
                libVLC

            val layout =
                leftLayout

            val url =
                leftUrl

            if (vlc == null) {

                val reason =
                    "LibVLC not initialized"

                leftLastStatus =
                    reason

                AppLogger.log(
                    tag = TAG,
                    message = "Start Left Failed : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.LEFT,
                        reason = reason
                    )
                )

                return
            }

            if (layout == null) {

                val reason =
                    "Left video layout is null"

                leftLastStatus =
                    reason

                AppLogger.log(
                    tag = TAG,
                    message = "Start Left Failed : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.LEFT,
                        reason = reason
                    )
                )

                return
            }

            if (url.isNullOrBlank()) {

                val reason =
                    "Left RTSP url is empty"

                leftLastStatus =
                    reason

                AppLogger.log(
                    tag = TAG,
                    message = "Start Left Failed : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.LEFT,
                        reason = reason
                    )
                )

                return
            }

            stopLeftInternal(
                clearData = false,
                notifyStopped = false
            )

            AppLogger.log(
                tag = TAG,
                message = "Starting Left Player"
            )

            leftPlayer =
                createPlayer(
                    side = PlayerSide.LEFT,
                    videoLayout = layout,
                    rtspUrl = url
                )

            isLeftPlaying =
                false

            leftLastStatus =
                "Starting left player"

            startPlayTimeoutWatcher(
                side = PlayerSide.LEFT,
                rtspUrl = url
            )

            leftPlayer?.play()

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            val reason =
                e.message ?: "Left player start failed"

            leftLastStatus =
                reason

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.LEFT,
                    reason = reason,
                    throwable = e,
                    retryCount = leftRetryCount,
                    maxRetryCount = MAX_RETRY_COUNT
                )
            )

            retryLeft(
                reason = reason
            )
        }
    }

    private fun startRight() {
        try {

            createLibVlcIfNeeded()

            val vlc =
                libVLC

            val layout =
                rightLayout

            val url =
                rightUrl

            if (vlc == null) {

                val reason =
                    "LibVLC not initialized"

                rightLastStatus =
                    reason

                AppLogger.log(
                    tag = TAG,
                    message = "Start Right Failed : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.RIGHT,
                        reason = reason
                    )
                )

                return
            }

            if (layout == null) {

                val reason =
                    "Right video layout is null"

                rightLastStatus =
                    reason

                AppLogger.log(
                    tag = TAG,
                    message = "Start Right Failed : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.RIGHT,
                        reason = reason
                    )
                )

                return
            }

            if (url.isNullOrBlank()) {

                val reason =
                    "Right RTSP url is empty"

                rightLastStatus =
                    reason

                AppLogger.log(
                    tag = TAG,
                    message = "Start Right Failed : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.RIGHT,
                        reason = reason
                    )
                )

                return
            }

            stopRightInternal(
                clearData = false,
                notifyStopped = false
            )

            AppLogger.log(
                tag = TAG,
                message = "Starting Right Player"
            )

            rightPlayer =
                createPlayer(
                    side = PlayerSide.RIGHT,
                    videoLayout = layout,
                    rtspUrl = url
                )

            isRightPlaying =
                false

            rightLastStatus =
                "Starting right player"

            startPlayTimeoutWatcher(
                side = PlayerSide.RIGHT,
                rtspUrl = url
            )

            rightPlayer?.play()

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            val reason =
                e.message ?: "Right player start failed"

            rightLastStatus =
                reason

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.RIGHT,
                    reason = reason,
                    throwable = e,
                    retryCount = rightRetryCount,
                    maxRetryCount = MAX_RETRY_COUNT
                )
            )

            retryRight(
                reason = reason
            )
        }
    }

    private fun createPlayer(
        side: PlayerSide,
        videoLayout: VLCVideoLayout,
        rtspUrl: String
    ): MediaPlayer {

        val vlc =
            libVLC
                ?: throw IllegalStateException(
                    "LibVLC is null"
                )

        val playerName =
            side.name

        val player =
            MediaPlayer(
                vlc
            )

        player.setEventListener { event ->

            when (event.type) {

                MediaPlayer.Event.Opening -> {

                    setLastStatus(
                        side = side,
                        status = "Opening"
                    )

                    AppLogger.log(
                        tag = TAG,
                        message = "$playerName Opening"
                    )

                    notifyStatus(
                        VlcPlayStatus.Opening(
                            side = side
                        )
                    )
                }

                MediaPlayer.Event.Buffering -> {

                    val percent =
                        event.buffering.toInt()

                    setLastStatus(
                        side = side,
                        status = "Buffering $percent%"
                    )

                    if (
                        percent == 0 ||
                        percent == 100 ||
                        percent % 25 == 0
                    ) {
                        AppLogger.log(
                            tag = TAG,
                            message = "$playerName Buffering : $percent%"
                        )
                    }

                    notifyStatus(
                        VlcPlayStatus.Buffering(
                            side = side,
                            percent = percent
                        )
                    )
                }

                MediaPlayer.Event.Playing -> {

                    AppLogger.log(
                        tag = TAG,
                        message = "$playerName Playing"
                    )

                    if (side == PlayerSide.LEFT) {

                        leftRetryCount =
                            0

                        isLeftPlaying =
                            true

                        leftLastStatus =
                            "Playing"

                    } else {

                        rightRetryCount =
                            0

                        isRightPlaying =
                            true

                        rightLastStatus =
                            "Playing"
                    }

                    notifyStatus(
                        VlcPlayStatus.Playing(
                            side = side
                        )
                    )
                }

                MediaPlayer.Event.Paused -> {

                    if (side == PlayerSide.LEFT) {

                        isLeftPlaying =
                            false

                        leftLastStatus =
                            "Paused"

                    } else {

                        isRightPlaying =
                            false

                        rightLastStatus =
                            "Paused"
                    }

                    AppLogger.log(
                        tag = TAG,
                        message = "$playerName Paused"
                    )

                    notifyStatus(
                        VlcPlayStatus.Paused(
                            side = side
                        )
                    )
                }

                MediaPlayer.Event.Stopped -> {

                    val reason =
                        "$playerName player stopped"

                    if (side == PlayerSide.LEFT) {

                        isLeftPlaying =
                            false

                        leftLastStatus =
                            "Stopped"

                    } else {

                        isRightPlaying =
                            false

                        rightLastStatus =
                            "Stopped"
                    }

                    AppLogger.log(
                        tag = TAG,
                        message = reason
                    )

                    notifyStatus(
                        VlcPlayStatus.Stopped(
                            side = side,
                            reason = reason
                        )
                    )
                }

                MediaPlayer.Event.EndReached -> {

                    val reason =
                        "$playerName stream ended"

                    if (side == PlayerSide.LEFT) {

                        isLeftPlaying =
                            false

                        leftLastStatus =
                            "EndReached"

                    } else {

                        isRightPlaying =
                            false

                        rightLastStatus =
                            "EndReached"
                    }

                    AppLogger.log(
                        tag = TAG,
                        message = "$playerName EndReached"
                    )

                    notifyStatus(
                        VlcPlayStatus.NotPlaying(
                            side = side,
                            reason = reason,
                            retryCount = getRetryCount(
                                side
                            ),
                            maxRetryCount = MAX_RETRY_COUNT
                        )
                    )

                    if (side == PlayerSide.LEFT) {
                        retryLeft(
                            reason = reason
                        )
                    } else {
                        retryRight(
                            reason = reason
                        )
                    }
                }

                MediaPlayer.Event.EncounteredError -> {

                    val reason =
                        "$playerName encountered playback error. Possible reasons: wrong RTSP URL, camera/NVR offline, network unreachable, username/password wrong, RTSP port blocked, codec not supported, or too many RTSP clients."

                    if (side == PlayerSide.LEFT) {

                        isLeftPlaying =
                            false

                        leftLastStatus =
                            "EncounteredError"

                    } else {

                        isRightPlaying =
                            false

                        rightLastStatus =
                            "EncounteredError"
                    }

                    AppLogger.log(
                        tag = TAG,
                        message = "$playerName EncounteredError : $reason"
                    )

                    notifyStatus(
                        VlcPlayStatus.Error(
                            side = side,
                            reason = reason,
                            retryCount = getRetryCount(
                                side
                            ),
                            maxRetryCount = MAX_RETRY_COUNT
                        )
                    )

                    if (side == PlayerSide.LEFT) {
                        retryLeft(
                            reason = reason
                        )
                    } else {
                        retryRight(
                            reason = reason
                        )
                    }
                }
            }
        }

        player.attachViews(
            videoLayout,
            null,
            false,
            false
        )

        val media =
            Media(
                vlc,
                Uri.parse(
                    rtspUrl
                )
            ).apply {

                setHWDecoderEnabled(
                    true,
                    false
                )

                addOption(":rtsp-tcp")
                addOption(":network-caching=$NETWORK_CACHE")
                addOption(":live-caching=$LIVE_CACHE")
                addOption(":file-caching=$FILE_CACHE")
                addOption(":clock-jitter=0")
                addOption(":clock-synchro=0")
                addOption(":no-audio")
                addOption(":drop-late-frames")
                addOption(":skip-frames")
            }

        player.media =
            media

        media.release()

        AppLogger.log(
            tag = TAG,
            message = "$playerName Media Prepared : ${maskRtspUrl(rtspUrl)}"
        )

        return player
    }

    private fun startPlayTimeoutWatcher(
        side: PlayerSide,
        rtspUrl: String
    ) {

        val maskedUrl =
            maskRtspUrl(
                rtspUrl
            )

        mainHandler.postDelayed(
            {

                if (isReleased) {
                    return@postDelayed
                }

                val isPlaying =
                    if (side == PlayerSide.LEFT) {
                        isLeftPlaying
                    } else {
                        isRightPlaying
                    }

                if (isPlaying) {
                    return@postDelayed
                }

                val lastStatus =
                    getLastStatus(
                        side
                    )

                val retryCount =
                    getRetryCount(
                        side
                    )

                val reason =
                    buildString {

                        append("Stream not playing after ")
                        append(PLAY_TIMEOUT_MS / 1000)
                        append(" seconds.")

                        if (lastStatus.isNotBlank()) {
                            append(" Last status: ")
                            append(lastStatus)
                            append(".")
                        }

                        append(" Possible reasons: ")
                        append("wrong RTSP URL, camera/NVR offline, network unreachable, username/password wrong, RTSP port blocked, codec not supported, or too many RTSP clients.")

                        append(" URL: ")
                        append(maskedUrl)
                    }

                AppLogger.log(
                    tag = TAG,
                    message = "${side.name} Play Timeout : $reason"
                )

                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = side,
                        reason = reason,
                        retryCount = retryCount,
                        maxRetryCount = MAX_RETRY_COUNT
                    )
                )

                if (side == PlayerSide.LEFT) {
                    retryLeft(
                        reason = reason
                    )
                } else {
                    retryRight(
                        reason = reason
                    )
                }

            },
            PLAY_TIMEOUT_MS
        )
    }

    private fun retryLeft(
        reason: String
    ) {

        if (isReleased) {

            notifyStatus(
                VlcPlayStatus.NotPlaying(
                    side = PlayerSide.LEFT,
                    reason = "Helper released. Left retry cancelled."
                )
            )

            return
        }

        if (leftRetryCount >= MAX_RETRY_COUNT) {

            val finalReason =
                "Left player not playing. Max retry reached. Last reason: $reason"

            AppLogger.log(
                tag = TAG,
                message = "LEFT Retry Stopped : $finalReason"
            )

            notifyStatus(
                VlcPlayStatus.NotPlaying(
                    side = PlayerSide.LEFT,
                    reason = finalReason,
                    retryCount = leftRetryCount,
                    maxRetryCount = MAX_RETRY_COUNT
                )
            )

            return
        }

        leftRetryCount++

        AppLogger.log(
            tag = TAG,
            message = "LEFT Retry $leftRetryCount/$MAX_RETRY_COUNT After ${RETRY_DELAY_MS}ms. Reason: $reason"
        )

        notifyStatus(
            VlcPlayStatus.NotPlaying(
                side = PlayerSide.LEFT,
                reason = "Retrying left player. Reason: $reason",
                retryCount = leftRetryCount,
                maxRetryCount = MAX_RETRY_COUNT
            )
        )

        mainHandler.postDelayed(
            {
                if (!isReleased) {
                    startLeft()
                }
            },
            RETRY_DELAY_MS
        )
    }

    private fun retryRight(
        reason: String
    ) {

        if (isReleased) {

            notifyStatus(
                VlcPlayStatus.NotPlaying(
                    side = PlayerSide.RIGHT,
                    reason = "Helper released. Right retry cancelled."
                )
            )

            return
        }

        if (rightRetryCount >= MAX_RETRY_COUNT) {

            val finalReason =
                "Right player not playing. Max retry reached. Last reason: $reason"

            AppLogger.log(
                tag = TAG,
                message = "RIGHT Retry Stopped : $finalReason"
            )

            notifyStatus(
                VlcPlayStatus.NotPlaying(
                    side = PlayerSide.RIGHT,
                    reason = finalReason,
                    retryCount = rightRetryCount,
                    maxRetryCount = MAX_RETRY_COUNT
                )
            )

            return
        }

        rightRetryCount++

        AppLogger.log(
            tag = TAG,
            message = "RIGHT Retry $rightRetryCount/$MAX_RETRY_COUNT After ${RETRY_DELAY_MS}ms. Reason: $reason"
        )

        notifyStatus(
            VlcPlayStatus.NotPlaying(
                side = PlayerSide.RIGHT,
                reason = "Retrying right player. Reason: $reason",
                retryCount = rightRetryCount,
                maxRetryCount = MAX_RETRY_COUNT
            )
        )

        mainHandler.postDelayed(
            {
                if (!isReleased) {
                    startRight()
                }
            },
            RETRY_DELAY_MS
        )
    }

    fun pauseAll() {
        try {

            AppLogger.log(
                tag = TAG,
                message = "Pause All"
            )

            leftPlayer?.pause()
            rightPlayer?.pause()

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.LEFT,
                    reason = e.message ?: "Pause left failed",
                    throwable = e
                )
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.RIGHT,
                    reason = e.message ?: "Pause right failed",
                    throwable = e
                )
            )
        }
    }

    fun resumeAll() {
        try {

            AppLogger.log(
                tag = TAG,
                message = "Resume All"
            )

            if (leftPlayer != null) {
                leftPlayer?.play()
            } else {
                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.LEFT,
                        reason = "Left player is null. Cannot resume."
                    )
                )
            }

            if (rightPlayer != null) {
                rightPlayer?.play()
            } else {
                notifyStatus(
                    VlcPlayStatus.NotPlaying(
                        side = PlayerSide.RIGHT,
                        reason = "Right player is null. Cannot resume."
                    )
                )
            }

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.LEFT,
                    reason = e.message ?: "Resume failed",
                    throwable = e
                )
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.RIGHT,
                    reason = e.message ?: "Resume failed",
                    throwable = e
                )
            )
        }
    }

    fun stopLeft() {
        stopLeftInternal(
            clearData = true,
            notifyStopped = true
        )
    }

    fun stopRight() {
        stopRightInternal(
            clearData = true,
            notifyStopped = true
        )
    }

    fun stopAll() {

        AppLogger.log(
            tag = TAG,
            message = "Stop All"
        )

        mainHandler.removeCallbacksAndMessages(
            null
        )

        stopLeftInternal(
            clearData = true,
            notifyStopped = true
        )

        stopRightInternal(
            clearData = true,
            notifyStopped = true
        )
    }

    private fun stopLeftInternal(
        clearData: Boolean,
        notifyStopped: Boolean
    ) {
        try {

            leftPlayer?.let { player ->

                AppLogger.log(
                    tag = TAG,
                    message = "Stopping Left Player"
                )

                runCatching {
                    player.stop()
                }

                runCatching {
                    player.detachViews()
                }

                runCatching {
                    player.release()
                }
            }

            leftPlayer =
                null

            isLeftPlaying =
                false

            leftLastStatus =
                "Stopped"

            if (clearData) {

                leftLayout =
                    null

                leftUrl =
                    null

                leftRetryCount =
                    0
            }

            if (notifyStopped) {

                notifyStatus(
                    VlcPlayStatus.Stopped(
                        side = PlayerSide.LEFT,
                        reason = if (clearData) {
                            "Left player stopped and data cleared"
                        } else {
                            "Left player stopped before restart"
                        }
                    )
                )
            }

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.LEFT,
                    reason = e.message ?: "Stop left failed",
                    throwable = e
                )
            )
        }
    }

    private fun stopRightInternal(
        clearData: Boolean,
        notifyStopped: Boolean
    ) {
        try {

            rightPlayer?.let { player ->

                AppLogger.log(
                    tag = TAG,
                    message = "Stopping Right Player"
                )

                runCatching {
                    player.stop()
                }

                runCatching {
                    player.detachViews()
                }

                runCatching {
                    player.release()
                }
            }

            rightPlayer =
                null

            isRightPlaying =
                false

            rightLastStatus =
                "Stopped"

            if (clearData) {

                rightLayout =
                    null

                rightUrl =
                    null

                rightRetryCount =
                    0
            }

            if (notifyStopped) {

                notifyStatus(
                    VlcPlayStatus.Stopped(
                        side = PlayerSide.RIGHT,
                        reason = if (clearData) {
                            "Right player stopped and data cleared"
                        } else {
                            "Right player stopped before restart"
                        }
                    )
                )
            }

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.RIGHT,
                    reason = e.message ?: "Stop right failed",
                    throwable = e
                )
            )
        }
    }

    fun release() {
        try {

            AppLogger.log(
                tag = TAG,
                message = "Release Helper"
            )

            isReleased =
                true

            mainHandler.removeCallbacksAndMessages(
                null
            )

            stopLeftInternal(
                clearData = true,
                notifyStopped = true
            )

            stopRightInternal(
                clearData = true,
                notifyStopped = true
            )

            libVLC?.release()
            libVLC =
                null

            AppLogger.log(
                tag = TAG,
                message = "Helper Released Successfully"
            )

        } catch (e: Exception) {

            AppLogger.logError(
                tag = TAG,
                exception = e
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.LEFT,
                    reason = e.message ?: "Helper release failed",
                    throwable = e
                )
            )

            notifyStatus(
                VlcPlayStatus.Error(
                    side = PlayerSide.RIGHT,
                    reason = e.message ?: "Helper release failed",
                    throwable = e
                )
            )
        }
    }

    private fun setLastStatus(
        side: PlayerSide,
        status: String
    ) {

        if (side == PlayerSide.LEFT) {

            leftLastStatus =
                status

        } else {

            rightLastStatus =
                status
        }
    }

    private fun getLastStatus(
        side: PlayerSide
    ): String {

        return if (side == PlayerSide.LEFT) {
            leftLastStatus
        } else {
            rightLastStatus
        }
    }

    private fun getRetryCount(
        side: PlayerSide
    ): Int {

        return if (side == PlayerSide.LEFT) {
            leftRetryCount
        } else {
            rightRetryCount
        }
    }

    private fun maskRtspUrl(
        url: String
    ): String {

        return try {

            url.replace(
                Regex("rtsp://([^:/@]+):([^@]+)@"),
                "rtsp://****:****@"
            )

        } catch (e: Exception) {

            "Invalid RTSP URL"
        }
    }
}
