package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.ui.screens.BackupSyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.theme.SamsungNotesTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.NotesViewModelFactory

sealed class CurrentScreen {
    object Home : CurrentScreen()
    data class Editor(val note: NoteEntity) : CurrentScreen()
    object BackupSync : CurrentScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = NoteRepository(database.noteDao())
        val factory = NotesViewModelFactory(repository)

        setContent {
            val viewModel: NotesViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = uiState.darkModeOverride ?: isSystemDark

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                viewModel.registerNetworkMonitor(applicationContext)
            }

            // Snackbar observer
            LaunchedEffect(uiState.messageSnackbar) {
                uiState.messageSnackbar?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearSnackbar()
                }
            }

            var currentScreen by remember { mutableStateOf<CurrentScreen>(CurrentScreen.Home) }

            SamsungNotesTheme(darkTheme = useDarkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    when (val screen = currentScreen) {
                        is CurrentScreen.Home -> {
                            HomeScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onOpenNote = { note ->
                                    currentScreen = CurrentScreen.Editor(note)
                                },
                                onCreateNewNote = { isDrawing, isAudio ->
                                    val newNote = NoteEntity(
                                        title = "",
                                        content = "",
                                        folder = if (uiState.selectedFolder == "Tümü") "Genel" else uiState.selectedFolder,
                                        isHandwritingMode = isDrawing,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    currentScreen = CurrentScreen.Editor(newNote)
                                },
                                onOpenBackupSync = {
                                    currentScreen = CurrentScreen.BackupSync
                                }
                            )
                        }
                        is CurrentScreen.Editor -> {
                            NoteEditorScreen(
                                initialNote = screen.note,
                                folders = uiState.folders,
                                onSaveAndClose = { savedNote ->
                                    if (savedNote.title.isNotBlank() || savedNote.content.isNotBlank() ||
                                        savedNote.drawingData.isNotBlank() || savedNote.audioPath.isNotBlank()
                                    ) {
                                        viewModel.saveNote(savedNote)
                                    }
                                    currentScreen = CurrentScreen.Home
                                },
                                onDeleteNote = { noteId ->
                                    viewModel.deleteNote(noteId)
                                    currentScreen = CurrentScreen.Home
                                }
                            )
                        }
                        is CurrentScreen.BackupSync -> {
                            BackupSyncScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onBack = { currentScreen = CurrentScreen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}
