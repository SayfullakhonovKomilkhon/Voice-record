package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AndroidAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private val tag = "AudioRecorder"

    fun start(outputFile: File): Boolean {
        return try {
            createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
                recorder = this
            }
            Log.i(tag, "Started recording to: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start recording", e)
            recorder = null
            false
        }
    }

    fun stop() {
        try {
            recorder?.stop()
            Log.i(tag, "Successfully stopped audio recording")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop recorder (might have been stopped already/too short)", e)
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}
