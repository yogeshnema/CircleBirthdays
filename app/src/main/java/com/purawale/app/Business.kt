package com.purawale.app

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class Business(
    val id: String = "",
    val name: String = "",
    val ownerName: String = "",
    val contactNumber: String = "",
    val type: String = "", // Business, Consultancy, Shop, Event Hall, Public Place, etc.
    val address: String = "",
    val locationLink: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addedBy: String = "",
    val treeId: String = "primary",
    val timestamp: Long = System.currentTimeMillis()
)
