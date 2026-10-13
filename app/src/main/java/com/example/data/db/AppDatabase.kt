package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [NoteEntity::class, FolderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "samsung_notes_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default folders and welcome notes in background
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateInitialData(database.noteDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(dao: NoteDao) {
            val defaultFolders = listOf(
                FolderEntity(name = "Kişisel", color = 0xFF42A5F5, icon = "person"),
                FolderEntity(name = "İş & Projeler", color = 0xFFFF7043, icon = "work"),
                FolderEntity(name = "Fikirler", color = 0xFFAB47BC, icon = "lightbulb"),
                FolderEntity(name = "Ders Notları", color = 0xFF26A69A, icon = "school")
            )
            for (f in defaultFolders) {
                dao.insertFolder(f)
            }

            val welcomeNote = NoteEntity(
                title = "Samsung Notes'a Hoş Geldiniz! 📝",
                content = """
                    Bu uygulama Samsung Notes deneyimini Android üzerinde eksiksiz yaşamanız için geliştirildi:
                    
                    ✨ Öne Çıkan Özellikler:
                    • 📁 Klasörler & Akıllı Etiketler (#proje, #önemli)
                    • 🎨 S-Pen & El Yazısı Çizim Tuvali (Fırça kalınlığı, renkler, silgi)
                    • 🎙️ Ses Kaydı & Otomatik Transkripsiyon (Söyledikleriniz metne dönüşür)
                    • 🔒 Güvenlik & Şifreleme (Özel notlarınızı PIN ile kilitleyin)
                    • 📄 PDF & TXT Dışa Aktarma (Tek dokunuşla paylaşın veya kaydedin)
                    • ☁️ Bulut & Yerel Yedekleme Paneli (.json ve şifreli veri yedekleri)
                    • 🌓 Göz Dostu Karanlık & Aydınlık Tema Seçenekleri
                    
                    Hemen yeni bir not oluşturarak veya bu notu düzenleyerek deneyebilirsiniz!
                """.trimIndent(),
                folder = "Kişisel",
                categoryColor = 0xFFFFE082,
                tags = "hoşgeldin,özellikler,ipucu",
                isPinned = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            dao.insertNote(welcomeNote)
        }
    }
}
