package com.example.circlebirthdays

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class ChatChannel(
    val id: String = "", // Typically "id1_id2" where id1 < id2
    val userIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Map<String, Int> = emptyMap() // Map of userId to count
)
