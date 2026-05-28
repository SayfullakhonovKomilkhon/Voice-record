package com.example.util

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MeetingRecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var handler = Handler(Looper.getMainLooper())

    private var wasRecordingBeforeCall = false
    private var isReceiverRegistered = false

    companion object {
        private const val TAG = "MeetingRecorderService"
        private const val CHANNEL_ID = "meeting_recorder_channel"
        private const val NOTIFICATION_ID = 1011

        // Exposing service's state reactively
        private val _isRecordingAndActive = MutableStateFlow(false)
        val isRecordingAndActive = _isRecordingAndActive.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused = _isPaused.asStateFlow()

        private val _recordingDurationSeconds = MutableStateFlow(0)
        val recordingDurationSeconds = _recordingDurationSeconds.asStateFlow()

        private val _recordingAmplitude = MutableStateFlow(0)
        val recordingAmplitude = _recordingAmplitude.asStateFlow()

        private val _meetingTitle = MutableStateFlow("")
        val meetingTitle = _meetingTitle.asStateFlow()

        private val _participantA = MutableStateFlow("")
        val participantA = _participantA.asStateFlow()

        private val _participantB = MutableStateFlow("")
        val participantB = _participantB.asStateFlow()

        private val _outputFilePath = MutableStateFlow<String?>(null)
        val outputFilePath = _outputFilePath.asStateFlow()

        private val _isPausedByPhoneCall = MutableStateFlow(false)

        // Utility intent starters to communicate with the service
        fun startService(context: Context, title: String, partA: String, partB: String, outputFile: File) {
            val intent = Intent(context, MeetingRecorderService::class.java).apply {
                action = "ACTION_START"
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_PART_A", partA)
                putExtra("EXTRA_PART_B", partB)
                putExtra("EXTRA_FILE_PATH", outputFile.absolutePath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MeetingRecorderService::class.java).apply {
                action = "ACTION_STOP"
            }
            context.startService(intent)
        }

        fun pauseRecording(context: Context) {
            val intent = Intent(context, MeetingRecorderService::class.java).apply {
                action = "ACTION_PAUSE"
            }
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, MeetingRecorderService::class.java).apply {
                action = "ACTION_RESUME"
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerPhoneStateReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "ACTION_START" -> {
                val title = intent.getStringExtra("EXTRA_TITLE") ?: "Новая встреча"
                val pA = intent.getStringExtra("EXTRA_PART_A") ?: "Участник A"
                val pB = intent.getStringExtra("EXTRA_PART_B") ?: "Участник B"
                val filePath = intent.getStringExtra("EXTRA_FILE_PATH") ?: ""
                
                startRecordingSession(title, pA, pB, filePath)
            }
            "ACTION_STOP" -> {
                stopRecordingSession()
            }
            "ACTION_PAUSE" -> {
                pauseRecordingSession(byPhoneCall = false)
            }
            "ACTION_RESUME" -> {
                resumeRecordingSession(byPhoneCall = false)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecordingSession(title: String, pA: String, pB: String, filePath: String) {
        if (_isRecordingAndActive.value) {
            Log.w(TAG, "Recording already in progress, ignoring start command")
            return
        }

        // Show Foreground service notification IMMEDIATELY here, before anything else!
        val initialNotification = buildRecordingNotification(title, "Запуск записи...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, initialNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground recording service", e)
        }

        if (filePath.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        _meetingTitle.value = title
        _participantA.value = pA
        _participantB.value = pB
        _outputFilePath.value = filePath
        _isRecordingAndActive.value = true
        _isPaused.value = false
        _recordingDurationSeconds.value = 0
        _recordingAmplitude.value = 0
        _isPausedByPhoneCall.value = false

        // Init recorder
        try {
            val file = File(filePath)
            val recorderInstance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorderInstance.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorderInstance
            Log.i(TAG, "Started recorder with output path: $filePath")

            // Show updated recording status
            updateNotification(title, "Идет запись... 00:00")

            startTimerAndAmplitude()
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting media recorder step", e)
            _isRecordingAndActive.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopRecordingSession() {
        Log.i(TAG, "Stopping recording session")
        stopTimerAndAmplitude()

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }

        _isRecordingAndActive.value = false
        _isPaused.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseRecordingSession(byPhoneCall: Boolean) {
        if (!_isRecordingAndActive.value || _isPaused.value) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
                if (byPhoneCall) {
                    _isPausedByPhoneCall.value = true
                }
                updateNotification(_meetingTitle.value, "Запись приостановлена" + if (byPhoneCall) " (входящий звонок)" else "")
                Log.i(TAG, "Recording paused. PhoneCallPause = $byPhoneCall")
            } catch (e: Exception) {
                Log.e(TAG, "Failed pausing recorder", e)
            }
        } else {
            // Devices older than API 24 do not support pause, stop the recorder instead and notify
            Log.w(TAG, "MediaRecorder.pause() is not supported on API < 24")
        }
    }

    private fun resumeRecordingSession(byPhoneCall: Boolean) {
        if (!_isRecordingAndActive.value || !_isPaused.value) return
        
        // If it was paused by a phone call and user manually tries to resume, let's allow it but clear flag
        // If resume is called after a phone call, ONLY resume if it was auto-paused by the phone call!
        if (byPhoneCall && !_isPausedByPhoneCall.value) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
                _isPausedByPhoneCall.value = false
                updateNotification(_meetingTitle.value, "Идет запись...")
                Log.i(TAG, "Recording resumed.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed resuming recorder", e)
            }
        }
    }

    private fun startTimerAndAmplitude() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (_isRecordingAndActive.value && !_isPaused.value) {
                    _recordingDurationSeconds.value += 1
                    val duration = _recordingDurationSeconds.value
                    val min = duration / 60
                    val sec = duration % 60
                    val textStr = String.format("Идет запись... %02d:%02d", min, sec)
                    updateNotification(_meetingTitle.value, textStr)
                }
            }
        }

        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive) {
                delay(100)
                if (_isRecordingAndActive.value && !_isPaused.value) {
                    try {
                        _recordingAmplitude.value = mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        _recordingAmplitude.value = 0
                    }
                } else {
                    _recordingAmplitude.value = 0
                }
            }
        }
    }

    private fun stopTimerAndAmplitude() {
        timerJob?.cancel()
        timerJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    private fun buildRecordingNotification(title: String, text: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 203, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Протокол: $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.presence_video_online) // Simple bright recorder status icon
            .setColor(0xFF2196F3.toInt())
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildRecordingNotification(title, text)
        val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Запись встреч",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отображает статус активной записи протокола встречи"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Telephony / Call State handling
    private fun registerPhoneStateReceiver() {
        if (isReceiverRegistered) return
        try {
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(phoneStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(phoneStateReceiver, filter)
            }
            isReceiverRegistered = true
            Log.i(TAG, "Successfully registered phone status change receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering phone receiver", e)
        }
    }

    private fun unregisterPhoneStateReceiver() {
        if (!isReceiverRegistered) return
        try {
            unregisterReceiver(phoneStateReceiver)
            isReceiverRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering phone receiver", e)
        }
    }

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                Log.i(TAG, "Telephony broadcast state received: $state")
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING,
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        // Pause recording dynamically and note that it was paused by telephony call and was active
                        if (_isRecordingAndActive.value && !_isPaused.value) {
                            wasRecordingBeforeCall = true
                            pauseRecordingSession(byPhoneCall = true)
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        // If it was running prior to the call interruption, resume recording now that call concluded
                        if (wasRecordingBeforeCall && _isRecordingAndActive.value && _isPaused.value) {
                            wasRecordingBeforeCall = false
                            resumeRecordingSession(byPhoneCall = true)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerAndAmplitude()
        unregisterPhoneStateReceiver()
        serviceJob.cancel()
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error in final service mediaRecorder release", e)
        }
    }
}
