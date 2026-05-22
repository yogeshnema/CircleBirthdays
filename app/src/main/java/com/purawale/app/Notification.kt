package com.purawale.app

import androidx.annotation.Keep

@Keep
data class AppNotification(
    val id: String = "",
    val type: String = "", // DM, GALLERY, MILESTONE, TRADITION, COOKBOOK, PROFILE_CHANGE, NEW_LOGIN, NEW_PROFILE, APPROVAL_REQUIRED, TAGGED_MEMORY, TAGGED_MILESTONE
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val readBy: List<String>? = emptyList(),
    val targetUserId: String? = null,
    val senderId: String? = null,
    val senderName: String? = null,
    val relatedId: String? = null,
    val isAdminOnly: Boolean = false,
    val topic: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
