package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfExporter
import com.example.ui.viewmodel.BackupFileItem
import com.example.ui.viewmodel.NotesUiState
import com.example.ui.viewmodel.NotesViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncScreen(
    uiState: NotesUiState,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dialog States
    var showRestoreFromTextInputDialog by remember { mutableStateOf(false) }
    var restoreRawInput by remember { mutableStateOf("") }
    var restorePasswordInput by remember { mutableStateOf("") }

    var showPasswordBackupDialog by remember { mutableStateOf(false) }
    var customBackupPassword by remember { mutableStateOf("") }

    var selectedBackupForRestore by remember { mutableStateOf<BackupFileItem?>(null) }
    var promptPasswordForFileRestore by remember { mutableStateOf("") }

    // Cross-Device Sync Dialogs
    var showExportSyncPacketDialog by remember { mutableStateOf(false) }
    var generatedSyncPacket by remember { mutableStateOf("") }
    var syncPassphraseInput by remember { mutableStateOf("123456") }

    var showImportSyncPacketDialog by remember { mutableStateOf(false) }
    var incomingSyncPacket by remember { mutableStateOf("") }
    var incomingPassphrase by remember { mutableStateOf("123456") }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr")) }

    LaunchedEffect(Unit) {
        viewModel.refreshBackupHistory(context)
    }

    BackHandler { onBack() }

    // Dialog 1: File Restore Prompt (asks for password if protected)
    if (selectedBackupForRestore != null) {
        AlertDialog(
            onDismissRequest = { selectedBackupForRestore = null },
            icon = { Icon(Icons.Filled.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Yedekten Geri Yükle") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${selectedBackupForRestore!!.fileName} yedeği geri yüklenecek.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mevcut notlarınız korunur, yedekteki notlar üzerine eklenir veya güncellenir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedBackupForRestore!!.isPasswordProtected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = promptPasswordForFileRestore,
                            onValueChange = { promptPasswordForFileRestore = it },
                            label = { Text("Yedek Parolası") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = selectedBackupForRestore!!.file
                        val pass = if (selectedBackupForRestore!!.isPasswordProtected) promptPasswordForFileRestore else null
                        viewModel.restoreFromFile(context, file, pass) { success, _ ->
                            if (success) {
                                selectedBackupForRestore = null
                                promptPasswordForFileRestore = ""
                            }
                        }
                    }
                ) {
                    Text("Geri Yüklemeyi Başlat")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedBackupForRestore = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog 2: Custom Password Backup Dialog
    if (showPasswordBackupDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordBackupDialog = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Özel Parolalı Şifreli Yedek") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Yedeğinizi kendi belirleyeceğiniz bir parola ile AES-256 olarak şifreleyin. Bu şifre olmadan başka hiç kimse yedeği açamaz:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customBackupPassword,
                        onValueChange = { customBackupPassword = it },
                        label = { Text("Yedek Parolası (En az 4 karakter)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_backup_password_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customBackupPassword.length >= 4) {
                            viewModel.createQuickEncryptedBackup(context, customBackupPassword) {
                                showPasswordBackupDialog = false
                                customBackupPassword = ""
                            }
                        } else {
                            viewModel.showMessage("Lütfen en az 4 karakterli bir parola girin")
                        }
                    }
                ) {
                    Text("Şifreli Yedek Oluştur")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPasswordBackupDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog 3: Text / String Restore Dialog
    if (showRestoreFromTextInputDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreFromTextInputDialog = false },
            title = { Text("Yedek Metnini Yapıştır") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = restoreRawInput,
                        onValueChange = { restoreRawInput = it },
                        placeholder = { Text("Şifreli yedek metnini veya JSON verisini yapıştırın...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("raw_restore_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restorePasswordInput,
                        onValueChange = { restorePasswordInput = it },
                        label = { Text("Yedek Parolası (Varsa)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreRawInput.isNotBlank()) {
                            viewModel.restoreFromRawString(
                                restoreRawInput,
                                restorePasswordInput.ifBlank { null }
                            ) { success, _ ->
                                if (success) {
                                    showRestoreFromTextInputDialog = false
                                    restoreRawInput = ""
                                    restorePasswordInput = ""
                                }
                            }
                        }
                    }
                ) {
                    Text("Geri Yükle")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreFromTextInputDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Dialog 4: Cross-Device Sync Packet Export Dialog
    if (showExportSyncPacketDialog) {
        AlertDialog(
            onDismissRequest = { showExportSyncPacketDialog = false },
            icon = { Icon(Icons.Filled.DeviceHub, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Cihaz Eşitleme Paketi") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Diğer cihazınızda 'Başka Cihazdan Eşitle' seçeneğini açıp bu şifreli paketi yapıştırın. Tüm notlarınız iki cihaz arasında eşitlenecektir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = generatedSyncPacket,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Eşitleme Paketi (AES-256 Şifreli)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SNote Sync", generatedSyncPacket)
                        clipboard.setPrimaryClip(clip)
                        viewModel.showMessage("Eşitleme paketi panoya kopyalandı 📋")
                        showExportSyncPacketDialog = false
                    }
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paketi Kopyala")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportSyncPacketDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Dialog 5: Cross-Device Sync Packet Import Dialog
    if (showImportSyncPacketDialog) {
        AlertDialog(
            onDismissRequest = { showImportSyncPacketDialog = false },
            icon = { Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Diğer Cihazdan Eşitle") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Diğer cihazınızdan aldığınız eşitleme paketini aşağıya yapıştırın:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = incomingSyncPacket,
                        onValueChange = { incomingSyncPacket = it },
                        placeholder = { Text("ENC:SNOTE:AES256:...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("incoming_sync_packet_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = incomingPassphrase,
                        onValueChange = { incomingPassphrase = it },
                        label = { Text("Eşitleme Parolası / Kodu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (incomingSyncPacket.isNotBlank()) {
                            viewModel.applyCrossDeviceSyncPacket(incomingSyncPacket, incomingPassphrase) { success, _ ->
                                if (success) {
                                    showImportSyncPacketDialog = false
                                    incomingSyncPacket = ""
                                }
                            }
                        }
                    }
                ) {
                    Text("Eşitle ve Birleştir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportSyncPacketDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yedekleme & Senkronizasyon", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Offline-First & Network Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.cloudSync.isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (uiState.cloudSync.isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.cloudSync.isOnline) Icons.Filled.CloudDone else Icons.Filled.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.cloudSync.isOnline) "Çevrimiçi Mod — Senkronizasyon Aktif"
                            else "Çevrimdışı Mod — Tam Yerel Bağımsızlık",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.cloudSync.isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                            else Color(0xFFE65100)
                        )
                        Text(
                            text = if (uiState.cloudSync.isOnline) "Tüm notlar yerel Room veritabanı ve bulutla çift yönlü senkronize edilebilir."
                            else "İnternet bağlantısı olmasa bile tüm notlar, çizimler ve ses kayıtları cihazınızda eksiksiz çalışır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 2: Hızlı Yerel Yedekleme Paneli (Fast Encrypted Local Backup)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hızlı Yerel Yedekleme Paneli",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "AES-256 Şifreli Yerel Saklama",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Verileriniz harici sunuculara bağımlı kalmadan cihazınızın güvenli alanında şifreli olarak yedeklenir. İstediğiniz an tek dokunuşla geri yükleyebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: One-tap Quick Backup & Password Protected Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.createQuickEncryptedBackup(context, null) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_one_tap_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⚡ Hızlı Yedek Al", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showPasswordBackupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("password_backup_button")
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Parolalı Yedek", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = { showRestoreFromTextInputDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Metin / Kod Yapıştırarak Geri Yükle", fontSize = 12.sp)
                    }

                    // Existing backups list
                    if (uiState.backupFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cihazdaki Mevcut Yedekler (${uiState.backupFiles.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.backupFiles.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (item.isPasswordProtected) Icons.Filled.Lock else Icons.Filled.Security,
                                                    contentDescription = null,
                                                    tint = if (item.isPasswordProtected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.fileName.take(28) + if (item.fileName.length > 28) "..." else "",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = "${dateFormat.format(Date(item.lastModified))} • ${item.sizeKb} KB",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Restore Button
                                            IconButton(
                                                onClick = { selectedBackupForRestore = item },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Restore,
                                                    contentDescription = "Geri Yükle",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Share Button
                                            IconButton(
                                                onClick = {
                                                    PdfExporter.shareFile(context, item.file, "application/octet-stream")
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Share,
                                                    contentDescription = "Paylaş",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = { viewModel.deleteBackupFile(context, item.file) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Sil",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Farklı Cihazlar Arasında Senkronizasyon (Cross-Device Cloud Sync)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8EAF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeviceHub,
                                contentDescription = null,
                                tint = Color(0xFF3F51B5)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Farklı Cihazlar Arası Senkronizasyon",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Çapraz Cihaz Eşitleme & Eşleştirme",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF3F51B5)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Telefonunuz, tabletiniz ve diğer cihazlarınız arasında notlarınızı uçtan uca şifreli olarak eşitleyin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Device Pairing Code Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bu Cihazın Eşitleme Kimliği",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.cloudSync.deviceSyncCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Cihaz Kodu", uiState.cloudSync.deviceSyncCode)
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.showMessage("Cihaz kimliği kopyalandı 📋")
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Kopyala")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cross-device packet actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.generateCrossDeviceSyncPacket(syncPassphraseInput) { packet ->
                                    generatedSyncPacket = packet
                                    showExportSyncPacketDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_sync_packet_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eşitleme Paketi Üret", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showImportSyncPacketDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_sync_packet_button")
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Diğer Cihazdan Al", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cloud Auto Sync & Now button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Otomatik Eşitleme",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "İnternet olduğunda arka planda eşitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.cloudSync.isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.triggerCloudSync() },
                        enabled = !uiState.cloudSync.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_cloud_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.cloudSync.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eşitleniyor...")
                        } else {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Şimdi Eşitle")
                        }
                    }
                }
            }

            // Section 4: Güvenlik & Şifreleme Katmanı Detayları
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Uçtan Uca Şifreleme & Güvenlik",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "AES-256 Bit & PBKDF2 Anahtar Koruması",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Tüm yedek dosyalarınız ve kilitli notlarınız AES-256 algoritmasıyla şifrelenir.\n• Parolanız sunuculara veya belleğe düz metin olarak iletilmez.\n• Çevrimdışı modda bile şifreli depolama tam olarak çalışmaya devam eder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
