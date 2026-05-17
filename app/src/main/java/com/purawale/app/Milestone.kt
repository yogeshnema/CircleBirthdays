package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Milestone(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val year: String = "",
    val imageUrl: String = "",
    val location: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val authorId: String = "",
    val authorName: String = "",
    val visibilityType: String = "GLOBAL", // GLOBAL, PRIVATE_FAMILY, OLD_IS_GOLD
    val familyContextId: String = "", // Base family ID for filtering
    val reactions: Map<String, List<String>> = emptyMap(),
    val comments: List<Comment> = emptyList()
)
