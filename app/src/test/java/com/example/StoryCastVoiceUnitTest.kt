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
    fun voiceCatalog_hasVoicesWithFiveMaleAndDiverseFemaleIncludingMaya() {
        val voices = VoiceCatalog.ALL_VOICES
        assertEquals(11, voices.size)

        val maleVoices = voices.filter { it.gender == VoiceGender.MALE }
        val femaleVoices = voices.filter { it.gender == VoiceGender.FEMALE }

        assertEquals(5, maleVoices.size)
        assertEquals(6, femaleVoices.size)

        // Verify Maya is included with Flash TTS configuration
        val maya = voices.firstOrNull { it.id == "female_maya" }
        assertTrue("Maya voice should be present", maya != null)
        assertTrue(maya!!.tags.contains("Flash TTS"))

        // Verify IDs and names are unique
        val uniqueIds = voices.map { it.id }.toSet()
        assertEquals(11, uniqueIds.size)

        for (voice in voices) {
            assertTrue(voice.name.isNotBlank())
            assertTrue(voice.title.isNotBlank())
            assertTrue(voice.toneDescription.isNotBlank())
            assertTrue(voice.previewQuote.isNotBlank())
            assertTrue(voice.basePitch in 0.5f..2.0f)
            assertTrue(voice.baseSpeed in 0.5f..2.0f)
        }

        // Verify male voices have lower pitch range (<= 0.68f) for deep masculine resonance
        for (maleVoice in maleVoices) {
            assertTrue(
                "Male voice ${maleVoice.name} should have masculine pitch <= 0.68f, was ${maleVoice.basePitch}",
                maleVoice.basePitch <= 0.68f
            )
        }

        // Verify female voices have distinctly higher pitch range (>= 0.90f)
        for (femaleVoice in femaleVoices) {
            assertTrue(
                "Female voice ${femaleVoice.name} should have feminine pitch >= 0.90f, was ${femaleVoice.basePitch}",
                femaleVoice.basePitch >= 0.90f
            )
        }

        // Verify clear separation between highest male pitch and lowest female pitch
        val maxMalePitch = maleVoices.maxOf { it.basePitch }
        val minFemalePitch = femaleVoices.minOf { it.basePitch }
        assertTrue(
            "Highest male pitch ($maxMalePitch) must be significantly lower than lowest female pitch ($minFemalePitch)",
            maxMalePitch < minFemalePitch
        )
    }

    @Test
    fun wrapPcmToWav_generatesValidWavHeaderWithRiffMarker() {
        val dummyPcm = ByteArray(4800) { 0 }
        val wavBytes = GeminiAudioEnchanter.wrapPcmToWav(dummyPcm, 24000, 1)

        assertEquals(4800 + 44, wavBytes.size)
        assertEquals('R'.code.toByte(), wavBytes[0])
        assertEquals('I'.code.toByte(), wavBytes[1])
        assertEquals('F'.code.toByte(), wavBytes[2])
        assertEquals('F'.code.toByte(), wavBytes[3])
        assertEquals('W'.code.toByte(), wavBytes[8])
        assertEquals('A'.code.toByte(), wavBytes[9])
        assertEquals('V'.code.toByte(), wavBytes[10])
        assertEquals('E'.code.toByte(), wavBytes[11])
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
