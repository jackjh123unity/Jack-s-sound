package com.example

import com.example.audio.GeminiAudioEnchanter
import com.example.data.model.VoiceCatalog
import com.example.data.model.VoiceGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryCastVoiceUnitTest {

    @Test
    fun voiceCatalog_hasExactTenVoicesWithFiveMaleAndFiveFemale() {
        val voices = VoiceCatalog.ALL_VOICES
        assertEquals(10, voices.size)

        val maleVoices = voices.filter { it.gender == VoiceGender.MALE }
        val femaleVoices = voices.filter { it.gender == VoiceGender.FEMALE }

        assertEquals(5, maleVoices.size)
        assertEquals(5, femaleVoices.size)

        // Verify IDs and names are unique
        val uniqueIds = voices.map { it.id }.toSet()
        assertEquals(10, uniqueIds.size)

        for (voice in voices) {
            assertTrue(voice.name.isNotBlank())
            assertTrue(voice.title.isNotBlank())
            assertTrue(voice.toneDescription.isNotBlank())
            assertTrue(voice.previewQuote.isNotBlank())
            assertTrue(voice.basePitch in 0.5f..2.0f)
            assertTrue(voice.baseSpeed in 0.5f..2.0f)
        }

        // Verify male voices have lower pitch range (< 0.90f) for masculine resonance
        for (maleVoice in maleVoices) {
            assertTrue(
                "Male voice ${maleVoice.name} should have masculine pitch <= 0.88f",
                maleVoice.basePitch <= 0.88f
            )
        }
    }

    @Test
    fun prepareForGeminiTts_transformsBreathsAndPausesToAngleTags() {
        val rawScript = "[with quiet wonder] The dragon slept. [short pause] Deep breath [deep breath] then woke."
        val prepared = GeminiAudioEnchanter.prepareForGeminiTts(rawScript)

        assertFalse(prepared.contains("[with quiet wonder]"))
        assertTrue(prepared.contains("<short pause>"))
        assertTrue(prepared.contains("<breath>"))
        assertTrue(prepared.contains("The dragon slept"))
    }

    @Test
    fun prepareForTts_removesActingTagsAndConvertsPauses() {
        val rawScript = "[with quiet wonder] The dragon slept. [short pause] Deep breath [deep breath] then woke."
        val prepared = GeminiAudioEnchanter.prepareForTts(rawScript)

        assertFalse(prepared.contains("[with quiet wonder]"))
        assertFalse(prepared.contains("[short pause]"))
        assertFalse(prepared.contains("[deep breath]"))
        assertTrue(prepared.contains(","))
        assertTrue(prepared.contains("The dragon slept"))
    }
}
