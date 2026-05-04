package com.example.circlebirthdays

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
@Entity(tableName = "members")
data class Member(
    @PrimaryKey val id: String = "",
    val familyId: String = "",
    val name: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val location: String? = null,
    val spouseName: String? = null,
    val fatherName: String? = null,
    val motherName: String? = null,
    val marriageDate: String? = null,
    val bereavementDate: String? = null,
    val photoUrl: String? = null,
    val immediateFamily: String = "",
    val address: String? = null,
    val password: String? = null,
    @get:PropertyName("isAdmin")
    @set:PropertyName("isAdmin")
    @PropertyName("isAdmin")
    var isAdmin: Boolean = false,
    @get:PropertyName("isEditor")
    @set:PropertyName("isEditor")
    @PropertyName("isEditor")
    var isEditor: Boolean = false,
    val status: String = "APPROVED",
    val lastLoggedIn: Long? = null,
    val relationship: String? = null,
    val fcmToken: String? = null
)
