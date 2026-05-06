package com.example.circlebirthdays

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
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

    suspend fun uploadPhoto(uri: Uri): String {
        val fileName = "photos/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun submitChange(member: Member, isApproved: Boolean) {
        try {
            val collection = if (isApproved) "members" else "pending_updates"
            val status = if (isApproved) "APPROVED" else "PENDING"
            val finalMember = member.copy(status = status)
            
            android.util.Log.d("FirebaseManager", "Submitting to $collection: ${finalMember.name}")
            db.collection(collection)
                .document(finalMember.id)
                .set(finalMember)
                .await()
            android.util.Log.d("FirebaseManager", "Successfully submitted: ${finalMember.name}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to submit change for ${member.name}", e)
            throw e
        }
    }

    suspend fun submitBatch(members: List<Member>, isApproved: Boolean) {
        try {
            val collection = if (isApproved) "members" else "pending_updates"
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
            android.util.Log.d("FirebaseManager", "Successfully submitted batch of ${members.size}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to submit batch", e)
            throw e
        }
    }

    suspend fun approveChange(pendingMember: Member) {
        try {
            db.runTransaction { transaction ->
                val memberRef = db.collection("members").document(pendingMember.id)
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
                transaction.delete(db.collection("pending_updates").document(pendingMember.id))
            }.await()
            android.util.Log.d("FirebaseManager", "Approved change for ${pendingMember.name}")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to approve change", e)
            throw e
        }
    }

    suspend fun clearAll() {
        try {
            val collections = listOf("members", "pending_updates", "memories", "discussions", "channels", "relationship_overrides")
            for (collectionName in collections) {
                val snapshot = db.collection(collectionName).get().await()
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                    // If it's a channel, we also need to delete messages subcollection
                    if (collectionName == "channels") {
                        val messages = doc.reference.collection("messages").get().await()
                        messages.documents.forEach { msg ->
                            batch.delete(msg.reference)
                        }
                    }
                }
                batch.commit().await()
            }
            android.util.Log.d("FirebaseManager", "Successfully cleared all database collections")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to clear database", e)
            throw e
        }
    }

    suspend fun updatePassword(memberId: String, hashedPw: String) {
        try {
            db.collection("members").document(memberId)
                .update("password", hashedPw)
                .await()
            android.util.Log.d("FirebaseManager", "Password updated for $memberId")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to update password", e)
            throw e
        }
    }

    suspend fun updateLastLoggedIn(memberId: String) {
        try {
            db.collection("members").document(memberId)
                .update("lastLoggedIn", System.currentTimeMillis())
                .await()
            
            // Also refresh FCM token
            FirebaseMessaging.getInstance().token.await()?.let { token ->
                db.collection("members").document(memberId).update("fcmToken", token).await()
            }
            android.util.Log.d("FirebaseManager", "Last logged in and token updated for $memberId")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to update last logged in", e)
        }
    }

    // Messaging
    suspend fun sendMessage(message: Message) {
        val channelId = if (message.senderId < message.receiverId) {
            "${message.senderId}_${message.receiverId}"
        } else {
            "${message.receiverId}_${message.senderId}"
        }

        val messageRef = db.collection("channels").document(channelId)
            .collection("messages").document()
        
        val messageWithId = message.copy(id = messageRef.id)
        
        db.runTransaction { transaction ->
            val channelRef = db.collection("channels").document(channelId)
            val channelSnapshot = transaction.get(channelRef)
            
            transaction.set(messageRef, messageWithId)

            val currentUnread = (channelSnapshot.get("unreadCount") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
            
            // Increment unread count for the receiver
            val receiverCount = currentUnread[message.receiverId] ?: 0L
            currentUnread[message.receiverId] = receiverCount + 1
            
            val channelData = mapOf(
                "id" to channelId,
                "userIds" to listOf(message.senderId, message.receiverId),
                "lastMessage" to message.text,
                "lastTimestamp" to message.timestamp,
                "unreadCount" to currentUnread
            )
            transaction.set(channelRef, channelData, com.google.firebase.firestore.SetOptions.merge())
        }.await()
    }

    suspend fun markChannelAsRead(channelId: String, userId: String) {
        db.runTransaction { transaction ->
            val channelRef = db.collection("channels").document(channelId)
            val snapshot = transaction.get(channelRef)
            if (snapshot.exists()) {
                val currentUnread = (snapshot.get("unreadCount") as? Map<String, Long>)?.toMutableMap() ?: mutableMapOf()
                currentUnread[userId] = 0L
                transaction.update(channelRef, "unreadCount", currentUnread)
            }
        }.await()
    }

    fun getMessages(senderId: String, receiverId: String, onResult: (List<Message>) -> Unit) {
        val channelId = if (senderId < receiverId) "${senderId}_$receiverId" else "${receiverId}_$senderId"
        db.collection("channels").document(channelId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                onResult(messages)
            }
    }

    fun getChannels(userId: String, onResult: (List<ChatChannel>) -> Unit) {
        db.collection("channels")
            .whereArrayContains("userIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val channels = snapshot?.toObjects(ChatChannel::class.java) ?: emptyList()
                onResult(channels.sortedByDescending { it.lastTimestamp })
            }
    }

    fun getMembers(onResult: (List<Member>) -> Unit, onError: (Exception) -> Unit = {}) {
        db.collection("members")
            .orderBy("familyId")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseManager", "Error fetching members", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(Member::class.java) ?: emptyList()
                onResult(members)
            }
    }

    fun getPendingChanges(onResult: (List<Member>) -> Unit, onError: (Exception) -> Unit = {}) {
        db.collection("pending_updates")
            .orderBy("familyId")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseManager", "Error fetching pending", error)
                    onError(error)
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(Member::class.java) ?: emptyList()
                onResult(members)
            }
    }

    suspend fun submitRelationshipOverride(override: RelationshipOverride) {
        try {
            db.collection("relationship_overrides")
                .document(override.id)
                .set(override)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to submit relationship override", e)
            throw e
        }
    }

    suspend fun updateManualRelationship(targetId: String, observerId: String, relationship: String) {
        try {
            db.runTransaction { transaction ->
                val memberRef = db.collection("members").document(targetId)
                val snapshot = transaction.get(memberRef)
                val member = snapshot.toObject(Member::class.java) ?: return@runTransaction
                
                val updatedManual = member.manualRelationships.toMutableMap()
                updatedManual[observerId] = relationship
                
                transaction.update(memberRef, "manualRelationships", updatedManual)
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to update manual relationship", e)
            throw e
        }
    }

    fun getRelationshipOverrides(onResult: (List<RelationshipOverride>) -> Unit) {
        db.collection("relationship_overrides")
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
                val memberRef = db.collection("members").document(override.targetId)
                val snapshot = transaction.get(memberRef)
                val member = snapshot.toObject(Member::class.java) ?: return@runTransaction
                
                val updatedManual = member.manualRelationships.toMutableMap()
                updatedManual[override.observerId] = override.relationship
                
                transaction.update(memberRef, "manualRelationships", updatedManual)
                transaction.delete(db.collection("relationship_overrides").document(override.id))
            }.await()
            
            // Note: Cloud Functions should ideally handle the actual FCM sending based on the 
            // document deletion/update, but we would trigger it here if we had a dedicated 
            // notification service endpoint.
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Failed to approve relationship override", e)
            throw e
        }
    }

    // Gallery / Memories
    suspend fun submitMemory(memory: Memory) {
        db.collection("memories").document(memory.id).set(memory).await()
    }

    fun getMemories(onlyApproved: Boolean, onResult: (List<Memory>) -> Unit) {
        val query = if (onlyApproved) {
            db.collection("memories").whereEqualTo("status", "APPROVED")
        } else {
            db.collection("memories")
        }
        
        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseManager", "Error fetching memories", error)
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
                        comments = comments
                    )
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseManager", "Error parsing memory ${doc.id}", e)
                    null
                }
            } ?: emptyList()
            onResult(memories.sortedByDescending { it.timestamp })
        }
    }

    suspend fun approveMemory(memoryId: String) {
        db.collection("memories").document(memoryId).update("status", "APPROVED").await()
    }

    suspend fun deleteMemory(memoryId: String) {
        db.collection("memories").document(memoryId).delete().await()
    }

    // Discussions
    suspend fun submitDiscussion(discussion: Discussion) {
        db.collection("discussions").document(discussion.id).set(discussion).await()
        // Subscribe the author to the discussion for comments notifications
        FirebaseMessaging.getInstance().subscribeToTopic("discussion_${discussion.id}").await()
    }

    fun getDiscussions(onlyApproved: Boolean, onResult: (List<Discussion>) -> Unit) {
        val query = if (onlyApproved) {
            db.collection("discussions").whereEqualTo("status", "APPROVED")
        } else {
            db.collection("discussions")
        }
        
        query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val discussions = snapshot?.toObjects(Discussion::class.java) ?: emptyList()
            onResult(discussions.sortedByDescending { it.timestamp })
        }
    }

    suspend fun approveDiscussion(discussionId: String) {
        db.collection("discussions").document(discussionId).update("status", "APPROVED").await()
    }

    suspend fun deleteDiscussion(discussionId: String) {
        db.collection("discussions").document(discussionId).delete().await()
    }

    suspend fun addDiscussionComment(discussionId: String, comment: Comment) {
        db.runTransaction { transaction ->
            val snapshot = transaction.get(db.collection("discussions").document(discussionId))
            val discussion = snapshot.toObject(Discussion::class.java)
            if (discussion != null) {
                val currentComments = discussion.comments.toMutableList()
                currentComments.add(comment)
                transaction.update(db.collection("discussions").document(discussionId), "comments", currentComments)
            }
        }.await()
        // Subscribe the commenter to the discussion for future notifications
        FirebaseMessaging.getInstance().subscribeToTopic("discussion_$discussionId").await()
    }

    suspend fun voteInPoll(discussionId: String, optionId: String, userId: String) {
        db.runTransaction { transaction ->
            val snapshot = transaction.get(db.collection("discussions").document(discussionId))
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
                transaction.update(db.collection("discussions").document(discussionId), "pollOptions", updatedOptions)
            }
        }.await()
        // Subscribe the voter to the discussion for future notifications
        FirebaseMessaging.getInstance().subscribeToTopic("discussion_$discussionId").await()
    }

    suspend fun toggleReaction(memoryId: String, emoji: String, userId: String) {
        db.runTransaction { transaction ->
            val docRef = db.collection("memories").document(memoryId)
            val snapshot = transaction.get(docRef)
            val data = snapshot.data ?: return@runTransaction
            
            val reactionsRaw = data["reactions"] as? Map<String, Any> ?: emptyMap()
            val currentEmojiValue = reactionsRaw[emoji]
            
            val userIds = when (currentEmojiValue) {
                is List<*> -> currentEmojiValue.filterIsInstance<String>().toMutableList()
                else -> mutableListOf<String>() // If it was a Long (legacy count) or null, start fresh
            }
            
            if (userIds.contains(userId)) {
                userIds.remove(userId)
            } else {
                userIds.add(userId)
            }
            
            val updatedReactions = reactionsRaw.toMutableMap()
            updatedReactions[emoji] = userIds
            transaction.update(docRef, "reactions", updatedReactions)
        }.await()
    }

    suspend fun addComment(memoryId: String, comment: Comment) {
        db.runTransaction { transaction ->
            val snapshot = transaction.get(db.collection("memories").document(memoryId))
            val memory = snapshot.toObject(Memory::class.java)
            if (memory != null) {
                val currentComments = memory.comments.toMutableList()
                currentComments.add(comment)
                transaction.update(db.collection("memories").document(memoryId), "comments", currentComments)
            }
        }.await()
        // Subscribe to memory updates
        FirebaseMessaging.getInstance().subscribeToTopic("memory_$memoryId").await()
    }
}
