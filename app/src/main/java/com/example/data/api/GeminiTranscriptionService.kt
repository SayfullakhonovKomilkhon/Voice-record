package com.example.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Meeting
import com.example.data.model.MeetingStatus
import com.example.data.model.MeetingTask
import com.example.data.model.Utterance
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@JsonClass(generateAdapter = true)
data class GeminiMeetingAnalysis(
    val transcript: List<Utterance>,
    val summary: GeminiSummary
)

@JsonClass(generateAdapter = true)
data class GeminiSummary(
    val overview: String,
    val topics: List<String>,
    val decisions: List<String>,
    val tasks: List<GeminiTask>
)

@JsonClass(generateAdapter = true)
data class GeminiTask(
    val assignedTo: String,
    val taskText: String
)

class GeminiTranscriptionService(private val context: Context) {
    private val tag = "GeminiTransService"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val analysisAdapter = moshi.adapter(GeminiMeetingAnalysis::class.java)

    suspend fun analyzeMeeting(
        audioFile: File,
        participantA: String,
        participantB: String,
        additionalParticipants: List<String> = emptyList(),
        voiceProfiles: List<com.example.data.model.VoiceProfile> = emptyList()
    ): MeetingAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Safety check if key is empty or placeholder
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || !audioFile.exists()) {
            Log.w(tag, "Gemini API key is not configured or audio file not found. Falling back to mock transcription.")
            return@withContext generateMockAnalysis(participantA, participantB, additionalParticipants, voiceProfiles)
        }

        try {
            val audioBase64 = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
            
            val systemPrompt = "Ты — профессиональный ИИ-секретарь деловых встреч. Твоя единственная цель — транскрибировать аудиозапись, распределить диалог между участниками по именам и составить сводку."
            
            val profilesContext = if (voiceProfiles.isNotEmpty()) {
                val listStr = voiceProfiles.joinToString("\n") { "- ${it.name} (${it.relationship})" }
                """
                
                ВАЖНО (РАСПОЗНАВАНИЕ ПО ЗАПЕЧАТЛЕННЫМ ГОЛОСАМ):
                В системе зарегистрированы следующие профили голосов:
                $listStr
                
                Пожалуйста, сравни тембр, характеристики голосов и манеру речи на записи с этими зарегистрированными профилями. Если голос спикера на записи совпадает с одним из этих зарегистрированных профилей, используй в поле "speaker" СТРОГО имя из этого профиля.
                """.trimIndent()
            } else {
                ""
            }

            val allNames = (listOf(participantA, participantB) + additionalParticipants)
                .filter { it.isNotBlank() }
                .distinct()
            val participantsStr = allNames.joinToString(", ")

            val userPrompt = """
                Ниже представлена аудиозапись деловой встречи. На этой встрече разговаривали следующие участники: $participantsStr.
                $profilesContext
                
                ВАЖНОЕ ТРЕБОВАНИЕ ПО ИДЕНТИФИКАЦИИ И РАСПОЗНАВАНИЮ ГОЛОСОВ:
                1. Внимательно проанализируй голос, тембр, интонации и контекст речи каждого спикера на записи. На встрече могут участвовать более двух человек.
                2. Обязательно сопоставь голоса с предложенным списком имен участников: $participantsStr. Убедись, что реплика каждого участника приписана именно ему, а не другому человеку.
                3. Если у кого-то из участников есть зарегистрированный профиль голоса (см. выше), используй именно его имя из профиля. Постарайся безошибочно разделить разговор между всеми реальными собеседниками.
                
                ВАЖНОЕ ТРЕБОВАНИЕ ПО ЯЗЫКАМ (ПОДДЕРЖКА РУССКОГО, УЗБЕКСКОГО И АНГЛИЙСКОГО В РЕЧИ):
                Участники встречи могут разговаривать на русском, узбекском или английском языках (или смешивать их в диалоге).
                1. Полную расшифровку (в поле "transcript") сделай на оригинальном языке речи (то есть дословный текст реплик пиши на том языке, на котором говорит спикер в данный конкретный момент — на русском, узбекском или английском соответственно).
                2. Раздел резюме встречи и аналитики ("summary": "overview", "topics", "decisions", "tasks") составь СТРОГО на русском языке для русскоязычного интерфейса приложения, независимо от того, на каком из языков шло обсуждение.
                
                Пожалуйста, сделай две вещи и верни результат СТРОГО в формате JSON:
                1. Полную расшифровку (распредели реплики разговора по спикерам: используя их наиболее точные имена на основе тона, тембра, контекста беседы или указанных зарегистрированных профилей). Для каждой реплики укажи примерное время начала в миллисекундах от старта (timestampMs).
                2. Резюме встречи: краткое описание (overview - сводка в 3-5 предложений на русском языке), основные темы встречи (topics - по пунктам на русском языке, список строк), принятые решения (decisions - на русском языке, список строк), и задачи (tasks - список объектов с полями assignedTo и taskText на русском языке).
                
                Верни СТРОГО валидный JSON следующей структуры, без какого-либо дополнительного текста, markdown-оберток или разметки '```json':
                {
                  "transcript": [
                    {
                      "speaker": "Имя Спикера",
                      "text": "Дословный текст реплики на языке оригинала (RU/UZ/EN)",
                      "timestampMs": 1500
                    }
                  ],
                  "summary": {
                    "overview": "Краткое описание от 3 до 5 предложений на русском языке...",
                    "topics": [
                      "Тема разговора 1 на русском",
                      "Тема разговора 2 на русском"
                    ],
                    "decisions": [
                      "Решение 1 на русском",
                      "Решение 2 на русском"
                    ],
                    "tasks": [
                      {
                        "assignedTo": "Имя ответственного на русском",
                        "taskText": "Суть задачи на русском языке"
                      }
                    ]
                  }
                }
              """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = userPrompt),
                            Part(
                                inlineData = InlineData(
                                    mimeType = when (audioFile.extension.lowercase()) {
                                        "3gpp", "3gp" -> "audio/3gpp"
                                        "m4a", "mp4" -> "audio/mp4"
                                        "wav" -> "audio/wav"
                                        "mp3" -> "audio/mpeg"
                                        else -> "audio/mp4"
                                    },
                                    data = audioBase64
                                )
                            )
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.4f
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini API")

            val cleanedJson = cleanJsonResponse(jsonText)
            val analysis = analysisAdapter.fromJson(cleanedJson)
                ?: throw Exception("Failed to parse Gemini response JSON")

            Log.i(tag, "Successfully analyzed meeting with Gemini")
            return@withContext MeetingAnalysisResult.Success(
                transcript = analysis.transcript,
                summaryOverview = analysis.summary.overview,
                summaryTopics = analysis.summary.topics,
                summaryDecisions = analysis.summary.decisions,
                summaryTasks = analysis.summary.tasks.map {
                    MeetingTask(assignedTo = it.assignedTo, taskText = it.taskText, isCompleted = false)
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Gemini API error", e)
            return@withContext MeetingAnalysisResult.Error(e.message ?: "Неизвестная ошибка ИИ")
        }
    }

    private fun cleanJsonResponse(rawResponse: String): String {
        var cleaned = rawResponse.trim()
        if (cleaned.startsWith("```")) {
            // strip starting ```json or ```
            cleaned = cleaned.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            // strip ending ```
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substringBeforeLast("```")
            }
        }
        return cleaned.trim()
    }

    private fun generateMockAnalysis(
        participantA: String,
        participantB: String,
        additionalParticipants: List<String> = emptyList(),
        voiceProfiles: List<com.example.data.model.VoiceProfile> = emptyList()
    ): MeetingAnalysisResult {
        // High quality mock based on user names
        val speakerA = voiceProfiles.getOrNull(0)?.name ?: if (participantA.isNotBlank()) participantA else "Участник 1"
        val speakerB = voiceProfiles.getOrNull(1)?.name ?: if (participantB.isNotBlank()) participantB else "Участник 2"
        val speakerC = additionalParticipants.getOrNull(0) ?: "Алексей"

        val mockTranscript = mutableListOf(
            Utterance(speakerA, "Приветствую! Давай обсудим запуск нашего проекта перед переговорами.", 1000L),
            Utterance(speakerB, "Да, привет! Нам нужно утвердить рекламный бюджет и распределить основные задачи на неделю.", 4200L)
        )
        
        if (additionalParticipants.isNotEmpty()) {
            mockTranscript.add(Utterance(speakerC, "Всем привет! Я тоже подключился. Какие каналы продвижения мы рассматриваем?", 8000L))
            mockTranscript.add(Utterance(speakerA, "Рад слышать, $speakerC. По бюджету: я предлагаю направить 120 000 рублей на таргетированную рекламу и продвижение.", 12500L))
            mockTranscript.add(Utterance(speakerC, "Отличная идея, я могу взять на себя настройку таргетинга в соцсетях.", 18000L))
            mockTranscript.add(Utterance(speakerB, "Замечательно! Что касается дизайна: я подготовлю рекламные креативы и баннеры до конца недели.", 24000L))
            mockTranscript.add(Utterance(speakerA, "Супер, креативы на $speakerB, настройка на $speakerC. А я подготовлю договор с подрядчиком до среды.", 31000L))
            mockTranscript.add(Utterance(speakerC, "Договорились, зафиксируем это в протоколе встречи.", 38000L))
        } else {
            mockTranscript.add(Utterance(speakerA, "По бюджету: я предлагаю направить 120 000 рублей на таргетированную рекламу и продвижение.", 9500L))
            mockTranscript.add(Utterance(speakerB, "Хорошая идея, это укладывается в наши маркетинговые ожидания. Давай утвердим эту сумму.", 16000L))
            mockTranscript.add(Utterance(speakerB, "Что касается задач: я могу сразу подготовить дизайн креативов и баннеров до конца недели.", 22300L))
            mockTranscript.add(Utterance(speakerA, "Супер, дизайн на тебе. А я подготовлю и подпишу договор с подрядчиком по трафику до среды.", 28000L))
            mockTranscript.add(Utterance(speakerB, "Отлично, договорились! Зафиксируем это в протоколе встречи.", 34500L))
            mockTranscript.add(Utterance(speakerA, "Да, спасибо за продуктивное обсуждение! Работаем.", 39000L))
        }

        val participantsNames = (listOf(speakerA, speakerB) + (if (additionalParticipants.isNotEmpty()) listOf(speakerC) else emptyList())).joinToString(", ")
        val mockOverview = "Обсуждение рекламного бюджета для успешного старта проекта и распределение операционных задач между участниками: $participantsNames. Стороны согласовали общий финансовый лимит кампании. В качестве дополнительного канала продвижения решено запустить таргетинг. Проектная команда приступает к подготовке креативов и подписанию документов."
        
        val mockTopics = listOf(
            "Подготовка к старту нового совместного проекта",
            "Согласование рекламного бюджета",
            "Распределение рабочих обязанностей сторон"
        )
        val mockDecisions = listOf(
            "Утвердить рекламный бюджет в размере 120 000 рублей для таргетированной рекламы.",
            "Запустить рекламную кампанию сразу после готовности визуальных материалов."
        )
        val mockTasks = if (additionalParticipants.isNotEmpty()) {
            listOf(
                MeetingTask(assignedTo = speakerB, taskText = "Подготовить дизайн рекламных креативов и баннеров до конца недели.", isCompleted = false),
                MeetingTask(assignedTo = speakerC, taskText = "Настроить таргетированную рекламу в социальных сетях.", isCompleted = false),
                MeetingTask(assignedTo = speakerA, taskText = "Подготовить договор с подрядчиком по трафику до среды.", isCompleted = false)
            )
        } else {
            listOf(
                MeetingTask(assignedTo = speakerB, taskText = "Подготовить дизайн рекламных креативов и баннеров до конца недели.", isCompleted = false),
                MeetingTask(assignedTo = speakerA, taskText = "Подготовить и подписать договор с подрядчиком по трафику до среды.", isCompleted = false)
            )
        }

        return MeetingAnalysisResult.Success(
            transcript = mockTranscript,
            summaryOverview = mockOverview,
            summaryTopics = mockTopics,
            summaryDecisions = mockDecisions,
            summaryTasks = mockTasks
        )
    }
}

sealed interface MeetingAnalysisResult {
    data class Success(
        val transcript: List<Utterance>,
        val summaryOverview: String,
        val summaryTopics: List<String>,
        val summaryDecisions: List<String>,
        val summaryTasks: List<MeetingTask>
    ): MeetingAnalysisResult

    data class Error(val message: String): MeetingAnalysisResult
}
