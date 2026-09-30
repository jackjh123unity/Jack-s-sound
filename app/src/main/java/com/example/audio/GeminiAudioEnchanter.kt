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

        // Try primary model: gemini-3.8-flash-tts, with fallback to gemini-3.8-flash-lite-tts
        val models = listOf("gemini-3.8-flash-tts", "gemini-3.8-flash-lite-tts")

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
                                    put("text", preparedText)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
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
                        return@withContext Base64.decode(base64Data, Base64.DEFAULT)
                    }
                }
            } catch (e: Exception) {
                // If this model fails or times out, loop tries the next model
            }
        }
        return@withContext null
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
