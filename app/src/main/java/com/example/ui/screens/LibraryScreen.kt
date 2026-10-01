package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.BookEntity
import com.example.data.model.VoiceCatalog
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.DeepSlateSurface
import com.example.ui.theme.GoldenAmberPrimary
import com.example.ui.theme.MidnightObsidian
import com.example.ui.theme.MysticPurpleSecondary
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<BookEntity>,
    onSelectBook: (Long) -> Unit,
    onImportDocument: (Uri) -> Unit = {},
    onImportPdf: (Uri) -> Unit = onImportDocument,
    onImportPastedDoc: (title: String, author: String, content: String) -> Unit = { _, _, _ -> },
    onImportGoogleDocLink: (url: String) -> Unit = {},
    onDeleteBook: (Long) -> Unit,
    onDownloadSingleFile: (Long) -> Unit,
    onOpenVoiceSelector: () -> Unit
) {
    var showImportChooser by remember { mutableStateOf(false) }
    var showPasteDocDialog by remember { mutableStateOf(false) }
    var pasteTitle by remember { mutableStateOf("") }
    var pasteAuthor by remember { mutableStateOf("") }
    var pasteContent by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportDocument(it) }
    }

    val fallbackPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportDocument(it) }
    }

    Scaffold(
        containerColor = MidnightObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A2146)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = GoldenAmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "StoryCast Audio",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Google Doc & Word to Audio Studio",
                                fontSize = 11.sp,
                                color = AmberGlow
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenVoiceSelector,
                        modifier = Modifier.testTag("btn_top_voice_selector")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Roster",
                            tint = MysticPurpleSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightObsidian)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showImportChooser = true },
                containerColor = GoldenAmberPrimary,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.PostAdd, contentDescription = "Import Story") },
                text = { Text("Import Google Doc / Word", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("btn_import_doc_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Atmospheric Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSlateSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2E3349), RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.audiobook_hero_1790734972693),
                            contentDescription = "Audiobook Hero",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xEE0F111A)),
                                        startY = 60f
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldenAmberPrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "10 NATURAL VOICES",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "5 Male • 5 Female",
                                    fontSize = 11.sp,
                                    color = AmberGlow
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Convert Word & Google Docs into Expressive Audiobooks",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Quick Format Support Badge Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = GoldenAmberPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Supported Formats",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Microsoft Word (.docx, .doc), Google Docs, Plain Text (.txt), and PDF",
                                fontSize = 11.sp,
                                color = Color(0xFFB0B7D0)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Ebooks (${books.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Offline Ready",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (books.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No ebooks loaded yet", color = Color.White, fontWeight = FontWeight.Medium)
                            Text("Tap 'Import PDF' to load an ebook", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(books, key = { it.id }) { book ->
                    val voice = VoiceCatalog.getById(book.selectedVoiceId)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBook(book.id) }
                            .testTag("book_card_${book.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D40))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Book Cover Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(64.dp, 84.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2130)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (book.coverImagePath != null) {
                                        AsyncImage(
                                            model = book.coverImagePath,
                                            contentDescription = book.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = book.coverDrawableRes ?: R.drawable.audiobook_hero_1790734972693),
                                            contentDescription = book.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "by ${book.author}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2A2342))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${book.totalPages} Chapters",
                                                fontSize = 11.sp,
                                                color = MysticPurpleSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF1E2538))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${voice.name} (${voice.gender.name})",
                                                fontSize = 11.sp,
                                                color = AmberGlow
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteBook(book.id) },
                                    modifier = Modifier.testTag("btn_delete_book_${book.id}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Book",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Bar for this Book
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Primary Action: Open / Listen
                                Button(
                                    onClick = { onSelectBook(book.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldenAmberPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_open_book_${book.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open & Listen", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Single-File Download Button
                                OutlinedButton(
                                    onClick = { onDownloadSingleFile(book.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (book.isSingleFileReady) SuccessGreen else MysticPurpleSecondary
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (book.isSingleFileReady) SuccessGreen else MysticPurpleSecondary
                                    ),
                                    modifier = Modifier.testTag("btn_single_download_${book.id}")
                                ) {
                                    Icon(
                                        imageVector = if (book.isSingleFileReady) Icons.Default.CheckCircle else Icons.Default.Download,
                                        contentDescription = "Download Single File",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (book.isSingleFileReady) "Single File Ready" else "Download 1-File",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        if (showImportChooser) {
            AlertDialog(
                onDismissRequest = { showImportChooser = false },
                containerColor = DeepSlateSurface,
                title = {
                    Text(
                        text = "Import Story / Document",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose how you want to add your story to StoryCast Audio:",
                            color = Color(0xFFD0D6E8),
                            fontSize = 13.sp
                        )

                        // Option 1: File from device
                        Card(
                            onClick = {
                                showImportChooser = false
                                try {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                } catch (e: Exception) {
                                    fallbackPickerLauncher.launch("*/*")
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("option_import_file")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2E46)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = GoldenAmberPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Choose File from Phone",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Word (.docx, .doc), Google Doc download, Text, or PDF",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA0A8C4)
                                    )
                                }
                            }
                        }

                        // Option 2: Paste Google Doc text or link
                        Card(
                            onClick = {
                                showImportChooser = false
                                showPasteDocDialog = true
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("option_paste_google_doc")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2A2E46)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = MysticPurpleSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Paste Google Doc Text / Link",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Paste your story text directly or provide a Google Doc URL",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA0A8C4)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImportChooser = false }) {
                        Text("Cancel", color = GoldenAmberPrimary)
                    }
                }
            )
        }

        if (showPasteDocDialog) {
            AlertDialog(
                onDismissRequest = { showPasteDocDialog = false },
                containerColor = DeepSlateSurface,
                title = {
                    Text(
                        text = "Import Google Doc / Story",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Paste your text from Google Docs or Word. It will be cleanly paginated and converted into expressive voice audio.",
                            color = Color(0xFFD0D6E8),
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = pasteTitle,
                            onValueChange = { pasteTitle = it },
                            label = { Text("Story / Document Title") },
                            placeholder = { Text("e.g. The King's Journey") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldenAmberPrimary,
                                unfocusedBorderColor = Color(0xFF3F4665),
                                focusedLabelColor = GoldenAmberPrimary,
                                unfocusedLabelColor = Color(0xFFA0A8C4)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_paste_title")
                        )

                        OutlinedTextField(
                            value = pasteAuthor,
                            onValueChange = { pasteAuthor = it },
                            label = { Text("Author (Optional)") },
                            placeholder = { Text("e.g. J. Doe") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldenAmberPrimary,
                                unfocusedBorderColor = Color(0xFF3F4665),
                                focusedLabelColor = GoldenAmberPrimary,
                                unfocusedLabelColor = Color(0xFFA0A8C4)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_paste_author")
                        )

                        OutlinedTextField(
                            value = pasteContent,
                            onValueChange = { pasteContent = it },
                            label = { Text("Story Text or Google Doc URL") },
                            placeholder = { Text("Paste story chapters here, or https://docs.google.com/document/d/...") },
                            minLines = 5,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldenAmberPrimary,
                                unfocusedBorderColor = Color(0xFF3F4665),
                                focusedLabelColor = GoldenAmberPrimary,
                                unfocusedLabelColor = Color(0xFFA0A8C4)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_paste_content")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = pasteContent.trim()
                            if (trimmed.isNotBlank()) {
                                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                    onImportGoogleDocLink(trimmed)
                                } else {
                                    val title = pasteTitle.ifBlank { "Google Doc Story" }
                                    val author = pasteAuthor.ifBlank { "Author" }
                                    onImportPastedDoc(title, author, trimmed)
                                }
                                showPasteDocDialog = false
                                pasteTitle = ""
                                pasteAuthor = ""
                                pasteContent = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenAmberPrimary),
                        enabled = pasteContent.isNotBlank(),
                        modifier = Modifier.testTag("btn_confirm_import_paste")
                    ) {
                        Text("Import to Audio Studio", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasteDocDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}
