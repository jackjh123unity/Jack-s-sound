package com.example.data.model

enum class VoiceGender {
    MALE, FEMALE
}

data class VoiceModel(
    val id: String,
    val name: String,
    val title: String,
    val gender: VoiceGender,
    val accent: String,
    val toneDescription: String,
    val personaPrompt: String,
    val basePitch: Float,
    val baseSpeed: Float,
    val geminiVoiceName: String,
    val previewQuote: String,
    val tags: List<String>,
    val emotionWarmth: Int = 95,
    val prosodySota: Float = 99.4f,
    val dynamicEnergyDb: Float = -18.4f,
    val streamingLatencyMs: Int = 121,
    val styleDirective: String = "Dynamic ASMR breathy whisper (-18dB) with ultra-low noise floor",
    val actingDirectives: List<String> = listOf("ASMR WHISPER (-18dB)", "BROADWAY BELT (+12dB)", "30dB DYNAMIC SPREAD", "ZERO CLIPPING")
)

object VoiceCatalog {
    val ALL_VOICES: List<VoiceModel> = listOf(
        // 5 MALE VOICES
        VoiceModel(
            id = "male_arthur",
            name = "Arthur",
            title = "The Grand Arch-Narrator",
            gender = VoiceGender.MALE,
            accent = "British Classical",
            toneDescription = "Deep textured baritone carrying ancient wisdom and cinematic gravitas",
            personaPrompt = "A masterful, cinematic fantasy narrator in their late 40s with a deep, textured British accent. Their voice is rich, warm, and highly expressive, carrying a natural weight of ancient wisdom. They speak with dramatic pacing, changing emotional depth dynamically and naturally taking deep breaths between epic phrases.",
            basePitch = 0.58f,
            baseSpeed = 0.92f,
            geminiVoiceName = "Charon",
            previewQuote = "Far beyond the misted crags of Eldoria, an ancient silence was suddenly broken.",
            tags = listOf("Cinematic", "Baritone", "Epic Fantasy")
        ),
        VoiceModel(
            id = "male_garrick",
            name = "Garrick",
            title = "The Highland Skald",
            gender = VoiceGender.MALE,
            accent = "Scottish Highland",
            toneDescription = "Low rumbling gravel register with rolling Scottish brogue and energetic warmth",
            personaPrompt = "Low, rumbling gravel register, thick rolling Scottish accent. Earthy, fierce, and full of campfire storytelling vigor.",
            basePitch = 0.54f,
            baseSpeed = 0.96f,
            geminiVoiceName = "Fenrir",
            previewQuote = "Mark my words, traveler: no one crosses the Whispering Ridge without paying the toll.",
            tags = listOf("Gravelly", "Campfire", "Rugged")
        ),
        VoiceModel(
            id = "male_marcus",
            name = "Marcus",
            title = "The Epic Chronicler",
            gender = VoiceGender.MALE,
            accent = "American Cinematic",
            toneDescription = "Resonant, authoritative, deep trailer-grade presence with slow breathy pauses",
            personaPrompt = "Deep, resonant, slow cinematic voice with breathy dramatic pauses and commanding authority.",
            basePitch = 0.50f,
            baseSpeed = 0.88f,
            geminiVoiceName = "Charon",
            previewQuote = "In the shadow of the fallen citadel, the last dragon took its breath.",
            tags = listOf("Deep Bass", "Authoritative", "Blockbuster")
        ),
        VoiceModel(
            id = "male_julian",
            name = "Julian",
            title = "The Oxford Scholar",
            gender = VoiceGender.MALE,
            accent = "Oxford English",
            toneDescription = "Refined, eloquent, gentle cadence with crystal-clear intellectual enunciation",
            personaPrompt = "Articulate, educated British scholar, gentle and measured with intriguing nuance and warm curiosity.",
            basePitch = 0.62f,
            baseSpeed = 0.98f,
            geminiVoiceName = "Charon",
            previewQuote = "The parchment spoke of three forgotten celestial keys, hidden in plain sight.",
            tags = listOf("Articulate", "Gentle", "Academic")
        ),
        VoiceModel(
            id = "male_rowan",
            name = "Rowan",
            title = "The Dashing Ranger",
            gender = VoiceGender.MALE,
            accent = "Mid-Atlantic Energetic",
            toneDescription = "Spirited, youthful, earnest hero's voice filled with courage and vigor",
            personaPrompt = "Youthful adventurer, spirited, bright timbre with heroic pacing and earnest passion.",
            basePitch = 0.65f,
            baseSpeed = 1.02f,
            geminiVoiceName = "Puck",
            previewQuote = "Draw your blade, my friend! The dawn will not wait for our hesitation.",
            tags = listOf("Youthful", "Heroic", "Fast-Paced")
        ),

        // FEMALE VOICES
        VoiceModel(
            id = "female_maya",
            name = "Maya",
            title = "Core Performer (ASMR to Broadway)",
            gender = VoiceGender.FEMALE,
            accent = "American Expressive",
            toneDescription = "Dynamic ASMR breathy whisper to full theatrical Broadway projection with 30dB dynamic spread",
            personaPrompt = "A premier theatrical AI voice performer. Shifting effortlessly between intimate ASMR breathy whisper (-18dB) with ultra-low noise floor, and rich, soaring Broadway vocal power (+12dB) with zero clipping.",
            basePitch = 1.08f,
            baseSpeed = 0.98f,
            geminiVoiceName = "Aoede",
            previewQuote = "Oh please, boys, hush up to this. I can whisper quiet enough to give you actual chills... or belt out truth that shakes the rafters!",
            tags = listOf("Flash TTS", "ASMR Whisper", "Broadway Belt", "30dB Spread"),
            emotionWarmth = 95,
            prosodySota = 99.4f,
            dynamicEnergyDb = -18.4f,
            streamingLatencyMs = 121,
            styleDirective = "Dynamic ASMR breathy whisper (-18dB) with ultra-low noise floor",
            actingDirectives = listOf("ASMR WHISPER (-18dB)", "BROADWAY BELT (+12dB)", "30dB DYNAMIC SPREAD", "ZERO CLIPPING")
        ),
        VoiceModel(
            id = "female_eleanor",
            name = "Eleanor",
            title = "The High Queen",
            gender = VoiceGender.FEMALE,
            accent = "British Royal",
            toneDescription = "Velvety British contralto, regal cadence, enchanting storytelling warmth",
            personaPrompt = "A mature, velvety British storyteller archetype with regal elegance, hypnotic cadence, and rich warmth.",
            basePitch = 1.05f,
            baseSpeed = 0.96f,
            geminiVoiceName = "Kore",
            previewQuote = "Before time possessed a name, the stars sang an elder song across the quiet void.",
            tags = listOf("Velvety", "Regal", "Enchanting")
        ),
        VoiceModel(
            id = "female_lyra",
            name = "Lyra",
            title = "The Ethereal Oracle",
            gender = VoiceGender.FEMALE,
            accent = "Celtic Lilt",
            toneDescription = "Soft, breathy Celtic whisper with ASMR intimacy and magical wonder",
            personaPrompt = "Ethereal Celtic mystic, whisper-soft with natural air flow, delicate, soothing, and spellbinding.",
            basePitch = 1.20f,
            baseSpeed = 0.94f,
            geminiVoiceName = "Aoede",
            previewQuote = "Listen closely... the forest remembers what the kingdom has long forgotten.",
            tags = listOf("Ethereal", "Whisper", "Mystical")
        ),
        VoiceModel(
            id = "female_sophia",
            name = "Sophia",
            title = "The Contemporary Bard",
            gender = VoiceGender.FEMALE,
            accent = "American Warm",
            toneDescription = "Crisp, expressive warmth with effortless modern storytelling flow",
            personaPrompt = "Engaging, modern, crisp, and emotionally dynamic, perfect for lively dialogue and narrative pace.",
            basePitch = 1.02f,
            baseSpeed = 1.00f,
            geminiVoiceName = "Kore",
            previewQuote = "She smiled, tucked the leather notebook into her coat, and stepped into the storm.",
            tags = listOf("Conversational", "Crisp", "Engaging")
        ),
        VoiceModel(
            id = "female_valeria",
            name = "Valeria",
            title = "The Shadow Sorceress",
            gender = VoiceGender.FEMALE,
            accent = "Eastern European Gothic",
            toneDescription = "Dark, smokey, suspenseful mezzo with lingering emotional tension",
            personaPrompt = "Deep, smokey, gothic storytelling presence with suspenseful pauses, chilling precision, and dark beauty.",
            basePitch = 0.92f,
            baseSpeed = 0.90f,
            geminiVoiceName = "Aoede",
            previewQuote = "The candlelight flickered... and in the darkness, two obsidian eyes opened.",
            tags = listOf("Gothic", "Suspense", "Smokey")
        ),
        VoiceModel(
            id = "female_tara",
            name = "Tara",
            title = "The Hearthside Weaver",
            gender = VoiceGender.FEMALE,
            accent = "Irish Comforting",
            toneDescription = "Comforting, maternal Irish warmth, gentle cadence like a cozy fireside tale",
            personaPrompt = "Soothing, gentle folk narrator, comforting pauses and warm rhythmic cadence like a grandmother by the hearth.",
            basePitch = 1.12f,
            baseSpeed = 0.96f,
            geminiVoiceName = "Aoede",
            previewQuote = "Pull your chair closer to the hearth, and let me tell you of the very first winter.",
            tags = listOf("Cozy", "Maternal", "Fireside")
        )
    )

    fun getById(id: String): VoiceModel {
        return ALL_VOICES.firstOrNull { it.id == id } ?: ALL_VOICES.first()
    }
}
