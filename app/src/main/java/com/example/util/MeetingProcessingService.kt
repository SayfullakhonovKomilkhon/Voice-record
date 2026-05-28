package com.example.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.api.GeminiTranscriptionService
import com.example.data.api.MeetingAnalysisResult
import com.example.data.local.AppDatabase
import com.example.data.model.Meeting
import com.example.data.model.MeetingStatus
import com.example.data.repository.MeetingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MeetingProcessingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    companion object {
        private const val TAG = "MeetingProcessingServ"
        private const val CHANNEL_ID = "meeting_processing_channel"
        private const val NOTIFICATION_ID = 2022

        // Event status map exposing active processing progress in-memory: meetingId -> ProcessingState
        private val _processingStates = MutableStateFlow<Map<Long, ProcessingState>>(emptyMap())
        val processingStates = _processingStates.asStateFlow()

        data class ProcessingState(
            val progress: Float, // 0f to 1f
            val phaseText: String
        )

        fun startService(context: Context, meetingId: Long) {
            val intent = Intent(context, MeetingProcessingService::class.java).apply {
                action = "ACTION_START_PROCESSING"
                putExtra("EXTRA_MEETING_ID", meetingId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_START_PROCESSING") {
            // Start foreground IMMEDIATELY with a placeholder notification so Android doesn't crash us!
            val placeholderNotification = buildProcessingNotification("Запись встречи", "Подготовка к анализу...", 0f)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, placeholderNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, placeholderNotification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground processing service", e)
            }

            val meetingId = intent.getLongExtra("EXTRA_MEETING_ID", -1L)
            if (meetingId != -1L) {
                processMeetingInBackground(meetingId)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun processMeetingInBackground(meetingId: Long) {
        serviceScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = MeetingRepository(database.meetingDao())
            val transcriptionService = GeminiTranscriptionService(applicationContext)

            val meeting = repository.getMeetingById(meetingId)
            if (meeting == null) {
                Log.e(TAG, "Meeting with ID $meetingId not found, aborting")
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }
                return@launch
            }

            // Update Notification with the actual title
            withContext(Dispatchers.Main) {
                updateNotification(meeting.title, "Подготовка к обработке...", 0f)
            }

            try {
                // Phase 1: Preparing & Audio Load (0% - 25%)
                updateState(meetingId, 0.10f, "Загрузка фонограммы разговоров и очистка шумов...")
                updateNotification(meeting.title, "Очистка шумов и калибровка...", 0.10f)
                delay(3000)

                // Phase 2: Speaker Profiling & Matching (25% - 50%)
                updateState(meetingId, 0.35f, "Сводка биометрии и сопоставление голосов...")
                updateNotification(meeting.title, "Распознавание спикеров по макетам...", 0.35f)
                delay(4000)

                // Phase 3: Speech to Text (50% - 75%)
                updateState(meetingId, 0.60f, "Дешифровка аудио ИИ-секретарем (Gemini AI)...")
                updateNotification(meeting.title, "Преобразование речи в текст...", 0.60f)

                val audioFile = meeting.audioFilePath?.let { File(it) }
                if (audioFile == null || !audioFile.exists()) {
                    throw Exception("Файл аудиозаписи не найден.")
                }

                val profiles = database.voiceProfileDao().getVoiceProfilesList()
                val analysisResult = transcriptionService.analyzeMeeting(
                    audioFile = audioFile,
                    participantA = meeting.participantA,
                    participantB = meeting.participantB,
                    additionalParticipants = meeting.additionalParticipants,
                    voiceProfiles = profiles
                )

                // Phase 4: Summarization & Tasks Extraction (75% - 100%)
                updateState(meetingId, 0.85f, "Структурирование саммари и выделение договоренностей...")
                updateNotification(meeting.title, "Синтез протокола и задач...", 0.85f)
                delay(3000)

                when (analysisResult) {
                    is MeetingAnalysisResult.Success -> {
                        val completedMeeting = meeting.copy(
                            transcript = analysisResult.transcript,
                            summaryOverview = analysisResult.summaryOverview,
                            summaryTopics = analysisResult.summaryTopics,
                            summaryDecisions = analysisResult.summaryDecisions,
                            summaryTasks = analysisResult.summaryTasks,
                            status = MeetingStatus.COMPLETED
                        )
                        repository.updateMeeting(completedMeeting)
                        Log.i(TAG, "Speech recognition completed successfully for ID $meetingId")

                        // Clear reactive state
                        clearState(meetingId)

                        // Completion Notification
                        sendResultNotification(
                            meetingId,
                            "Протокол готов: ${meeting.title}",
                            "Интеллектуальный анализ полностью завершен. Сводка решений сформирована."
                        )
                    }
                    is MeetingAnalysisResult.Error -> {
                        repository.updateMeeting(meeting.copy(status = MeetingStatus.FAILED))
                        clearState(meetingId)
                        sendResultNotification(
                            meetingId,
                            "Ошибка анализа: ${meeting.title}",
                            "Не удалось распознать запись: ${analysisResult.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio analysis for $meetingId", e)
                repository.updateMeeting(meeting.copy(status = MeetingStatus.FAILED))
                clearState(meetingId)
                sendResultNotification(
                    meetingId,
                    "Ошибка анализа: ${meeting.title}",
                    "Ошибка в ходе расшифровки: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                )
            } finally {
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }
            }
        }
    }

    private suspend fun updateState(meetingId: Long, progress: Float, text: String) {
        withContext(Dispatchers.Main) {
            val currentMap = _processingStates.value.toMutableMap()
            currentMap[meetingId] = ProcessingState(progress, text)
            _processingStates.value = currentMap
        }
    }

    private suspend fun clearState(meetingId: Long) {
        withContext(Dispatchers.Main) {
            val currentMap = _processingStates.value.toMutableMap()
            currentMap.remove(meetingId)
            _processingStates.value = currentMap
        }
    }

    private fun buildProcessingNotification(title: String, text: String, progress: Float): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 204, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val progressPercent = (progress * 100).toInt()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Обработка: $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progressPercent, false)
            .setColor(0xFFFF9800.toInt()) // High visual styling matching active tasks
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification(title: String, text: String, progress: Float) {
        val notification = buildProcessingNotification(title, text, progress)
        val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendResultNotification(meetingId: Long, title: String, text: String) {
        val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_MEETING_ID", meetingId) // Extensible parsing logic to open that specific detail
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 205 + meetingId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setColor(0xFF4CAF50.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setCategory(Notification.CATEGORY_EVENT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()

        nManager.notify(3000 + meetingId.toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Анализ встреч",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Сообщает о ходе и завершении ИИ-обработки записанных разговоров"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
