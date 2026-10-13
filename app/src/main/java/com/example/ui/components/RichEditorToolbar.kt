package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RichEditorToolbar(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    textColor: Long,
    onTextColorChange: (Long) -> Unit,
    onInsertText: (String) -> Unit,
    wordCount: Int,
    charCount: Int,
    modifier: Modifier = Modifier
) {
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

    val colorList = listOf(
        0xFF1C1B1F, // Normal
        0xFFD32F2F, // Kırmızı
        0xFF1976D2, // Mavi
        0xFF388E3C, // Yeşil
        0xFFE65100, // Samsung Amber
        0xFF7B1FA2  // Mor
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rich_editor_toolbar"),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Row 1: Scrolling format action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Font Size Picker
                Box {
                    IconButton(onClick = { showFontSizeMenu = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FormatSize, contentDescription = "Yazı Boyutu", modifier = Modifier.size(20.dp))
                            Text(
                                text = "${fontSize}sp",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showFontSizeMenu,
                        onDismissRequest = { showFontSizeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Küçük (14sp)") },
                            onClick = {
                                onFontSizeChange(14)
                                showFontSizeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Normal (16sp)") },
                            onClick = {
                                onFontSizeChange(16)
                                showFontSizeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Alt Başlık (20sp)") },
                            onClick = {
                                onFontSizeChange(20)
                                showFontSizeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Büyük Başlık (24sp)") },
                            onClick = {
                                onFontSizeChange(24)
                                showFontSizeMenu = false
                            }
                        )
                    }
                }

                // Text Color Picker
                Box {
                    IconButton(onClick = { showColorMenu = true }) {
                        Icon(
                            Icons.Filled.FormatColorText,
                            contentDescription = "Metin Rengi",
                            tint = Color(textColor),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showColorMenu,
                        onDismissRequest = { showColorMenu = false }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            colorList.forEach { colorVal ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (textColor == colorVal) 2.dp else 1.dp,
                                            color = if (textColor == colorVal) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onTextColorChange(colorVal)
                                            showColorMenu = false
                                        }
                                )
                            }
                        }
                    }
                }

                // Bold
                IconButton(onClick = { onInsertText("**metin**") }) {
                    Icon(Icons.Filled.FormatBold, contentDescription = "Kalın", modifier = Modifier.size(20.dp))
                }

                // Italic
                IconButton(onClick = { onInsertText("*metin*") }) {
                    Icon(Icons.Filled.FormatItalic, contentDescription = "İtalik", modifier = Modifier.size(20.dp))
                }

                // Checkbox task list
                IconButton(onClick = { onInsertText("\n[ ] Görev ") }) {
                    Icon(Icons.Filled.CheckBox, contentDescription = "Yapılacak", modifier = Modifier.size(20.dp))
                }

                // Bullet List
                IconButton(onClick = { onInsertText("\n• ") }) {
                    Icon(Icons.Filled.FormatListBulleted, contentDescription = "Madde İşareti", modifier = Modifier.size(20.dp))
                }

                // Numbered List
                IconButton(onClick = { onInsertText("\n1. ") }) {
                    Icon(Icons.Filled.FormatListNumbered, contentDescription = "Numaralı Liste", modifier = Modifier.size(20.dp))
                }

                // Insert Tag
                IconButton(onClick = { onInsertText(" #etiket") }) {
                    Icon(Icons.Filled.Label, contentDescription = "Etiket Ekle", modifier = Modifier.size(20.dp))
                }

                // Insert Timestamp
                IconButton(onClick = {
                    val timeStr = SimpleDateFormat("HH:mm, dd MMM", Locale("tr")).format(Date())
                    onInsertText(" [$timeStr] ")
                }) {
                    Icon(Icons.Filled.Schedule, contentDescription = "Zaman Damgası", modifier = Modifier.size(20.dp))
                }
            }

            // Row 2: Status indicator (word count & char count)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount kelime  |  $charCount karakter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Markdown & Zengin Metin",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}
