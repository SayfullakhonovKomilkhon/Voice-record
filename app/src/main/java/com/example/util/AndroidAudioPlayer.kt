package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

class AndroidAudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private val tag = "AudioPlayer"

    fun playFile(file: File, onCompletion: () -> Unit) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()
                setOnCompletionListener {
                    onCompletion()
                    stop()
                }
            }
            Log.i(tag, "Playing: ${file.name}")
        } catch (e: Exception) {
            Log.e(tag, "Playback error", e)
        }
    }

    fun stop() {
        try {
            player?.stop()
        } catch (e: Exception) {
            // Already released or idle
        } finally {
            player?.release()
            player = null
        }
    }

    fun isPlaying(): Boolean {
        return try {
            player?.isPlaying ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun pause() {
        try {
            if (player?.isPlaying == true) {
                player?.pause()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pausing player", e)
        }
    }

    fun resume() {
        try {
            player?.start()
        } catch (e: Exception) {
            Log.e(tag, "Error resuming player", e)
        }
    }

    fun seekTo(ms: Int) {
        try {
            player?.seekTo(ms)
        } catch (e: Exception) {
            Log.e(tag, "Error seeking player", e)
        }
    }

    fun getCurrentPosition(): Int {
        return try {
            player?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getDuration(): Int {
        return try {
            player?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
