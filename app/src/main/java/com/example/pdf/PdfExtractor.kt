package com.example.pdf

import android.content.Context
import android.net.Uri
import com.example.document.DocumentExtractor
import com.example.document.ExtractedDocumentResult

typealias ExtractedPdfResult = ExtractedDocumentResult

object PdfExtractor {
    suspend fun extractFromUri(context: Context, uri: Uri): ExtractedPdfResult {
        return DocumentExtractor.extractFromUri(context, uri)
    }
}
