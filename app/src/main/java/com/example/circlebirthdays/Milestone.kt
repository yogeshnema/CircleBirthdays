package com.example.circlebirthdays

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Milestone(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val year: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val authorId: String = "",
    val authorName: String = ""
)
