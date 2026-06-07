package com.purawale.app

import androidx.annotation.Keep

@Keep
data class ActivityLog(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val action: String = "", // LOGIN, LIKE, REACTION, COMMENT, CHANGE_PROPOSED
    val targetType: String? = null, // MEMORY, RECIPE, TRADITION, MILESTONE, MEMBER
    val targetId: String? = null,
    val targetName: String? = null,
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
