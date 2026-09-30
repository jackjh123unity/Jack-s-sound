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
    val totalPages: Int,
    val pagesText: List<String>,
    val coverImagePath: String? = null,
    val documentType: String = "Word Document"
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
                    if (docxResult.pagesText.isNotEmpty()) return@withContext docxResult
                } catch (e: Exception) {
                    // Not docx
                }

                // Try PDF
                try {
                    val pdfResult = extractFromPdf(context, uri, displayName)
                    if (pdfResult.pagesText.isNotEmpty() && !pdfResult.pagesText.first().contains("endstream")) {
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
        val cleanTitle = title.ifBlank { "Google Doc Story" }.replace(Regex("[_\\-]"), " ")
        val cleanAuthor = author.ifBlank { "Unknown Author" }

        val pages = paginateText(rawContent)
        ExtractedDocumentResult(
            title = cleanTitle,
            author = cleanAuthor,
            totalPages = pages.size.coerceAtLeast(1),
            pagesText = if (pages.isEmpty()) listOf("No readable text found in document.") else pages,
            documentType = "Google Doc"
        )
    }

    suspend fun extractFromGoogleDocUrl(docUrl: String): ExtractedDocumentResult? = withContext(Dispatchers.IO) {
        try {
            // Check if it's a Google Doc URL: https://docs.google.com/document/d/<DOC_ID>/...
            val match = Regex("/document/d/([a-zA-Z0-9_-]+)").find(docUrl)
            val docId = match?.groupValues?.get(1) ?: return@withContext null

            // Construct export txt URL
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
                    val detectedTitle = lines.firstOrNull()?.take(50) ?: "Google Doc"
                    return@withContext extractFromText(detectedTitle, "Google Docs", text)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Extracts clean text from Word .docx documents (including Google Docs exported as Word).
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

        val cleanTitle = if (detectedTitle.isNotBlank()) {
            detectedTitle
        } else {
            cleanFileNameToTitle(fileName)
        }

        val pages = mutableListOf<String>()
        if (documentXml != null) {
            val pPattern = Pattern.compile("<w:p[ >](.*?)</w:p>", Pattern.DOTALL)
            val pMatcher = pPattern.matcher(documentXml!!)
            val tPattern = Pattern.compile("<w:t[^>]*>([^<]*)</w:t>")
            val pageBreakPattern = Pattern.compile("<w:br[^>]+w:type=[\"']page[\"']")

            var currentPage = StringBuilder()
            var currentWordCount = 0

            while (pMatcher.find()) {
                val pContent = pMatcher.group(1) ?: continue
                val isExplicitPageBreak = pageBreakPattern.matcher(pContent).find()

                val pText = StringBuilder()
                val tMatcher = tPattern.matcher(pContent)
                while (tMatcher.find()) {
                    val rawT = tMatcher.group(1) ?: ""
                    pText.append(unescapeXml(rawT))
                }

                val paragraphStr = pText.toString().trim()
                if (paragraphStr.isNotEmpty()) {
                    val wordsInP = paragraphStr.split(Regex("\\s+")).filter { it.isNotBlank() }.size

                    if (isExplicitPageBreak && currentPage.isNotEmpty()) {
                        pages.add(currentPage.toString().trim())
                        currentPage = StringBuilder()
                        currentWordCount = 0
                    } else if (currentWordCount + wordsInP > 280 && currentPage.isNotEmpty()) {
                        pages.add(currentPage.toString().trim())
                        currentPage = StringBuilder()
                        currentWordCount = 0
                    }

                    if (currentPage.isNotEmpty()) {
                        currentPage.append("\n\n")
                    }
                    currentPage.append(paragraphStr)
                    currentWordCount += wordsInP
                }
            }

            if (currentPage.isNotEmpty()) {
                pages.add(currentPage.toString().trim())
            }
        }

        val finalPages = if (pages.isEmpty()) {
            listOf("Document is empty or contains non-text elements.")
        } else {
            pages
        }

        return ExtractedDocumentResult(
            title = cleanTitle,
            author = detectedAuthor,
            totalPages = finalPages.size,
            pagesText = finalPages,
            documentType = "Google Doc Word (.docx)"
        )
    }

    /**
     * Extracts readable text from legacy .doc files.
     */
    private fun extractFromDoc(context: Context, uri: Uri, fileName: String): ExtractedDocumentResult {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
        val cleanTitle = cleanFileNameToTitle(fileName)

        // Legacy .doc extraction: scan for UTF-16LE / ASCII text blocks
        val extracted = extractPrintableTextFromBinary(rawBytes)
        val pages = paginateText(extracted)

        return ExtractedDocumentResult(
            title = cleanTitle,
            author = "Document Author",
            totalPages = pages.size.coerceAtLeast(1),
            pagesText = if (pages.isEmpty()) listOf("No readable text found in document.") else pages,
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

        val cleanTitle = cleanFileNameToTitle(fileName)
        val pages = paginateText(content)

        return ExtractedDocumentResult(
            title = cleanTitle,
            author = "Google Doc / Plain Text",
            totalPages = pages.size.coerceAtLeast(1),
            pagesText = if (pages.isEmpty()) listOf("Document is empty.") else pages,
            documentType = "Google Doc / Text"
        )
    }

    /**
     * Enhanced PDF extractor with safeguards against dumping raw PDF syntax objects.
     */
    private fun extractFromPdf(context: Context, uri: Uri, fileName: String): ExtractedDocumentResult {
        val cleanTitle = cleanFileNameToTitle(fileName)
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

        val parsedPages = tryExtractCleanPdfText(rawBytes, pageCount)

        return ExtractedDocumentResult(
            title = cleanTitle,
            author = "Unknown Author",
            totalPages = parsedPages.size.coerceAtLeast(1),
            pagesText = parsedPages,
            coverImagePath = coverPath,
            documentType = "PDF Document"
        )
    }

    private fun tryExtractCleanPdfText(bytes: ByteArray, expectedPages: Int): List<String> {
        val textBlocks = mutableListOf<String>()
        val streamPattern = Pattern.compile("stream[\\r\\n]+(.*?)endstream", Pattern.DOTALL)
        val matcher = streamPattern.matcher(String(bytes, Charsets.ISO_8859_1))

        val fullTextBuilder = StringBuilder()

        while (matcher.find()) {
            val streamContent = matcher.group(1) ?: continue
            val rawStreamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
            val decompressed = tryDecompressFlate(rawStreamBytes) ?: rawStreamBytes
            val decompressedStr = String(decompressed, Charsets.ISO_8859_1)

            // Look for BT ... ET blocks
            val btPattern = Pattern.compile("BT\\s+(.*?)\\s+ET", Pattern.DOTALL)
            val btMatcher = btPattern.matcher(decompressedStr)
            while (btMatcher.find()) {
                val textBlock = btMatcher.group(1) ?: continue

                // 1. Literal strings: (Hello world) Tj
                val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|'|\")")
                val tjMatcher = tjPattern.matcher(textBlock)
                while (tjMatcher.find()) {
                    val rawWord = tjMatcher.group(1) ?: ""
                    val cleaned = cleanPdfString(rawWord)
                    if (cleaned.isNotBlank() && !isPdfSyntaxKeyword(cleaned)) {
                        fullTextBuilder.append(cleaned).append(" ")
                    }
                }

                // 2. TJ array: [(Hello) -10 (World)] TJ
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
            val paginated = paginateText(allExtracted)
            if (paginated.isNotEmpty()) return paginated
        }

        // If PDF was scanned or encrypted, return helpful message instead of garbage raw code
        return listOf(
            "This document appears to contain scanned image pages or specialized font encodings.\n\n" +
            "💡 Tip: For perfect voice reading, open your story in Google Docs or Word, choose 'Download as Microsoft Word (.docx)', and import the .docx file directly into StoryCast Audio!"
        )
    }

    private fun isPdfSyntaxKeyword(str: String): Boolean {
        val s = str.trim()
        return s.startsWith("endstream") || s.startsWith("endobj") || s.startsWith("obj") ||
                s.contains("MediaBox") || s.contains("ProcSet") || s.contains("ExtGState") ||
                s.contains("Font") && s.contains("/R")
    }

    private fun paginateText(content: String): List<String> {
        val paragraphs = content.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return emptyList()

        val pages = mutableListOf<String>()
        var currentPage = StringBuilder()
        var currentWords = 0

        for (p in paragraphs) {
            val words = p.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            if (currentWords + words > 260 && currentPage.isNotEmpty()) {
                pages.add(currentPage.toString().trim())
                currentPage = StringBuilder()
                currentWords = 0
            }
            if (currentPage.isNotEmpty()) {
                currentPage.append("\n\n")
            }
            currentPage.append(p)
            currentWords += words
        }

        if (currentPage.isNotEmpty()) {
            pages.add(currentPage.toString().trim())
        }

        return pages
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
        // Replace msf:1000007491 or random ids
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
