package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folder: String = "Genel",
    val categoryColor: Long = 0xFFFFE082, // Amber 200
    val tags: String = "", // comma-separated e.g. "toplanti,proje"
    val drawingData: String = "", // serialized JSON strokes
    val audioPath: String = "", // local file path to audio
    val audioDurationMs: Long = 0,
    val transcription: String = "", // voice-to-text transcribed content
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val encryptedPasswordHash: String = "", // hashed PIN or pass
    val encryptedContent: String = "", // AES cipher string
    val fontSize: Int = 16,
    val textColor: Long = 0xFF1C1B1F,
    val isHandwritingMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isTrash: Boolean = false
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long = 0xFFFFB300,
    val icon: String = "folder"
)

data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Long,
    val strokeWidth: Float,
    val isEraser: Boolean = false
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("color", color)
        obj.put("strokeWidth", strokeWidth.toDouble())
        obj.put("isEraser", isEraser)
        val ptsArr = JSONArray()
        for (pt in points) {
            val ptObj = JSONObject()
            ptObj.put("x", pt.x.toDouble())
            ptObj.put("y", pt.y.toDouble())
            ptsArr.put(ptObj)
        }
        obj.put("points", ptsArr)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): DrawingStroke {
            val color = obj.optLong("color", 0xFF000000)
            val strokeWidth = obj.optDouble("strokeWidth", 6.0).toFloat()
            val isEraser = obj.optBoolean("isEraser", false)
            val ptsArr = obj.optJSONArray("points") ?: JSONArray()
            val points = mutableListOf<DrawingPoint>()
            for (i in 0 until ptsArr.length()) {
                val ptObj = ptsArr.getJSONObject(i)
                points.add(DrawingPoint(ptObj.optDouble("x").toFloat(), ptObj.optDouble("y").toFloat()))
            }
            return DrawingStroke(points, color, strokeWidth, isEraser)
        }

        fun listToJson(strokes: List<DrawingStroke>): String {
            val arr = JSONArray()
            for (s in strokes) {
                arr.put(s.toJson())
            }
            return arr.toString()
        }

        fun listFromJson(jsonStr: String): List<DrawingStroke> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<DrawingStroke>()
                for (i in 0 until arr.length()) {
                    list.add(fromJson(arr.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
