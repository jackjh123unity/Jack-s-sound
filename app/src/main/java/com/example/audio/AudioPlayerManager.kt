package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.CountDownTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentBookId: Long? = null,
    val currentPageNumber: Int = 1,
    val currentAudioPath: String? = null,
    val isSingleFileMode: Boolean = false,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val sleepTimerMinutesLeft: Int? = null,
    val isCompleted: Boolean = false
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepTimer: CountDownTimer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var onTrackFinished: (() -> Unit)? = null

    fun playAudioFile(
        bookId: Long,
        pageNumber: Int,
        audioPath: String,
        isSingleFileMode: Boolean,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        startPositionMs: Int = 0,
        onFinished: () -> Unit = {}
    ) {
        val file = File(audioPath)
        if (!file.exists() || file.length() == 0L) {
            return
        }

        stop()
        onTrackFinished = onFinished

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                setOnPreparedListener { mp ->
                    if (startPositionMs > 0 && startPositionMs < mp.duration) {
                        mp.seekTo(startPositionMs)
                    }
                    applyPlaybackParams(speed, pitch)
                    mp.start()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        currentBookId = bookId,
                        currentPageNumber = pageNumber,
                        currentAudioPath = audioPath,
                        isSingleFileMode = isSingleFileMode,
                        durationMs = mp.duration,
                        currentPositionMs = mp.currentPosition,
                        speed = speed,
                        pitch = pitch,
                        isCompleted = false
                    )
                    startProgressTracking()
                }

                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration,
                        isCompleted = true
                    )
                    stopProgressTracking()
                    onTrackFinished?.invoke()
                }

                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            stopProgressTracking()
        } else {
            mp.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        val clamped = positionMs.coerceIn(0, mp.duration)
        mp.seekTo(clamped)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = clamped)
    }

    fun skipForward(deltaMs: Int = 15000) {
        val current = _playbackState.value.currentPositionMs
        val duration = _playbackState.value.durationMs
        seekTo((current + deltaMs).coerceAtMost(duration))
    }

    fun skipBackward(deltaMs: Int = 15000) {
        val current = _playbackState.value.currentPositionMs
        seekTo((current - deltaMs).coerceAtLeast(0))
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        applyPlaybackParams(_playbackState.value.speed, clamped)
        _playbackState.value = _playbackState.value.copy(pitch = clamped)
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.5f)
        applyPlaybackParams(clamped, _playbackState.value.pitch)
        _playbackState.value = _playbackState.value.copy(speed = clamped)
    }

    private fun applyPlaybackParams(speed: Float, pitch: Float) {
        mediaPlayer?.let { mp ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = mp.playbackParams
                    params.speed = speed.coerceIn(0.5f, 2.5f)
                    params.pitch = pitch.coerceIn(0.5f, 2.0f)
                    mp.playbackParams = params
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimer?.cancel()
        sleepTimer = null

        if (minutes == null || minutes <= 0) {
            _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = null)
            return
        }

        _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = minutes)
        val millis = minutes * 60 * 1000L

        sleepTimer = object : CountDownTimer(millis, 60000L) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = (millisUntilFinished / 60000L).toInt() + 1
                _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = mins)
            }

            override fun onFinish() {
                _playbackState.value = _playbackState.value.copy(sleepTimerMinutesLeft = null)
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    stopProgressTracking()
                }
            }
        }.start()
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = mp.currentPosition,
                            durationMs = mp.duration
                        )
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun release() {
        stop()
        sleepTimer?.cancel()
    }
}
