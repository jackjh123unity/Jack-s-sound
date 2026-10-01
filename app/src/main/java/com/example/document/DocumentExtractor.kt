package com.example.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

data class ExtractedDocumentResult(
    val title: String,
    val author: String,
    val totalChapters: Int,
    val chapters: List<ExtractedChapter>,
    val initialChapterNumber: Int = 1,
    val coverImagePath: String? = null,
    val documentType: String = "Word Document",
    val totalPages: Int = totalChapters,
    val pagesText: List<String> = chapters.map { it.content }
)

object DocumentExtractor {

    suspend fun extractFromUri(context: Context, uri: Uri): ExtractedDocumentResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = getDisplayName(context, uri)
        val extension = displayName.substringAfterLast('.', "").lowercase()

        when {
            extension in listOf("docx", "dotx") -> {
                extractFromDocx(context, uri, displayName)
            }
            extension in listOf("doc") -> {
                extractFromDoc(context, uri, displayName)
            }
            extension in listOf("txt", "text", "md", "rtf") -> {
                extractFromPlainText(context, uri, displayName)
            }
            extension == "pdf" -> {
                extractFromPdf(context, uri, displayName)
            }
            else -> {
                // Try DOCX first (standard zip check)
                try {
                    val docxResult = extractFromDocx(context, uri, displayName)
                    if (docxResult.chapters.isNotEmpty()) return@withContext docxResult
                } catch (e: Exception) {
                    // Not docx
                }

                // Try PDF
                try {
                    val pdfResult = extractFromPdf(context, uri, displayName)
                    if (pdfResult.chapters.isNotEmpty() && !pdfResult.chapters.first().content.contains("endstream")) {
                        return@withContext pdfResult
                    }
                } catch (e: Exception) {
                    // Not pdf
                }

                // Fallback to text reading
                extractFromPlainText(context, uri, displayName)
            }
        }
    }

    suspend fun extractFromText(title: String, author: String, rawContent: String): ExtractedDocumentResult = withContext(Dispatchers.Default) {
        val scan = ChapterScanner.scanDocumentForChapters(rawContent, title, author)
        ExtractedDocumentResult(
            title = scan.title,
            author = scan.author,
            totalChapters = scan.chapters.size,
            chapters = scan.chapters,
            initialChapterNumber = scan.initialChapterNumber,
            documentType = "Google Doc / Text"
        )
    }

    suspend fun extractFromGoogleDocUrl(docUrl: String): ExtractedDocumentResult? = withContext(Dispatchers.IO) {
        try {
            val match = Regex("/document/d/([a-zA-Z0-9_-]+)").find(docUrl)
            val docId = match?.groupValues?.get(1) ?: return@withContext null

            val exportUrl = "https://docs.google.com/document/d/$docId/export?format=txt"
            val url = URL(exportUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (text.isNotBlank()) {
                    val lines = text.lines().filter { it.isNotBlank() }
                    val detectedTitle = lines.firstOrNull()?.take(60) ?: "Google Doc"
                    return@withContext extractFromText(detectedTitle, "Google Docs", text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Extracts clean chapters from Word .docx documents (including Google Docs exported as Word).
     */
    private fun extractFromDocx(context: Context, uri: Uri, fileName: String): ExtractedDocumentResult {
        var documentXml: String? = null
        var coreXml: String? = null

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    documentXml = zip.bufferedReader(Charsets.UTF_8).readText()
                } else if (entry.name == "docProps/core.xml") {
                    coreXml = zip.bufferedReader(Charsets.UTF_8).readText()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        var detectedTitle = ""
        var detectedAuthor = "Google Doc / Word Author"

        if (coreXml != null) {
            val titleMatch = Regex("<dc:title>([^<]+)</dc:title>").find(coreXml)
            if (titleMatch != null) detectedTitle = titleMatch.groupValues[1]

            val authorMatch = Regex("<dc:creator>([^<]+)</dc:creator>").find(coreXml)
            if (authorMatch != null) detectedAuthor = authorMatch.groupValues[1]
        }

        val (parsedTitle, parsedAuthor) = ChapterScanner.parseTitleAndAuthor(
            if (detectedTitle.isNotBlank()) detectedTitle else cleanFileNameToTitle(fileName),
            detectedAuthor
        )

        val fullTextBuilder = StringBuilder()
        if (documentXml != null) {
            val pPattern = Pattern.compile("<w:p[ >](.*?)</w:p>", Pattern.DOTALL)
            val pMatcher = pPattern.matcher(documentXml!!)
            val tPattern = Pattern.compile("<w:t[^>]*>([^<]*)</w:t>")

            while (pMatcher.find()) {
                val pContent = pMatcher.group(1) ?: continue
                val hasPageBreak = pContent.contains("<w:br w:type=\"page\"") ||
                        pContent.contains("<w:pageBreakBefore") ||
                        pContent.contains("<w:lastRenderedPageBreak")
                if (hasPageBreak) {
                    fullTextBuilder.append("\n===PAGEBREAK===\n\n")
                }

                val isHeading = pContent.contains("w:val=\"Heading") || pContent.contains("w:val=\"Title")

                val pText = StringBuilder()
                val tMatcher = tPattern.matcher(pContent)
                while (tMatcher.find()) {
                    val rawT = tMatcher.group(1) ?: ""
                    pText.append(unescapeXml(rawT))
                }

                val paragraphStr = pText.toString().trim()
                if (paragraphStr.isNotEmpty()) {
                    if (isHeading && !ChapterScanner.isChapterHeading(paragraphStr)) {
                        fullTextBuilder.append("===HEADING===").append(paragraphStr).append("\n\n")
                    } else {
                        fullTextBuilder.append(paragraphStr).append("\n\n")
                    }
                }
            }
        }

        val rawFullText = fullTextBuilder.toString().trim()
        val scan = ChapterScanner.scanDocumentForChapters(
            rawFullText,
            parsedTitle,
            parsedAuthor
        )

        return ExtractedDocumentResult(
            title = scan.title,
            author = scan.author,
            totalChapters = scan.chapters.size,
            chapters = scan.chapters,
            initialChapterNumber = scan.initialChapterNumber,
            documentType = "Google Doc Word (.docx)"
        )
    }

    /**
     * Extracts readable text from legacy .doc files.
     */
    private fun extractFromDoc(context: Context, uri: Uri, fileName: String): ExtractedDocumentResult {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
        val (parsedTitle, parsedAuthor) = ChapterScanner.parseTitleAndAuthor(cleanFileNameToTitle(fileName))

        val extracted = extractPrintableTextFromBinary(rawBytes)
        val scan = ChapterScanner.scanDocumentForChapters(extracted, parsedTitle, parsedAuthor)

        return ExtractedDocumentResult(
            title = scan.title,
            author = scan.author,
            totalChapters = scan.chapters.size,
            chapters = scan.chapters,
            initialChapterNumber = scan.initialChapterNumber,
            documentType = "Word Document (.doc)"
        )
    }

    /**
     * Extracts text from plain text or markdown files (.txt, .md).
     */
    private fun extractFromPlainText(context: Context, uri: Uri, fileName: String): ExtractedDocumentResult {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: ""

        val (parsedTitle, parsedAuthor) = ChapterScanner.parseTitleAndAuthor(cleanFileNameToTitle(fileName))
        val scan = ChapterScanner.scanDocumentForChapters(content, parsedTitle, parsedAuthor)

        return ExtractedDocumentResult(
            title = scan.title,
            author = scan.author,
            totalChapters = scan.chapters.size,
            chapters = scan.chapters,
            initialChapterNumber = scan.initialChapterNumber,
            documentType = "Google Doc / Text"
        )
    }

    /**
     * Enhanced PDF extractor scanning into chapters.
     */
    private fun extractFromPdf(context: Context, uri: Uri, fileName: String): ExtractedDocumentResult {
        val (parsedTitle, parsedAuthor) = ChapterScanner.parseTitleAndAuthor(cleanFileNameToTitle(fileName))
        val tempFile = File(context.cacheDir, "import_pdf_${UUID.randomUUID()}.pdf")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        var coverPath: String? = null
        var pageCount = 1

        try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount.coerceAtLeast(1)

            if (pageCount > 0) {
                val firstPage = renderer.openPage(0)
                val width = firstPage.width.coerceAtMost(720)
                val height = (firstPage.height * (width.toFloat() / firstPage.width)).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                firstPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                firstPage.close()

                val coverFile = File(context.filesDir, "cover_${UUID.randomUUID()}.jpg")
                FileOutputStream(coverFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                coverPath = coverFile.absolutePath
            }
            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val rawBytes = try { tempFile.readBytes() } catch (e: Exception) { byteArrayOf() }
        tempFile.delete()

        val fullText = tryExtractCleanPdfText(rawBytes)
        val scan = ChapterScanner.scanDocumentForChapters(fullText, parsedTitle, parsedAuthor)

        return ExtractedDocumentResult(
            title = scan.title,
            author = scan.author,
            totalChapters = scan.chapters.size,
            chapters = scan.chapters,
            initialChapterNumber = scan.initialChapterNumber,
            coverImagePath = coverPath,
            documentType = "PDF Document"
        )
    }

    private fun tryExtractCleanPdfText(bytes: ByteArray): String {
        val streamPattern = Pattern.compile("stream[\\r\\n]+(.*?)endstream", Pattern.DOTALL)
        val matcher = streamPattern.matcher(String(bytes, Charsets.ISO_8859_1))

        val fullTextBuilder = StringBuilder()

        while (matcher.find()) {
            val streamContent = matcher.group(1) ?: continue
            val rawStreamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
            val decompressed = tryDecompressFlate(rawStreamBytes) ?: rawStreamBytes
            val decompressedStr = String(decompressed, Charsets.ISO_8859_1)

            val btPattern = Pattern.compile("BT\\s+(.*?)\\s+ET", Pattern.DOTALL)
            val btMatcher = btPattern.matcher(decompressedStr)
            while (btMatcher.find()) {
                val textBlock = btMatcher.group(1) ?: continue

                val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|'|\")")
                val tjMatcher = tjPattern.matcher(textBlock)
                while (tjMatcher.find()) {
                    val rawWord = tjMatcher.group(1) ?: ""
                    val cleaned = cleanPdfString(rawWord)
                    if (cleaned.isNotBlank() && !isPdfSyntaxKeyword(cleaned)) {
                        fullTextBuilder.append(cleaned).append(" ")
                    }
                }

                val arrayPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ")
                val arrayMatcher = arrayPattern.matcher(textBlock)
                while (arrayMatcher.find()) {
                    val arrayContent = arrayMatcher.group(1) ?: continue
                    val innerTjMatcher = Pattern.compile("\\((.*?)\\)").matcher(arrayContent)
                    while (innerTjMatcher.find()) {
                        val innerWord = cleanPdfString(innerTjMatcher.group(1) ?: "")
                        if (innerWord.isNotBlank() && !isPdfSyntaxKeyword(innerWord)) {
                            fullTextBuilder.append(innerWord).append(" ")
                        }
                    }
                    fullTextBuilder.append(" ")
                }
            }
        }

        val allExtracted = fullTextBuilder.toString().trim()
        if (allExtracted.length > 50) {
            return allExtracted
        }

        return "This document appears to contain scanned image pages or specialized font encodings.\n\n" +
                "💡 Tip: For perfect voice reading, open your story in Google Docs or Word, choose 'Download as Microsoft Word (.docx)', and import the .docx file directly into StoryCast Audio!"
    }

    private fun isPdfSyntaxKeyword(str: String): Boolean {
        val s = str.trim()
        return s.startsWith("endstream") || s.startsWith("endobj") || s.startsWith("obj") ||
                s.contains("MediaBox") || s.contains("ProcSet") || s.contains("ExtGState") ||
                s.contains("Font") && s.contains("/R")
    }

    private fun extractPrintableTextFromBinary(bytes: ByteArray): String {
        val sb = StringBuilder()
        var currentRun = StringBuilder()

        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c.isLetterOrDigit() || c in " \t\n.,!?;:'\"-()") {
                currentRun.append(c)
            } else {
                if (currentRun.length >= 8) {
                    val s = currentRun.toString().trim()
                    if (s.split(' ').size >= 2) {
                        sb.append(s).append("\n\n")
                    }
                }
                currentRun.clear()
            }
        }
        return sb.toString().trim()
    }

    private fun getDisplayName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            result = cursor.getString(idx)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.lastPathSegment?.substringAfterLast('/')
        }
        return result ?: "Story Document"
    }

    private fun cleanFileNameToTitle(fileName: String): String {
        val noExt = fileName.substringBeforeLast('.')
        if (noExt.lowercase().startsWith("msf:") || noExt.matches(Regex("^[0-9_-]+$"))) {
            return "Imported Story"
        }
        val clean = noExt.replace(Regex("[_\\-]"), " ").trim()
        return clean.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }.ifBlank { "Imported Story" }
    }

    private fun unescapeXml(str: String): String {
        return str.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun cleanPdfString(str: String): String {
        return str.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\r", "\r")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
    }

    private fun tryDecompressFlate(data: ByteArray): ByteArray? {
        return try {
            val inflater = Inflater(false)
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            inflater.setInput(data)
            while (!inflater.finished() && !inflater.needsInput()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    outputStream.write(buffer, 0, count)
                }
            }
            inflater.end()
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
