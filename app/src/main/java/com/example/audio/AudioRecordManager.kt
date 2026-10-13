package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

class AudioRecordManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    fun startRecording(): String? {
        try {
            stopRecording()
            stopPlaying()

            val audioDir = File(context.filesDir, "audio_notes").apply { mkdirs() }
            val file = File(audioDir, "note_audio_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            recordStartTime = SystemClock.elapsedRealtime()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            return null
        }
    }

    fun stopRecording(): Pair<String?, Long> {
        if (!isRecording) return Pair(null, 0L)
        val duration = SystemClock.elapsedRealtime() - recordStartTime
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return Pair(currentOutputFile?.absolutePath, duration)
    }

    fun startPlaying(filePath: String, onComplete: () -> Unit): Boolean {
        try {
            stopPlaying()
            val file = File(filePath)
            if (!file.exists()) return false

            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    this@AudioRecordManager.isPlaying = false
                    onComplete()
                }
            }
            mediaPlayer = player
            isPlaying = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
            return false
        }
    }

    fun stopPlaying() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            isPlaying = false
        }
    }

    fun release() {
        stopRecording()
        stopPlaying()
    }
}
