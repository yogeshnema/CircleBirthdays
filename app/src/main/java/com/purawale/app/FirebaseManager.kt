package com.purawale.app

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance().apply {
        // Explicitly enable local persistence to allow the app to work offline
        // and sync when connectivity is restored.
        val settings = com.google.firebase.firestore.firestoreSettings {
            setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
        }
        firestoreSettings = settings
    }
    private val storage = FirebaseStorage.getInstance()

    private fun normalizedTreeId(treeId: String?): String {
        return treeId?.takeIf { it.isNotBlank() } ?: "primary"
    }

    private fun belongsToTree(itemTreeId: String?, selectedTreeId: String): Boolean {
        return normalizedTreeId(itemTreeId) == normalizedTreeId(selectedTreeId)
    }

    suspend fun uploadPhoto(uri: Uri): String {
        val fileName = "photos/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadPhoto(data: ByteArray): String {
        val fileName = "photos/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putBytes(data).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadAudio(uri: Uri): String {
        val fileName = "audio/${UUID.randomUUID()}.mp3"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadGameAsset(sessionId: String, file: File): String {
        val ref = storage.reference.child("games/$sessionId/${file.name}")
        ref.putFile(android.net.Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun submitChange(member: Member, isApproved: Boolean) {
        try {
            val collection = if (isApproved) AppConfig.Collections.MEMBERS else AppConfig.Collections.PENDING_UPDATES
            val status = if (isApproved) "APPROVED" else "PENDING"
            val finalMember = member.copy(status = status)
            
            Log.d("FirebaseManager", "Submitting to $collection: ${finalMember.name}")
            db.collection(collection)
                .document(finalMember.id)
                .set(finalMember)
                .await()
            
            logActivity(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userId = member.requestedBy ?: member.id,
                    userName = member.requestedByName ?: member.name,
                    action = if (isApproved) "CHANGE_APPLIED" else "CHANGE_PROPOSED",
                    targetType = "MEMBER",
                    targetId = member.id,
                    targetName = member.name
                )
            )
            Log.d("FirebaseManager", "Successfully submitted: ${finalMember.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to submit change for ${member.name}", e)
            throw e
        }
    }

    suspend fun submitBatch(members: List<Member>, isApproved: Boolean) {
        try {
            val collection = if (isApproved) AppConfig.Collections.MEMBERS else AppConfig.Collections.PENDING_UPDATES
            val status = if (isApproved) "APPROVED" else "PENDING"
            
            // Firestore batches are limited to 500 operations
            members.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { member ->
                    val finalMember = member.copy(status = status)
                    val docRef = db.collection(collection).document(finalMember.id)
                    batch.set(docRef, finalMember)
                }
                batch.commit().await()
            }
            Log.d("FirebaseManager", "Successfully submitted batch of ${members.size}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to submit batch", e)
            throw e
        }
    }

    suspend fun approveChange(pendingMember: Member) {
        try {
            db.runTransaction { transaction ->
                val memberRef = db.collection(AppConfig.Collections.MEMBERS).document(pendingMember.id)
                val snapshot = transaction.get(memberRef)
                val existing = if (snapshot.exists()) snapshot.toObject(Member::class.java) else null
                
                // If non-admin, keep the existing global relationship and update manualRelationships instead
                val baseMember = pendingMember.copy(
                    status = "APPROVED",
                    relationship = if (pendingMember.requestedBy != null) (existing?.relationship ?: "") else pendingMember.relationship,
                    manualRelationships = existing?.manualRelationships ?: emptyMap(),
                    fcmToken = existing?.fcmToken ?: pendingMember.fcmToken,
                    lastLoggedIn = existing?.lastLoggedIn ?: pendingMember.lastLoggedIn,
                    password = existing?.password ?: pendingMember.password,
                    requestedBy = null,
                    requestedByName = null,
                    requestedRelationship = null
                )
                
                val finalManual = baseMember.manualRelationships.toMutableMap()
                if (pendingMember.requestedBy != null && pendingMember.requestedRelationship != null) {
                    finalManual[pendingMember.requestedBy] = pendingMember.requestedRelationship
                }
                
                transaction.set(memberRef, baseMember.copy(manualRelationships = finalManual))
                transaction.delete(db.collection(AppConfig.Collections.PENDING_UPDATES).document(pendingMember.id))
            }.await()
            logActivity(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userId = pendingMember.requestedBy ?: pendingMember.id,
                    userName = pendingMember.requestedByName ?: pendingMember.name,
                    action = "CHANGE_APPROVED",
                    targetType = "MEMBER",
                    targetId = pendingMember.id,
                    targetName = pendingMember.name
                )
            )
            Log.d("FirebaseManager", "Approved change for ${pendingMember.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to approve change", e)
            throw e
        }
    }

    suspend fun deletePendingChange(memberId: String) {
        if (memberId.isBlank()) return
        db.collection(AppConfig.Collections.PENDING_UPDATES)
            .document(memberId)
            .delete()
            .await()
    }

    suspend fun clearAll() {
        try {
            val collections = listOf(
                AppConfig.Collections.MEMBERS,
                AppConfig.Collections.PENDING_UPDATES,
                AppConfig.Collections.MEMORIES,
                AppConfig.Collections.DISCUSSIONS,
                AppConfig.Collections.CHANNELS,
                AppConfig.Collections.RELATIONSHIP_OVERRIDES
            )
            for (collectionName in collections) {
                val snapshot = db.collection(collectionName).get().await()
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                    // If it's a channel, we also need to delete messages subcollection
                    if (collectionName == AppConfig.Collections.CHANNELS) {
                        val messages = doc.reference.collection(AppConfig.Collections.MESSAGES).get().await()
                        messages.documents.forEach { msg ->
                            batch.delete(msg.reference)
                        }
                    }
                }
                batch.commit().await()
            }
            Log.d("FirebaseManager", "Successfully cleared all database collections")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to clear database", e)
            throw e
        }
    }

    suspend fun updatePassword(memberId: String, hashedPw: String) {
        if (memberId.isBlank()) return
        try {
            db.collection(AppConfig.Collections.MEMBERS).document(memberId)
                .update("password", hashedPw)
                .await()
            Log.d("FirebaseManager", "Password updated for $memberId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to update password", e)
            throw e
        }
    }

    suspend fun removeProfilePhoto(memberId: String) {
        if (memberId.isBlank()) return
        try {
            db.collection(AppConfig.Collections.MEMBERS).document(memberId)
                .update("photoUrl", null)
                .await()
            Log.d("FirebaseManager", "Photo removed for $memberId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to remove photo", e)
            throw e
        }
    }

    suspend fun updateLastLoggedIn(memberId: String) {
        if (memberId.isBlank()) return
        try {
            val now = System.currentTimeMillis()
            db.collection(AppConfig.Collections.MEMBERS).document(memberId)
                .update("lastLoggedIn", now)
                .await()
            
            // Also refresh FCM token
            FirebaseMessaging.getInstance().token.await()?.let { token ->
                db.collection(AppConfig.Collections.MEMBERS).document(memberId).update("fcmToken", token).await()
            }

            // Log activity
            val memberSnapshot = db.collection(AppConfig.Collections.MEMBERS).document(memberId).get().await()
            val memberName = memberSnapshot.getString("name") ?: "A user"
            logActivity(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    userId = memberId,
                    userName = memberName,
                    action = "LOGIN",
                    timestamp = now
                )
            )
            
            // Trigger a notification for other admins about this login
            
            triggerNotification(
                topic = "admin_only",
                title = "New Login",
                body = "$memberName just logged in.",
                type = "NEW_LOGIN",
                adminOnly = true
            )

            Log.d("FirebaseManager", "Last logged in and token updated for $memberId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to update last logged in", e)
        }
    }

    // Messaging
    suspend fun sendMessage(message: Message) {
        val channelId = if (message.senderId < message.receiverId) {
            "${message.senderId}_${message.receiverId}"
        } else {
            "${message.receiverId}_${message.senderId}"
        }

        val messagesCollection = db.collection(AppConfig.Collections.CHANNELS).document(channelId)
            .collection(AppConfig.Collections.MESSAGES)
            
        val messageRef = if (message.id.isNotEmpty()) {
            messagesCollection.document(message.id)
        } else {
            messagesCollection.document()
        }
        
        val messageWithId = if (message.id.isEmpty()) message.copy(id = messageRef.id) else message
        
        // Use a batch instead of a transaction for the main message write to allow latency compensation.
        // Transactions disable latency compensation (offline persistence) because they require a round-trip.
        val batch = db.batch()
        val channelRef = db.collection(AppConfig.Collections.CHANNELS).document(channelId)
        
        batch.set(messageRef, messageWithId)

        // Note: In a high-concurrency app, we'd use a transaction for the unreadCount.
        // But for a simple DM between two people, a batch with a merge is usually sufficient 
        // and provides a better UX by appearing instantly.
        val channelUpdate = mapOf(
            "id" to channelId,
            "userIds" to listOf(message.senderId, message.receiverId),
            "lastMessage" to message.text,
            "lastTimestamp" to message.timestamp
        )
        batch.set(channelRef, channelUpdate, com.google.firebase.firestore.SetOptions.merge())
        // batch.commit().await() // Removed await to allow latency compensation

        // Use a standard commit for the batch to benefit from local cache immediate update.
        batch.commit()
        
        // Increment unread count in a separate background transaction to not block the UI
        try {
            CoroutineScope(Dispatchers.IO).launch {
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(channelRef)
                    val currentUnread = (snapshot.get("unreadCount") as? Map<*, *>)?.filterKeys { it is String }?.map { it.key as String to ((it.value as? Number)?.toLong() ?: 0L) }?.toMap()?.toMutableMap() ?: mutableMapOf()
                    val receiverCount = currentUnread[message.receiverId] ?: 0L
                    currentUnread[message.receiverId] = receiverCount + 1
                    transaction.update(channelRef, "unreadCount", currentUnread)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to increment unread count", e)
        }
        
        // commitTask.await() // Removed await to allow latency compensation and background processing

        // Trigger push notification request for recipient
        val recipientSnapshot = db.collection(AppConfig.Collections.MEMBERS).document(message.receiverId).get().await()
        val recipientToken = recipientSnapshot.getString("fcmToken")
        val senderSnapshot = db.collection(AppConfig.Collections.MEMBERS).document(message.senderId).get().await()
        val isRecipientAdmin = recipientSnapshot.getBoolean("isAdmin") == true || (recipientSnapshot.getString("phoneNumber") == AppConfig.ADMIN_PHONE && recipientSnapshot.getString("name") == AppConfig.ADMIN_NAME)
        val isSenderAdmin = senderSnapshot.getBoolean("isAdmin") == true || (senderSnapshot.getString("phoneNumber") == AppConfig.ADMIN_PHONE && senderSnapshot.getString("name") == AppConfig.ADMIN_NAME)
        val senderName = if (isSenderAdmin && !isRecipientAdmin) "Admin" else (senderSnapshot.getString("name") ?: "Someone")
        
        // Trigger notification in database so it shows in Notification Center for the recipient
        triggerNotification(
            toToken = recipientToken,
            title = "New Message from $senderName",
            body = message.text,
            type = "CHAT",
            senderId = message.senderId,
            targetUserId = message.receiverId
        )
    }

    fun getNotifications(userId: String, isAdmin: Boolean, onResult: (List<AppNotification>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.NOTIFICATIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Error fetching notifications", error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(AppNotification::class.java) ?: emptyList()
                val filtered = notifications.filter { notification ->
                    val isForMe = when {
                        notification.adminOnly -> isAdmin
                        notification.targetUserId != null -> notification.targetUserId == userId
                        notification.topic == "all" -> true
                        notification.topic == "gallery" -> true
                        notification.topic == "recipes" -> true
                        notification.topic == "traditions" -> true
                        notification.topic == "memorylane" -> true
                        notification.topic == "all_discussions" -> true
                        notification.topic == "events" -> true
                        else -> true // General broadcast
                    }
                    // Don't show notifications to the sender of the action
                    isForMe && notification.senderId != userId
                }
                onResult(filtered)
            }
    }

    suspend fun markNotificationAsRead(notificationId: String, userId: String) {
        if (notificationId.isBlank()) return
        db.runTransaction { transaction ->
            val ref = db.collection(AppConfig.Collections.NOTIFICATIONS).document(notificationId)
            val snapshot = transaction.get(ref)
            if (snapshot.exists()) {
                val readBy = snapshot.get("readBy") as? List<String> ?: emptyList()
                if (!readBy.contains(userId)) {
                    transaction.update(ref, "readBy", readBy + userId)
                }
            }
        }.await()
    }

    private suspend fun triggerNotification(
        toToken: String? = null,
        topic: String? = null,
        title: String,
        body: String,
        type: String,
        senderId: String? = null,
        senderName: String? = null,
        targetUserId: String? = null,
        relatedId: String? = null,
        adminOnly: Boolean = false,
        metadata: Map<String, String> = emptyMap()
    ) {
        val notificationId = UUID.randomUUID().toString()
        val notification = AppNotification(
            id = notificationId,
            type = type,
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            readBy = emptyList(),
            targetUserId = targetUserId,
            senderId = senderId,
            senderName = senderName,
            relatedId = relatedId,
            adminOnly = adminOnly,
            topic = topic,
            metadata = metadata
        )

        db.collection(AppConfig.Collections.NOTIFICATIONS).document(notificationId).set(notification).await()
    }

    suspend fun markChannelAsRead(channelId: String, userId: String) {
        if (channelId.isBlank()) return
        db.runTransaction { transaction ->
            val channelRef = db.collection(AppConfig.Collections.CHANNELS).document(channelId)
            val snapshot = transaction.get(channelRef)
            if (snapshot.exists()) {
                val currentUnread = (snapshot.get("unreadCount") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
                currentUnread[userId] = 0L
                transaction.update(channelRef, "unreadCount", currentUnread)
            }
        }.await()
    }

    fun getMessages(senderId: String, receiverId: String, onResult: (List<Message>) -> Unit): ListenerRegistration {
        val channelId = if (senderId < receiverId) "${senderId}_$receiverId" else "${receiverId}_$senderId"
        return db.collection(AppConfig.Collections.CHANNELS).document(channelId)
            .collection(AppConfig.Collections.MESSAGES)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                onResult(messages)
            }
    }

    fun getChannels(userId: String, onResult: (List<ChatChannel>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.CHANNELS)
            .whereArrayContains("userIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val channels = snapshot?.toObjects(ChatChannel::class.java) ?: emptyList()
                onResult(channels.sortedByDescending { it.lastTimestamp })
            }
    }

    suspend fun getMemberByPhone(phone: String): Member? {
        val snapshot = db.collection(AppConfig.Collections.MEMBERS)
            .whereEqualTo("phoneNumber", phone)
            .limit(1)
            .get()
            .await()
        return snapshot.toObjects(Member::class.java).firstOrNull()
    }

    fun getMembers(treeId: String = "primary", onResult: (List<Member>) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration {
        return db.collection(AppConfig.Collections.MEMBERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Error fetching members", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(Member::class.java).orEmpty()
                    .filter { belongsToTree(it.treeId, treeId) }
                    .sortedWith(compareBy<Member> { it.familyId }.thenBy { it.name })
                onResult(members)
            }
    }

    fun getAllMembers(onResult: (List<Member>) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration {
        return db.collection(AppConfig.Collections.MEMBERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Error fetching all members", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(Member::class.java).orEmpty()
                    .sortedWith(compareBy<Member> { it.familyId }.thenBy { it.name })
                onResult(members)
            }
    }

    suspend fun getMembersOnce(): List<Member> {
        return try {
            val snapshot = db.collection(AppConfig.Collections.MEMBERS).get().await()
            snapshot.toObjects(Member::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPendingChanges(onResult: (List<Member>) -> Unit, onError: (Exception) -> Unit = {}): ListenerRegistration {
        return db.collection(AppConfig.Collections.PENDING_UPDATES)
            .orderBy("familyId")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Error fetching pending", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(Member::class.java) ?: emptyList()
                onResult(members)
            }
    }

    suspend fun submitRelationshipOverride(override: RelationshipOverride) {
        try {
            db.collection(AppConfig.Collections.RELATIONSHIP_OVERRIDES)
                .document(override.id)
                .set(override)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to submit relationship override", e)
            throw e
        }
    }

    suspend fun updateManualRelationship(targetId: String, observerId: String, relationship: String) {
        if (targetId.isBlank()) return
        try {
            db.runTransaction { transaction ->
                val memberRef = db.collection(AppConfig.Collections.MEMBERS).document(targetId)
                val snapshot = transaction.get(memberRef)
                val member = snapshot.toObject(Member::class.java) ?: return@runTransaction
                
                val updatedManual = member.manualRelationships.toMutableMap()
                updatedManual[observerId] = relationship
                
                transaction.update(memberRef, "manualRelationships", updatedManual)
            }.await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to update manual relationship", e)
            throw e
        }
    }

    fun getRelationshipOverrides(onResult: (List<RelationshipOverride>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.RELATIONSHIP_OVERRIDES)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val overrides = snapshot?.toObjects(RelationshipOverride::class.java) ?: emptyList()
                onResult(overrides)
            }
    }

    suspend fun approveRelationshipOverride(override: RelationshipOverride) {
        try {
            db.runTransaction { transaction ->
                val memberRef = db.collection(AppConfig.Collections.MEMBERS).document(override.targetId)
                val snapshot = transaction.get(memberRef)
                val member = snapshot.toObject(Member::class.java) ?: return@runTransaction
                
                val updatedManual = member.manualRelationships.toMutableMap()
                updatedManual[override.observerId] = override.relationship
                
                transaction.update(memberRef, "manualRelationships", updatedManual)
                transaction.delete(db.collection(AppConfig.Collections.RELATIONSHIP_OVERRIDES).document(override.id))
            }.await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to approve relationship override", e)
            throw e
        }
    }

    suspend fun rejectRelationshipOverride(overrideId: String) {
        try {
            db.collection(AppConfig.Collections.RELATIONSHIP_OVERRIDES).document(overrideId).delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to reject relationship override", e)
            throw e
        }
    }

    // Gallery / Memories
    suspend fun submitMemory(memory: Memory, treeId: String) {
        val finalMemory = memory.copy(treeId = treeId)
        db.collection(AppConfig.Collections.MEMORIES).document(finalMemory.id).set(finalMemory).await()
        
        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = memory.userId,
                userName = memory.userName,
                action = "CREATE",
                targetType = "MEMORY",
                targetId = finalMemory.id,
                targetName = finalMemory.caption
            )
        )

        triggerNotification(
            topic = if (treeId == "primary") "gallery" else "gallery_$treeId",
            title = "New Gallery Post",
            body = "${if (finalMemory.userName == "Admin") "Admin" else finalMemory.userName} shared a new memory!",
            type = "GALLERY"
        )

        // Notify tagged members
        finalMemory.taggedMemberIds.forEach { memberId ->
            triggerNotification(
                topic = "user_$memberId",
                title = "You were tagged in a photo!",
                body = "${finalMemory.userName} tagged you in a new memory: ${finalMemory.caption}",
                type = "TAGGED_MEMORY"
            )
        }

        if (finalMemory.status == "APPROVED") {
            awardPoints(finalMemory.userId, 10)
        }
    }

    fun getMemories(treeId: String = "primary", onlyApproved: Boolean, onResult: (List<Memory>) -> Unit): ListenerRegistration {
        var query: Query = db.collection(AppConfig.Collections.MEMORIES)
        
        if (onlyApproved) {
            query = query.whereEqualTo("status", "APPROVED")
        }
        
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseManager", "Error fetching memories", error)
                return@addSnapshotListener
            }
            
            val memories = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    // Safe parsing for reactions to handle legacy Long counts
                    val reactionsRaw = data["reactions"] as? Map<String, Any>
                    val reactions = reactionsRaw?.mapValues { entry ->
                        when (val value = entry.value) {
                            is List<*> -> value.filterIsInstance<String>()
                            else -> emptyList<String>() // Treat Long or other types as empty list
                        }
                    } ?: emptyMap()

                    // Safe parsing for comments
                    val commentsRaw = data["comments"] as? List<Map<String, Any>>
                    val comments = commentsRaw?.map { c ->
                        Comment(
                            id = c["id"] as? String ?: "",
                            userId = c["userId"] as? String ?: "",
                            userName = c["userName"] as? String ?: "",
                            text = c["text"] as? String ?: "",
                            timestamp = (c["timestamp"] as? Long) ?: 0L
                        )
                    } ?: emptyList()

                    Memory(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        userName = data["userName"] as? String ?: "",
                        imageUrl = data["imageUrl"] as? String ?: "",
                        caption = data["caption"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                        status = data["status"] as? String ?: "PENDING",
                        reactions = reactions,
                        comments = comments,
                        taggedMemberIds = data["taggedMemberIds"] as? List<String> ?: emptyList(),
                        treeId = data["treeId"] as? String ?: "primary"
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "Error parsing memory ${doc.id}", e)
                    null
                }
            }?.filter { belongsToTree(it.treeId, treeId) } ?: emptyList()
            onResult(memories.sortedByDescending { it.timestamp })
        }
    }


    suspend fun approveMemory(memoryId: String) {
        if (memoryId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.MEMORIES).document(memoryId)
        val memory = docRef.get().await().toObject(Memory::class.java)
        docRef.update("status", "APPROVED").await()
        if (memory != null) {
            awardPoints(memory.userId, 10)
        }
    }

    suspend fun deleteMemory(memoryId: String, userId: String, userName: String) {
        if (memoryId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.MEMORIES).document(memoryId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("caption") ?: "Photo"
        
        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "MEMORY",
                targetId = memoryId,
                targetName = title
            )
        )
    }

    // Discussions
    suspend fun submitDiscussion(discussion: Discussion, treeId: String) {
        val finalDiscussion = discussion.copy(treeId = treeId)
        db.collection(AppConfig.Collections.DISCUSSIONS).document(finalDiscussion.id).set(finalDiscussion).await()
        
        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = finalDiscussion.userId,
                userName = finalDiscussion.userName,
                action = "CREATE",
                targetType = "DISCUSSION",
                targetId = finalDiscussion.id,
                targetName = finalDiscussion.title
            )
        )

        // Trigger notification
        triggerNotification(
            topic = if (treeId == "primary") "all_discussions" else "discussions_$treeId",
            title = "New Discussion",
            body = "${if (finalDiscussion.userName == "Admin") "Admin" else finalDiscussion.userName} started: ${finalDiscussion.title}",
            type = "DISCUSSION"
        )

        // Subscribe the author to the discussion for comments notifications
        FirebaseMessaging.getInstance().subscribeToTopic("discussion_${finalDiscussion.id}").await()
    }

    fun getDiscussions(treeId: String = "primary", onlyApproved: Boolean, onResult: (List<Discussion>) -> Unit): ListenerRegistration {
        var query: Query = db.collection(AppConfig.Collections.DISCUSSIONS)
        
        if (onlyApproved) {
            query = query.whereEqualTo("status", "APPROVED")
        }
        
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val discussions = snapshot?.toObjects(Discussion::class.java).orEmpty()
                .filter { belongsToTree(it.treeId, treeId) }
            onResult(discussions.sortedByDescending { it.timestamp })
        }
    }


    suspend fun approveDiscussion(discussionId: String) {
        if (discussionId.isBlank()) return
        db.collection(AppConfig.Collections.DISCUSSIONS).document(discussionId).update("status", "APPROVED").await()
    }

    suspend fun deleteDiscussion(discussionId: String, userId: String, userName: String) {
        if (discussionId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.DISCUSSIONS).document(discussionId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("title") ?: "Discussion"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "DISCUSSION",
                targetId = discussionId,
                targetName = title
            )
        )
    }

    suspend fun addDiscussionComment(discussionId: String, comment: Comment) {
        if (discussionId.isBlank()) return
        
        val docRef = db.collection(AppConfig.Collections.DISCUSSIONS).document(discussionId)
        // Use a simple update with arrayUnion instead of a transaction.
        // arrayUnion is atomic and supports offline persistence (latency persistence).
        docRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()

        val discussion = docRef.get().await().toObject(Discussion::class.java)
        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = comment.userId,
                userName = comment.userName,
                action = "COMMENT",
                targetType = "DISCUSSION",
                targetId = discussionId,
                targetName = discussion?.title,
                details = comment.text
            )
        )

        // Trigger notification
        triggerNotification(
            topic = "discussion_$discussionId",
            title = "New Comment in Discussion",
            body = "${if (comment.userName == "Admin") "Admin" else comment.userName} commented: ${comment.text}",
            type = "DISCUSSION_COMMENT"
        )

        // Subscribe the commenter to the discussion for future notifications
        FirebaseMessaging.getInstance().subscribeToTopic("discussion_$discussionId").await()
    }

    suspend fun voteInPoll(discussionId: String, optionId: String, userId: String) {
        if (discussionId.isBlank()) return
        db.runTransaction { transaction ->
            val snapshot = transaction.get(db.collection(AppConfig.Collections.DISCUSSIONS).document(discussionId))
            val discussion = snapshot.toObject(Discussion::class.java)
            if (discussion != null && discussion.pollOptions != null) {
                val updatedOptions = discussion.pollOptions.map { option ->
                    if (option.id == optionId) {
                        if (option.voterIds.contains(userId)) {
                            // Toggle off: remove vote
                            option.copy(voterIds = option.voterIds.filter { it != userId })
                        } else {
                            // Toggle on: add vote
                            option.copy(voterIds = option.voterIds + userId)
                        }
                    } else {
                        // Remove from other options to ensure single choice
                        option.copy(voterIds = option.voterIds.filter { it != userId })
                    }
                }
                transaction.update(db.collection(AppConfig.Collections.DISCUSSIONS).document(discussionId), "pollOptions", updatedOptions)
            }
        }.await()
        // Subscribe the voter to the discussion for future notifications
        FirebaseMessaging.getInstance().subscribeToTopic("discussion_$discussionId").await()
    }

    suspend fun toggleReaction(memoryId: String, emoji: String, userId: String, userName: String) {
        if (memoryId.isBlank()) return
        var targetUserId: String? = null
        var memoryTitle: String? = null
        var isAdded = false

        db.runTransaction { transaction ->
            val docRef = db.collection(AppConfig.Collections.MEMORIES).document(memoryId)
            val snapshot = transaction.get(docRef)
            val data = snapshot.data ?: return@runTransaction
            
            targetUserId = data["userId"] as? String
            memoryTitle = data["caption"] as? String ?: "Photo"

            val reactionsRaw = data["reactions"] as? Map<String, Any> ?: emptyMap()
            val currentEmojiValue = reactionsRaw[emoji]
            
            val userIds = when (currentEmojiValue) {
                is List<*> -> currentEmojiValue.filterIsInstance<String>().toMutableList()
                else -> mutableListOf<String>() // If it was a Long (legacy count) or null, start fresh
            }
            
            if (userIds.contains(userId)) {
                userIds.remove(userId)
                isAdded = false
            } else {
                userIds.add(userId)
                isAdded = true
            }
            
            val updatedReactions = reactionsRaw.toMutableMap()
            updatedReactions[emoji] = userIds
            transaction.update(docRef, "reactions", updatedReactions)

            if (isAdded) {
                transaction.set(
                    db.collection(AppConfig.Collections.ACTIVITY_LOG).document(UUID.randomUUID().toString()),
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        userName = userName,
                        action = "REACTION",
                        targetType = "MEMORY",
                        targetId = memoryId,
                        targetName = memoryTitle,
                        details = emoji
                    )
                )
            }
        }.await()

        if (isAdded && targetUserId != null && targetUserId != userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Reaction",
                body = "$userName reacted $emoji to your photo: \"$memoryTitle\"",
                type = "MEMORY_REACTION",
                senderId = userId,
                senderName = userName,
                relatedId = memoryId
            )
        }
    }

    suspend fun addComment(memoryId: String, comment: Comment) {
        if (memoryId.isBlank()) return
        var targetUserId: String? = null
        var memoryTitle: String? = null

        val docRef = db.collection(AppConfig.Collections.MEMORIES).document(memoryId)
        val snapshot = docRef.get().await()
        val memory = snapshot.toObject(Memory::class.java)
        if (memory != null) {
            targetUserId = memory.userId
            memoryTitle = memory.caption
        }

        docRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = comment.userId,
                userName = comment.userName,
                action = "COMMENT",
                targetType = "MEMORY",
                targetId = memoryId,
                targetName = memoryTitle,
                details = comment.text
            )
        )

        // Trigger notification to memory topic (others following it)
        val treeIdSuffix = if (memory?.treeId == "primary") "" else "_${memory?.treeId}"
        triggerNotification(
            topic = "memory_${memoryId}${treeIdSuffix}",
            title = "New Comment on Photo",
            body = "${if (comment.userName == "Admin") "Admin" else comment.userName} commented: ${comment.text}",
            type = "MEMORY_COMMENT",
            senderId = comment.userId,
            senderName = comment.userName,
            relatedId = memoryId
        )

        // Specifically notify the owner if they are not the commenter
        if (targetUserId != null && targetUserId != comment.userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Comment on Your Photo",
                body = "${comment.userName} commented on your photo \"${memoryTitle ?: "Photo"}\": ${comment.text}",
                type = "MEMORY_COMMENT",
                senderId = comment.userId,
                senderName = comment.userName,
                relatedId = memoryId
            )
        }

        // Subscribe to memory updates
        FirebaseMessaging.getInstance().subscribeToTopic("memory_${memoryId}${treeIdSuffix}").await()
    }

    // Cookbook
    fun getRecipes(treeId: String = "primary", onResult: (List<Recipe>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.RECIPES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val recipes = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        
                        // Handle instructions (check multiple possible keys used by different platforms)
                        val instructionsRaw = data["instructions"] ?: data["instruction"] ?: data["steps"] ?: data["method"]
                        val instructions = when (instructionsRaw) {
                            is String -> instructionsRaw
                            is List<*> -> instructionsRaw.filterIsInstance<String>().joinToString("\n")
                            else -> ""
                        }

                        // Handle ingredients as either List<String> or String
                        val ingredientsRaw = data["ingredients"]
                        val ingredients = when (ingredientsRaw) {
                            is List<*> -> ingredientsRaw.filterIsInstance<String>()
                            is String -> ingredientsRaw.split("\n").filter { it.isNotBlank() }
                            else -> emptyList<String>()
                        }

                        // Safe parsing for reactions
                        val reactionsRaw = data["reactions"] as? Map<String, Any>
                        val reactions = reactionsRaw?.mapValues { entry ->
                            when (val value = entry.value) {
                                is List<*> -> value.filterIsInstance<String>()
                                else -> emptyList<String>()
                            }
                        } ?: emptyMap()

                        // Safe parsing for comments
                        val commentsRaw = data["comments"] as? List<Map<String, Any>>
                        val comments = commentsRaw?.map { c ->
                            Comment(
                                id = c["id"] as? String ?: "",
                                userId = c["userId"] as? String ?: "",
                                userName = c["userName"] as? String ?: "",
                                text = c["text"] as? String ?: "",
                                timestamp = (c["timestamp"] as? Long) ?: 0L
                            )
                        } ?: emptyList()

                        Recipe(
                            id = doc.id,
                            title = data["title"] as? String ?: "",
                            authorId = data["authorId"] as? String ?: "",
                            authorName = data["authorName"] as? String ?: "",
                            category = data["category"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            ingredients = ingredients,
                            instructions = instructions,
                            imageUrl = data["imageUrl"] as? String ?: "",
                            reactions = reactions,
                            comments = comments,
                            timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                            treeId = data["treeId"] as? String ?: "primary"
                        )
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Error parsing recipe ${doc.id}", e)
                        null
                    }
                }?.filter { belongsToTree(it.treeId, treeId) } ?: emptyList()

                onResult(recipes.sortedByDescending { it.timestamp })
            }
    }


    suspend fun submitRecipe(recipe: Recipe, treeId: String) {
        val finalRecipe = recipe.copy(treeId = treeId)
        db.collection(AppConfig.Collections.RECIPES).document(finalRecipe.id).set(finalRecipe).await()
        
        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = finalRecipe.authorId,
                userName = finalRecipe.authorName,
                action = "CREATE",
                targetType = "RECIPE",
                targetId = finalRecipe.id,
                targetName = finalRecipe.title
            )
        )

        triggerNotification(
            topic = if (treeId == "primary") "recipes" else "recipes_$treeId",
            title = "New Recipe Added",
            body = "${if (finalRecipe.authorName == "Admin") "Admin" else finalRecipe.authorName} added a recipe: ${finalRecipe.title}",
            type = "RECIPE"
        )
        // Award points for recipe submission (20 points as per snapshot)
        awardPoints(finalRecipe.authorId, 20)
    }

    suspend fun deleteRecipe(recipeId: String, userId: String, userName: String) {
        if (recipeId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.RECIPES).document(recipeId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("title") ?: "Recipe"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "RECIPE",
                targetId = recipeId,
                targetName = title
            )
        )
    }

    suspend fun toggleRecipeReaction(recipeId: String, emoji: String, userId: String, userName: String) {
        if (recipeId.isBlank()) return
        var targetUserId: String? = null
        var recipeTitle: String? = null
        var isAdded = false

        db.runTransaction { transaction ->
            val docRef = db.collection(AppConfig.Collections.RECIPES).document(recipeId)
            val snapshot = transaction.get(docRef)
            val data = snapshot.data ?: return@runTransaction
            
            targetUserId = data["authorId"] as? String
            recipeTitle = data["title"] as? String ?: "Recipe"

            val reactionsRaw = data["reactions"] as? Map<String, Any> ?: emptyMap()
            val currentEmojiValue = reactionsRaw[emoji]
            
            val userIds = when (currentEmojiValue) {
                is List<*> -> currentEmojiValue.filterIsInstance<String>().toMutableList()
                else -> mutableListOf<String>()
            }
            
            if (userIds.contains(userId)) {
                userIds.remove(userId)
                isAdded = false
            } else {
                userIds.add(userId)
                isAdded = true
            }
            
            val updatedReactions = reactionsRaw.toMutableMap()
            updatedReactions[emoji] = userIds
            transaction.update(docRef, "reactions", updatedReactions)

            if (isAdded) {
                transaction.set(
                    db.collection(AppConfig.Collections.ACTIVITY_LOG).document(UUID.randomUUID().toString()),
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        userName = userName,
                        action = "REACTION",
                        targetType = "RECIPE",
                        targetId = recipeId,
                        targetName = recipeTitle,
                        details = emoji
                    )
                )
            }
        }.await()

        if (isAdded && targetUserId != null && targetUserId != userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Reaction",
                body = "$userName reacted $emoji to your recipe: \"$recipeTitle\"",
                type = "RECIPE_REACTION",
                senderId = userId,
                senderName = userName,
                relatedId = recipeId
            )
        }
    }

    suspend fun addRecipeComment(recipeId: String, comment: Comment) {
        if (recipeId.isBlank()) return
        var targetUserId: String? = null
        var recipeTitle: String? = null

        val docRef = db.collection(AppConfig.Collections.RECIPES).document(recipeId)
        val snapshot = docRef.get().await()
        val recipe = snapshot.toObject(Recipe::class.java)
        if (recipe != null) {
            targetUserId = recipe.authorId
            recipeTitle = recipe.title
        }

        docRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = comment.userId,
                userName = comment.userName,
                action = "COMMENT",
                targetType = "RECIPE",
                targetId = recipeId,
                targetName = recipeTitle,
                details = comment.text
            )
        )

        // Trigger notification to general recipes topic
        val treeIdSuffix = if (recipe?.treeId == "primary") "" else "_${recipe?.treeId}"
        triggerNotification(
            topic = if (recipe?.treeId == "primary") "recipes" else "recipes$treeIdSuffix",
            title = "New Comment on Recipe",
            body = "${comment.userName} commented on \"${recipeTitle ?: recipeId}\": ${comment.text}",
            type = "RECIPE_COMMENT",
            senderId = comment.userId,
            senderName = comment.userName,
            relatedId = recipeId
        )

        // Specifically notify the owner
        if (targetUserId != null && targetUserId != comment.userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Comment on Your Recipe",
                body = "${comment.userName} commented on your recipe \"$recipeTitle\": ${comment.text}",
                type = "RECIPE_COMMENT",
                senderId = comment.userId,
                senderName = comment.userName,
                relatedId = recipeId
            )
        }
    }

    // Traditions
    fun getTraditions(treeId: String = "primary", onResult: (List<Tradition>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.TRADITIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val traditions = snapshot?.toObjects(Tradition::class.java).orEmpty()
                    .filter { belongsToTree(it.treeId, treeId) }
                onResult(traditions.sortedByDescending { it.timestamp })
            }
    }


    suspend fun submitTradition(tradition: Tradition, treeId: String) {
        val finalTradition = tradition.copy(treeId = treeId)
        db.collection(AppConfig.Collections.TRADITIONS).document(finalTradition.id).set(finalTradition).await()
        
        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = finalTradition.authorId,
                userName = finalTradition.authorName,
                action = "CREATE",
                targetType = "TRADITION",
                targetId = finalTradition.id,
                targetName = finalTradition.title
            )
        )

        triggerNotification(
            topic = if (treeId == "primary") "traditions" else "traditions_$treeId",
            title = "New Tradition Shared",
            body = "${if (finalTradition.authorName == "Admin") "Admin" else finalTradition.authorName} shared: ${finalTradition.title}",
            type = "TRADITION"
        )
        // Award points for tradition submission (15 points as per snapshot)
        awardPoints(finalTradition.authorId, 15)
    }

    suspend fun deleteTradition(traditionId: String, userId: String, userName: String) {
        if (traditionId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.TRADITIONS).document(traditionId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("title") ?: "Tradition"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "TRADITION",
                targetId = traditionId,
                targetName = title
            )
        )
    }

    suspend fun toggleTraditionReaction(traditionId: String, emoji: String, userId: String, userName: String) {
        if (traditionId.isBlank()) return
        var targetUserId: String? = null
        var traditionTitle: String? = null
        var isAdded = false

        db.runTransaction { transaction ->
            val docRef = db.collection(AppConfig.Collections.TRADITIONS).document(traditionId)
            val snapshot = transaction.get(docRef)
            val data = snapshot.data ?: return@runTransaction
            
            targetUserId = data["authorId"] as? String
            traditionTitle = data["title"] as? String ?: "Tradition"

            val reactionsRaw = data["reactions"] as? Map<String, Any> ?: emptyMap()
            val currentEmojiValue = reactionsRaw[emoji]
            
            val userIds = when (currentEmojiValue) {
                is List<*> -> currentEmojiValue.filterIsInstance<String>().toMutableList()
                else -> mutableListOf<String>()
            }
            
            if (userIds.contains(userId)) {
                userIds.remove(userId)
                isAdded = false
            } else {
                userIds.add(userId)
                isAdded = true
            }
            
            val updatedReactions = reactionsRaw.toMutableMap()
            updatedReactions[emoji] = userIds
            transaction.update(docRef, "reactions", updatedReactions)

            if (isAdded) {
                transaction.set(
                    db.collection(AppConfig.Collections.ACTIVITY_LOG).document(UUID.randomUUID().toString()),
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        userName = userName,
                        action = "REACTION",
                        targetType = "TRADITION",
                        targetId = traditionId,
                        targetName = traditionTitle,
                        details = emoji
                    )
                )
            }
        }.await()

        if (isAdded && targetUserId != null && targetUserId != userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Reaction",
                body = "$userName reacted $emoji to your tradition: \"$traditionTitle\"",
                type = "TRADITION_REACTION",
                senderId = userId,
                senderName = userName,
                relatedId = traditionId
            )
        }
    }

    suspend fun addTraditionComment(traditionId: String, comment: Comment) {
        if (traditionId.isBlank()) return
        var targetUserId: String? = null
        var traditionTitle: String? = null

        val docRef = db.collection(AppConfig.Collections.TRADITIONS).document(traditionId)
        val snapshot = docRef.get().await()
        val tradition = snapshot.toObject(Tradition::class.java)
        if (tradition != null) {
            targetUserId = tradition.authorId
            traditionTitle = tradition.title
        }

        docRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = comment.userId,
                userName = comment.userName,
                action = "COMMENT",
                targetType = "TRADITION",
                targetId = traditionId,
                targetName = traditionTitle,
                details = comment.text
            )
        )

        // Trigger notification to general traditions topic
        val treeIdSuffix = if (tradition?.treeId == "primary") "" else "_${tradition?.treeId}"
        triggerNotification(
            topic = if (tradition?.treeId == "primary") "traditions" else "traditions$treeIdSuffix",
            title = "New Comment on Tradition",
            body = "${comment.userName} commented on \"${traditionTitle ?: traditionId}\": ${comment.text}",
            type = "TRADITION_COMMENT",
            senderId = comment.userId,
            senderName = comment.userName,
            relatedId = traditionId
        )

        // Specifically notify the owner
        if (targetUserId != null && targetUserId != comment.userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Comment on Your Tradition",
                body = "${comment.userName} commented on your tradition \"$traditionTitle\": ${comment.text}",
                type = "TRADITION_COMMENT",
                senderId = comment.userId,
                senderName = comment.userName,
                relatedId = traditionId
            )
        }
    }

    // Memory Lane
    fun getMilestones(treeId: String = "primary", onResult: (List<Milestone>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.MEMORY_LANE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Error fetching milestones", error)
                    return@addSnapshotListener
                }
                val milestones = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        
                        // Safe parsing for reactions
                        val reactionsRaw = data["reactions"] as? Map<String, Any>
                        val reactions = reactionsRaw?.mapValues { entry ->
                            when (val value = entry.value) {
                                is List<*> -> value.filterIsInstance<String>()
                                else -> emptyList<String>()
                            }
                        } ?: emptyMap()

                        // Safe parsing for comments
                        val commentsRaw = data["comments"] as? List<Map<String, Any>>
                        val comments = commentsRaw?.map { c ->
                            Comment(
                                id = c["id"] as? String ?: "",
                                userId = c["userId"] as? String ?: "",
                                userName = c["userName"] as? String ?: "",
                                text = c["text"] as? String ?: "",
                                timestamp = (c["timestamp"] as? Long) ?: 0L
                            )
                        } ?: emptyList()

                        Milestone(
                            id = doc.id,
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            year = data["year"] as? String ?: "",
                            imageUrl = data["imageUrl"] as? String ?: "",
                            audioUrl = data["audioUrl"] as? String ?: "",
                            location = data["location"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                            authorId = data["authorId"] as? String ?: "",
                            authorName = data["authorName"] as? String ?: "",
                            visibilityType = data["visibilityType"] as? String ?: "GLOBAL",
                            familyContextId = data["familyContextId"] as? String ?: "",
                            reactions = reactions,
                            comments = comments,
                            taggedMemberIds = data["taggedMemberIds"] as? List<String> ?: emptyList(),
                            treeId = data["treeId"] as? String ?: "primary"
                        )
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Error parsing milestone ${doc.id}", e)
                        null
                    }
                }?.filter { belongsToTree(it.treeId, treeId) } ?: emptyList()
                onResult(milestones.sortedBy { it.year })
            }
    }


    suspend fun submitMilestone(milestone: Milestone, treeId: String) {
        val finalMilestone = milestone.copy(treeId = treeId)
        db.collection(AppConfig.Collections.MEMORY_LANE).document(finalMilestone.id).set(finalMilestone).await()
        
        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = finalMilestone.authorId,
                userName = finalMilestone.authorName,
                action = "CREATE",
                targetType = "MILESTONE",
                targetId = finalMilestone.id,
                targetName = finalMilestone.title
            )
        )

        triggerNotification(
            topic = if (treeId == "primary") "memorylane" else "memorylane_$treeId",
            title = "New Milestone Shared",
            body = "${if (finalMilestone.authorName == "Admin") "Admin" else finalMilestone.authorName} shared: ${finalMilestone.title}",
            type = "MILESTONE"
        )

        // Notify tagged members
        finalMilestone.taggedMemberIds.forEach { memberId ->
            triggerNotification(
                topic = "user_$memberId",
                title = "You were tagged!",
                body = "${finalMilestone.authorName} tagged you in a new milestone: ${finalMilestone.title}",
                type = "TAGGED_MILESTONE"
            )
        }
        // Award points for milestone submission (25 points as per snapshot)
        awardPoints(finalMilestone.authorId, 25)
    }

    suspend fun toggleMilestoneReaction(milestoneId: String, emoji: String, userId: String, userName: String) {
        if (milestoneId.isBlank()) return
        var targetUserId: String? = null
        var milestoneTitle: String? = null
        var isAdded = false

        db.runTransaction { transaction ->
            val docRef = db.collection(AppConfig.Collections.MEMORY_LANE).document(milestoneId)
            val snapshot = transaction.get(docRef)
            val data = snapshot.data ?: return@runTransaction
            
            targetUserId = data["authorId"] as? String
            milestoneTitle = data["title"] as? String ?: "Milestone"

            val reactionsRaw = data["reactions"] as? Map<String, Any> ?: emptyMap()
            val currentEmojiValue = reactionsRaw[emoji]
            
            val userIds = when (currentEmojiValue) {
                is List<*> -> currentEmojiValue.filterIsInstance<String>().toMutableList()
                else -> mutableListOf<String>()
            }
            
            if (userIds.contains(userId)) {
                userIds.remove(userId)
                isAdded = false
            } else {
                userIds.add(userId)
                isAdded = true
            }
            
            val updatedReactions = reactionsRaw.toMutableMap()
            updatedReactions[emoji] = userIds
            transaction.update(docRef, "reactions", updatedReactions)

            if (isAdded) {
                transaction.set(
                    db.collection(AppConfig.Collections.ACTIVITY_LOG).document(UUID.randomUUID().toString()),
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        userName = userName,
                        action = "REACTION",
                        targetType = "MILESTONE",
                        targetId = milestoneId,
                        targetName = milestoneTitle,
                        details = emoji
                    )
                )
            }
        }.await()

        if (isAdded && targetUserId != null && targetUserId != userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Reaction",
                body = "$userName reacted $emoji to your milestone: \"$milestoneTitle\"",
                type = "MILESTONE_REACTION",
                senderId = userId,
                senderName = userName,
                relatedId = milestoneId
            )
        }
    }

    suspend fun addMilestoneComment(milestoneId: String, comment: Comment) {
        if (milestoneId.isBlank()) return
        var targetUserId: String? = null
        var milestoneTitle: String? = null

        val docRef = db.collection(AppConfig.Collections.MEMORY_LANE).document(milestoneId)
        val snapshot = docRef.get().await()
        val data = snapshot.data
        if (data != null) {
            targetUserId = data["authorId"] as? String
            milestoneTitle = data["title"] as? String
        }

        docRef.update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = comment.userId,
                userName = comment.userName,
                action = "COMMENT",
                targetType = "MILESTONE",
                targetId = milestoneId,
                targetName = milestoneTitle,
                details = comment.text
            )
        )

        // Trigger notification to general topic
        val treeIdSuffix = if (data?.get("treeId") == "primary") "" else "_${data?.get("treeId")}"
        triggerNotification(
            topic = if (data?.get("treeId") == "primary") "memorylane" else "memorylane$treeIdSuffix",
            title = "New Comment on Milestone",
            body = "${comment.userName} commented on \"${milestoneTitle ?: "Milestone"}\": ${comment.text}",
            type = "MILESTONE_COMMENT",
            senderId = comment.userId,
            senderName = comment.userName,
            relatedId = milestoneId
        )

        // Specifically notify the owner
        if (targetUserId != null && targetUserId != comment.userId) {
            triggerNotification(
                topic = "user_$targetUserId",
                title = "New Comment on Your Milestone",
                body = "${comment.userName} commented on your milestone \"$milestoneTitle\": ${comment.text}",
                type = "MILESTONE_COMMENT",
                senderId = comment.userId,
                senderName = comment.userName,
                relatedId = milestoneId
            )
        }
    }

    suspend fun deleteMilestone(milestoneId: String, userId: String, userName: String) {
        if (milestoneId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.MEMORY_LANE).document(milestoneId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("title") ?: "Milestone"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "MILESTONE",
                targetId = milestoneId,
                targetName = title
            )
        )
    }

    // Calendar Events
    suspend fun submitCalendarEvent(event: CalendarEvent, treeId: String) {
        val eventId = if (event.id.isEmpty()) UUID.randomUUID().toString() else event.id
        val finalEvent = event.copy(id = eventId, treeId = treeId)
        db.collection(AppConfig.Collections.CALENDAR_EVENTS).document(eventId).set(finalEvent).await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = "Admin", // Calendar events are usually admin-driven in this app context
                userName = "Admin",
                action = "CREATE",
                targetType = "EVENT",
                targetId = eventId,
                targetName = event.title
            )
        )

        triggerNotification(
            topic = if (treeId == "primary") "events" else "events_$treeId",
            title = "New Family Event: ${event.title}",
            body = "${event.type} on ${event.date}${if (event.location.isNotEmpty()) " at ${event.location}" else ""}",
            type = "CALENDAR_EVENT",
            relatedId = eventId
        )
    }

    fun getCalendarEvents(treeId: String = "primary", onResult: (List<CalendarEvent>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.CALENDAR_EVENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val events = snapshot?.toObjects(CalendarEvent::class.java).orEmpty()
                    .filter { belongsToTree(it.treeId, treeId) }
                onResult(events)
            }
    }

    suspend fun deleteCalendarEvent(eventId: String, userId: String, userName: String) {
        if (eventId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.CALENDAR_EVENTS).document(eventId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("title") ?: "Event"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "EVENT",
                targetId = eventId,
                targetName = title
            )
        )
    }

    // Deletion Requests
    suspend fun submitDeletionRequest(request: DeletionRequest) {
        db.collection(AppConfig.Collections.DELETION_REQUESTS).document(request.id).set(request).await()
    }

    fun getDeletionRequests(onResult: (List<DeletionRequest>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.DELETION_REQUESTS)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val requests = snapshot?.toObjects(DeletionRequest::class.java) ?: emptyList()
                onResult(requests.sortedByDescending { it.timestamp })
            }
    }


    suspend fun deleteDeletionRequest(requestId: String) {
        if (requestId.isBlank()) return
        db.collection(AppConfig.Collections.DELETION_REQUESTS).document(requestId).delete().await()
    }

    suspend fun awardPoints(userId: String, points: Int) {
        if (userId.isBlank()) return
        try {
            val userRef = db.collection("members").document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("points") ?: 0
                val newPoints = currentPoints + points
                val newLevel = (newPoints / 100).toInt() + 1
                transaction.update(userRef, "points", newPoints)
                transaction.update(userRef, "level", newLevel)
            }.await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error awarding points", e)
            throw e
        }
    }

    suspend fun submitTriviaScore(userId: String, score: Int) {
        val points = score * 10
        awardPoints(userId, points)
    }

    suspend fun completeGame(sessionId: String, winnerId: String, gameType: String) {
        if (sessionId.isBlank()) return
        try {
            val points = when (gameType.uppercase()) {
                "SNAKES_LADDERS" -> 50
                "CHESS" -> 100
                "CHAUPAD" -> 75
                "HANGMAN" -> 30
                "RUMMY" -> 80
                else -> 0
            }
            
            if (winnerId.isNotBlank() && points > 0) {
                awardPoints(winnerId, points)
            }

            db.collection(AppConfig.Collections.GAME_SESSIONS).document(sessionId).update(
                mapOf(
                    "winnerId" to winnerId,
                    "status" to "FINISHED",
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error completing game", e)
            throw e
        }
    }

    suspend fun getTriviaQuestions(): List<TriviaQuestion> {
        return try {
            db.collection("trivia")
                .get()
                .await()
                .toObjects(TriviaQuestion::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching trivia questions", e)
            emptyList()
        }
    }

    suspend fun submitTriviaQuestion(question: TriviaQuestion) {
        try {
            val ref = db.collection("trivia").document()
            val finalQuestion = if (question.id.isBlank()) question.copy(id = ref.id) else question
            ref.set(finalQuestion).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error submitting trivia question", e)
            throw e
        }
    }

    // Multiplayer Games
    suspend fun createGameSession(gameType: String, player: Member): String {
        val sessionId = UUID.randomUUID().toString()
        val session = GameSession(
            id = sessionId,
            gameType = gameType,
            players = listOf(player.id),
            playerNames = mapOf(player.id to player.name),
            status = "WAITING",
            currentTurn = player.id,
            lastUpdated = System.currentTimeMillis()
        )
        
        // Pre-initialize basic state if needed (e.g. for Rummy to show waiting screen with partial state)
        val initialGameState = when (gameType) {
            "RUMMY" -> mapOf("deckCount" to 52, "discardPile" to emptyList<String>())
            "ANTAKSHARI" -> mapOf("recordings" to emptyList<Map<String, String>>(), "lastLetter" to "")
            else -> emptyMap<String, Any>()
        }

        db.collection(AppConfig.Collections.GAME_SESSIONS).document(sessionId).set(
            session.copy(gameState = initialGameState)
        ).await()

        // Trigger challenge notification to all
        val gameName = when(gameType) {
            "CHESS" -> "Chess"
            "SNAKES_LADDERS" -> "Snakes & Ladders"
            "CHAUPAD" -> "Chaupad"
            "HANGMAN" -> "Hangman"
            "RUMMY" -> "Rummy"
            "ANTAKSHARI" -> "Antakshari"
            else -> "a game"
        }
        triggerNotification(
            topic = "all",
            title = "New Game Challenge!",
            body = "${player.name} is waiting in the $gameName lobby. Join now!",
            type = "GAME_CHALLENGE",
            senderId = player.id,
            senderName = player.name,
            relatedId = sessionId,
            metadata = mapOf("gameType" to gameType)
        )

        return sessionId
    }


    suspend fun joinGameSession(sessionId: String, player: Member) {
        db.runTransaction { transaction ->
            val ref = db.collection(AppConfig.Collections.GAME_SESSIONS).document(sessionId)
            val snapshot = transaction.get(ref)
            val session = snapshot.toObject(GameSession::class.java) ?: return@runTransaction
            
            if (session.players.size < 2 && !session.players.contains(player.id)) {
                val newPlayers = session.players + player.id
                val newPlayerNames = session.playerNames + (player.id to player.name)
                transaction.update(ref, "players", newPlayers)
                transaction.update(ref, "playerNames", newPlayerNames)
                transaction.update(ref, "status", "ACTIVE")
                transaction.update(ref, "lastUpdated", System.currentTimeMillis())

                // Initialize game state for Rummy
                if (session.gameType == "RUMMY") {
                    val deck = (Suit.entries.flatMap { suit ->
                        listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K").map { rank ->
                            "$rank${suit.symbol}"
                        }
                    }).shuffled().toMutableList()
                    
                    val hand1 = mutableListOf<String>()
                    val hand2 = mutableListOf<String>()
                    repeat(10) {
                        if (deck.isNotEmpty()) hand1.add(deck.removeAt(0))
                        if (deck.isNotEmpty()) hand2.add(deck.removeAt(0))
                    }
                    
                    val discardPile = mutableListOf<String>()
                    if (deck.isNotEmpty()) discardPile.add(deck.removeAt(0))
                    
                    val gameState = mapOf(
                        "deck" to deck,
                        "deckCount" to deck.size,
                        "discardPile" to discardPile,
                        "hand_${session.players[0]}" to hand1,
                        "hand_${player.id}" to hand2
                    )
                    transaction.update(ref, "gameState", gameState)
                } else if (session.gameType == "HANGMAN") {
                    val categories = mapOf(
                        "ANIMALS" to listOf("ELEPHANT", "GIRAFFE", "KANGAROO", "PANDA", "LEOPARD", "TIGER", "CHEETAH", "CHIMPANZEE", "RHINOCEROS", "PLATYPUS", "HAMSTER", "IGUANA"),
                        "MOVIES" to listOf("INCEPTION", "AVATAR", "TITANIC", "GLADIATOR", "JOKER", "SHOLAY", "DANGAL", "INTERSTELLAR", "BAHUBALI", "LAGAAN", "PARASITE", "HAMILTON"),
                        "COUNTRIES" to listOf("INDIA", "BRAZIL", "CANADA", "GERMANY", "JAPAN", "FRANCE", "AUSTRALIA", "ARGENTINA", "EGYPT", "THAILAND", "NORWAY", "MEXICO"),
                        "FRUITS" to listOf("ORANGE", "BANANA", "CHERRY", "MANGO", "PINEAPPLE", "WATERMELON", "POMEGRANATE", "KIWI", "AVOCADO", "STRAWBERRY", "GUAVA"),
                        "BIRDS" to listOf("PEACOCK", "PARROT", "EAGLE", "SPARROW", "PENGUIN", "FLAMINGO", "WOODPECKER", "HUMMINGBIRD", "OSTRICH", "KINGFISHER"),
                        "PROFESSIONS" to listOf("ENGINEER", "DOCTOR", "ASTRONAUT", "TEACHER", "CHEF", "FARMER", "PILOT", "SCIENTIST", "LAWYER", "ARTIST"),
                        "SPORTS" to listOf("CRICKET", "FOOTBALL", "BASKETBALL", "TENNIS", "HOCKEY", "BADMINTON", "VOLLEYBALL", "KABADDI", "CHESS")
                    )
                    val category = categories.keys.random()
                    val word = categories[category]!!.random().uppercase()
                    
                    val gameState = mapOf(
                        "word" to word,
                        "category" to category,
                        "guessedLetters" to emptyList<String>()
                    )
                    transaction.update(ref, "gameState", gameState)
                }
            }
        }.await()
    }

    suspend fun updateGameState(sessionId: String, newState: Map<String, Any>, nextTurnId: String?, winnerId: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "gameState" to newState,
            "lastUpdated" to System.currentTimeMillis()
        )
        if (nextTurnId != null) updates["currentTurn"] = nextTurnId
        
        if (winnerId != null) {
            updates["winnerId"] = winnerId
            updates["status"] = "FINISHED"
        } else if (nextTurnId == null || nextTurnId == "") {
            updates["status"] = "FINISHED"
        }
        
        db.collection(AppConfig.Collections.GAME_SESSIONS).document(sessionId).update(updates).await()
    }

    fun getGameSession(sessionId: String, onResult: (GameSession?) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.GAME_SESSIONS).document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                onResult(snapshot?.toObject(GameSession::class.java))
            }
    }

    fun getActiveGameSessions(onResult: (List<GameSession>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.GAME_SESSIONS)
            .whereEqualTo("status", "WAITING")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val sessions = snapshot?.toObjects(GameSession::class.java) ?: emptyList()
                onResult(sessions)
            }
    }

    // Business Directory
    fun getBusinesses(treeId: String = "primary", onResult: (List<Business>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.BUSINESSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val businesses = snapshot?.toObjects(Business::class.java).orEmpty()
                    .filter { belongsToTree(it.treeId, treeId) }
                    .sortedByDescending { it.timestamp }
                onResult(businesses)
            }
    }

    suspend fun submitBusiness(business: Business, treeId: String) {
        val id = if (business.id.isEmpty()) UUID.randomUUID().toString() else business.id
        val finalBusiness = business.copy(id = id, treeId = treeId)
        db.collection(AppConfig.Collections.BUSINESSES).document(id).set(finalBusiness).await()
        
        triggerNotification(
            topic = if (treeId == "primary") "business" else "business_$treeId",
            title = "New Business Added",
            body = "${finalBusiness.ownerName} added their business: ${finalBusiness.name}",
            type = "BUSINESS"
        )
        awardPoints(business.addedBy, 15)
    }

    suspend fun deleteBusiness(businessId: String, userId: String, userName: String) {
        if (businessId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.BUSINESSES).document(businessId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("name") ?: "Business"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "BUSINESS",
                targetId = businessId,
                targetName = title
            )
        )
    }

    // Achievements
    fun getAchievements(treeId: String = "primary", onResult: (List<Achievement>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.ACHIEVEMENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val achievements = snapshot?.toObjects(Achievement::class.java).orEmpty()
                    .filter { belongsToTree(it.treeId, treeId) }
                    .sortedByDescending { it.timestamp }
                onResult(achievements)
            }
    }

    suspend fun submitAchievement(achievement: Achievement, treeId: String) {
        val id = if (achievement.id.isEmpty()) UUID.randomUUID().toString() else achievement.id
        val finalAchievement = achievement.copy(id = id, treeId = treeId)
        db.collection(AppConfig.Collections.ACHIEVEMENTS).document(id).set(finalAchievement).await()

        triggerNotification(
            topic = if (treeId == "primary") "achievements" else "achievements_$treeId",
            title = "New Achievement Shared!",
            body = "${finalAchievement.memberName} achieved: ${finalAchievement.title}",
            type = "ACHIEVEMENT"
        )
        awardPoints(finalAchievement.addedBy, 30)
    }

    suspend fun deleteAchievement(achievementId: String, userId: String, userName: String) {
        if (achievementId.isBlank()) return
        val docRef = db.collection(AppConfig.Collections.ACHIEVEMENTS).document(achievementId)
        val snapshot = docRef.get().await()
        val title = snapshot.getString("title") ?: "Achievement"

        docRef.delete().await()

        logActivity(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = "DELETE",
                targetType = "ACHIEVEMENT",
                targetId = achievementId,
                targetName = title
            )
        )
    }

    private suspend fun logActivity(log: ActivityLog) {
        try {
            val id = if (log.id.isEmpty()) UUID.randomUUID().toString() else log.id
            db.collection(AppConfig.Collections.ACTIVITY_LOG).document(id).set(log.copy(id = id)).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to log activity", e)
        }
    }

    fun getActivityLogs(onResult: (List<ActivityLog>) -> Unit): ListenerRegistration {
        return db.collection(AppConfig.Collections.ACTIVITY_LOG)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val logs = snapshot?.toObjects(ActivityLog::class.java) ?: emptyList()
                onResult(logs)
            }
    }
}
