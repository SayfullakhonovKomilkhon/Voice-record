package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiMeetingAnalysis
import com.example.data.api.GeminiTranscriptionService
import com.example.data.api.MeetingAnalysisResult
import com.example.data.local.AppDatabase
import com.example.data.model.Meeting
import com.example.data.model.MeetingStatus
import com.example.data.model.MeetingTask
import com.example.data.model.VoiceProfile
import com.example.data.model.DictionaryTerm
import com.example.data.repository.MeetingRepository
import com.example.util.AndroidAudioPlayer
import com.example.util.AndroidAudioRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import com.example.data.local.SessionManager
import java.util.Calendar

class MeetingViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "MeetingViewModel"

    private val database = AppDatabase.getDatabase(application)
    private val repository = MeetingRepository(database.meetingDao())
    private val transcriptionService = GeminiTranscriptionService(application)
    private val recorder = AndroidAudioRecorder(application)
    private val player = AndroidAudioPlayer(application)

    // Main persistent data
    val allMeetings: StateFlow<List<Meeting>> = repository.allMeetings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDictionaryTerms: StateFlow<List<DictionaryTerm>> = database.dictionaryTermDao().getAllTerms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _storageSizeByte = MutableStateFlow(0L)
    val storageSizeByte: StateFlow<Long> = _storageSizeByte.asStateFlow()

    // UI state flows for recording flow
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    val isPaused: StateFlow<Boolean> = com.example.util.MeetingRecorderService.isPaused

    val activeProcessingStates = com.example.util.MeetingProcessingService.processingStates

    private val _participantA = MutableStateFlow("Иван")
    val participantA: StateFlow<String> = _participantA.asStateFlow()

    private val _participantB = MutableStateFlow("Сергей")
    val participantB: StateFlow<String> = _participantB.asStateFlow()

    private val _meetingTitle = MutableStateFlow("")
    val meetingTitle: StateFlow<String> = _meetingTitle.asStateFlow()

    // Status indicators
    private val _currentPlayingMeetingId = MutableStateFlow<Long?>(null)
    val currentPlayingMeetingId: StateFlow<Long?> = _currentPlayingMeetingId.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _processingMeetingId = MutableStateFlow<Long?>(null)
    val processingMeetingId: StateFlow<Long?> = _processingMeetingId.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _premiumLimitReached = MutableStateFlow(false)
    val premiumLimitReached: StateFlow<Boolean> = _premiumLimitReached.asStateFlow()

    fun resetPremiumLimitReached() {
        _premiumLimitReached.value = false
    }

    fun calculateCurrentMonthRecordingDurationMs(): Long {
        val meetings = allMeetings.value
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        return meetings
            .filter { it.date >= startOfMonth }
            .sumOf { it.durationMs }
    }

    init {
        viewModelScope.launch {
            com.example.util.MeetingRecorderService.isRecordingAndActive.collect {
                _isRecording.value = it
            }
        }
        viewModelScope.launch {
            com.example.util.MeetingRecorderService.recordingDurationSeconds.collect {
                _recordingSeconds.value = it
            }
        }
        viewModelScope.launch {
            com.example.util.MeetingRecorderService.recordingAmplitude.collect {
                _amplitude.value = it
            }
        }
        viewModelScope.launch {
            com.example.util.MeetingProcessingService.processingStates.collect { states ->
                _processingMeetingId.value = states.keys.firstOrNull()
            }
        }
        updateStorageSize()
    }

    // Internals
    private var activeRecordingFile: File? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var recordingStartTime = 0L

    fun setParticipantA(name: String) { _participantA.value = name }
    fun setParticipantB(name: String) { _participantB.value = name }
    fun setMeetingTitle(title: String) { _meetingTitle.value = title }
    fun clearError() { _apiError.value = null }

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        
        val sessionManager = SessionManager(getApplication())
        if (!sessionManager.isProActive) {
            val totalDurationMs = calculateCurrentMonthRecordingDurationMs()
            val limitMs = 3 * 3600 * 1000L // 3 hours in ms
            if (totalDurationMs >= limitMs) {
                _premiumLimitReached.value = true
                return false
            }
        }
        _premiumLimitReached.value = false
        
        val timestamp = System.currentTimeMillis()
        val file = File(getApplication<Application>().filesDir, "recording_$timestamp.3gpp")
        
        recordingStartTime = System.currentTimeMillis()
        activeRecordingFile = file

        val title = _meetingTitle.value.ifBlank { "Переговоры" }
        com.example.util.MeetingRecorderService.startService(
            getApplication(),
            title,
            _participantA.value,
            _participantB.value,
            file
        )
        return true
    }

    fun stopRecording(onMeetingCreated: (Long) -> Unit) {
        if (!_isRecording.value) return
        
        val duration = _recordingSeconds.value * 1000L
        com.example.util.MeetingRecorderService.stopService(getApplication())

        val file = activeRecordingFile

        viewModelScope.launch {
            if (file != null && file.exists()) {
                val inputTitle = _meetingTitle.value.trim()
                val finalTitle = inputTitle.ifBlank { "Встреча от " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date()) }
                
                // 1. Save entry to local db in Transcribing Pending state
                val meeting = Meeting(
                    title = finalTitle,
                    durationMs = if (duration > 0) duration else (System.currentTimeMillis() - recordingStartTime),
                    audioFilePath = file.absolutePath,
                    status = MeetingStatus.TRANSCRIPTION_PENDING,
                    participantA = _participantA.value,
                    participantB = _participantB.value
                )
                val newId = repository.insertMeeting(meeting)

                // Reset inputs for next meeting
                _meetingTitle.value = ""

                // 2. Start the foreground processing service
                com.example.util.MeetingProcessingService.startService(getApplication(), newId)
                
                onMeetingCreated(newId)
            }
        }
    }

    fun pauseRecording() {
        com.example.util.MeetingRecorderService.pauseRecording(getApplication())
    }

    fun resumeRecording() {
        com.example.util.MeetingRecorderService.resumeRecording(getApplication())
    }

    // Force retry transcription for a failed meeting
    fun retryTranscription(meeting: Meeting) {
        val path = meeting.audioFilePath ?: return
        val file = File(path)
        if (file.exists()) {
            viewModelScope.launch {
                repository.updateMeeting(meeting.copy(status = MeetingStatus.TRANSCRIPTION_PENDING))
                com.example.util.MeetingProcessingService.startService(getApplication(), meeting.id)
            }
        } else {
            _apiError.value = "Файл аудиозаписи удален с устройства."
        }
    }

    // Interactive Checklist capability: toggle decisions / tasks
    fun toggleTaskCompletion(meetingId: Long, taskIndex: Int) {
        viewModelScope.launch {
            val meeting = repository.getMeetingById(meetingId) ?: return@launch
            val tasks = meeting.summaryTasks?.toMutableList() ?: return@launch
            if (taskIndex in tasks.indices) {
                val updatedTask = tasks[taskIndex].copy(isCompleted = !tasks[taskIndex].isCompleted)
                tasks[taskIndex] = updatedTask
                repository.updateMeeting(meeting.copy(summaryTasks = tasks))
            }
        }
    }

    // Player controls
    private var playbackProgressJob: Job? = null

    fun playMeetingAudio(meeting: Meeting) {
        val path = meeting.audioFilePath ?: return
        val file = File(path)
        if (!file.exists()) {
            _apiError.value = "Запись отсутствует на устройстве."
            return
        }

        if (_currentPlayingMeetingId.value == meeting.id) {
            if (player.isPlaying()) {
                player.pause()
                _isAudioPlaying.value = false
                playbackProgressJob?.cancel()
            } else {
                player.resume()
                _isAudioPlaying.value = true
                startPlaybackProgressLoop()
            }
        } else {
            stopPlaying()
            _currentPlayingMeetingId.value = meeting.id
            _isAudioPlaying.value = true
            _playbackPositionMs.value = 0L
            player.playFile(file) {
                stopPlaying()
            }
            startPlaybackProgressLoop()
        }
    }

    private fun startPlaybackProgressLoop() {
        playbackProgressJob?.cancel()
        playbackProgressJob = viewModelScope.launch {
            while (true) {
                _playbackPositionMs.value = player.getCurrentPosition().toLong()
                _isAudioPlaying.value = player.isPlaying()
                delay(100)
            }
        }
    }

    fun seekPlayingMeetingTo(ms: Long) {
        if (_currentPlayingMeetingId.value != null) {
            player.seekTo(ms.toInt())
            _playbackPositionMs.value = ms
        }
    }

    fun stopPlaying() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
        player.stop()
        _currentPlayingMeetingId.value = null
        _isAudioPlaying.value = false
        _playbackPositionMs.value = 0L
    }

    fun updateTranscriptUtterance(meetingId: Long, index: Int, speaker: String, text: String) {
        viewModelScope.launch {
            val meeting = repository.getMeetingById(meetingId) ?: return@launch
            val transcriptList = meeting.transcript?.toMutableList() ?: return@launch
            if (index in transcriptList.indices) {
                transcriptList[index] = transcriptList[index].copy(speaker = speaker, text = text)
                repository.updateMeeting(meeting.copy(transcript = transcriptList))
            }
        }
    }

    fun updateMeetingDetails(
        meetingId: Long,
        title: String,
        summaryOverview: String,
        summaryDecisions: List<String>,
        summaryTasks: List<MeetingTask>
    ) {
        viewModelScope.launch {
            val meeting = repository.getMeetingById(meetingId) ?: return@launch
            repository.updateMeeting(meeting.copy(
                title = title,
                summaryOverview = summaryOverview,
                summaryDecisions = summaryDecisions,
                summaryTasks = summaryTasks
            ))
        }
    }

    fun toggleFavorite(meetingId: Long) {
        viewModelScope.launch {
            val meeting = repository.getMeetingById(meetingId) ?: return@launch
            repository.updateMeeting(meeting.copy(isFavorite = !meeting.isFavorite))
        }
    }

    fun updateMeetingTags(meetingId: Long, tags: List<String>) {
        viewModelScope.launch {
            val meeting = repository.getMeetingById(meetingId) ?: return@launch
            repository.updateMeeting(meeting.copy(tags = tags))
        }
    }

    // Delete meeting
    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch {
            if (player.isPlaying() && _currentPlayingMeetingId.value == meeting.id) {
                stopPlaying()
            }
            // Delete actual file
            meeting.audioFilePath?.let {
                val file = File(it)
                if (file.exists()) {
                    file.delete()
                }
            }
            repository.deleteMeeting(meeting)
        }
    }

    // --- VOICE PROFILES EXTENSION STATE AND METHODS ---
    val allVoiceProfiles: StateFlow<List<VoiceProfile>> = database.voiceProfileDao().getAllVoiceProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isProfileRecording = MutableStateFlow(false)
    val isProfileRecording: StateFlow<Boolean> = _isProfileRecording.asStateFlow()

    private val _profileRecordingSeconds = MutableStateFlow(0)
    val profileRecordingSeconds: StateFlow<Int> = _profileRecordingSeconds.asStateFlow()

    private val _profileAmplitude = MutableStateFlow(0)
    val profileAmplitude: StateFlow<Int> = _profileAmplitude.asStateFlow()

    private var activeProfileFile: File? = null
    private var profileTimerJob: Job? = null
    private var profileAmplitudeJob: Job? = null
    private var profileStartTime = 0L

    fun startProfileRecording(): Boolean {
        if (_isProfileRecording.value) return false
        
        val timestamp = System.currentTimeMillis()
        val file = File(getApplication<Application>().filesDir, "profile_$timestamp.3gpp")
        
        profileStartTime = System.currentTimeMillis()
        val started = recorder.start(file)
        if (started) {
            activeProfileFile = file
            _isProfileRecording.value = true
            _profileRecordingSeconds.value = 0
            _profileAmplitude.value = 0

            profileTimerJob = viewModelScope.launch {
                while (_isProfileRecording.value) {
                    delay(1000)
                    _profileRecordingSeconds.value += 1
                }
            }

            profileAmplitudeJob = viewModelScope.launch {
                while (_isProfileRecording.value) {
                    delay(100)
                    _profileAmplitude.value = recorder.getMaxAmplitude()
                }
            }
            return true
        }
        return false
    }

    fun stopProfileRecording(): File? {
        if (!_isProfileRecording.value) return null
        
        recorder.stop()
        _isProfileRecording.value = false
        profileTimerJob?.cancel()
        profileAmplitudeJob?.cancel()

        return activeProfileFile
    }

    private val _playingProfileId = MutableStateFlow<Long?>(null)
    val playingProfileId: StateFlow<Long?> = _playingProfileId.asStateFlow()

    fun playProfileSample(profile: VoiceProfile) {
        val file = File(profile.audioFilePath)
        if (file.exists()) {
            _playingProfileId.value = profile.id
            player.playFile(file) {
                _playingProfileId.value = null
            }
        }
    }

    fun stopProfileSample() {
        player.stop()
        _playingProfileId.value = null
    }

    fun deleteVoiceProfile(profile: VoiceProfile) {
        viewModelScope.launch {
            if (player.isPlaying() && _playingProfileId.value == profile.id) {
                stopProfileSample()
            }
            val file = File(profile.audioFilePath)
            if (file.exists()) {
                file.delete()
            }
            database.voiceProfileDao().deleteVoiceProfile(profile)
        }
    }

    suspend fun saveVoiceProfile(name: String, relationship: String, filePath: String, durationSecs: Int, phrase: String): Long {
        val profile = VoiceProfile(
            name = name,
            relationship = relationship,
            audioFilePath = filePath,
            durationSeconds = durationSecs,
            phraseText = phrase
        )
        val id = database.voiceProfileDao().insertVoiceProfile(profile)
        updateStorageSize()
        return id
    }

    fun updateStorageSize() {
        viewModelScope.launch(Dispatchers.IO) {
            var totalSize = 0L
            val filesDir = getApplication<Application>().filesDir
            filesDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }
            _storageSizeByte.value = totalSize
        }
    }

    fun clearOldRecordings() {
        viewModelScope.launch {
            try {
                val meetingsList = repository.allMeetings.first()
                meetingsList.forEach { meeting ->
                    meeting.audioFilePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    val updatedMeeting = meeting.copy(audioFilePath = null)
                    repository.updateMeeting(updatedMeeting)
                }

                val filesDir = getApplication<Application>().filesDir
                filesDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.contains("recording_") || file.name.contains("profile_"))) {
                        file.delete()
                    }
                }

                updateStorageSize()
            } catch (e: Exception) {
                Log.e(tag, "Error clearing old recordings: ${e.message}", e)
            }
        }
    }

    fun deleteAccountAndAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                recorder.stop()
                player.stop()

                val filesDir = getApplication<Application>().filesDir
                filesDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }

                getApplication<Application>().cacheDir.deleteRecursively()

                database.dictionaryTermDao().deleteAllTerms()
                database.clearAllTables()

                val sessionManager = SessionManager(getApplication())
                sessionManager.clearSession()
                sessionManager.isOnboardingCompleted = false
                sessionManager.isProActive = false

                updateStorageSize()
                onComplete()
            } catch (e: Exception) {
                Log.e(tag, "Error deleting account and all data: ${e.message}", e)
                onComplete()
            }
        }
    }

    fun addDictionaryTerm(term: String, explanation: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = DictionaryTerm(term = term, explanation = explanation)
            database.dictionaryTermDao().insertTerm(item)
        }
    }

    fun deleteDictionaryTerm(term: DictionaryTerm) {
        viewModelScope.launch(Dispatchers.IO) {
            database.dictionaryTermDao().deleteTerm(term)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.stop()
        player.stop()
        timerJob?.cancel()
        amplitudeJob?.cancel()
    }
}
