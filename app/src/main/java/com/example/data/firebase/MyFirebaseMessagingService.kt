package com.example.data.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * 5. PUSH-УВЕДОМЛЕНИЯ: Служба приема и обработки push-сообщений Firebase Cloud Messaging (FCM)
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMessagingServ"
        private const val CHANNEL_ID = "fcm_push_channel"
        private const val EXECUTED_NOTICE_ID = 5055
    }

    /**
     * Вызывается при обновлении токена на серверах Google FCM
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New token generated: $token")
        
        try {
            FirebaseManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prep FirebaseManager inside onNewToken background hook", e)
        }
        
        // Associate token in Firestore if logged in
        val uid = FirebaseManager.auth.currentUser?.uid
        if (uid != null) {
            FirebaseManager.firestore.collection("users").document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener {
                    Log.i(TAG, "Refreshed FCM token bound successfully in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed binding refreshed FCM token to Firestore", e)
                }
        }
    }

    /**
     * Вызывается при получении Push-уведомления (когда приложение запущено)
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Incoming message from sender: ${remoteMessage.from}")

        try {
            FirebaseManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prep FirebaseManager inside onMessageReceived background hook", e)
        }

        // 1. Process custom data payload
        val meetingId = remoteMessage.data["meetingId"]
        
        // 2. Process display elements
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Встреча расшифрована!"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Анализ встречи успешно завершен."

        // Dispatch local notification overlay
        sendLocalFcmNotification(title, body, meetingId)
    }

    /**
     * Вывод системного уведомления на экран смартфона с deep-link на нужную встречу
     */
    private fun sendLocalFcmNotification(title: String, body: String, meetingId: String?) {
        val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createFcmChannel(nManager)

        // On push-tap, open MainActivity and forward the target meetingId parameter
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (meetingId != null) {
                putExtra("OPEN_MEETING_ID_STRING", meetingId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            901,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.presence_video_online) // visible status indicator image
            .setColor(0xFF1E88E5.toInt()) // elegant brand highlight blue
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        nManager.notify(EXECUTED_NOTICE_ID, notification)
    }

    private fun createFcmChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Важные оповещения от ИИ-секретаря",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сообщения об окончании расшифровки аудиофайлов и готовых саммари встреч"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
