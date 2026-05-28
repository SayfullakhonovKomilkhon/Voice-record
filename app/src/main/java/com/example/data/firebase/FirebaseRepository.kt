package com.example.data.firebase

import android.net.Uri
import android.util.Log
import com.example.data.model.Meeting
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Result wrapper for secure, async Auth operations
 */
sealed class AuthResult {
    data class Success(val userId: String, val email: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Upload progress element wrapping storage tasks
 */
sealed class FileUploadState {
    data class Progress(val percentage: Float) : FileUploadState()
    data class Success(val storagePath: String) : FileUploadState()
    data class Error(val message: String) : FileUploadState()
}

/**
 * State container representing the real-time synchronization payload
 */
data class MeetingSyncData(
    val status: String,
    val summary: String = "",
    val topics: List<String> = emptyList(),
    val decisions: String = "",
    val errorText: String? = null,
    val segments: List<TranscriptionSegment> = emptyList()
)

data class TranscriptionSegment(
    val speaker: String,
    val text: String,
    val start: Double,
    val end: Double,
    val timestamp: Long
)

/**
 * Repository interface facilitating client transactions with Google Firebase Services
 */
class FirebaseRepository {

    companion object {
        private const val TAG = "FirebaseRepository"
    }

    /**
     * 1. АУТЕНТИФИКАЦИЯ: Регистрация нового пользователя
     */
    suspend fun registerUser(email: String, password: String): AuthResult {
        return try {
            val authInstance = FirebaseManager.auth
            val result = authInstance.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Initialize default user record in Firestore
                FirebaseManager.firestore.collection("users").document(user.uid).set(
                    mapOf(
                        "email" to email,
                        "tier" to "free",
                        "monthlyUsedSeconds" to 0,
                        "lastResetMonth" to java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                    )
                ).await()
                
                // Register standard FCM push token immediately
                registerCurrentFcmToken(user.uid)
                
                AuthResult.Success(user.uid, user.email ?: email)
            } else {
                AuthResult.Error("Пользователь не создан.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during user registration", e)
            AuthResult.Error(e.localizedMessage ?: "Ошибка при регистрации")
        }
    }

    /**
     * 1. АУТЕНТИФИКАЦИЯ: Вход существующего пользователя
     */
    suspend fun loginUser(email: String, password: String): AuthResult {
        return try {
            val result = FirebaseManager.auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Register standard FCM push token
                registerCurrentFcmToken(user.uid)
                AuthResult.Success(user.uid, user.email ?: email)
            } else {
                AuthResult.Error("Информация о пользователе отсутствует.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during login", e)
            AuthResult.Error(e.localizedMessage ?: "Ошибка аутентификации")
        }
    }

    /**
     * Выход из учетной записи
     */
    fun logout() {
        FirebaseManager.auth.signOut()
    }

    /**
     * 5. PUSH-УВЕДОМЛЕНИЯ: Нахождение и регистрация FCM токена
     */
    suspend fun registerCurrentFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token != null) {
                Log.i(TAG, "Fetched FCM Token: $token")
                FirebaseManager.firestore.collection("users").document(userId)
                    .update("fcmTokens", FieldValue.arrayUnion(token))
                    .await()
                Log.i(TAG, "FCM Token associated with User: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token for user $userId", e)
        }
    }

    /**
     * 2. ЗАГРУЗКА АУДИО: Загрузка файла переговоров на Firebase Storage с отслеживанием прогресса
     */
    fun uploadMeetingAudio(
        meetingId: String,
        audioFile: File,
        extension: String = "wav"
    ): Flow<FileUploadState> = callbackFlow {
        val uid = FirebaseManager.auth.currentUser?.uid
        if (uid == null) {
            trySend(FileUploadState.Error("Пользователь не авторизован"))
            close()
            return@callbackFlow
        }

        val storagePath = "meetings/$uid/$meetingId/audio.$extension"
        val storageRef = FirebaseManager.storage.reference.child(storagePath)
        val uploadTask = storageRef.putFile(Uri.fromFile(audioFile))

        // Set listen listeners
        uploadTask.addOnProgressListener { snapshot ->
            val totalBytes = snapshot.totalByteCount.coerceAtLeast(1)
            val transferred = snapshot.bytesTransferred
            val progress = (transferred.toFloat() / totalBytes.toFloat()) * 100f
            trySend(FileUploadState.Progress(progress))
        }.addOnSuccessListener { _ ->
            // Document creation payload inside firestore
            Log.i(TAG, "Audio successfully uploaded to: $storagePath")
            trySend(FileUploadState.Success(storagePath))
            close()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed uploading audio file", exception)
            trySend(FileUploadState.Error(exception.localizedMessage ?: "Ошибка загрузки файла"))
            close()
        }

        // Cancel firebase storage upload standard connection on scope closure
        awaitClose {
            if (!uploadTask.isComplete && !uploadTask.isCanceled) {
                uploadTask.cancel()
            }
        }
    }

    /**
     * Инициализация записи встречи (вызывается на клиенте ДО загрузки файла аудио)
     */
    suspend fun createFirestoreMeetingEnvelope(
        meetingId: String,
        title: String,
        participantA: String,
        participantB: String,
        durationSeconds: Int,
        audioExtension: String = "wav"
    ) {
        val uid = FirebaseManager.auth.currentUser?.uid ?: throw Exception("Not authenticated")
        
        val meetingPayload = mapOf(
            "id" to meetingId,
            "ownerId" to uid,
            "title" to title,
            "participantA" to participantA,
            "participantB" to participantB,
            "duration" to durationSeconds,
            "audioExtension" to audioExtension,
            "status" to "recording", // status cycle in Firestore: recording/processing/completed/failed
            "createdAt" to FieldValue.serverTimestamp()
        )
        
        FirebaseManager.firestore.collection("meetings").document(meetingId)
            .set(meetingPayload)
            .await()
    }

    /**
     * 3. CLOUD FUNCTIONS: Триггер аналитического разбора встречи
     */
    suspend fun triggerMeetingProcessing(
        meetingId: String,
        vocabulary: String = "",
        durationSeconds: Int = 120
    ): String {
        val payload = mapOf(
            "meetingId" to meetingId,
            "vocabulary" to vocabulary,
            "duration" to durationSeconds
        )

        val result = FirebaseManager.functions
            .getHttpsCallable("processMeeting")
            .call(payload)
            .await()

        val parsedData = result.data as? Map<*, *>
        return parsedData?.get("summary")?.toString() ?: "Встреча находится на анализе."
    }

    /**
     * 4. REALTIME LISTENER: Подписка на изменение статуса встречи и всей сопутствующей аналитики в Firestore
     */
    fun subscribeToMeetingUpdates(meetingId: String): Flow<MeetingSyncData> = callbackFlow {
        val meetingRef = FirebaseManager.firestore.collection("meetings").document(meetingId)
        
        val listener = meetingRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore subscription error for $meetingId", error)
                trySend(MeetingSyncData(status = "failed", errorText = error.localizedMessage))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("status") ?: "recording"
                val summary = snapshot.getString("summaryOverview") ?: ""
                val errorText = snapshot.getString("errorText")
                val topics = snapshot.get("summaryTopics") as? List<String> ?: emptyList()
                
                // Read subcollections or update details
                if (status == "completed") {
                    // Pull transcripts and decisions
                    meetingRef.collection("segments").orderBy("timestamp")
                        .get()
                        .addOnSuccessListener { segmentsSnap ->
                            val segList = segmentsSnap.map { doc ->
                                TranscriptionSegment(
                                    speaker = doc.getString("speaker") ?: "Спикер",
                                    text = doc.getString("text") ?: "",
                                    start = doc.getDouble("start") ?: 0.0,
                                    end = doc.getDouble("end") ?: 0.0,
                                    timestamp = doc.getLong("timestamp") ?: 0L
                                )
                            }
                            
                            // Load and push decisions
                            meetingRef.collection("analysis").document("summary_decisions")
                                .get()
                                .addOnSuccessListener { decDoc ->
                                    val decisions = decDoc.getString("content") ?: ""
                                    trySend(
                                        MeetingSyncData(
                                            status = status,
                                            summary = summary,
                                            topics = topics,
                                            decisions = decisions,
                                            errorText = errorText,
                                            segments = segList
                                        )
                                    )
                                }
                        }
                } else {
                    // Send basic envelope updates (transcribing or downloading)
                    trySend(
                        MeetingSyncData(
                            status = status,
                            summary = summary,
                            topics = topics,
                            errorText = errorText
                        )
                    )
                }
            }
        }

        awaitClose {
            listener.remove()
        }
    }
}
