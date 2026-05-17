package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Discussion(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "TEXT", // TEXT, IMAGE, POLL
    val title: String = "",
    val content: String = "", // Text content or Image URL
    val pollOptions: List<PollOption>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, APPROVED
    val comments: List<Comment> = emptyList()
)

@Keep
data class PollOption(
    val id: String = "",
    val text: String = "",
    val voterIds: List<String> = emptyList()
)
