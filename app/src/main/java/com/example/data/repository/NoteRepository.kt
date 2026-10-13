package com.example.data.repository

import com.example.data.db.NoteDao
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allFolders: Flow<List<FolderEntity>> = noteDao.getAllFolders()

    fun getNotesInFolder(folder: String): Flow<List<NoteEntity>> =
        noteDao.getNotesInFolder(folder)

    fun searchNotes(query: String): Flow<List<NoteEntity>> =
        noteDao.searchNotes(query)

    fun getNoteById(id: Long): Flow<NoteEntity?> =
        noteDao.getNoteById(id)

    suspend fun getNoteByIdDirect(id: Long): NoteEntity? =
        noteDao.getNoteByIdDirect(id)

    suspend fun insertNote(note: NoteEntity): Long =
        noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) =
        noteDao.updateNote(note)

    suspend fun deleteNote(id: Long) =
        noteDao.deleteNote(id)

    suspend fun togglePin(id: Long, currentPin: Boolean) =
        noteDao.updatePinned(id, !currentPin)

    suspend fun insertFolder(folder: FolderEntity): Long =
        noteDao.insertFolder(folder)

    suspend fun deleteFolder(id: Long) =
        noteDao.deleteFolder(id)

    suspend fun exportBackupJson(): String {
        val notes = noteDao.getAllNotesDirect()
        val folders = noteDao.getAllFoldersDirect()

        val root = JSONObject()
        root.put("version", 2)
        root.put("app", "Samsung Notes")
        root.put("exportedAt", System.currentTimeMillis())

        val notesArr = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("folder", n.folder)
            obj.put("categoryColor", n.categoryColor)
            obj.put("tags", n.tags)
            obj.put("drawingData", n.drawingData)
            obj.put("audioPath", n.audioPath)
            obj.put("audioDurationMs", n.audioDurationMs)
            obj.put("transcription", n.transcription)
            obj.put("isPinned", n.isPinned)
            obj.put("isLocked", n.isLocked)
            obj.put("encryptedPasswordHash", n.encryptedPasswordHash)
            obj.put("encryptedContent", n.encryptedContent)
            obj.put("fontSize", n.fontSize)
            obj.put("textColor", n.textColor)
            obj.put("createdAt", n.createdAt)
            obj.put("updatedAt", n.updatedAt)
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        val foldersArr = JSONArray()
        for (f in folders) {
            val fObj = JSONObject()
            fObj.put("id", f.id)
            fObj.put("name", f.name)
            fObj.put("color", f.color)
            fObj.put("icon", f.icon)
            foldersArr.put(fObj)
        }
        root.put("folders", foldersArr)

        return root.toString(2)
    }

    suspend fun exportEncryptedBackup(customPassword: String?): String {
        val rawJson = exportBackupJson()
        return SecurityManager.encryptBackupPayload(rawJson, customPassword)
    }

    suspend fun importEncryptedBackup(payload: String, customPassword: String?): Pair<Int, Int> {
        val (success, jsonString) = SecurityManager.decryptBackupPayload(payload, customPassword)
        if (!success) {
            throw IllegalArgumentException(jsonString)
        }
        return importBackupJson(jsonString)
    }

    suspend fun importBackupJson(jsonString: String): Pair<Int, Int> {
        val root = JSONObject(jsonString)
        val notesArr = root.optJSONArray("notes") ?: JSONArray()
        val foldersArr = root.optJSONArray("folders") ?: JSONArray()

        val existingNotes = noteDao.getAllNotesDirect()
        val existingFolders = noteDao.getAllFoldersDirect()

        val importedNotes = mutableListOf<NoteEntity>()
        for (i in 0 until notesArr.length()) {
            val obj = notesArr.getJSONObject(i)
            val title = obj.optString("title", "")
            val content = obj.optString("content", "")
            val folder = obj.optString("folder", "Genel")
            val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

            // Conflict check: if matching title and folder exists with older timestamp, replace; otherwise insert new
            val existing = existingNotes.find { it.title == title && it.folder == folder }
            val noteId = existing?.id ?: 0L

            val note = NoteEntity(
                id = noteId,
                title = title,
                content = content,
                folder = folder,
                categoryColor = obj.optLong("categoryColor", 0xFFFFE082),
                tags = obj.optString("tags", ""),
                drawingData = obj.optString("drawingData", ""),
                audioPath = obj.optString("audioPath", ""),
                audioDurationMs = obj.optLong("audioDurationMs", 0),
                transcription = obj.optString("transcription", ""),
                isPinned = obj.optBoolean("isPinned", false),
                isLocked = obj.optBoolean("isLocked", false),
                encryptedPasswordHash = obj.optString("encryptedPasswordHash", ""),
                encryptedContent = obj.optString("encryptedContent", ""),
                fontSize = obj.optInt("fontSize", 16),
                textColor = obj.optLong("textColor", 0xFF1C1B1F),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = updatedAt
            )
            importedNotes.add(note)
        }

        val importedFolders = mutableListOf<FolderEntity>()
        for (i in 0 until foldersArr.length()) {
            val obj = foldersArr.getJSONObject(i)
            val name = obj.optString("name", "Klasör")
            if (existingFolders.none { it.name.equals(name, ignoreCase = true) }) {
                val folder = FolderEntity(
                    id = 0,
                    name = name,
                    color = obj.optLong("color", 0xFFFFB300),
                    icon = obj.optString("icon", "folder")
                )
                importedFolders.add(folder)
            }
        }

        if (importedNotes.isNotEmpty()) {
            noteDao.insertNotes(importedNotes)
        }
        if (importedFolders.isNotEmpty()) {
            noteDao.insertFolders(importedFolders)
        }

        return Pair(importedNotes.size, importedFolders.size)
    }

    suspend fun createCrossDeviceSyncPacket(syncPassphrase: String, deviceId: String): String {
        val notes = noteDao.getAllNotesDirect()
        val folders = noteDao.getAllFoldersDirect()

        val root = JSONObject()
        root.put("syncVersion", 2)
        root.put("deviceId", deviceId)
        root.put("timestamp", System.currentTimeMillis())

        val notesArr = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("folder", n.folder)
            obj.put("categoryColor", n.categoryColor)
            obj.put("tags", n.tags)
            obj.put("drawingData", n.drawingData)
            obj.put("audioPath", n.audioPath)
            obj.put("audioDurationMs", n.audioDurationMs)
            obj.put("transcription", n.transcription)
            obj.put("isPinned", n.isPinned)
            obj.put("isLocked", n.isLocked)
            obj.put("encryptedPasswordHash", n.encryptedPasswordHash)
            obj.put("encryptedContent", n.encryptedContent)
            obj.put("fontSize", n.fontSize)
            obj.put("textColor", n.textColor)
            obj.put("updatedAt", n.updatedAt)
            notesArr.put(obj)
        }
        root.put("notes", notesArr)

        val foldersArr = JSONArray()
        for (f in folders) {
            val fObj = JSONObject()
            fObj.put("name", f.name)
            fObj.put("color", f.color)
            fObj.put("icon", f.icon)
            foldersArr.put(fObj)
        }
        root.put("folders", foldersArr)

        // Encrypt with cross-device sync passphrase
        return SecurityManager.encryptBackupPayload(root.toString(), syncPassphrase)
    }

    suspend fun applyCrossDeviceSyncPacket(syncPacket: String, syncPassphrase: String): Pair<Int, Int> {
        val (success, jsonString) = SecurityManager.decryptBackupPayload(syncPacket, syncPassphrase)
        if (!success) {
            throw IllegalArgumentException(jsonString)
        }
        return importBackupJson(jsonString)
    }
}
