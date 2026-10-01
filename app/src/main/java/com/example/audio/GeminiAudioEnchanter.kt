package com.example.audio

import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiAudioEnchanter {

    fun isGeminiAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
    }

    /**
     * Synthesizes audio using Google AI Studio Gemini 3.8 Flash TTS ("gemini-3.8-flash-tts").
     * Supports multi-tonal delivery, emotional inflections, and inline vocal tags
     * (<breath>, <short pause>, <sigh>, <gasp>, <whisper>).
     * Returns the decoded WAV byte array or null if unavailable.
     */
    suspend fun generateGeminiStudioAudio(
        text: String,
        voice: VoiceModel
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (!isGeminiAvailable()) return@withContext null

        val apiKey = BuildConfig.GEMINI_API_KEY
        val preparedText = prepareForGeminiTts(text)
        if (preparedText.isBlank()) return@withContext null

        // Direct Voice Acting Directive: inject voice persona prompt as leading acting directive
        val directActingPrompt = if (preparedText.startsWith("[")) {
            preparedText
        } else {
            "[${voice.personaPrompt}] $preparedText"
        }

        // Try supported studio TTS models in order
        val models = listOf(
            "gemini-2.5-flash-preview-tts",
            "gemini-3.8-flash-tts",
            "gemini-3.8-flash-lite-tts",
            "gemini-2.0-flash"
        )

        for (modelName in models) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 25000
                conn.readTimeout = 40000
                conn.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", directActingPrompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.8) // Raising to 0.8 unlocks model's creative vocal range & emotional tags
                        put("responseModalities", JSONArray().apply {
                            put("AUDIO")
                        })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", voice.geminiVoiceName)
                                })
                            })
                        })
                    })
                }

                conn.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(responseStr)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    val inlineData = firstPart?.optJSONObject("inlineData")
                    val base64Data = inlineData?.optString("data")

                    if (!base64Data.isNullOrBlank()) {
                        val decoded = Base64.decode(base64Data, Base64.DEFAULT)
                        if (decoded.size >= 4 && decoded[0] == 'R'.code.toByte() && decoded[1] == 'I'.code.toByte() && decoded[2] == 'F'.code.toByte() && decoded[3] == 'F'.code.toByte()) {
                            return@withContext decoded
                        }
                        return@withContext wrapPcmToWav(decoded, 24000, 1)
                    }
                }
            } catch (e: Exception) {
                // If this model fails or times out, loop tries the next model
            }
        }
        return@withContext null
    }

    /**
     * Wraps raw 16-bit PCM audio in a valid RIFF/WAV container so MediaPlayer can play it.
     */
    fun wrapPcmToWav(pcmData: ByteArray, sampleRate: Int = 24000, channels: Int = 1): ByteArray {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * 16 / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 16 / 8).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavData = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, 44)
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)
        return wavData
    }

    /**
     * Converts raw text or bracket-enchanted script into Gemini 3.8 Flash TTS compatible text.
     * Uses angle brackets for inline vocal tags like <breath>, <short pause>, <sigh>, <gasp>, <whisper>
     * and strips narrative instruction brackets like [with quiet wonder] so they aren't spoken aloud.
     */
    fun prepareForGeminiTts(text: String): String {
        var processed = text
            // Translate breath and pause markers to Gemini 3.8 inline vocal tags
            .replace(Regex("\\[(deep breath|breath)\\]", RegexOption.IGNORE_CASE), "<breath>")
            .replace(Regex("\\[sigh\\]", RegexOption.IGNORE_CASE), "<sigh>")
            .replace(Regex("\\[gasp\\]", RegexOption.IGNORE_CASE), "<gasp>")
            .replace(Regex("\\[(short pause|micro pause)\\]", RegexOption.IGNORE_CASE), "<short pause>")
            .replace(Regex("\\[(long pause|pause)\\]", RegexOption.IGNORE_CASE), "<long pause>")
            .replace(Regex("\\[(whispering|whisper)\\]", RegexOption.IGNORE_CASE), "<whisper>")
            .replace(Regex("\\[laugh\\]", RegexOption.IGNORE_CASE), "<laugh>")

        // Remove any remaining bracketed acting notes (e.g. [with quiet wonder], [cautious], [regal])
        processed = processed.replace(Regex("\\[[^\\]]+\\]"), "")

        // Normalize multiple spaces and returns
        return processed
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    /**
     * Enchants raw ebook text by injecting natural speech expression tags,
     * dramatic pauses, breath markers, and vocal tone directives tailored
     * to the chosen voice archetype.
     */
    suspend fun enchantScript(
        rawText: String,
        voice: VoiceModel,
        useAiIfAvailable: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) return@withContext rawText

        if (useAiIfAvailable && BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY")) {
            try {
                val aiEnchanted = callGeminiForEnchantment(rawText, voice)
                if (aiEnchanted.isNotBlank()) {
                    return@withContext aiEnchanted
                }
            } catch (e: Exception) {
                // Graceful fallback to heuristic enchantment
            }
        }

        // Heuristic script enchantment based on literary structure & punctuation
        return@withContext heuristicEnchant(rawText, voice)
    }

    private fun callGeminiForEnchantment(text: String, voice: VoiceModel): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = true

        val prompt = """
            You are an expert audiobook director preparing a fantasy ebook script for natural, humanized voice performance.
            Voice persona: ${voice.name} (${voice.title}) - ${voice.toneDescription}.
            
            Inject expressive inline direction tags:
            - Breaths and sighs: [deep breath], [sigh], [gasp]
            - Pacing and pauses: [short pause], [long pause]
            - Tone shifts: [whispering], [with growing intensity], [with quiet wonder], [anxiously], [resigned], [triumphant]
            
            Do NOT change or distort the story words, only weave in dramatic performance tags at natural pauses, paragraph breaks, and dialogue cues.
            Return ONLY the enchanted text with no preamble or markdown code fences.
            
            Text:
            $text
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 2048)
            })
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseStr)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val resultText = parts?.optJSONObject(0)?.optString("text")
            if (!resultText.isNullOrBlank()) {
                return resultText.trim()
            }
        }
        return ""
    }

    /**
     * Algorithmic, offline text enchanter that parses dialogue, paragraph pacing,
     * emotional punctuation, and inserts realistic breaths and pauses.
     */
    fun heuristicEnchant(rawText: String, voice: VoiceModel): String {
        val lines = rawText.split("\n")
        val result = StringBuilder()

        result.append("[${voice.toneDescription.split(",").firstOrNull() ?: "expressive"}] ")

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                result.append("\n\n[long pause]\n")
                continue
            }

            var processed = trimmed

            // Add breath before intense dialog or exclamation
            processed = processed.replace(Regex("([!\\?])\\s+(\"[A-Z])"), "$1 [deep breath] $2")

            // Ellipses turn into micro pause
            processed = processed.replace("...", "... [short pause] ")

            // Add dramatic breath at beginning of long descriptive sentences
            if (processed.length > 120 && !processed.contains("[")) {
                val commaIndex = processed.indexOf(',')
                if (commaIndex in 30..80) {
                    processed = processed.substring(0, commaIndex + 1) + " [deep breath] " + processed.substring(commaIndex + 1)
                }
            }

            // Dialogue styling tags
            if (processed.startsWith("\"") || processed.startsWith("“")) {
                if (processed.contains("!")) {
                    processed = "[with growing intensity] $processed"
                } else if (processed.contains("whisper", ignoreCase = true)) {
                    processed = "[whispering] $processed"
                }
            }

            // Paragraph transition breath
            if (index > 0 && index % 2 == 0) {
                result.append("[deep breath] ")
            }

            result.append(processed).append("\n")
        }

        return result.toString().trim()
    }

    /**
     * Converts enchanted script into audio-friendly text for Android TTS.
     * Replaces tag cues like [short pause] with natural rhythmic pauses (commas/ellipses)
     * and strips bracketed acting tags so the speech engine doesn't spell them out.
     */
    fun prepareForTts(text: String): String {
        return text
            .replace(Regex("\\[(short pause|micro pause)\\]", RegexOption.IGNORE_CASE), ", ")
            .replace(Regex("\\[(long pause|pause)\\]", RegexOption.IGNORE_CASE), "... ")
            .replace(Regex("\\[(deep breath|breath|sigh|gasp)\\]", RegexOption.IGNORE_CASE), " ... ")
            .replace(Regex("\\[[^\\]]+\\]"), "") // Remove remaining acting bracket directives
            .replace(Regex("<[^>]+>"), "")       // Remove XML/HTML style markers
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }
}
