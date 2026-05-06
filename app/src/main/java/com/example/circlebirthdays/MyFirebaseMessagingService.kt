package com.example.circlebirthdays

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        android.util.Log.d("FCM", "From: ${remoteMessage.from}")
        android.util.Log.d("FCM", "Data: ${remoteMessage.data}")
        
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Notification"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["type"]
        val senderId = remoteMessage.data["senderId"]
        
        sendNotification(title, body, type, senderId)
    }

    override fun onNewToken(token: String) {
        // Token is updated in FirebaseManager.updateLastLoggedIn on next login/app start
        android.util.Log.d("FCM", "Refreshed token: $token")
    }

    private fun sendNotification(title: String, messageBody: String, type: String? = null, senderId: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (type != null) {
                putExtra("navigate_to", type)
            }
            if (senderId != null) {
                putExtra("sender_id", senderId)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "family_circle_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Family Circle Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for family updates and messages"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = senderId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
