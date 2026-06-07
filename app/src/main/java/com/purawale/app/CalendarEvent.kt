package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CalendarEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "", // YYYY-MM-DD
    val type: String = "GENERAL", // MARRIAGE, GET_TOGETHER, COMMUNITY_SERVICE, GENERAL
    val location: String = "",
    val mapsLink: String = "",
    val inviteCardUrl: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val createdByName: String = "",
    val treeId: String = "primary"
)
