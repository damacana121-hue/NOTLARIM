package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke

enum class PenTool {
    PEN, HIGHLIGHTER, ERASER
}

@Composable
fun DrawingCanvas(
    initialStrokesJson: String,
    onStrokesChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember {
        mutableStateListOf<DrawingStroke>().apply {
            addAll(DrawingStroke.listFromJson(initialStrokesJson))
        }
    }
    val undoStack = remember { mutableStateListOf<DrawingStroke>() }

    var selectedTool by remember { mutableStateOf(PenTool.PEN) }
    var selectedColor by remember { mutableLongStateOf(0xFF1E1E24) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    var currentPoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    val palette = listOf(
        0xFF1E1E24, // Siyah
        0xFFE65100, // Samsung Amber
        0xFF1976D2, // Mavi
        0xFFD32F2F, // Kırmızı
        0xFF388E3C, // Yeşil
        0xFF7B1FA2, // Mor
        0xFFFBC02D  // Sarı
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Drawing Control Bar (Tools, Colors, Sliders, Undo/Redo)
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Row 1: Tools + Undo/Redo + Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedTool == PenTool.PEN,
                            onClick = { selectedTool = PenTool.PEN },
                            label = { Text("Kalem", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = selectedTool == PenTool.HIGHLIGHTER,
                            onClick = { selectedTool = PenTool.HIGHLIGHTER },
                            label = { Text("Vurgu", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Filled.Highlight, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = selectedTool == PenTool.ERASER,
                            onClick = { selectedTool = PenTool.ERASER },
                            label = { Text("Silgi", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Filled.AutoFixNormal, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    val last = strokes.removeAt(strokes.lastIndex)
                                    undoStack.add(last)
                                    onStrokesChanged(DrawingStroke.listToJson(strokes))
                                }
                            },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Geri Al", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val item = undoStack.removeAt(undoStack.lastIndex)
                                    strokes.add(item)
                                    onStrokesChanged(DrawingStroke.listToJson(strokes))
                                }
                            },
                            enabled = undoStack.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "İleri Al", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                strokes.clear()
                                undoStack.clear()
                                onStrokesChanged("")
                            }
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Tuvali Temizle", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Row 2: Color Palette & Stroke Width Slider
                if (selectedTool != PenTool.ERASER) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Palette dots
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            palette.forEach { colorHex ->
                                val isSelected = selectedColor == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = colorHex }
                                )
                            }
                        }

                        // Width Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(130.dp)
                        ) {
                            Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Slider(
                                value = strokeWidth,
                                onValueChange = { strokeWidth = it },
                                valueRange = 2f..24f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // The Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(Color(0xFFFEFDFB)) // Warm paper background
                .testTag("drawing_canvas")
                .pointerInput(selectedTool, selectedColor, strokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints = listOf(DrawingPoint(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints = currentPoints + DrawingPoint(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                val isEraser = selectedTool == PenTool.ERASER
                                val finalColor = if (selectedTool == PenTool.HIGHLIGHTER) {
                                    // 40% opacity for highlighter
                                    val base = Color(selectedColor).copy(alpha = 0.38f)
                                    (base.value.toLong())
                                } else {
                                    selectedColor
                                }
                                val width = if (selectedTool == PenTool.HIGHLIGHTER) strokeWidth * 2.5f
                                else if (isEraser) strokeWidth * 3f
                                else strokeWidth

                                val newStroke = DrawingStroke(
                                    points = currentPoints,
                                    color = if (isEraser) 0xFFFEFDFB else finalColor,
                                    strokeWidth = width,
                                    isEraser = isEraser
                                )
                                strokes.add(newStroke)
                                undoStack.clear()
                                onStrokesChanged(DrawingStroke.listToJson(strokes))
                                currentPoints = emptyList()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw completed strokes
                for (stroke in strokes) {
                    if (stroke.points.size < 2) continue
                    val path = Path()
                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.color),
                        style = Stroke(
                            width = stroke.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw currently active stroke
                if (currentPoints.size >= 2) {
                    val activePath = Path()
                    activePath.moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        activePath.lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                    val isEraser = selectedTool == PenTool.ERASER
                    val activeColor = if (isEraser) Color(0xFFFEFDFB)
                    else if (selectedTool == PenTool.HIGHLIGHTER) Color(selectedColor).copy(alpha = 0.38f)
                    else Color(selectedColor)

                    val activeWidth = if (selectedTool == PenTool.HIGHLIGHTER) strokeWidth * 2.5f
                    else if (isEraser) strokeWidth * 3f
                    else strokeWidth

                    drawPath(
                        path = activePath,
                        color = activeColor,
                        style = Stroke(
                            width = activeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
