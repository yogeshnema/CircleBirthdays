package com.purawale.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.purawale.app.AppNotification
import com.purawale.app.FirebaseManager
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    var notifications by mutableStateOf<List<AppNotification>>(emptyList())
        private set

    var unreadCount by mutableIntStateOf(0)
        private set

    private var listenerRegistration: ListenerRegistration? = null

    fun startListening(userId: String, isAdmin: Boolean) {
        listenerRegistration?.remove()
        listenerRegistration = FirebaseManager.getNotifications(userId, isAdmin) { fetched ->
            notifications = fetched
            unreadCount = fetched.count { it.readBy?.contains(userId) != true }
        }
    }

    fun markAsRead(notificationId: String, userId: String) {
        viewModelScope.launch {
            FirebaseManager.markNotificationAsRead(notificationId, userId)
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            notifications.forEach { notification ->
                if (notification.readBy?.contains(userId) != true) {
                    FirebaseManager.markNotificationAsRead(notification.id, userId)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
