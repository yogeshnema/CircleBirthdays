package com.example.circlebirthdays

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class BirthdayWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val members = FirebaseManager.getMembersOnce()
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val upcomingBirthdays = members.filter { member ->
            if (member.dateOfBirth.isEmpty()) return@filter false
            try {
                val dob = dateFormat.parse(member.dateOfBirth) ?: return@filter false
                val dobCal = Calendar.getInstance().apply { time = dob }
                
                // Check if today is the birthday (ignoring year)
                today.get(Calendar.MONTH) == dobCal.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == dobCal.get(Calendar.DAY_OF_MONTH)
            } catch (e: Exception) {
                false
            }
        }

        upcomingBirthdays.forEach { member ->
            sendNotification(member)
        }

        return Result.success()
    }

    private fun sendNotification(member: Member) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "birthday_notifications"

        val channel = NotificationChannel(
            channelId,
            "Birthday Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Using mipmap icon
            .setContentTitle("Birthday Today!")
            .setContentText("It's ${member.name}'s birthday today! Don't forget to wish them.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(member.id.hashCode(), notification)
    }
}
