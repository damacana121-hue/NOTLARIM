package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.pdf.PdfExporter
import com.example.ui.components.NoteCard
import com.example.ui.components.SecurityDialogMode
import com.example.ui.components.SecurityLockDialog
import com.example.ui.viewmodel.NotesUiState
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: NotesUiState,
    viewModel: NotesViewModel,
    onOpenNote: (NoteEntity) -> Unit,
    onCreateNewNote: (isDrawing: Boolean, isAudio: Boolean) -> Unit,
    onOpenBackupSync: () -> Unit
) {
    val context = LocalContext.current
    var isSearchExpanded by remember { mutableStateOf(false) }
    var securityDialogMode by remember { mutableStateOf<SecurityDialogMode?>(null) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // Security Dialog
    if (securityDialogMode != null) {
        SecurityLockDialog(
            mode = securityDialogMode!!,
            onDismiss = { securityDialogMode = null },
            onUnlockConfirmed = { pin ->
                val mode = securityDialogMode
                if (mode is SecurityDialogMode.Unlock) {
                    val success = viewModel.unlockNoteWithPin(mode.note, pin)
                    if (success) {
                        securityDialogMode = null
                        onOpenNote(mode.note)
                    } else {
                        viewModel.showMessage("Hatalı PIN kodu!")
                    }
                }
            },
            onLockConfirmed = { pin ->
                val mode = securityDialogMode
                if (mode is SecurityDialogMode.Lock) {
                    viewModel.lockNoteWithPin(mode.note, pin)
                    securityDialogMode = null
                }
            },
            onRemoveLockConfirmed = { pin ->
                val mode = securityDialogMode
                if (mode is SecurityDialogMode.RemoveLock) {
                    viewModel.removeLock(mode.note, pin)
                    securityDialogMode = null
                }
            }
        )
    }

    // Add Folder Dialog
    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            icon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Yeni Klasör Ekle") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Klasör Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_folder_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName, 0xFFFFB300)
                            newFolderName = ""
                            showAddFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_folder_button")
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddFolderDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Samsung Notes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${uiState.notes.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.cloudSync.isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                modifier = Modifier.clickable { onOpenBackupSync() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (uiState.cloudSync.isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.cloudSync.isOnline) "Bulut" else "Yerel Mod",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (uiState.cloudSync.isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Search Toggle
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(Icons.Filled.Search, contentDescription = "Arama")
                        }

                        // Grid / List View Toggle
                        IconButton(onClick = { viewModel.toggleGridView() }) {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                contentDescription = "Görünüm Değiştir"
                            )
                        }

                        // Dark Mode Toggle
                        IconButton(onClick = {
                            val nextMode = when (uiState.darkModeOverride) {
                                null -> true
                                true -> false
                                false -> null
                            }
                            viewModel.setDarkModeOverride(nextMode)
                        }) {
                            Icon(
                                imageVector = if (uiState.darkModeOverride == true) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Tema Değiştir"
                            )
                        }

                        // Cloud & Backup Hub
                        IconButton(onClick = onOpenBackupSync) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = "Bulut ve Yedekleme",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                // Search Bar Expanded
                AnimatedVisibility(visible = isSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Notlarda, ses kayıtlarında veya etiketlerde ara...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Temizle")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field")
                        )
                    }
                }

                // Folder Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Tümü" folder chip
                    FilterChip(
                        selected = uiState.selectedFolder == "Tümü",
                        onClick = { viewModel.selectFolder("Tümü") },
                        label = { Text("Tüm Notlar") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    // Database folders
                    uiState.folders.forEach { folder ->
                        FilterChip(
                            selected = uiState.selectedFolder == folder.name,
                            onClick = { viewModel.selectFolder(folder.name) },
                            label = { Text(folder.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(folder.color))
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    // Add new folder chip
                    FilterChip(
                        selected = false,
                        onClick = { showAddFolderDialog = true },
                        label = { Text("+ Klasör") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Smart Tag Filter Row (derived from existing notes)
                val allTags = remember(uiState.notes) {
                    uiState.notes.flatMap { it.getTagList() }.distinct()
                }
                if (allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Etiketler:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.selectedTag != null) {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.selectTag(null) },
                                label = { Text("Filtreyi Kaldır ✕") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                        allTags.forEach { tag ->
                            FilterChip(
                                selected = uiState.selectedTag == tag,
                                onClick = {
                                    if (uiState.selectedTag == tag) viewModel.selectTag(null)
                                    else viewModel.selectTag(tag)
                                },
                                label = { Text("#$tag", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Samsung Notes Speed Dial Action Bar
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // S-Pen Drawing FAB
                SmallFloatingActionButton(
                    onClick = { onCreateNewNote(true, false) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("fab_draw_note")
                ) {
                    Icon(Icons.Filled.Create, contentDescription = "El Yazısı / Çizim Notu")
                }

                // Voice Note FAB
                SmallFloatingActionButton(
                    onClick = { onCreateNewNote(false, true) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.testTag("fab_audio_note")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Sesli Not")
                }

                // Primary New Note FAB
                ExtendedFloatingActionButton(
                    onClick = { onCreateNewNote(false, false) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                    text = { Text("Yeni Not") },
                    modifier = Modifier.testTag("fab_new_note")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.notes.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "Aramanızla eşleşen not bulunamadı" else "Henüz hiç not eklenmedi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Samsung Notes ile fikirlerinizi, çizimlerinizi veya sesli notlarınızı hemen kaydedin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onCreateNewNote(false, false) },
                        modifier = Modifier.testTag("empty_create_note_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("İlk Notunu Oluştur")
                    }
                }
            } else {
                if (uiState.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            val isUnlocked = uiState.unlockedNoteIds.contains(note.id)
                            NoteCard(
                                note = note,
                                isUnlocked = isUnlocked,
                                onClick = {
                                    if (note.isLocked && !isUnlocked) {
                                        securityDialogMode = SecurityDialogMode.Unlock(note)
                                    } else {
                                        onOpenNote(note)
                                    }
                                },
                                onTogglePin = { viewModel.togglePin(note.id, note.isPinned) },
                                onDelete = { viewModel.deleteNote(note.id) },
                                onExportPdf = {
                                    val file = PdfExporter.exportNoteToPdf(context, note)
                                    if (file != null) {
                                        PdfExporter.shareFile(context, file)
                                    } else {
                                        viewModel.showMessage("PDF oluşturulamadı")
                                    }
                                },
                                onLockClicked = {
                                    if (note.isLocked) {
                                        securityDialogMode = SecurityDialogMode.RemoveLock(note)
                                    } else {
                                        securityDialogMode = SecurityDialogMode.Lock(note)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            val isUnlocked = uiState.unlockedNoteIds.contains(note.id)
                            NoteCard(
                                note = note,
                                isUnlocked = isUnlocked,
                                onClick = {
                                    if (note.isLocked && !isUnlocked) {
                                        securityDialogMode = SecurityDialogMode.Unlock(note)
                                    } else {
                                        onOpenNote(note)
                                    }
                                },
                                onTogglePin = { viewModel.togglePin(note.id, note.isPinned) },
                                onDelete = { viewModel.deleteNote(note.id) },
                                onExportPdf = {
                                    val file = PdfExporter.exportNoteToPdf(context, note)
                                    if (file != null) {
                                        PdfExporter.shareFile(context, file)
                                    } else {
                                        viewModel.showMessage("PDF oluşturulamadı")
                                    }
                                },
                                onLockClicked = {
                                    if (note.isLocked) {
                                        securityDialogMode = SecurityDialogMode.RemoveLock(note)
                                    } else {
                                        securityDialogMode = SecurityDialogMode.Lock(note)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
