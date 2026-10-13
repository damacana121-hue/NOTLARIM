package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.DrawingStroke
import com.example.data.model.NoteEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun exportNoteToPdf(context: Context, note: NoteEntity): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header Background Banner
        paint.color = Color.parseColor("#E65100")
        canvas.drawRect(0f, 0f, 595f, 48f, paint)

        // App Brand Text
        paint.color = Color.WHITE
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Samsung Notes", 36f, 30f, paint)

        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr"))
        val dateStr = dateFormat.format(Date(note.updatedAt))
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val dateWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, 595f - 36f - dateWidth, 30f, paint)

        var currentY = 80f

        // Note Title
        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        val title = if (note.title.isBlank()) "Başlıksız Not" else note.title
        canvas.drawText(title, 36f, currentY, paint)
        currentY += 28f

        // Folder & Tags metadata
        paint.color = Color.parseColor("#616161")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        val meta = StringBuilder("Klasör: ${note.folder}")
        if (note.tags.isNotBlank()) {
            meta.append("  |  Etiketler: #${note.tags.replace(",", " #")}")
        }
        canvas.drawText(meta.toString(), 36f, currentY, paint)
        currentY += 16f

        // Divider Line
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, currentY, 595f - 36f, currentY, paint)
        currentY += 24f

        // Note Content (Word wrapped)
        paint.color = Color.parseColor("#212121")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val content = if (note.isLocked && note.content.isBlank()) "[Bu not şifrelenmiştir]" else note.content

        val lines = wrapText(content, 595f - 72f, paint)
        for (line in lines) {
            if (currentY > 750f) break // Stay on 1st page or multi-page support
            canvas.drawText(line, 36f, currentY, paint)
            currentY += 18f
        }

        // Transcription section if exists
        if (note.transcription.isNotBlank()) {
            currentY += 12f
            paint.color = Color.parseColor("#E65100")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("🎙️ Sesli Transkripsiyon:", 36f, currentY, paint)
            currentY += 16f

            paint.color = Color.parseColor("#424242")
            paint.textSize = 11f
            paint.isFakeBoldText = false
            val transLines = wrapText(note.transcription, 595f - 72f, paint)
            for (tLine in transLines) {
                if (currentY > 780f) break
                canvas.drawText(tLine, 36f, currentY, paint)
                currentY += 16f
            }
        }

        // Render Drawing Canvas strokes if available
        if (note.drawingData.isNotBlank()) {
            currentY += 16f
            paint.color = Color.parseColor("#E65100")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("✍️ El Yazısı & Çizim Notu:", 36f, currentY, paint)
            currentY += 20f

            val strokes = DrawingStroke.listFromJson(note.drawingData)
            val strokePaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            for (stroke in strokes) {
                if (stroke.isEraser) continue
                strokePaint.color = stroke.color.toInt()
                strokePaint.strokeWidth = stroke.strokeWidth.coerceIn(1.5f, 8f)
                val path = Path()
                for ((idx, pt) in stroke.points.withIndex()) {
                    // scale/offset point onto PDF canvas
                    val px = 36f + (pt.x * 0.45f).coerceIn(0f, 520f)
                    val py = currentY + (pt.y * 0.35f).coerceIn(0f, 160f)
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, strokePaint)
            }
        }

        pdfDocument.finishPage(page)

        return try {
            val exportDir = File(context.cacheDir, "exported_pdf").apply { mkdirs() }
            val cleanTitle = note.title.ifBlank { "Not_${note.id}" }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(exportDir, "${cleanTitle}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (para in paragraphs) {
            if (para.isBlank()) {
                result.add("")
                continue
            }
            val words = para.split(" ")
            var currentLine = StringBuilder()
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Notu Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareText(context: Context, title: String, content: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content\n\n— Samsung Notes ile paylaşıldı")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Notu Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
