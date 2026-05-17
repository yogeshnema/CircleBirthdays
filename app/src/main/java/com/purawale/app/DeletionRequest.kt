package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class DeletionRequest(
    val id: String = "",
    val collectionName: String = "", // Matches web "memorylane", "recipes", "traditions"
    val docId: String = "",
    val title: String = "",
    val reason: String = "",
    val requestedBy: String = "",
    val requestedByName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
)
