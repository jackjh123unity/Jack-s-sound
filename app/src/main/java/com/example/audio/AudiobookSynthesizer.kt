package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.UUID

class AudiobookSynthesizer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()
    private var previewPlayer: MediaPlayer? = null

    init {
        try {
            val previewDir = File(context.cacheDir, "voice_previews")
            if (previewDir.exists()) {
                previewDir.deleteRecursively()
            }
        } catch (e: Exception) {}

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsInitialized = true
                initDeferred.complete(true)
            } else {
                initDeferred.complete(false)
            }
        }
    }

    suspend fun awaitInitialization(): Boolean {
        return if (isTtsInitialized) true else initDeferred.await()
    }

    /**
     * Configures the local Android TTS pitch, speech rate, and system voice matching the given VoiceModel.
     * Enforces unmistakable deep masculine timbres for male voices and bright melodic tones for female voices.
     */
    fun configureVoice(
        voice: VoiceModel,
        userSpeedMultiplier: Float = 1.0f,
        userPitchMultiplier: Float = 1.0f
    ) {
        val engine = tts ?: return
        if (!isTtsInitialized) return

        // Effective speed combines voice personality baseline and user adjustable speed
        val effectiveSpeed = (voice.baseSpeed * userSpeedMultiplier).coerceIn(0.5f, 2.5f)
        engine.setSpeechRate(effectiveSpeed)

        // Strict masculine / feminine pitch enforcement:
        // Male pitch range is locked to deep resonant baritone/bass (0.45f .. 0.68f)
        // Female pitch range is locked to melodic feminine frequencies (0.90f .. 1.50f)
        val tunedPitch = voice.basePitch * userPitchMultiplier
        if (voice.gender == VoiceGender.MALE) {
            engine.setPitch(tunedPitch.coerceIn(0.45f, 0.68f))
        } else {
            engine.setPitch(tunedPitch.coerceIn(0.90f, 1.50f))
        }

        // Attempt to find best matching voice on device (Google TTS, Samsung, or system TTS)
        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val englishVoices = availableVoices.filter { it.locale.language.equals("en", ignoreCase = true) }
                val pool = if (englishVoices.isNotEmpty()) englishVoices else availableVoices.toList()

                val targetCountry = when {
                    voice.accent.contains("British", ignoreCase = true) || voice.accent.contains("Scottish", ignoreCase = true) -> "GB"
                    voice.accent.contains("Irish", ignoreCase = true) -> "IE"
                    else -> "US"
                }

                // 1. First priority: match target gender and matching country accent
                var matched = pool.firstOrNull { sysVoice ->
                    isVoiceMatchingGender(sysVoice, voice.gender) && sysVoice.locale.country.equals(targetCountry, ignoreCase = true)
                }

                // 2. Second priority: match target gender in any English locale
                if (matched == null) {
                    matched = pool.firstOrNull { sysVoice ->
                        isVoiceMatchingGender(sysVoice, voice.gender)
                    }
                }

                // 3. For male voices: if no voice is explicitly tagged male, but multiple voices exist,
                // pick a voice that is NOT female (or second variant) to avoid the default female voice
                if (matched == null && voice.gender == VoiceGender.MALE && pool.size > 1) {
                    matched = pool.firstOrNull { sysVoice ->
                        !isVoiceMatchingGender(sysVoice, VoiceGender.FEMALE)
                    } ?: pool.getOrNull(1)
                }

                if (matched != null) {
                    engine.voice = matched
                }
            }
        } catch (e: Exception) {
            // Ignore voice selection failure, defaults will apply
        }
    }

    private fun isVoiceMatchingGender(sysVoice: Voice, targetGender: VoiceGender): Boolean {
        val name = sysVoice.name.lowercase(Locale.ROOT)

        // Check features
        val features = sysVoice.features
        if (features != null) {
            for (f in features) {
                val fl = f.lowercase(Locale.ROOT)
                if (targetGender == VoiceGender.MALE) {
                    if (fl == "male" || fl.contains("gender=male") || fl.contains("gender:male")) return true
                    if (fl == "female" || fl.contains("gender=female") || fl.contains("gender:female")) return false
                } else {
                    if (fl == "female" || fl.contains("gender=female") || fl.contains("gender:female")) return true
                    if (fl == "male" || fl.contains("gender=male") || fl.contains("gender:male")) return false
                }
            }
        }

        val isFemale = name.contains("female") ||
                name.contains("woman") ||
                name.contains("#female") ||
                name.contains("-female") ||
                name.contains("_female") ||
                name.contains("-f-") ||
                name.contains("_f_") ||
                name.contains("-sfg") ||
                name.contains("-tpd") ||
                name.contains("-fis") ||
                name.contains("-gba") ||
                name.contains("-afh") ||
                name.contains("-cnd") ||
                Regex("(^|[-_])(sf|f[0-9]{2})([-_]|$)").containsMatchIn(name)

        val isMale = !isFemale && (
                name.contains("#male") ||
                name.contains("-male") ||
                name.contains("_male") ||
                name.contains(":male") ||
                name.contains("/male") ||
                name.contains("-m-") ||
                name.contains("_m_") ||
                name.contains("man") ||
                name.contains("guy") ||
                name.contains("-iom") ||
                name.contains("-iob") ||
                name.contains("-iol") ||
                name.contains("-iod") ||
                name.contains("-ioe") ||
                name.contains("-rjs") ||
                name.contains("-gbb") ||
                name.contains("-aub") ||
                name.contains("-cxx") ||
                Regex("(^|[-_])(sm|m[0-9]{2})([-_]|$)").containsMatchIn(name) ||
                (name.contains("male") && !name.contains("female"))
        )

        return when (targetGender) {
            VoiceGender.MALE -> isMale
            VoiceGender.FEMALE -> isFemale
        }
    }

    /**
     * Synthesizes page text directly into an audio WAV file on disk.
     * Uses Google AI Studio Gemini 3.8 Flash TTS ("gemini-3.8-flash-tts") as primary studio voice engine.
     * Gracefully falls back to optimized offline local TTS if Gemini is unavailable or rate-limited.
     */
    suspend fun synthesizePageToFile(
        text: String,
        outputFile: File,
        voice: VoiceModel,
        speed: Float,
        pitch: Float = 1.0f
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. Try Gemini 3.8 Flash Studio TTS first
        if (GeminiAudioEnchanter.isGeminiAvailable()) {
            try {
                val studioAudioBytes = GeminiAudioEnchanter.generateGeminiStudioAudio(text, voice)
                if (studioAudioBytes != null && studioAudioBytes.size > 200) {
                    outputFile.parentFile?.mkdirs()
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(studioAudioBytes)
                    }
                    return@withContext true
                }
            } catch (e: Exception) {
                // Proceed to local Android TTS fallback
            }
        }

        // 2. Offline / Fallback: Local Android TTS synthesis
        awaitInitialization()
        val engine = tts ?: return@withContext false

        configureVoice(voice, speed, pitch)
        val cleanText = GeminiAudioEnchanter.prepareForTts(text)
        if (cleanText.isBlank()) return@withContext false

        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val utteranceId = "page_synth_${UUID.randomUUID()}"
        val deferred = CompletableDeferred<Boolean>()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    deferred.complete(true)
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    deferred.complete(false)
                }
            }
        })

        val params = Bundle()
        val result = engine.synthesizeToFile(cleanText, params, outputFile, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            deferred.await()
        } else {
            false
        }
    }

    /**
     * Audition/preview voice with a short quote using Gemini 3.8 Flash Studio Voices.
     * Caches preview audio to disk for instant subsequent auditioning, applying tuned speed and pitch.
     */
    suspend fun previewVoice(
        voice: VoiceModel,
        speed: Float = 1.0f,
        pitch: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        val previewDir = File(context.cacheDir, "voice_previews")
        previewDir.mkdirs()
        val previewFile = File(previewDir, "preview_v2_${voice.id}.wav")

        if (!previewFile.exists() || previewFile.length() < 200) {
            if (GeminiAudioEnchanter.isGeminiAvailable()) {
                val studioAudioBytes = GeminiAudioEnchanter.generateGeminiStudioAudio(voice.previewQuote, voice)
                if (studioAudioBytes != null && studioAudioBytes.isNotEmpty()) {
                    try {
                        FileOutputStream(previewFile).use { it.write(studioAudioBytes) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (previewFile.exists() && previewFile.length() > 200) {
            withContext(Dispatchers.Main) {
                stopSpeaking()
                try {
                    previewPlayer?.release()
                    previewPlayer = MediaPlayer().apply {
                        setDataSource(previewFile.absolutePath)
                        setOnPreparedListener { mp ->
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val params = mp.playbackParams
                                    params.speed = speed.coerceIn(0.5f, 2.5f)
                                    params.pitch = pitch.coerceIn(0.5f, 2.0f)
                                    mp.playbackParams = params
                                }
                            } catch (e: Exception) {}
                            mp.start()
                        }
                        setOnCompletionListener { mp ->
                            mp.release()
                            previewPlayer = null
                        }
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            // Local Android TTS fallback
            awaitInitialization()
            val engine = tts ?: return@withContext
            withContext(Dispatchers.Main) {
                stopSpeaking()
                configureVoice(voice, speed, pitch)
                val utteranceId = "preview_${voice.id}_${System.currentTimeMillis()}"
                engine.speak(voice.previewQuote, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }
    }

    fun stopSpeaking() {
        previewPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (e: Exception) {}
            previewPlayer = null
        }
        tts?.stop()
    }

    /**
     * Concatenates a list of valid WAV files into a single unified continuous audio file.
     * This fulfills the requirement: "download every page as audio in a single file".
     */
    suspend fun mergeWavFiles(wavFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val validFiles = wavFiles.filter { it.exists() && it.length() > 44 }
        if (validFiles.isEmpty()) return@withContext false

        if (outputFile.exists()) {
            outputFile.delete()
        }

        outputFile.parentFile?.mkdirs()

        try {
            var totalAudioDataSize = 0L
            val firstFile = validFiles.first()
            val header = ByteArray(44)

            // Read the 44-byte RIFF header of the first file
            FileInputStream(firstFile).use { fis ->
                fis.read(header)
            }

            FileOutputStream(outputFile).use { fos ->
                // Write placeholder header
                fos.write(header)

                // Append PCM payload of all files
                val buffer = ByteArray(8192)
                for (file in validFiles) {
                    FileInputStream(file).use { fis ->
                        // Skip 44 bytes header
                        fis.skip(44)
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            totalAudioDataSize += bytesRead
                        }
                    }
                }
            }

            // Update WAV header with final sizes
            RandomAccessFile(outputFile, "rw").use { raf ->
                // Total file size minus 8 bytes (RIFF identifier & size field)
                val totalDataLen = totalAudioDataSize + 36
                raf.seek(4)
                raf.write(intToByteArray(totalDataLen.toInt()))

                // Subchunk2Size (data size) at offset 40
                raf.seek(40)
                raf.write(intToByteArray(totalAudioDataSize.toInt()))
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
