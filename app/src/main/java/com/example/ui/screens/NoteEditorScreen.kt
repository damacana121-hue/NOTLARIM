package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.pdf.PdfExporter
import com.example.security.SecurityManager
import com.example.ui.components.AudioRecorderBar
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.RichEditorToolbar
import com.example.ui.components.SecurityDialogMode
import com.example.ui.components.SecurityLockDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity,
    folders: List<FolderEntity>,
    onSaveAndClose: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    val context = LocalContext.current

    // Note form state
    var title by remember { mutableStateOf(initialNote.title) }
    // If locked and decrypted content is available or plain content
    var content by remember {
        mutableStateOf(
            if (initialNote.isLocked && initialNote.encryptedContent.isNotBlank() && initialNote.content.isBlank()) {
                // If unlocked during session
                initialNote.content
            } else {
                initialNote.content
            }
        )
    }
    var folder by remember { mutableStateOf(initialNote.folder) }
    var categoryColor by remember { mutableLongStateOf(initialNote.categoryColor) }
    var tagsText by remember { mutableStateOf(initialNote.tags) }
    var drawingData by remember { mutableStateOf(initialNote.drawingData) }
    var audioPath by remember { mutableStateOf(initialNote.audioPath) }
    var audioDurationMs by remember { mutableLongStateOf(initialNote.audioDurationMs) }
    var transcription by remember { mutableStateOf(initialNote.transcription) }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }
    var fontSize by remember { mutableIntStateOf(initialNote.fontSize) }
    var textColor by remember { mutableLongStateOf(initialNote.textColor) }

    // Active tab: 0: Text, 1: Drawing, 2: Audio & Transcription
    var selectedTab by remember {
        mutableIntStateOf(
            if (initialNote.isHandwritingMode || initialNote.drawingData.isNotBlank()) 1
            else if (initialNote.audioPath.isNotBlank()) 2
            else 0
        )
    }

    var showFolderMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var securityDialogMode by remember { mutableStateOf<SecurityDialogMode?>(null) }

    val categoryColors = listOf(
        0xFFFFE082, // Amber
        0xFFFFAB91, // Coral
        0xFFA5D6A7, // Mint
        0xFF90CAF9, // Sky Blue
        0xFFCE93D8, // Lavender
        0xFFF48FB1  // Rose
    )

    fun createCurrentNoteEntity(): NoteEntity {
        return initialNote.copy(
            title = title,
            content = content,
            folder = folder,
            categoryColor = categoryColor,
            tags = tagsText,
            drawingData = drawingData,
            audioPath = audioPath,
            audioDurationMs = audioDurationMs,
            transcription = transcription,
            isPinned = isPinned,
            fontSize = fontSize,
            textColor = textColor,
            isHandwritingMode = selectedTab == 1,
            updatedAt = System.currentTimeMillis()
        )
    }

    // Save and pop on Back
    BackHandler {
        onSaveAndClose(createCurrentNoteEntity())
    }

    // Security Dialog
    if (securityDialogMode != null) {
        SecurityLockDialog(
            mode = securityDialogMode!!,
            onDismiss = { securityDialogMode = null },
            onUnlockConfirmed = { pin ->
                val isValid = SecurityManager.verifyPin(pin, initialNote.encryptedPasswordHash)
                if (isValid) {
                    content = SecurityManager.decrypt(initialNote.encryptedContent, pin)
                    securityDialogMode = null
                }
            },
            onLockConfirmed = { pin ->
                val pinHash = SecurityManager.hashPin(pin)
                val cipher = SecurityManager.encrypt(content, pin)
                val lockedNote = createCurrentNoteEntity().copy(
                    isLocked = true,
                    encryptedPasswordHash = pinHash,
                    encryptedContent = cipher,
                    content = ""
                )
                securityDialogMode = null
                onSaveAndClose(lockedNote)
            },
            onRemoveLockConfirmed = { pin ->
                val isValid = SecurityManager.verifyPin(pin, initialNote.encryptedPasswordHash)
                if (isValid) {
                    content = SecurityManager.decrypt(initialNote.encryptedContent, pin)
                    val unlocked = createCurrentNoteEntity().copy(
                        isLocked = false,
                        encryptedPasswordHash = "",
                        encryptedContent = "",
                        content = content
                    )
                    securityDialogMode = null
                    onSaveAndClose(unlocked)
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Notu Sil?") },
            text = { Text("Bu not kalıcı olarak silinecektir. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNote(initialNote.id)
                    }
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Folder & Color picker chips in header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Folder Selector
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showFolderMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(folder, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            DropdownMenu(
                                expanded = showFolderMenu,
                                onDismissRequest = { showFolderMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Genel") },
                                    onClick = {
                                        folder = "Genel"
                                        showFolderMenu = false
                                    }
                                )
                                folders.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f.name) },
                                        onClick = {
                                            folder = f.name
                                            showFolderMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Category Color Dot Picker
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(categoryColor))
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable { showColorMenu = true }
                            )

                            DropdownMenu(
                                expanded = showColorMenu,
                                onDismissRequest = { showColorMenu = false }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categoryColors.forEach { col ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(col))
                                                .border(
                                                    width = if (categoryColor == col) 2.dp else 1.dp,
                                                    color = if (categoryColor == col) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    categoryColor = col
                                                    showColorMenu = false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onSaveAndClose(createCurrentNoteEntity()) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kaydet ve Geri Dön")
                    }
                },
                actions = {
                    // Pin toggle
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Sabitle",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // PDF Export & Share
                    IconButton(
                        onClick = {
                            val entity = createCurrentNoteEntity()
                            val pdfFile = PdfExporter.exportNoteToPdf(context, entity)
                            if (pdfFile != null) {
                                PdfExporter.shareFile(context, pdfFile)
                            }
                        },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF Aktar", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Plain text share
                    IconButton(
                        onClick = {
                            PdfExporter.shareText(context, title, content)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Paylaş")
                    }

                    // Lock / Security
                    IconButton(
                        onClick = {
                            if (initialNote.isLocked) {
                                securityDialogMode = SecurityDialogMode.RemoveLock(createCurrentNoteEntity())
                            } else {
                                securityDialogMode = SecurityDialogMode.Lock(createCurrentNoteEntity())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (initialNote.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "Güvenlik Kilidi",
                            tint = if (initialNote.isLocked) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Delete
                    if (initialNote.id != 0L) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (selectedTab == 0) {
                // Calculate word and char count
                val words = remember(content) {
                    content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                }
                RichEditorToolbar(
                    fontSize = fontSize,
                    onFontSizeChange = { fontSize = it },
                    textColor = textColor,
                    onTextColorChange = { textColor = it },
                    onInsertText = { toInsert ->
                        content = "$content$toInsert"
                    },
                    wordCount = words,
                    charCount = content.length
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Samsung Notes Editor Mode Switcher Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Metin") },
                    icon = { Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("El Yazısı & Çizim") },
                    icon = { Icon(Icons.Filled.Create, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Ses Kaydı") },
                    icon = { Icon(Icons.Filled.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Title input & Tag input (Common for all tabs)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                "Başlık...",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 22.sp
                                )
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tags bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Etiketler: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BasicTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (tagsText.isEmpty()) {
                                Text(
                                    "proje, önemli, toplantı (virgülle ayırın)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_tags_input")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Text Note Editor with formatting
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            textStyle = TextStyle(
                                fontSize = fontSize.sp,
                                color = Color(textColor),
                                lineHeight = (fontSize * 1.45).sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (content.isEmpty()) {
                                    Text(
                                        "Notunuzu buraya yazmaya başlayın...\n\nAşağıdaki zengin araç çubuğunu kullanarak yazı boyutunu, renkleri, madde imlerini ve yapılacaklar listesini düzenleyebilirsiniz.",
                                        style = TextStyle(
                                            fontSize = fontSize.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            lineHeight = (fontSize * 1.45).sp
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("note_content_input")
                        )
                    }
                }
                1 -> {
                    // S-Pen & Drawing Canvas
                    DrawingCanvas(
                        initialStrokesJson = drawingData,
                        onStrokesChanged = { newJson ->
                            drawingData = newJson
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // Voice memo & Transcription Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AudioRecorderBar(
                            existingAudioPath = audioPath,
                            existingDurationMs = audioDurationMs,
                            onAudioRecorded = { path, duration ->
                                audioPath = path
                                audioDurationMs = duration
                            },
                            onTranscriptionReceived = { transcribedText ->
                                transcription = if (transcription.isBlank()) transcribedText
                                else "$transcription\n$transcribedText"
                                // Also append to note content so it's fully indexed and readable
                                content = if (content.isBlank()) "🎙️ Transkripsiyon:\n$transcribedText"
                                else "$content\n\n🎙️ Transkripsiyon:\n$transcribedText"
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Konuşmadan Metne Transkripsiyon (Otomatik İndekslenir)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = transcription,
                            onValueChange = { transcription = it },
                            placeholder = { Text("Transkribe edilen ses kayıtları burada listelenir...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("transcription_text_field")
                        )
                    }
                }
            }
        }
    }
}
