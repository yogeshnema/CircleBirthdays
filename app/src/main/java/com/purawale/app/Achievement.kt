package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Achievement(
    val id: String = "",
    val memberName: String = "",
    val memberId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val location: String = "",
    val mapsLink: String = "",
    val imageUrl: String = "",
    val treeId: String = "primary",
    val timestamp: Long = System.currentTimeMillis(),
    val addedBy: String = ""
)
