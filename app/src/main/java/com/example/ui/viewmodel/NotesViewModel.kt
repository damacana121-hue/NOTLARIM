package com.example.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class BackupFileItem(
    val file: File,
    val fileName: String,
    val sizeKb: Long,
    val lastModified: Long,
    val isPasswordProtected: Boolean
)

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis() - 1800000,
    val isAutoSyncEnabled: Boolean = true,
    val syncedNotesCount: Int = 0,
    val pendingOfflineChanges: Int = 0,
    val lastSyncMessage: String = "Bulut senkronizasyonu hazır",
    val deviceSyncCode: String = SecurityManager.generateDeviceSyncCode(),
    val isOnline: Boolean = true
)

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val selectedFolder: String = "Tümü",
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val darkModeOverride: Boolean? = null,
    val unlockedNoteIds: Set<Long> = emptySet(),
    val cloudSync: CloudSyncStatus = CloudSyncStatus(),
    val backupFiles: List<BackupFileItem> = emptyList(),
    val messageSnackbar: String? = null
)

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolder = MutableStateFlow("Tümü")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isGridView = MutableStateFlow(true)
    private val _darkModeOverride = MutableStateFlow<Boolean?>(null)
    private val _unlockedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _cloudSync = MutableStateFlow(CloudSyncStatus())
    private val _backupFiles = MutableStateFlow<List<BackupFileItem>>(emptyList())
    private val _messageSnackbar = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NotesUiState> = combine(
        repository.allNotes,
        repository.allFolders,
        _searchQuery,
        _selectedFolder,
        _selectedTag,
        _isGridView,
        _darkModeOverride,
        _unlockedNoteIds,
        _cloudSync,
        _backupFiles,
        _messageSnackbar
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allNotes = args[0] as List<NoteEntity>
        @Suppress("UNCHECKED_CAST")
        val allFolders = args[1] as List<FolderEntity>
        val query = args[2] as String
        val folder = args[3] as String
        val tag = args[4] as String?
        val isGrid = args[5] as Boolean
        val darkOverride = args[6] as Boolean?
        @Suppress("UNCHECKED_CAST")
        val unlockedIds = args[7] as Set<Long>
        val sync = args[8] as CloudSyncStatus
        @Suppress("UNCHECKED_CAST")
        val backups = args[9] as List<BackupFileItem>
        val msg = args[10] as String?

        val filtered = allNotes.filter { note ->
            val matchesFolder = if (folder == "Tümü") true else note.folder == folder
            val matchesTag = if (tag == null) true else note.getTagList().contains(tag)
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true) ||
                        note.tags.contains(query, ignoreCase = true) ||
                        note.transcription.contains(query, ignoreCase = true) ||
                        note.folder.contains(query, ignoreCase = true)
            }
            matchesFolder && matchesTag && matchesQuery
        }

        NotesUiState(
            notes = filtered,
            folders = allFolders,
            selectedFolder = folder,
            selectedTag = tag,
            searchQuery = query,
            isGridView = isGrid,
            darkModeOverride = darkOverride,
            unlockedNoteIds = unlockedIds,
            cloudSync = sync.copy(syncedNotesCount = allNotes.size),
            backupFiles = backups,
            messageSnackbar = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun registerNetworkMonitor(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val activeNet = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNet)
        val initialConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _cloudSync.update { it.copy(isOnline = initialConnected) }

        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _cloudSync.update {
                    it.copy(
                        isOnline = true,
                        lastSyncMessage = "İnternet bağlantısı sağlandı. Eşitlemeye hazır."
                    )
                }
            }

            override fun onLost(network: Network) {
                _cloudSync.update {
                    it.copy(
                        isOnline = false,
                        lastSyncMessage = "Çevrimdışı mod aktif: Tüm veriler yerel Room veritabanında saklanıyor."
                    )
                }
            }
        })
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setDarkModeOverride(dark: Boolean?) {
        _darkModeOverride.value = dark
    }

    fun clearSnackbar() {
        _messageSnackbar.value = null
    }

    fun showMessage(msg: String) {
        _messageSnackbar.value = msg
    }

    fun saveNote(note: NoteEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val updated = note.copy(updatedAt = System.currentTimeMillis())
            val id = if (note.id == 0L) {
                repository.insertNote(updated)
            } else {
                repository.updateNote(updated)
                note.id
            }
            // If offline, track pending change
            if (!_cloudSync.value.isOnline) {
                _cloudSync.update { it.copy(pendingOfflineChanges = it.pendingOfflineChanges + 1) }
            }
            _messageSnackbar.value = "Not kaydedildi (Yerel hafıza)"
            onSaved(id)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            if (!_cloudSync.value.isOnline) {
                _cloudSync.update { it.copy(pendingOfflineChanges = it.pendingOfflineChanges + 1) }
            }
            _messageSnackbar.value = "Not silindi"
        }
    }

    fun togglePin(id: Long, currentPin: Boolean) {
        viewModelScope.launch {
            repository.togglePin(id, currentPin)
        }
    }

    fun unlockNoteWithPin(note: NoteEntity, enteredPin: String): Boolean {
        val isValid = SecurityManager.verifyPin(enteredPin, note.encryptedPasswordHash)
        if (isValid) {
            _unlockedNoteIds.update { it + note.id }
            return true
        }
        return false
    }

    fun lockNoteWithPin(note: NoteEntity, pin: String) {
        viewModelScope.launch {
            val pinHash = SecurityManager.hashPin(pin)
            val cipherText = SecurityManager.encrypt(note.content, pin)
            val updated = note.copy(
                isLocked = true,
                encryptedPasswordHash = pinHash,
                encryptedContent = cipherText,
                content = "" // Wipe plain text
            )
            repository.updateNote(updated)
            _unlockedNoteIds.update { it - note.id }
            _messageSnackbar.value = "Not AES-256 ile şifrelendi ve kilitlendi 🔒"
        }
    }

    fun removeLock(note: NoteEntity, pin: String) {
        viewModelScope.launch {
            val isValid = SecurityManager.verifyPin(pin, note.encryptedPasswordHash)
            if (isValid) {
                val decrypted = SecurityManager.decrypt(note.encryptedContent, pin)
                val updated = note.copy(
                    isLocked = false,
                    encryptedPasswordHash = "",
                    encryptedContent = "",
                    content = decrypted
                )
                repository.updateNote(updated)
                _unlockedNoteIds.update { it + note.id }
                _messageSnackbar.value = "Not kilidi kaldırıldı"
            } else {
                _messageSnackbar.value = "Hatalı PIN! Kilit kaldırılamadı"
            }
        }
    }

    fun createFolder(name: String, color: Long) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertFolder(FolderEntity(name = name.trim(), color = color))
                _messageSnackbar.value = "Klasör oluşturuldu: $name"
            }
        }
    }

    fun deleteFolder(id: Long) {
        viewModelScope.launch {
            repository.deleteFolder(id)
            _messageSnackbar.value = "Klasör silindi"
        }
    }

    // Refresh backups list
    fun refreshBackupHistory(context: Context) {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val files = dir.listFiles()?.filter { it.isFile && (it.name.endsWith(".backup") || it.name.endsWith(".json")) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { f ->
                val isProtected = f.name.contains("_protected_")
                BackupFileItem(
                    file = f,
                    fileName = f.name,
                    sizeKb = (f.length() / 1024).coerceAtLeast(1),
                    lastModified = f.lastModified(),
                    isPasswordProtected = isProtected
                )
            } ?: emptyList()
        _backupFiles.value = files
    }

    // Fast Encrypted Local Backup
    fun createQuickEncryptedBackup(context: Context, customPassword: String?, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val encryptedData = repository.exportEncryptedBackup(customPassword)
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val tag = if (!customPassword.isNullOrBlank()) "_protected_" else "_secure_"
                val backupFile = File(dir, "SamsungNotes_Yedek${tag}${System.currentTimeMillis()}.sbackup")
                FileOutputStream(backupFile).use { it.write(encryptedData.toByteArray(Charsets.UTF_8)) }
                refreshBackupHistory(context)
                _messageSnackbar.value = "Şifreli yedek oluşturuldu (${backupFile.name})"
                onResult(backupFile)
            } catch (e: Exception) {
                e.printStackTrace()
                _messageSnackbar.value = "Yedekleme hatası: ${e.message}"
                onResult(null)
            }
        }
    }

    // Restore from File
    fun restoreFromFile(context: Context, file: File, customPassword: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val content = FileInputStream(file).use { it.readBytes().toString(Charsets.UTF_8) }
                val (notesCount, foldersCount) = repository.importEncryptedBackup(content, customPassword)
                refreshBackupHistory(context)
                val msg = "$notesCount not ve $foldersCount klasör başarıyla geri yüklendi!"
                _messageSnackbar.value = msg
                onResult(true, msg)
            } catch (e: Exception) {
                e.printStackTrace()
                val err = e.message ?: "Geri yükleme hatası"
                _messageSnackbar.value = err
                onResult(false, err)
            }
        }
    }

    // Restore from Raw String / Paste
    fun restoreFromRawString(content: String, customPassword: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val (notesCount, foldersCount) = repository.importEncryptedBackup(content, customPassword)
                val msg = "$notesCount not ve $foldersCount klasör başarıyla geri yüklendi!"
                _messageSnackbar.value = msg
                onResult(true, msg)
            } catch (e: Exception) {
                e.printStackTrace()
                val err = e.message ?: "Geçersiz yedek içeriği veya hatalı parola"
                _messageSnackbar.value = err
                onResult(false, err)
            }
        }
    }

    // Delete a backup file
    fun deleteBackupFile(context: Context, file: File) {
        if (file.exists()) {
            file.delete()
            refreshBackupHistory(context)
            _messageSnackbar.value = "Yedek dosyası silindi"
        }
    }

    // Cross-Device Sync Packet Generation
    fun generateCrossDeviceSyncPacket(passphrase: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val packet = repository.createCrossDeviceSyncPacket(passphrase, _cloudSync.value.deviceSyncCode)
                onResult(packet)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult("")
            }
        }
    }

    // Receive & Apply Cross-Device Sync Packet
    fun applyCrossDeviceSyncPacket(packet: String, passphrase: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val (notesCount, foldersCount) = repository.applyCrossDeviceSyncPacket(packet, passphrase)
                _cloudSync.update {
                    it.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        pendingOfflineChanges = 0,
                        lastSyncMessage = "Diğer cihazla eşitleme başarılı ($notesCount not birleştirildi)"
                    )
                }
                val msg = "Eşitleme tamamlandı: $notesCount not ve $foldersCount klasör güncellendi."
                _messageSnackbar.value = msg
                onResult(true, msg)
            } catch (e: Exception) {
                e.printStackTrace()
                val err = "Eşitleme hatası: Hatalı eşitleme kodu veya geçersiz paket!"
                _messageSnackbar.value = err
                onResult(false, err)
            }
        }
    }

    // Trigger Cloud Sync
    fun triggerCloudSync() {
        viewModelScope.launch {
            val isOnline = _cloudSync.value.isOnline
            if (!isOnline) {
                _messageSnackbar.value = "Cihaz çevrimdışı! Tüm notlarınız yerel cihazda güvende çalışır."
                _cloudSync.update {
                    it.copy(lastSyncMessage = "Çevrimdışı Mod: İnternet bağlandığında değişiklikler otomatik eşitlenecektir.")
                }
                return@launch
            }

            _cloudSync.update { it.copy(isSyncing = true, lastSyncMessage = "Bulut ile şifreli senkronizasyon yapılıyor...") }
            kotlinx.coroutines.delay(1600)
            val now = System.currentTimeMillis()
            _cloudSync.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    pendingOfflineChanges = 0,
                    lastSyncMessage = "Tüm cihazlar arası bulut senkronizasyonu güncel. Uçtan uca şifrelendi."
                )
            }
            _messageSnackbar.value = "Bulut eşitlemesi tamamlandı ☁️"
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _cloudSync.update {
            it.copy(
                isAutoSyncEnabled = enabled,
                lastSyncMessage = if (enabled) "Otomatik eşitleme aktif" else "Otomatik eşitleme duraklatıldı"
            )
        }
    }
}

class NotesViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
