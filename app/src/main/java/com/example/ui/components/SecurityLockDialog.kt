package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity

sealed class SecurityDialogMode {
    data class Unlock(val note: NoteEntity) : SecurityDialogMode()
    data class Lock(val note: NoteEntity) : SecurityDialogMode()
    data class RemoveLock(val note: NoteEntity) : SecurityDialogMode()
}

@Composable
fun SecurityLockDialog(
    mode: SecurityDialogMode,
    onDismiss: () -> Unit,
    onUnlockConfirmed: (pin: String) -> Unit,
    onLockConfirmed: (pin: String) -> Unit,
    onRemoveLockConfirmed: (pin: String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isUnlock = mode is SecurityDialogMode.Unlock
    val isLock = mode is SecurityDialogMode.Lock
    val isRemove = mode is SecurityDialogMode.RemoveLock

    val title = when {
        isUnlock -> "Kilitli Notu Aç"
        isLock -> "Notu PIN ile Kilitle & Şifrele"
        else -> "Not Kilidini Kaldır"
    }

    val icon = if (isUnlock || isRemove) Icons.Filled.LockOpen else Icons.Filled.Lock

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        isUnlock -> "Bu not askeri düzeyde AES şifreleme ile korunmaktadır. Devam etmek için PIN kodunuzu girin:"
                        isLock -> "Not içeriği bu PIN ile AES-256 olarak şifrelenecek ve koruma altına alınacaktır:"
                        else -> "Not kilidini kalıcı olarak kaldırmak için mevcut PIN kodunu girin:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        pinText = it
                        errorMessage = null
                    },
                    label = { Text("4-6 Haneli PIN / Şifre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                            Icon(
                                imageVector = if (isPinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (isPinVisible) "Gizle" else "Göster"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length < 4) {
                        errorMessage = "Lütfen en az 4 haneli bir PIN girin"
                        return@Button
                    }
                    when (mode) {
                        is SecurityDialogMode.Unlock -> onUnlockConfirmed(pinText)
                        is SecurityDialogMode.Lock -> onLockConfirmed(pinText)
                        is SecurityDialogMode.RemoveLock -> onRemoveLockConfirmed(pinText)
                    }
                },
                modifier = Modifier.testTag("confirm_pin_button")
            ) {
                Text(if (isUnlock) "Kilidi Aç" else if (isLock) "Kilitle" else "Kaldır")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
