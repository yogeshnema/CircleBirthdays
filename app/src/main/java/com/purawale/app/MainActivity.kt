package com.purawale.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.purawale.app.ui.components.AIChatAssistant
import com.purawale.app.ui.notifications.NotificationCenterScreen
import com.purawale.app.ui.screens.*
import com.purawale.app.ui.theme.CircleBirthdaysTheme
import com.purawale.app.viewmodel.MainViewModel
import com.purawale.app.viewmodel.NotificationViewModel
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()
        scheduleBirthdayWork()

        intentState.value = intent

        setContent {
            CircleBirthdaysTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CircleBirthdaysApp(intentState.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentState.value = intent
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Birthday Notifications"
            val descriptionText = "Notifications for family birthdays and anniversaries"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel("BIRTHDAY_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBirthdayWork() {
        val birthdayWorkRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(
            1, java.util.concurrent.TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BirthdayNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            birthdayWorkRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val now = System.currentTimeMillis()
        return calendar.timeInMillis - now
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleBirthdaysApp(intent: Intent? = null) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val notificationViewModel: NotificationViewModel = viewModel()
    
    val prefs = remember { context.getSharedPreferences("circle_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        viewModel.init(prefs)
    }

    LaunchedEffect(intent, viewModel.members) {
        intent?.let {
            val screen = it.getStringExtra("screen")
            if (screen == "notifications") {
                viewModel.navigateTo(Screen.Notifications)
            }
        }
    }

    val members = viewModel.members
    val allMembers = viewModel.allMembers
    val currentUser = viewModel.currentUser
    val currentScreen = viewModel.currentScreen
    val error = viewModel.error
    val loginError = viewModel.loginError
    val isLoading = viewModel.isLoading
    val pendingMembers = viewModel.pendingMembers
    val deletionRequests = viewModel.deletionRequests
    val memories = viewModel.memories
    val traditions = viewModel.traditions
    val milestones = viewModel.milestones
    val discussions = viewModel.discussions
    val recipes = viewModel.recipes
    val channels = viewModel.channels
    val unreadNotificationsCount = notificationViewModel.unreadCount

    val isHindi = viewModel.isHindi

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            notificationViewModel.startListening(user.id, user.isAdmin)
        }
    }

    CompositionLocalProvider(LocalLanguage provides isHindi) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5D4037))
                }
            } else {
                when (val screen = currentScreen) {
                    is Screen.Login -> LoginScreen(
                        members = allMembers.ifEmpty { members },
                        onLoginSuccess = { phone -> viewModel.login(phone, prefs) },
                        error = loginError ?: error,
                        isHindi = isHindi,
                        onLanguageToggle = { viewModel.toggleLanguage(prefs) }
                    )
                    is Screen.Dashboard -> currentUser?.let { user ->
                        DashboardScreen(
                            user = user,
                            allMembers = allMembers.ifEmpty { members },
                            pendingMembers = pendingMembers,
                            deletionRequests = deletionRequests,
                            channels = channels,
                            unreadNotificationsCount = unreadNotificationsCount,
                            currentTreeId = viewModel.currentTreeId,
                            onSwitchTree = { treeId -> viewModel.switchTree(treeId, prefs) },
                            onNavigateToProfiles = { viewModel.navigateTo(Screen.ProfileList) },
                            onNavigateToGallery = { viewModel.navigateTo(Screen.Gallery) },
                            onNavigateToDiscussions = { viewModel.navigateTo(Screen.Discussions) },
                            onNavigateToMessages = { viewModel.navigateTo(Screen.Messages) },
                            onLogout = { viewModel.logout(prefs) },
                            onViewProfile = { viewModel.navigateTo(Screen.EditProfile(user, isReadOnly = true)) },
                            onEditProfile = { viewModel.navigateTo(Screen.EditProfile(user)) },
                            onPasswordChange = { newHash -> viewModel.updatePassword(user.id, newHash) },
                            onNavigateToCookbook = { viewModel.navigateTo(Screen.Cookbook) },
                            onNavigateToTraditions = { viewModel.navigateTo(Screen.Traditions) },
                            onNavigateToMemoryLane = { viewModel.navigateTo(Screen.MemoryLane) },
                            onNavigateToFamilyTree = { viewModel.navigateTo(Screen.FamilyTree) },
                            onNavigateToCalendar = { viewModel.navigateTo(Screen.Calendar) },
                            onNavigateToNotifications = { viewModel.navigateTo(Screen.Notifications) },
                            onNavigateToEmergency = { viewModel.navigateTo(Screen.Emergency) },
                            onNavigateToFamilyGames = { viewModel.navigateTo(Screen.FamilyGames) },
                            onNavigateToBusinessDirectory = { viewModel.navigateTo(Screen.BusinessDirectory) },
                            onNavigateToAchievements = { viewModel.navigateTo(Screen.Achievements) },
                            onNavigateToHelp = { viewModel.navigateTo(Screen.Help) },
                            onNavigateToLoginLog = { viewModel.navigateTo(Screen.LoginLog) },
                            onNavigateToActivityLog = { viewModel.navigateTo(Screen.ActivityLog) },
                            onGenerateAICard = { member, type -> viewModel.navigateTo(Screen.AICardGenerator(member, type)) },
                            showWelcomeTour = viewModel.showWelcomeTour,
                            onFinishWelcomeTour = { showAgain -> viewModel.finishWelcomeTour(prefs, showAgain) }
                        )
                    }
                    is Screen.ProfileList -> currentUser?.let { user ->
                        ProfileListScreen(
                            members = members,
                            pendingMembers = pendingMembers,
                            currentUser = user,
                            currentTreeId = viewModel.currentTreeId,
                            onView = { member -> viewModel.navigateTo(Screen.EditProfile(member, isReadOnly = true)) },
                            onEdit = { member -> viewModel.navigateTo(Screen.EditProfile(member)) },
                            onAdd = { viewModel.navigateTo(Screen.EditProfile(null)) },
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onHome = { viewModel.navigateTo(Screen.Dashboard) },
                            onImportCsv = { uri -> viewModel.importMembersFromCsv(uri, context) },
                            onApprove = { member -> viewModel.approvePendingMember(member) },
                            onClearAll = { viewModel.clearAllMembers() },
                            onChat = { member -> viewModel.navigateTo(Screen.Chat(member)) },
                            overrides = viewModel.relationshipOverrides,
                            onApproveOverride = { override -> viewModel.approveRelationshipOverride(override) },
                            onRejectOverride = { id -> viewModel.rejectRelationshipOverride(id) },
                            onResetPassword = { member -> viewModel.updatePassword(member.id, "1234".hash()) },
                            onRemovePhoto = { member -> viewModel.saveMember(member.copy(photoUrl = ""), null) },
                            onRejectPending = { member -> viewModel.rejectPendingMember(member.id) }
                        )
                    }
                    is Screen.EditProfile -> currentUser?.let { user ->
                        EditProfileScreen(
                            member = screen.member,
                            currentUser = user,
                            currentTreeId = viewModel.currentTreeId,
                            isReadOnly = screen.isReadOnly,
                            onSave = { member, uri -> 
                                viewModel.saveMember(member, uri)
                                viewModel.navigateTo(Screen.ProfileList)
                            },
                            onCancel = { viewModel.navigateTo(Screen.ProfileList) },
                            onHome = { viewModel.navigateTo(Screen.Dashboard) },
                            onRequestOverride = { rel -> 
                                screen.member?.let { viewModel.requestRelationshipOverride(it, rel) }
                            }
                        )
                    }
                    is Screen.Gallery -> currentUser?.let { user ->
                        GalleryScreen(
                            user = user,
                            memories = memories,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onUpload = { caption, uri, taggedIds -> viewModel.uploadMemory(caption, uri, taggedIds) },
                            onApprove = { id -> viewModel.approveMemory(id) },
                            onDelete = { id -> viewModel.deleteMemory(id) },
                            onToggleReaction = { id, emoji -> viewModel.toggleMemoryReaction(id, emoji) },
                            onAddComment = { id, text -> viewModel.addMemoryComment(id, text) },
                            onImageClick = { /* Open full screen */ }
                        )
                    }
                    is Screen.Cookbook -> currentUser?.let { user ->
                        CookbookScreen(
                            user = user,
                            recipes = recipes,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onAddRecipe = { recipe, uri -> viewModel.addRecipe(recipe, uri) },
                            onEditRecipe = { recipe, uri -> viewModel.editRecipe(recipe, uri) },
                            onDelete = { id -> viewModel.deleteRecipe(id) },
                            onToggleReaction = { id, emoji -> viewModel.toggleRecipeReaction(id, emoji) },
                            onAddComment = { id, text -> viewModel.addRecipeComment(id, text) }
                        )
                    }
                    is Screen.Traditions -> currentUser?.let { user ->
                        TraditionsScreen(
                            user = user,
                            traditions = traditions,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onAddTradition = { tradition, uri -> viewModel.addTradition(tradition, uri) },
                            onEditTradition = { tradition, uri -> viewModel.editTradition(tradition, uri) },
                            onDelete = { id -> viewModel.deleteTradition(id) },
                            onToggleReaction = { id, emoji -> viewModel.toggleTraditionReaction(id, emoji) },
                            onAddComment = { id, text -> viewModel.addTraditionComment(id, text) }
                        )
                    }
                    is Screen.MemoryLane -> currentUser?.let { user ->
                        MemoryLaneScreen(
                            user = user,
                            milestones = milestones,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onAddMilestone = { milestone, imgUri, audioUri, taggedIds -> 
                                viewModel.addMilestone(milestone, imgUri, audioUri, taggedIds) 
                            },
                            onDeleteMilestone = { milestone -> viewModel.deleteMilestone(milestone.id) },
                            onToggleReaction = { id, emoji -> viewModel.toggleMilestoneReaction(id, emoji) },
                            onAddComment = { id, text -> viewModel.addMilestoneComment(id, text) }
                        )
                    }
                    is Screen.Discussions -> currentUser?.let { user ->
                        DiscussionsScreen(
                            user = user,
                            discussions = discussions,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onPost = { disc, uri -> viewModel.postDiscussion(disc, uri) },
                            onApprove = { id -> viewModel.approveDiscussion(id) },
                            onDelete = { id -> viewModel.deleteDiscussion(id) },
                            onVote = { discId, optId, userId -> viewModel.voteDiscussion(discId, optId, userId) },
                            onAddComment = { id, text -> viewModel.addDiscussionComment(id, text) },
                            onImageClick = { /* Open full screen */ }
                        )
                    }
                    is Screen.Messages -> currentUser?.let { user ->
                        MessagesScreen(
                            user = user,
                            channels = channels,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onNavigateToChat = { otherMember -> 
                                viewModel.navigateTo(Screen.Chat(otherMember))
                            }
                        )
                    }
                    is Screen.Chat -> currentUser?.let { user ->
                        LaunchedEffect(screen.otherMember.id) {
                            viewModel.listenToMessages(screen.otherMember)
                        }
                        ChatScreen(
                            user = user,
                            otherMember = screen.otherMember,
                            messages = viewModel.chatMessages,
                            onBack = { 
                                viewModel.stopListeningToMessages()
                                viewModel.navigateTo(Screen.Messages) 
                            },
                            onSendMessage = { text -> 
                                val channelId = if (user.id < screen.otherMember.id) "${user.id}_${screen.otherMember.id}" else "${screen.otherMember.id}_${user.id}"
                                viewModel.sendChatMessage(channelId, text, null) 
                            }
                        )
                    }
                    is Screen.FamilyTree -> currentUser?.let { user ->
                        FamilyTreeScreen(
                            currentUser = user,
                            members = members,
                            currentTreeId = viewModel.currentTreeId,
                            onNavigateBack = { 
                                if (viewModel.currentTreeId != "primary") {
                                    viewModel.switchTree("primary", prefs)
                                } else {
                                    viewModel.navigateTo(Screen.Dashboard)
                                }
                            },
                            onViewMember = { member -> viewModel.navigateTo(Screen.EditProfile(member, isReadOnly = true)) },
                            onEditMember = { member -> viewModel.navigateTo(Screen.EditProfile(member)) },
                            onSwitchTree = { treeId ->
                                viewModel.switchTree(treeId, prefs)
                                viewModel.navigateTo(Screen.ProfileList)
                            }
                        )
                    }
                    is Screen.Calendar -> currentUser?.let { user ->
                        CalendarScreen(
                            allMembers = members,
                            currentUser = user,
                            currentTreeId = viewModel.currentTreeId,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }
                    is Screen.Notifications -> currentUser?.let { user ->
                        NotificationCenterScreen(
                            notifications = notificationViewModel.notifications,
                            userId = user.id,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onNotificationClick = { notification -> 
                                notificationViewModel.markAsRead(notification.id, user.id)
                            },
                            onMarkAllRead = { notificationViewModel.markAllAsRead(user.id) }
                        )
                    }
                    is Screen.FamilyGames -> currentUser?.let { user ->
                        FamilyGamesScreen(
                            user = user,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onStartGame = { gameType -> viewModel.navigateTo(Screen.GameLobby(gameType)) },
                            onTakeTrivia = { viewModel.navigateTo(Screen.Trivia) },
                            onManageTrivia = { viewModel.navigateTo(Screen.ManageTrivia) },
                            onQuestClick = { quest ->
                                when (quest.type) {
                                    "ADD_PHOTO" -> viewModel.navigateTo(Screen.Gallery)
                                    "POST_RECIPE" -> viewModel.navigateTo(Screen.Cookbook)
                                    else -> {}
                                }
                            }
                        )
                    }
                    is Screen.Antakshari -> currentUser?.let { user ->
                        val session = viewModel.currentGameSession ?: viewModel.activeGames.find { it.id == screen.sessionId }
                        val otherId = session?.players?.find { it != user.id } ?: ""
                        val otherMember = members.find { it.id == otherId }
                        AntakshariScreen(
                            user = user,
                            otherPlayer = otherMember,
                            otherPlayerName = otherMember?.name ?: "Unknown",
                            session = session,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onSendRecording = { file -> viewModel.sendAntakshariRecording(screen.sessionId, file) }
                        )
                    }
                    is Screen.Trivia -> {
                        TriviaScreen(
                            questions = viewModel.triviaQuestions,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onComplete = { score -> viewModel.submitTriviaScore(score) }
                        )
                    }
                    is Screen.Hangman -> currentUser?.let { user ->
                        val session = viewModel.activeGames.find { it.id == screen.sessionId }
                        val otherId = session?.players?.find { it != user.id } ?: ""
                        val otherMember = members.find { it.id == otherId }
                        HangmanScreen(
                            user = user,
                            otherPlayer = otherMember,
                            otherPlayerName = otherMember?.name ?: "Unknown",
                            session = session,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onUpdateState = { state, p1, p2 -> viewModel.updateGameState(screen.sessionId, state, p1, p2) }
                        )
                    }
                    is Screen.Chaupad -> currentUser?.let { user ->
                        val session = viewModel.activeGames.find { it.id == screen.sessionId }
                        val otherId = session?.players?.find { it != user.id } ?: ""
                        val otherMember = members.find { it.id == otherId }
                        ChaupadScreen(
                            user = user,
                            otherPlayer = otherMember,
                            otherPlayerName = otherMember?.name ?: "Unknown",
                            session = session,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onUpdateState = { state, p1, p2 -> viewModel.updateGameState(screen.sessionId, state, p1, p2) }
                        )
                    }
                    is Screen.Chess -> currentUser?.let { user ->
                        val session = viewModel.activeGames.find { it.id == screen.sessionId }
                        val otherId = session?.players?.find { it != user.id } ?: ""
                        val otherMember = members.find { it.id == otherId }
                        ChessScreen(
                            user = user,
                            otherPlayer = otherMember,
                            otherPlayerName = otherMember?.name ?: "Unknown",
                            session = session,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onUpdateState = { state, p1, p2 -> viewModel.updateGameState(screen.sessionId, state, p1, p2) }
                        )
                    }
                    is Screen.Rummy -> currentUser?.let { user ->
                        val session = viewModel.activeGames.find { it.id == screen.sessionId }
                        val otherId = session?.players?.find { it != user.id } ?: ""
                        val otherMember = members.find { it.id == otherId }
                        RummyScreen(
                            user = user,
                            otherPlayer = otherMember,
                            otherPlayerName = otherMember?.name ?: "Unknown",
                            session = session,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onUpdateState = { state, p1, p2 -> viewModel.updateGameState(screen.sessionId, state, p1, p2) }
                        )
                    }
                    is Screen.SnakesAndLadders -> currentUser?.let { user ->
                        val session = viewModel.activeGames.find { it.id == screen.sessionId }
                        val otherId = session?.players?.find { it != user.id } ?: ""
                        val otherMember = members.find { it.id == otherId }
                        SnakesAndLaddersScreen(
                            user = user,
                            otherPlayer = otherMember,
                            otherPlayerName = otherMember?.name ?: "Unknown",
                            session = session,
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onUpdateState = { state, p1, p2 -> viewModel.updateGameState(screen.sessionId, state, p1, p2) }
                        )
                    }
                    is Screen.ManageTrivia -> ManageTriviaScreen(onBack = { viewModel.navigateTo(Screen.FamilyGames) })
                    is Screen.GameLobby -> currentUser?.let { user ->
                        GameLobbyScreen(
                            user = user,
                            gameType = screen.gameType,
                            activeSessions = viewModel.activeGames.filter { it.gameType == screen.gameType && it.status == "WAITING" },
                            onBack = { viewModel.navigateTo(Screen.FamilyGames) },
                            onCreateSession = { viewModel.createGameSession(screen.gameType) },
                            onJoinSession = { session -> viewModel.joinGameSession(session) }
                        )
                    }
                    is Screen.BusinessDirectory -> currentUser?.let { user ->
                        BusinessDirectoryScreen(
                            user = user,
                            businesses = viewModel.businesses,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onAddBusiness = { viewModel.addBusiness(it) },
                            onDeleteBusiness = { viewModel.deleteBusiness(it) }
                        )
                    }
                    is Screen.Achievements -> currentUser?.let { user ->
                        AchievementsScreen(
                            user = user,
                            achievements = viewModel.achievements,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            onAddAchievement = { achievement, uri -> viewModel.addAchievement(achievement, uri) },
                            onDeleteAchievement = { id -> viewModel.deleteAchievement(id) }
                        )
                    }
                    is Screen.Help -> HelpScreen(
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onHome = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                    is Screen.Emergency -> currentUser?.let { user ->
                        EmergencyScreen(
                            currentUser = user,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) }
                        )
                    }
                    is Screen.LoginLog -> LoginLogScreen(
                        members = allMembers,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onHome = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                    is Screen.AICardGenerator -> currentUser?.let { user ->
                        AICardGeneratorScreen(
                            member = screen.member,
                            eventType = screen.eventType,
                            currentUser = user,
                            allMembers = members,
                            onBack = { viewModel.navigateTo(Screen.Dashboard) },
                            aiDesignCache = viewModel.aiDesignCache,
                            onUpdateCache = { viewModel.aiDesignCache = it }
                        )
                    }
                    is Screen.ActivityLog -> ActivityLogScreen(
                        logs = viewModel.activityLogs,
                        members = allMembers,
                        onBack = { viewModel.navigateTo(Screen.Dashboard) },
                        onHome = { viewModel.navigateTo(Screen.Dashboard) }
                    )
                }
            }

            val isGameScreen = currentScreen is Screen.FamilyGames ||
                    currentScreen is Screen.GameLobby ||
                    currentScreen is Screen.SnakesAndLadders ||
                    currentScreen is Screen.Chess ||
                    currentScreen is Screen.Chaupad ||
                    currentScreen is Screen.Hangman ||
                    currentScreen is Screen.Rummy ||
                    currentScreen is Screen.Antakshari

            // AI Chat Assistant
            if (currentScreen !is Screen.Login && currentScreen !is Screen.FamilyTree && !isGameScreen) {
                AIChatAssistant(
                    allMembers = allMembers,
                    currentUser = currentUser
                )
            }
        }
    }
}
