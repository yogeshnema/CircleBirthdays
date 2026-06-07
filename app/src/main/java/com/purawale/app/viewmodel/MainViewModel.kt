package com.purawale.app.viewmodel

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import com.purawale.app.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    var members by mutableStateOf<List<Member>>(emptyList())
    var allMembers by mutableStateOf<List<Member>>(emptyList())
    var pendingMembers by mutableStateOf<List<Member>>(emptyList())
    var memories by mutableStateOf<List<Memory>>(emptyList())
    var discussions by mutableStateOf<List<Discussion>>(emptyList())
    var recipes by mutableStateOf<List<Recipe>>(emptyList())
    var traditions by mutableStateOf<List<Tradition>>(emptyList())
    var milestones by mutableStateOf<List<Milestone>>(emptyList())
    var deletionRequests by mutableStateOf<List<DeletionRequest>>(emptyList())
    var channels by mutableStateOf<List<ChatChannel>>(emptyList())
    var chatMessages by mutableStateOf<List<Message>>(emptyList())
    var relationshipOverrides by mutableStateOf<List<RelationshipOverride>>(emptyList())
    var activeGames by mutableStateOf<List<GameSession>>(emptyList())
    var currentGameSession by mutableStateOf<GameSession?>(null)
    var triviaQuestions by mutableStateOf<List<TriviaQuestion>>(emptyList())
    var businesses by mutableStateOf<List<Business>>(emptyList())
    var achievements by mutableStateOf<List<Achievement>>(emptyList())
    var activityLogs by mutableStateOf<List<ActivityLog>>(emptyList())
    
    // AI Card Generator Cache
    var aiDesignCache by mutableStateOf<Map<String, Any>>(emptyMap())
    
    var currentUser by mutableStateOf<Member?>(null)
    var loginError by mutableStateOf<String?>(null)
    var currentScreen by mutableStateOf<Screen>(Screen.Login)
    var isHindi by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var selectedMemoryForFullScreen by mutableStateOf<Memory?>(null)

    private val listeners = mutableListOf<ListenerRegistration>()
    private val userListeners = mutableListOf<ListenerRegistration>()
    private var messageListener: ListenerRegistration? = null
    private var memoriesListener: ListenerRegistration? = null
    private var discussionsListener: ListenerRegistration? = null

    private fun normalizedTreeId(treeId: String?): String {
        return treeId?.takeIf { it.isNotBlank() } ?: "primary"
    }

    private fun findLoginMember(phone: String): Member? {
        return allMembers.find { it.phoneNumber == phone } ?: members.find { it.phoneNumber == phone }
    }

    fun init(prefs: android.content.SharedPreferences) {
        isHindi = prefs.getBoolean("is_hindi", false)
        currentTreeId = "primary"
        startSync(prefs)
    }

    fun toggleLanguage() {
        isHindi = !isHindi
    }

    fun toggleLanguage(prefs: android.content.SharedPreferences) {
        isHindi = !isHindi
        prefs.edit { putBoolean("is_hindi", isHindi) }
    }

    var currentTreeId by mutableStateOf("primary")
        private set

    private fun startSync(prefs: android.content.SharedPreferences) {
        listeners.forEach { it.remove() }
        listeners.clear()
        
        memoriesListener?.remove()
        discussionsListener?.remove()

        listeners += FirebaseManager.getActivityLogs { activityLogs = it }

        listeners += FirebaseManager.getAllMembers(
            onResult = { fetched ->
                val populated = FamilyUtils.populateAllLinks(fetched, currentUser)
                allMembers = populated

                currentUser?.let { current ->
                    populated.find { it.id == current.id }?.let { updateCurrentUser(it) }
                }

                if (currentUser == null) {
                    val lastLogin = prefs.getLong("last_login_time", 0L)
                    val savedUserId = prefs.getString("user_id", null)
                    val currentTime = System.currentTimeMillis()

                    if (savedUserId != null) {
                        val userInList = populated.find { it.id == savedUserId }
                        if (userInList != null && (currentTime - lastLogin) < 10L * 24 * 60 * 60 * 1000) {
                            updateCurrentUser(userInList)
                            currentScreen = Screen.Dashboard
                            viewModelScope.launch {
                                FirebaseManager.updateLastLoggedIn(userInList.id)
                            }
                        } else if (userInList == null || (currentTime - lastLogin) >= 10L * 24 * 60 * 60 * 1000) {
                            logout(prefs)
                        }
                    }
                }
            },
            onError = { error ->
                Log.e("MainViewModel", "Firestore all-members error: ${error.message}")
                if (error.message?.contains("Unable to resolve host") == true) {
                    loginError = "No Internet connection. Please check your network."
                }
            }
        )

        listeners += FirebaseManager.getMembers(
            treeId = currentTreeId,
            onResult = { fetched ->
                Log.d("MainViewModel", "Fetched ${fetched.size} members")
                val populated = FamilyUtils.populateAllLinks(fetched, currentUser)
                members = populated
                
                // Sync currentUser with members list
                currentUser?.let { current ->
                    val updated = populated.find { it.id == current.id }
                    if (updated != null) {
                        updateCurrentUser(updated)
                    } else if (currentTreeId == "primary") {
                        // User no longer exists in primary list (e.g. deleted)
                        logout(prefs)
                    }
                }

            },
            onError = { error ->
                Log.e("MainViewModel", "Firestore error: ${error.message}")
                if (error.message?.contains("Unable to resolve host") == true) {
                    loginError = "No Internet connection. Please check your network."
                }
            }
        )

        listeners += FirebaseManager.getPendingChanges(
            onResult = { fetched ->
                pendingMembers = FamilyUtils.populateAllLinks(fetched)
            }
        )

        memoriesListener = FirebaseManager.getMemories(currentTreeId, true) { memories = it }
        discussionsListener = FirebaseManager.getDiscussions(currentTreeId, true) { discussions = it }

        listeners += FirebaseManager.getRecipes(currentTreeId) { recipes = it }
        listeners += FirebaseManager.getTraditions(currentTreeId) { traditions = it }
        listeners += FirebaseManager.getMilestones(currentTreeId) { milestones = it }
        listeners += FirebaseManager.getDeletionRequests { deletionRequests = it }
        listeners += FirebaseManager.getActiveGameSessions { activeGames = it }
        listeners += FirebaseManager.getBusinesses(currentTreeId) { businesses = it }
        listeners += FirebaseManager.getAchievements(currentTreeId) { achievements = it }
        listeners += FirebaseManager.getRelationshipOverrides { relationshipOverrides = it }
    }

    fun switchTree(treeId: String, prefs: android.content.SharedPreferences) {
        if (currentTreeId != treeId) {
            currentTreeId = treeId
            prefs.edit { putString("current_tree_id", currentTreeId) }
            // If switching to a non-primary tree, we might need a visual indicator
            // or reset certain states if needed.
            startSync(prefs)
        }
    }

    private fun updateCurrentUser(user: Member) {
        currentUser = if (user.phoneNumber == AppConfig.ADMIN_PHONE && user.name == AppConfig.ADMIN_NAME) {
            user.copy(isAdmin = true)
        } else {
            user
        }
        onUserChanged(currentUser)
    }

    private fun onUserChanged(user: Member?) {
        userListeners.forEach { it.remove() }
        userListeners.clear()

        if (user != null) {
            // A user is a moderator if they are a global Admin OR the owner of this secondary tree
            val isBranchModerator = user.isAdmin || (currentTreeId == user.id)
            
            userListeners += FirebaseManager.getMemories(treeId = currentTreeId, onlyApproved = !isBranchModerator) { memories = it }
            userListeners += FirebaseManager.getDiscussions(treeId = currentTreeId, onlyApproved = !isBranchModerator) { discussions = it }
            userListeners += FirebaseManager.getRecipes(treeId = currentTreeId) { recipes = it }
            userListeners += FirebaseManager.getTraditions(treeId = currentTreeId) { traditions = it }
            userListeners += FirebaseManager.getMilestones(treeId = currentTreeId) { milestones = it }
            userListeners += FirebaseManager.getBusinesses(treeId = currentTreeId) { businesses = it }
            userListeners += FirebaseManager.getAchievements(treeId = currentTreeId) { achievements = it }
            userListeners += FirebaseManager.getChannels(user.id) { channels = it }

            if (user.isAdmin || user.phoneNumber == AppConfig.ADMIN_PHONE) {
                FirebaseMessaging.getInstance().subscribeToTopic("admin_approvals")
                FirebaseMessaging.getInstance().subscribeToTopic("admin_only")
                userListeners += FirebaseManager.getRelationshipOverrides { relationshipOverrides = it }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_approvals")
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_only")
            }
            
            // Tree-aware topic subscriptions
            val suffix = if (currentTreeId == "primary") "" else "_$currentTreeId"
            
            FirebaseMessaging.getInstance().subscribeToTopic("events$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("recipes$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("traditions$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("gallery$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("memorylane$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("all_discussions$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("business$suffix")
            FirebaseMessaging.getInstance().subscribeToTopic("achievements$suffix")
        }
    }

    fun login(phone: String, prefs: android.content.SharedPreferences) {
        val loginUser = findLoginMember(phone)
        if (loginUser != null) {
            updateCurrentUser(loginUser)
            loginError = null
            currentScreen = Screen.Dashboard
            prefs.edit {
                putString("user_id", loginUser.id)
                putString("current_tree_id", currentTreeId)
                putLong("last_login_time", System.currentTimeMillis())
            }
            startSync(prefs)
            viewModelScope.launch { FirebaseManager.updateLastLoggedIn(loginUser.id) }
        } else {
            loginError = "Access Denied: Phone number not found."
        }
    }

    fun logout(prefs: android.content.SharedPreferences) {
        currentUser = null
        currentScreen = Screen.Login
        currentTreeId = "primary"
        prefs.edit { clear() }
        userListeners.forEach { it.remove() }
        userListeners.clear()
        stopListeningToMessages()
    }

    fun listenToMessages(otherMember: Member) {
        val user = currentUser ?: return
        messageListener?.remove()
        messageListener = FirebaseManager.getMessages(user.id, otherMember.id) {
            chatMessages = it
        }
    }

    fun stopListeningToMessages() {
        messageListener?.remove()
        messageListener = null
        chatMessages = emptyList()
    }

    private var gameListener: ListenerRegistration? = null

    fun listenToGame(sessionId: String) {
        gameListener?.remove()
        gameListener = FirebaseManager.getGameSession(sessionId) {
            currentGameSession = it
        }
    }

    fun stopListeningToGame() {
        gameListener?.remove()
        gameListener = null
        currentGameSession = null
    }

    fun handleIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        val senderId = intent?.getStringExtra("sender_id")
        
        if (navigateTo == "CHAT" && senderId != null && members.isNotEmpty()) {
            members.find { it.id == senderId }?.let { otherMember ->
                currentScreen = Screen.Chat(otherMember)
            }
        } else if (navigateTo == "EDIT_PROFILE" && senderId != null && members.isNotEmpty()) {
            val targetMember = members.find { it.id == senderId }
            val isReadOnly = currentUser?.let { !it.isAdmin && targetMember?.id != it.id } ?: true
            currentScreen = Screen.EditProfile(targetMember, isReadOnly = isReadOnly)
        } else if (navigateTo == "RECIPE") {
            currentScreen = Screen.Cookbook
        } else if (navigateTo == "TRADITION") {
            currentScreen = Screen.Traditions
        } else if (navigateTo == "GALLERY") {
            currentScreen = Screen.Gallery
        } else if (navigateTo == "BUSINESS") {
            currentScreen = Screen.BusinessDirectory
        } else if (navigateTo == "NOTIFICATION_CENTER") {
            currentScreen = Screen.Notifications
        } else if (navigateTo == "OVERRIDE_APPROVED" || navigateTo == "DELETION_REQUEST" || navigateTo == "PENDING_APPROVAL") {
            currentScreen = Screen.Dashboard
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
        if (screen is Screen.Antakshari) {
            listenToGame(screen.sessionId)
        } else if (screen is Screen.FamilyGames || screen is Screen.Dashboard) {
            stopListeningToGame()
        }
    }

    fun updatePassword(userId: String, newHash: String) {
        viewModelScope.launch {
            try {
                FirebaseManager.updatePassword(userId, newHash)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun saveMember(member: Member, photoUri: android.net.Uri?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val withPhoto = if (photoUri != null) {
                    val url = FirebaseManager.uploadPhoto(photoUri)
                    member.copy(photoUrl = url)
                } else member

                val branchOwnerId = currentTreeId.takeIf { it != "primary" }
                val isBranchRoot = branchOwnerId != null && withPhoto.id == branchOwnerId
                val finalMember = when {
                    currentUser?.isAdmin == true -> withPhoto
                    branchOwnerId != null && !isBranchRoot -> withPhoto.copy(
                        treeId = currentTreeId,
                        isPrimaryTree = false
                    )
                    branchOwnerId != null && isBranchRoot -> withPhoto.copy(
                        treeId = "primary",
                        isPrimaryTree = true
                    )
                    else -> withPhoto.copy(
                        treeId = "primary",
                        isPrimaryTree = true
                    )
                }
                
                FirebaseManager.submitChange(finalMember, currentUser?.isAdmin == true)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            try {
                // Implementation depends on requirements, maybe a deletion request or direct delete
                FirebaseManager.submitDeletionRequest(DeletionRequest(
                    id = java.util.UUID.randomUUID().toString(),
                    collectionName = AppConfig.Collections.MEMBERS,
                    docId = memberId,
                    title = members.find { it.id == memberId }?.name ?: "Unknown",
                    requestedBy = currentUser?.id ?: "",
                    requestedByName = currentUser?.name ?: "",
                    timestamp = System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun rejectPendingMember(memberId: String) {
        viewModelScope.launch {
            try {
                FirebaseManager.deletePendingChange(memberId)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun uploadMemory(caption: String, uri: android.net.Uri, taggedIds: List<String>) {
        viewModelScope.launch {
            isLoading = true
            try {
                val url = FirebaseManager.uploadPhoto(uri)
                val user = currentUser
                val memory = Memory(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user?.id ?: "",
                    userName = user?.name ?: "Unknown",
                    imageUrl = url,
                    caption = caption,
                    timestamp = System.currentTimeMillis(),
                    status = if (user?.isAdmin == true) "APPROVED" else "PENDING",
                    taggedMemberIds = taggedIds
                )
                FirebaseManager.submitMemory(memory, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun approveMemory(id: String) {
        viewModelScope.launch {
            try {
                FirebaseManager.approveMemory(id)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteMemory(id, user.id, user.name)
                } ?: FirebaseManager.deleteMemory(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun toggleMemoryReaction(id: String, emoji: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                FirebaseManager.toggleReaction(id, emoji, user.id, user.name)
            }
        }
    }

    fun addMemoryComment(id: String, text: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val comment = Comment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = user.name,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.addComment(id, comment)
            }
        }
    }

    fun addRecipe(recipe: Recipe, uri: android.net.Uri?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val finalRecipe = if (uri != null) {
                    val url = FirebaseManager.uploadPhoto(uri)
                    recipe.copy(imageUrl = url)
                } else recipe
                FirebaseManager.submitRecipe(finalRecipe, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun editRecipe(recipe: Recipe, uri: android.net.Uri?) {
        addRecipe(recipe, uri)
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteRecipe(id, user.id, user.name)
                } ?: FirebaseManager.deleteRecipe(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun toggleRecipeReaction(id: String, emoji: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                FirebaseManager.toggleRecipeReaction(id, emoji, user.id, user.name)
            }
        }
    }

    fun addRecipeComment(id: String, text: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val comment = Comment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = user.name,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.addRecipeComment(id, comment)
            }
        }
    }

    fun addTradition(tradition: Tradition, uri: android.net.Uri?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val finalTradition = if (uri != null) {
                    val url = FirebaseManager.uploadPhoto(uri)
                    tradition.copy(imageUrl = url)
                } else tradition
                FirebaseManager.submitTradition(finalTradition, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun editTradition(tradition: Tradition, uri: android.net.Uri?) {
        addTradition(tradition, uri)
    }

    fun deleteTradition(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteTradition(id, user.id, user.name)
                } ?: FirebaseManager.deleteTradition(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun toggleTraditionReaction(id: String, emoji: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                FirebaseManager.toggleTraditionReaction(id, emoji, user.id, user.name)
            }
        }
    }

    fun addTraditionComment(id: String, text: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val comment = Comment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = user.name,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.addTraditionComment(id, comment)
            }
        }
    }

    fun addMilestone(milestone: Milestone, imgUri: android.net.Uri?, audioUri: android.net.Uri?, taggedIds: List<String>) {
        viewModelScope.launch {
            isLoading = true
            try {
                var finalMilestone = milestone.copy(taggedMemberIds = taggedIds)
                if (imgUri != null) {
                    finalMilestone = finalMilestone.copy(imageUrl = FirebaseManager.uploadPhoto(imgUri))
                }
                if (audioUri != null) {
                    finalMilestone = finalMilestone.copy(audioUrl = FirebaseManager.uploadAudio(audioUri))
                }
                FirebaseManager.submitMilestone(finalMilestone, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteMilestone(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteMilestone(id, user.id, user.name)
                } ?: FirebaseManager.deleteMilestone(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun toggleMilestoneReaction(id: String, emoji: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                FirebaseManager.toggleMilestoneReaction(id, emoji, user.id, user.name)
            }
        }
    }

    fun addMilestoneComment(id: String, text: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val comment = Comment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = user.name,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.addMilestoneComment(id, comment)
            }
        }
    }

    fun addBusiness(business: Business) {
        viewModelScope.launch {
            isLoading = true
            try {
                FirebaseManager.submitBusiness(business, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteBusiness(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteBusiness(id, user.id, user.name)
                } ?: FirebaseManager.deleteBusiness(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun addAchievement(achievement: Achievement, imageUri: Uri?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val finalAchievement = if (imageUri != null) {
                    val url = FirebaseManager.uploadPhoto(imageUri)
                    achievement.copy(imageUrl = url)
                } else achievement
                FirebaseManager.submitAchievement(finalAchievement, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteAchievement(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteAchievement(id, user.id, user.name)
                } ?: FirebaseManager.deleteAchievement(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun postDiscussion(discussion: Discussion, uri: android.net.Uri?) {
        viewModelScope.launch {
            isLoading = true
            try {
                val finalDiscussion = if (uri != null) {
                    val url = FirebaseManager.uploadPhoto(uri)
                    discussion.copy(content = url)
                } else discussion
                FirebaseManager.submitDiscussion(finalDiscussion, currentTreeId)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun approveDiscussion(id: String) {
        viewModelScope.launch {
            FirebaseManager.approveDiscussion(id)
        }
    }

    fun deleteDiscussion(id: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    FirebaseManager.deleteDiscussion(id, user.id, user.name)
                } ?: FirebaseManager.deleteDiscussion(id, "System", "System")
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun voteDiscussion(discussionId: String, optionId: String, userId: String) {
        viewModelScope.launch {
            FirebaseManager.voteInPoll(discussionId, optionId, userId)
        }
    }

    fun addDiscussionComment(id: String, text: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val comment = Comment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = user.name,
                    text = text,
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.addDiscussionComment(id, comment)
            }
        }
    }

    fun createChatChannel(name: String, memberIds: List<String>) {
        // Simple implementation - could be more complex for group chats
    }

    fun sendChatMessage(channelId: String, text: String, uri: android.net.Uri?) {
        viewModelScope.launch {
            currentUser?.let { user ->
                val otherId = channelId.split("_").find { it != user.id } ?: return@launch
                val imageUrl = uri?.let { FirebaseManager.uploadPhoto(it) }
                val message = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    senderId = user.id,
                    receiverId = otherId,
                    text = text,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis()
                )
                FirebaseManager.sendMessage(message)
            }
        }
    }

    fun deleteChatMessage(channelId: String, msgId: String) {
        // Implementation for deleting messages
    }

    fun updateGameState(sessionId: String, state: Map<String, Any>, turn: String?, winner: String?) {
        viewModelScope.launch {
            FirebaseManager.updateGameState(sessionId, state, turn, winner)
        }
    }

    fun submitTriviaScore(score: Int) {
        viewModelScope.launch {
            currentUser?.let { user ->
                FirebaseManager.submitTriviaScore(user.id, score)
            }
        }
    }

    fun sendAntakshariRecording(sessionId: String, file: java.io.File) {
        viewModelScope.launch {
            try {
                val uri = android.net.Uri.fromFile(file)
                val url = FirebaseManager.uploadAudio(uri)
                val session = (if (currentGameSession?.id == sessionId) currentGameSession else null)
                    ?: activeGames.find { it.id == sessionId }
                
                val newState = session?.gameState?.toMutableMap() ?: mutableMapOf()
                val recordings = (newState["recordings"] as? List<Map<String, String>> ?: emptyList()).toMutableList()
                recordings.add(mapOf(
                    "url" to url,
                    "senderId" to (currentUser?.id ?: ""),
                    "timestamp" to java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                ))
                newState["recordings"] = recordings
                
                // Determine next turn (switch to other player)
                val nextTurnId = session?.players?.find { it != currentUser?.id }
                
                FirebaseManager.updateGameState(sessionId, newState, nextTurnId, null)
            } catch (e: Exception) {
                error = e.message
                Log.e("MainViewModel", "Error sending recording", e)
            }
        }
    }

    fun completeQuest(quest: FamilyQuest) {
        // Points are now awarded automatically upon action
    }

    fun createGameSession(gameType: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                isLoading = true
                try {
                    val sessionId = FirebaseManager.createGameSession(gameType, user)
                    navigateTo(Screen.fromGameType(gameType, sessionId))
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun joinGameSession(session: GameSession) {
        viewModelScope.launch {
            currentUser?.let { user ->
                isLoading = true
                try {
                    FirebaseManager.joinGameSession(session.id, user)
                    navigateTo(Screen.fromGameType(session.gameType, session.id))
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun importMembersFromCsv(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Simplified CSV parsing logic would go here
                // For now, just a placeholder for the UI call
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun clearAllMembers() {
        viewModelScope.launch {
            FirebaseManager.clearAll()
        }
    }

    fun approveRelationshipOverride(override: RelationshipOverride) {
        viewModelScope.launch {
            try {
                FirebaseManager.approveRelationshipOverride(override)
            } catch (e: Exception) {
                error = "Approval failed: ${e.message}"
            }
        }
    }

    fun rejectRelationshipOverride(overrideId: String) {
        viewModelScope.launch {
            try {
                FirebaseManager.rejectRelationshipOverride(overrideId)
            } catch (e: Exception) {
                error = "Rejection failed: ${e.message}"
            }
        }
    }

    fun requestRelationshipOverride(targetMember: Member, relationship: String) {
        viewModelScope.launch {
            currentUser?.let { user ->
                FirebaseManager.submitRelationshipOverride(RelationshipOverride(
                    id = "${user.id}_${targetMember.id}",
                    observerId = user.id,
                    observerName = user.name,
                    targetId = targetMember.id,
                    targetName = targetMember.name,
                    relationship = relationship,
                    status = "PENDING"
                ))
            }
        }
    }

    override fun onCleared() {
        listeners.forEach { it.remove() }
        userListeners.forEach { it.remove() }
        messageListener?.remove()
        super.onCleared()
    }
}
