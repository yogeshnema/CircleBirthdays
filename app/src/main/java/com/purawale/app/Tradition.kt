package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Tradition(
    val id: String = "",
    val title: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val reactions: Map<String, List<String>> = emptyMap(),
    val comments: List<Comment> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
