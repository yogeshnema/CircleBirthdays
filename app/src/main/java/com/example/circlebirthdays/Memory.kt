package com.example.circlebirthdays

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Memory(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, APPROVED
    val reactions: Map<String, List<String>> = emptyMap(), // emoji to list of userIds
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
