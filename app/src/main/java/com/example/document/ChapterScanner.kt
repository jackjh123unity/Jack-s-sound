package com.example.document

import java.util.Locale

data class ExtractedChapter(
    val chapterNumber: Int,
    val title: String,
    val content: String,
    val wordCount: Int = 0
)

data class ChapterScanResult(
    val title: String,
    val author: String,
    val chapters: List<ExtractedChapter>,
    val initialChapterNumber: Int = 1
)

object ChapterScanner {

    private val ROMAN_MAP = mapOf(
        'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50,
        'C' to 100, 'D' to 500, 'M' to 1000
    )

    private val WORD_NUM_MAP = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
        "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18,
        "nineteen" to 19, "twenty" to 20, "twenty-one" to 21, "twenty-two" to 22
    )

    fun parseRomanNumeral(s: String): Int? {
        val roman = s.trim().uppercase(Locale.US)
        if (!roman.matches(Regex("^[IVXLCDM]+$"))) return null
        var result = 0
        var prev = 0
        for (i in roman.length - 1 downTo 0) {
            val curr = ROMAN_MAP[roman[i]] ?: return null
            if (curr < prev) {
                result -= curr
            } else {
                result += curr
                prev = curr
            }
        }
        return if (result in 1..300) result else null
    }

    fun parseWordOrArabicOrRoman(token: String): Int? {
        val clean = token.trim().lowercase(Locale.US).trim('.', ':', '-', '—', ' ')
        clean.toIntOrNull()?.let { return it }
        WORD_NUM_MAP[clean]?.let { return it }
        return parseRomanNumeral(clean)
    }

    /**
     * Extracts title and author from string or filename, e.g. "The Abyssal Strain By Jack"
     */
    fun parseTitleAndAuthor(candidate: String, fallbackAuthor: String = "Author"): Pair<String, String> {
        val clean = candidate
            .replace(".docx", "", ignoreCase = true)
            .replace(".doc", "", ignoreCase = true)
            .replace(".pdf", "", ignoreCase = true)
            .replace(".txt", "", ignoreCase = true)
            .replace(Regex("[_\\-]"), " ")
            .trim()

        val byMatch = Regex("(?i)^(.+?)\\s+(?:by|written by|author:?)\\s+(.+)$").find(clean)
        if (byMatch != null) {
            val titlePart = cleanTitleCase(byMatch.groupValues[1])
            val authorPart = cleanTitleCase(byMatch.groupValues[2])
            return Pair(titlePart, authorPart)
        }

        return Pair(cleanTitleCase(clean), fallbackAuthor)
    }

    /**
     * Scans raw document text and organizes it strictly into real Chapters.
     * Identifies Book Title, Author, Chapter 1, Chapter 2, etc.
     */
    fun scanDocumentForChapters(
        rawText: String,
        documentTitle: String,
        documentAuthor: String
    ): ChapterScanResult {
        var finalTitle = documentTitle
        var finalAuthor = documentAuthor

        // Normalize non-breaking spaces and line breaks
        val normalized = rawText
            .replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace('\u200B', ' ')
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val rawLines = normalized.lines().map { it.trim().trim('\u00A0', ' ', '\t') }

        // Try to refine title/author from top 10 lines if document has "Title By Author"
        for (i in 0 until minOf(10, rawLines.size)) {
            val line = rawLines[i]
            if (line.contains(" by ", ignoreCase = true) && line.length < 80) {
                val (t, a) = parseTitleAndAuthor(line, finalAuthor)
                if (t.isNotBlank() && a.isNotBlank()) {
                    finalTitle = t
                    finalAuthor = a
                    break
                }
            } else if (i == 0 && line.isNotBlank() && line.length < 60 && !isChapterHeading(line)) {
                val next = rawLines.getOrNull(1)?.trim() ?: ""
                if (next.startsWith("by ", ignoreCase = true) && next.length < 40) {
                    finalTitle = cleanTitleCase(line)
                    finalAuthor = cleanTitleCase(next.substringAfter("by ", "").trim())
                    break
                }
            }
        }

        // Identify chapter boundaries
        val chapterBlocks = mutableListOf<ChapterBlock>()
        var currentHeading = ""
        var currentLines = mutableListOf<String>()

        var index = 0
        while (index < rawLines.size) {
            val line = rawLines[index]

            if (line == "===PAGEBREAK===") {
                // If next line is a candidate heading, let it trigger
                index++
                continue
            }

            val cleanLine = line.removePrefix("===HEADING===").trim()

            if (isChapterHeading(cleanLine)) {
                // Check if current block has text to save
                if (currentLines.any { it.isNotBlank() }) {
                    chapterBlocks.add(ChapterBlock(currentHeading, currentLines.toList()))
                    currentLines.clear()
                }

                // Check if the next line is a chapter subtitle (e.g. line 1: "CHAPTER I", line 2: "THE SCRIBE IN THE SHALLOWS")
                var headingText = cleanLine
                val nextLine = rawLines.getOrNull(index + 1)?.removePrefix("===HEADING===")?.trim() ?: ""
                if (isSubHeading(nextLine, cleanLine)) {
                    headingText = "$cleanLine: $nextLine"
                    index++ // consume subtitle line
                }
                currentHeading = headingText
            } else {
                currentLines.add(cleanLine)
            }
            index++
        }

        if (currentLines.any { it.isNotBlank() }) {
            chapterBlocks.add(ChapterBlock(currentHeading, currentLines.toList()))
        }

        val chapters = mutableListOf<ExtractedChapter>()
        var initialChapterNum = 1

        if (chapterBlocks.isEmpty()) {
            // Document has no headings -> split by section breaks or natural paragraph blocks
            val naturalChapters = splitIntoNaturalChapters(normalized)
            return ChapterScanResult(
                title = cleanTitleCase(finalTitle),
                author = cleanTitleCase(finalAuthor),
                chapters = naturalChapters,
                initialChapterNumber = 1
            )
        }

        var chapterIndex = 1
        var foundChapter1 = false

        for (block in chapterBlocks) {
            val content = block.lines.joinToString("\n").trim()
            if (content.isBlank() && block.heading.isBlank()) continue

            val heading = block.heading.trim()

            if (heading.isBlank()) {
                // Preamble before the first chapter (Title, Author, Dedication, Copyright)
                val wordCount = countWords(content)
                if (wordCount < 180 && (content.contains("by", ignoreCase = true) || content.length < 400 || content.contains("copyright", ignoreCase = true))) {
                    val firstLine = content.lines().firstOrNull()?.trim() ?: ""
                    if (firstLine.isNotBlank()) {
                        val (t, a) = parseTitleAndAuthor(firstLine, finalAuthor)
                        if (finalTitle == "Imported Story" || finalTitle.startsWith("msf:")) {
                            finalTitle = t
                            finalAuthor = a
                        }
                    }
                    // Skip creating an empty preamble chapter
                    continue
                } else {
                    // Substantial intro / prologue
                    chapters.add(
                        ExtractedChapter(
                            chapterNumber = chapterIndex,
                            title = "Prologue / Introduction",
                            content = content,
                            wordCount = wordCount
                        )
                    )
                    chapterIndex++
                }
            } else {
                val formattedTitle = formatChapterTitle(heading, chapterIndex)
                val words = countWords(content)

                // Check if this is Chapter 1 / Chapter I / Chapter One
                if (!foundChapter1 && isFirstChapter(heading)) {
                    initialChapterNum = chapterIndex
                    foundChapter1 = true
                }

                chapters.add(
                    ExtractedChapter(
                        chapterNumber = chapterIndex,
                        title = formattedTitle,
                        content = content,
                        wordCount = words
                    )
                )
                chapterIndex++
            }
        }

        // If only 1 chapter was found but text is very long (> 2500 words), split into natural chapters
        val finalChapters = if (chapters.size <= 1 && (chapters.firstOrNull()?.wordCount ?: 0) > 2500) {
            splitIntoNaturalChapters(chapters.first().content)
        } else if (chapters.isEmpty()) {
            splitIntoNaturalChapters(normalized)
        } else {
            chapters
        }

        return ChapterScanResult(
            title = cleanTitleCase(finalTitle),
            author = cleanTitleCase(finalAuthor),
            chapters = finalChapters,
            initialChapterNumber = initialChapterNum.coerceIn(1, finalChapters.size)
        )
    }

    private data class ChapterBlock(
        val heading: String,
        val lines: List<String>
    )

    fun isChapterHeading(line: String): Boolean {
        if (line.isBlank() || line.length > 100) return false
        val clean = line.trim().trim('*', '#', ' ', '\t')

        // 1. Chapter 1, CHAPTER I, Chapter One, Chapter 1: ..., CHAPTER I THE SCRIBE IN THE SHALLOWS
        if (clean.matches(Regex("(?i)^\\s*(?:chapter|ch\\.?)\\s*([0-9ivxlcdm]+|[a-z]+)($|[:\\s\\-\\.\\—\\–].*)"))) {
            return true
        }

        // 2. Number dot Title: "1. The Scribe in the Shallows" or "2. The Deep Trench"
        if (clean.matches(Regex("^\\s*([0-9]{1,3})\\s*[\\.\\:\\-\\—\\–]\\s*([A-Za-z].*)$"))) {
            return true
        }

        // 3. Standalone number on its own line: "1", "2", "3"
        if (clean.matches(Regex("^[0-9]{1,3}$"))) {
            return true
        }

        // 4. Roman numeral alone or with title: "I", "I. The Scribe...", "I: The Scribe..."
        if (clean.matches(Regex("^([IVXLCDM]{1,6})\\s*(?:[\\.\\:\\-\\—\\–\\s]+(.*))?$"))) {
            return true
        }

        // 5. Named sections: Prologue, Epilogue, Introduction, Preface, Interlude, Afterword
        if (clean.matches(Regex("(?i)^\\s*(prologue|epilogue|introduction|preface|interlude|afterword)($|[:\\s\\-\\.\\—\\–].*)"))) {
            return true
        }

        // 6. Multi-part books: Part 1, Book 1, Act 1
        if (clean.matches(Regex("(?i)^\\s*(?:part|book|act)\\s*([0-9ivxlcdm]+|[a-z]+)($|[:\\s\\-\\.\\—\\–].*)"))) {
            return true
        }

        return false
    }

    private fun isSubHeading(nextLine: String, currentHeading: String): Boolean {
        val next = nextLine.trim().trim('*', '#', ' ')
        if (next.isBlank() || next.length > 80) return false
        if (isChapterHeading(next)) return false
        if (next.endsWith(".") && next.split(" ").size > 8) return false
        return next.matches(Regex("^[A-Z0-9\\s\\-',:?!]+$")) || next.split(" ").size <= 6
    }

    private fun isFirstChapter(heading: String): Boolean {
        val clean = heading.trim().lowercase(Locale.US)
        if (clean.contains("chapter 1") || clean.contains("chapter i") || clean.contains("chapter one")) return true
        if (clean == "1" || clean.startsWith("1.") || clean.startsWith("1:") || clean.startsWith("1 -")) return true
        if (clean == "i" || clean.startsWith("i:") || clean.startsWith("i -") || clean.startsWith("i.")) return true
        return false
    }

    private fun formatChapterTitle(rawHeading: String, fallbackNum: Int): String {
        val clean = rawHeading.trim().replace(Regex("\\s+"), " ")

        // If it starts with "CHAPTER I THE SCRIBE IN THE SHALLOWS" or "Chapter 1: The..."
        val chapterPattern = Regex("(?i)^(?:chapter|ch\\.?)\\s*([0-9ivxlcdm]+|[a-z]+)(?:[:\\s\\-\\.\\—\\–]+(.*))?$")
        val match = chapterPattern.find(clean)
        if (match != null) {
            val numToken = match.groupValues[1]
            val subtitle = match.groupValues.getOrNull(2)?.trim() ?: ""
            val parsedNum = parseWordOrArabicOrRoman(numToken) ?: fallbackNum

            val cleanSubtitle = subtitle.trim().trim(':', '-', '.', '—', '–', ' ')
            return if (cleanSubtitle.isNotBlank()) {
                "Chapter $parsedNum: ${cleanTitleCase(cleanSubtitle)}"
            } else {
                "Chapter $parsedNum"
            }
        }

        // Number dot title: "1. The Scribe in the Shallows"
        val numberDotPattern = Regex("^([0-9]{1,3})\\s*[\\.\\:\\-\\—\\–]\\s*(.*)$")
        val numMatch = numberDotPattern.find(clean)
        if (numMatch != null) {
            val num = numMatch.groupValues[1].toIntOrNull() ?: fallbackNum
            val subtitle = numMatch.groupValues[2].trim().trim(':', '-', '.', '—', '–', ' ')
            return if (subtitle.isNotBlank()) {
                "Chapter $num: ${cleanTitleCase(subtitle)}"
            } else {
                "Chapter $num"
            }
        }

        // Named sections
        if (clean.matches(Regex("(?i)^prologue.*"))) return "Prologue"
        if (clean.matches(Regex("(?i)^epilogue.*"))) return "Epilogue"
        if (clean.matches(Regex("(?i)^introduction.*"))) return "Introduction"
        if (clean.matches(Regex("(?i)^preface.*"))) return "Preface"

        // Roman numeral alone: "I: The Scribe in the Shallows"
        val romanAlone = Regex("^([IVXLCDM]+)(?:[:\\s\\-\\.\\—\\–]+(.*))?$")
        val romanMatch = romanAlone.find(clean)
        if (romanMatch != null) {
            val roman = romanMatch.groupValues[1]
            val num = parseRomanNumeral(roman) ?: fallbackNum
            val subtitle = romanMatch.groupValues.getOrNull(2)?.trim()?.trim(':', '-', '.', '—', '–', ' ') ?: ""
            return if (subtitle.isNotBlank()) {
                "Chapter $num: ${cleanTitleCase(subtitle)}"
            } else {
                "Chapter $num"
            }
        }

        return cleanTitleCase(clean)
    }

    private fun splitIntoNaturalChapters(fullText: String): List<ExtractedChapter> {
        val clean = fullText.trim()
        val paragraphs = clean.split(Regex("\n{2,}")).filter { it.isNotBlank() }

        val chapters = mutableListOf<ExtractedChapter>()
        var currentChapterLines = mutableListOf<String>()
        var currentWords = 0
        var chapterIndex = 1

        for (p in paragraphs) {
            val pWords = countWords(p)
            // Check for explicit scene break marks
            val isSceneBreak = p.trim() in listOf("***", "* * *", "---", "___", "###")

            if ((currentWords >= 1000 && pWords > 0) || (currentWords >= 600 && isSceneBreak)) {
                if (currentChapterLines.isNotEmpty()) {
                    val content = currentChapterLines.joinToString("\n\n").trim()
                    chapters.add(
                        ExtractedChapter(
                            chapterNumber = chapterIndex,
                            title = "Chapter $chapterIndex",
                            content = content,
                            wordCount = currentWords
                        )
                    )
                    chapterIndex++
                    currentChapterLines.clear()
                    currentWords = 0
                }
                if (isSceneBreak) continue
            }

            if (!isSceneBreak) {
                currentChapterLines.add(p)
                currentWords += pWords
            }
        }

        if (currentChapterLines.isNotEmpty()) {
            val content = currentChapterLines.joinToString("\n\n").trim()
            chapters.add(
                ExtractedChapter(
                    chapterNumber = chapterIndex,
                    title = "Chapter $chapterIndex",
                    content = content,
                    wordCount = currentWords
                )
            )
        }

        return if (chapters.isEmpty()) {
            listOf(
                ExtractedChapter(
                    chapterNumber = 1,
                    title = "Chapter 1",
                    content = clean,
                    wordCount = countWords(clean)
                )
            )
        } else {
            chapters
        }
    }

    private fun cleanTitleCase(str: String): String {
        val trimmed = str.trim()
        if (trimmed.isBlank()) return ""
        val words = trimmed.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            val w = word.lowercase(Locale.US)
            if (w in listOf("a", "an", "the", "in", "on", "of", "and", "or", "for", "by", "to") && word != words.first()) {
                w
            } else {
                word.lowercase(Locale.US).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        }
    }

    private fun countWords(text: String): Int {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
}
