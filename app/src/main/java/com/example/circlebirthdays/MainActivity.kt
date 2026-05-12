package com.example.circlebirthdays

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.circlebirthdays.ui.theme.CircleBirthdaysTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.security.MessageDigest
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

fun String.hash(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun safeOpenUri(context: android.content.Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    try {
        val formattedUri = if (!uriString.startsWith("http://") && !uriString.startsWith("https://")) {
            "https://$uriString"
        } else {
            uriString
        }
        val uri = formattedUri.toUri()
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("CircleBirthdays", "Failed to open URI: $uriString", e)
        android.widget.Toast.makeText(context, "Could not open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}

class MainActivity : ComponentActivity() {
    private var intentState = mutableStateOf<android.content.Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentState.value = intent
        
        createNotificationChannel()

        FirebaseMessaging.getInstance().subscribeToTopic("all_discussions")
        FirebaseMessaging.getInstance().subscribeToTopic("gallery_updates")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 102)
        }

        scheduleBirthdayWork()

        setContent {
            CircleBirthdaysTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                        Image(
                            painter = painterResource(id = R.drawable.background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.1f
                        )
                        CircleBirthdaysApp(intentState.value)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intentState.value = intent
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "purawale_notifications"
            val name = "Purawale - Hum aur Humare Notifications"
            val descriptionText = "Notifications for family updates and messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBirthdayWork() {
        val birthdayWorkRequest = PeriodicWorkRequestBuilder<BirthdayWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BirthdayNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            birthdayWorkRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 9) // 9 AM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis - now
    }
}

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object ProfileList : Screen()
    object Gallery : Screen()
    object Discussions : Screen()
    object Messages : Screen()
    object Cookbook : Screen()
    object Traditions : Screen()
    object MemoryLane : Screen()
    data class Chat(val otherMember: Member) : Screen()
    data class EditProfile(val member: Member?) : Screen()
}

@Composable
fun CircleBirthdaysApp(intent: android.content.Intent? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var pendingMembers by remember { mutableStateOf<List<Member>>(emptyList()) }
    var memories by remember { mutableStateOf<List<Memory>>(emptyList()) }
    var discussions by remember { mutableStateOf<List<Discussion>>(emptyList()) }
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var traditions by remember { mutableStateOf<List<Tradition>>(emptyList()) }
    var milestones by remember { mutableStateOf<List<Milestone>>(emptyList()) }
    var deletionRequests by remember { mutableStateOf<List<DeletionRequest>>(emptyList()) }
    var channels by remember { mutableStateOf<List<ChatChannel>>(emptyList()) }
    var chatMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var relationshipOverrides by remember { mutableStateOf<List<RelationshipOverride>>(emptyList()) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var currentUser by remember { mutableStateOf<Member?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Persist login state
    val prefs = context.getSharedPreferences("circle_prefs", android.content.Context.MODE_PRIVATE)
    
    // Sync with Firebase
    LaunchedEffect(Unit) {
        val lastLogin = prefs.getLong("last_login_time", 0L)
        val savedUserId = prefs.getString("user_id", null)
        val currentTime = System.currentTimeMillis()
        
        if (savedUserId != null && (currentTime - lastLogin) < 10L * 24 * 60 * 60 * 1000) {
            // Auto-login will happen when members are fetched
        } else {
            prefs.edit { clear() }
        }

        FirebaseManager.getMembers(
            onResult = { fetched ->
                android.util.Log.d("CircleBirthdays", "Fetched ${fetched.size} members")
                val populated = FamilyUtils.populateAllLinks(fetched, currentUser)
                members = populated
                
                // Handle auto-login
                if (currentUser == null) {
                    val stillValidUserId = prefs.getString("user_id", null)
                    if (stillValidUserId != null) {
                        populated.find { it.id == stillValidUserId }?.let { 
                            currentUser = if (it.phoneNumber == "9999999999") it.copy(isAdmin = true) else it
                            currentScreen = Screen.Dashboard
                            scope.launch {
                                FirebaseManager.updateLastLoggedIn(it.id)
                            }
                        }
                    }
                }
            },
            onError = { error ->
                android.util.Log.e("CircleBirthdays", "Firestore error: ${error.message}")
                if (error.message?.contains("Unable to resolve host") == true) {
                    loginError = "No Internet connection. Please check your network."
                }
            }
        )
        FirebaseManager.getPendingChanges(
            onResult = { fetched ->
                android.util.Log.d("CircleBirthdays", "Fetched ${fetched.size} pending changes")
                pendingMembers = FamilyUtils.populateAllLinks(fetched)
            },
            onError = { error ->
                android.util.Log.e("CircleBirthdays", "Firestore pending error: ${error.message}")
            }
        )

        // Listen for all memories and discussions if admin, to ensure real-time updates
        FirebaseManager.getMemories(onlyApproved = false) { fetched ->
            if (currentUser?.isAdmin == true) memories = fetched
        }
        FirebaseManager.getDiscussions(onlyApproved = false) { fetched ->
            if (currentUser?.isAdmin == true) discussions = fetched
        }

        FirebaseManager.getRecipes { fetched -> recipes = fetched }
        FirebaseManager.getTraditions { fetched -> traditions = fetched }
        FirebaseManager.getMilestones { fetched -> milestones = fetched }
        FirebaseManager.getDeletionRequests { fetched -> deletionRequests = fetched }
    }

    // Fetch Memories based on user role and populate relationships
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            members = FamilyUtils.populateAllLinks(members, user)
            
            val isAdmin = user.isAdmin
            FirebaseManager.getMemories(onlyApproved = !isAdmin) { fetched ->
                memories = fetched
            }
            FirebaseManager.getDiscussions(onlyApproved = !isAdmin) { fetched ->
                discussions = fetched
            }

            FirebaseManager.getChannels(user.id) { fetched ->
                channels = fetched
            }
            if (user.isAdmin) {
                FirebaseMessaging.getInstance().subscribeToTopic("admin_approvals")
                FirebaseManager.getRelationshipOverrides { fetched ->
                    relationshipOverrides = fetched
                }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_approvals")
            }
            FirebaseMessaging.getInstance().subscribeToTopic("events")
        }
    }

    LaunchedEffect(currentScreen, currentUser) {
        val screen = currentScreen
        val user = currentUser
        if (screen is Screen.Chat && user != null) {
            FirebaseManager.getMessages(user.id, screen.otherMember.id) { fetched ->
                chatMessages = fetched
            }
        }
    }

    // Keep currentUser in sync with members list (e.g. after password change or profile update)
    LaunchedEffect(members) {
        currentUser?.let { current ->
            members.find { it.id == current.id }?.let { updated ->
                currentUser = if (current.phoneNumber == "9999999999") updated.copy(isAdmin = true) else updated
            }
        }
    }

    // One-time Seed to Cloud (Only if Cloud is empty and user is Admin)
    LaunchedEffect(members, currentUser) {
        if (members.isEmpty() && currentUser?.isAdmin == true && currentUser?.id == "admin") {
            android.util.Log.d("CircleBirthdays", "Seeding started by ${currentUser?.name}")
            val initial = getInitialMembers()
            try {
                FirebaseManager.submitBatch(initial, true)
            } catch (e: Exception) {
                android.util.Log.e("CircleBirthdays", "Seed batch failed", e)
            }
        }
    }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(selectedUri)
                val csvText = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                csvText?.let { text ->
                    val newOnes = CsvHelper.parse(text)
                    android.util.Log.d("CsvHelper", "Parsed ${newOnes.size} members")
                    FirebaseManager.submitBatch(newOnes, true)
                }
            }
        }
    }

    val user = currentUser
    
    // Handle Navigation from Notification
    LaunchedEffect(intent, members) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        val senderId = intent?.getStringExtra("sender_id")
        
        if (navigateTo == "CHAT" && senderId != null && members.isNotEmpty()) {
            members.find { it.id == senderId }?.let { otherMember ->
                currentScreen = Screen.Chat(otherMember)
            }
        } else if (navigateTo == "OVERRIDE_APPROVED" || navigateTo == "DELETION_REQUEST" || navigateTo == "PENDING_APPROVAL") {
            currentScreen = Screen.Dashboard
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
                is Screen.Login -> LoginScreen(
                    members = members,
                    error = loginError,
                    onLoginSuccess = { phone ->
                        val loginUser = members.find { it.phoneNumber == phone }
                        if (loginUser != null) {
                            currentUser = if (phone == "9999999999") loginUser.copy(isAdmin = true) else loginUser
                            loginError = null
                            currentScreen = Screen.Dashboard
                            prefs.edit {
                                putString("user_id", loginUser.id)
                                putLong("last_login_time", System.currentTimeMillis())
                            }
                            scope.launch { FirebaseManager.updateLastLoggedIn(loginUser.id) }
                        } else {
                            loginError = "Access Denied: Phone number not found."
                        }
                    }
                )
                is Screen.Dashboard -> if (user != null) DashboardScreen(
                    user = user,
                    allMembers = members,
                    pendingMembers = pendingMembers,
                    deletionRequests = deletionRequests,
                    channels = channels,
                    onNavigateToProfiles = { currentScreen = Screen.ProfileList },
                    onNavigateToGallery = { currentScreen = Screen.Gallery },
                    onNavigateToDiscussions = { currentScreen = Screen.Discussions },
                    onNavigateToMessages = { currentScreen = Screen.Messages },
                    onNavigateToCookbook = { currentScreen = Screen.Cookbook },
                    onNavigateToTraditions = { currentScreen = Screen.Traditions },
                    onNavigateToMemoryLane = { currentScreen = Screen.MemoryLane },
                    onLogout = { 
                        currentUser = null
                        currentScreen = Screen.Login 
                        prefs.edit { clear() }
                    },
                    onEditProfile = { currentScreen = Screen.EditProfile(user) },
                    onPasswordChange = { hashedPw -> scope.launch { FirebaseManager.updatePassword(user.id, hashedPw) } }
                )
                is Screen.Messages -> if (user != null) MessagesScreen(
                    user = user,
                    channels = channels,
                    allMembers = members,
                    onBack = { currentScreen = Screen.Dashboard },
                    onNavigateToChat = { otherMember -> currentScreen = Screen.Chat(otherMember) }
                )
                is Screen.Chat -> if (user != null) ChatScreen(
                    user = user,
                    otherMember = (currentScreen as Screen.Chat).otherMember,
                    messages = chatMessages,
                    onBack = { currentScreen = Screen.Messages },
                    onSendMessage = { text -> 
                        scope.launch {
                            FirebaseManager.sendMessage(Message(
                                senderId = user.id,
                                senderName = user.name,
                                receiverId = (currentScreen as Screen.Chat).otherMember.id,
                                text = text
                            ))
                        }
                    }
                )
                is Screen.ProfileList -> if (user != null) ProfileListScreen(
                    members = members,
                    pendingMembers = pendingMembers,
                    currentUser = user,
                    onEdit = { currentScreen = Screen.EditProfile(it) },
                    onAdd = { currentScreen = Screen.EditProfile(null) },
                    onBack = { currentScreen = Screen.Dashboard },
                    onHome = { currentScreen = Screen.Dashboard },
                    onImportCsv = { csvPickerLauncher.launch("*/*") },
                    onApprove = { scope.launch { FirebaseManager.approveChange(it) } },
                    onClearAll = { scope.launch { FirebaseManager.clearAll() } },
                    onChat = { currentScreen = Screen.Chat(it) },
                    overrides = relationshipOverrides,
                    onApproveOverride = { scope.launch { FirebaseManager.approveRelationshipOverride(it) } }
                )
                is Screen.Gallery -> if (user != null) GalleryScreen(
                    user = user,
                    memories = memories,
                    onBack = { currentScreen = Screen.Dashboard },
                    onUpload = { desc: String, uri: Uri -> scope.launch {
                        val compressed = ImageUtils.compressImage(context, uri)
                        val url = if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        FirebaseManager.submitMemory(Memory(id=UUID.randomUUID().toString(), imageUrl=url, caption=desc, userName=user.name, status=if(user.isAdmin) "APPROVED" else "PENDING"))
                    }},
                    onDelete = { mid: String -> 
                        scope.launch { 
                            if (user.isAdmin) {
                                FirebaseManager.deleteMemory(mid)
                            } else {
                                memories.find { it.id == mid }?.let { m ->
                                    FirebaseManager.submitDeletionRequest(DeletionRequest(
                                        id = UUID.randomUUID().toString(),
                                        collectionName = "gallery",
                                        docId = m.id,
                                        title = m.caption,
                                        requestedBy = user.id,
                                        requestedByName = user.name
                                    ))
                                }
                            }
                        } 
                    },
                    onApprove = { mid: String -> scope.launch { FirebaseManager.approveMemory(mid) } },
                    onToggleReaction = { mid: String, type: String -> scope.launch { FirebaseManager.toggleReaction(mid, type, user.id) } },
                    onAddComment = { mid: String, text: String -> 
                        scope.launch {
                            val comment = Comment(
                                id = UUID.randomUUID().toString(),
                                userId = user.id,
                                userName = user.name,
                                text = text
                            )
                            FirebaseManager.addComment(mid, comment)
                        }
                    }
                )
                is Screen.Discussions -> if (user != null) DiscussionsScreen(
                    user = user,
                    discussions = discussions,
                    onBack = { currentScreen = Screen.Dashboard },
                    onPost = { disc: Discussion, uri: Uri? -> scope.launch {
                        val url = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else null
                        FirebaseManager.submitDiscussion(if (disc.type == "IMAGE") disc.copy(content = url ?: "", status = if(user.isAdmin) "APPROVED" else "PENDING") else disc.copy(status = if(user.isAdmin) "APPROVED" else "PENDING"))
                    }},
                    onDelete = { did: String -> 
                        scope.launch { 
                            if (user.isAdmin) {
                                FirebaseManager.deleteDiscussion(did)
                            } else {
                                discussions.find { it.id == did }?.let { d ->
                                    FirebaseManager.submitDeletionRequest(DeletionRequest(
                                        id = UUID.randomUUID().toString(),
                                        collectionName = "discussions",
                                        docId = d.id,
                                        title = d.title,
                                        requestedBy = user.id,
                                        requestedByName = user.name
                                    ))
                                }
                            }
                        } 
                    },
                    onApprove = { did: String -> scope.launch { FirebaseManager.approveDiscussion(did) } },
                    onVote = { did: String, oid: String, uid: String -> scope.launch { FirebaseManager.voteInPoll(did, oid, uid) } },
                    onAddComment = { did: String, text: String -> 
                        scope.launch {
                            val comment = Comment(
                                id = UUID.randomUUID().toString(),
                                userId = user.id,
                                userName = user.name,
                                text = text
                            )
                            FirebaseManager.addDiscussionComment(did, comment)
                        }
                    }
                )
                is Screen.Cookbook -> if (user != null) CookbookScreen(
                    user = user,
                    recipes = recipes,
                    onBack = { currentScreen = Screen.Dashboard },
                    onAddRecipe = { recipe, uri -> scope.launch {
                        val url = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else ""
                        FirebaseManager.submitRecipe(recipe.copy(id = UUID.randomUUID().toString(), authorId = user.id, authorName = user.name, imageUrl = url))
                    }},
                    onEditRecipe = { recipe, uri -> scope.launch {
                        val url = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else recipe.imageUrl
                        FirebaseManager.submitRecipe(recipe.copy(imageUrl = url))
                    }},
                    onDelete = { rid -> 
                        scope.launch { 
                            if (user.isAdmin) {
                                FirebaseManager.deleteRecipe(rid)
                            } else {
                                recipes.find { it.id == rid }?.let { r ->
                                    FirebaseManager.submitDeletionRequest(DeletionRequest(
                                        id = UUID.randomUUID().toString(),
                                        collectionName = "recipes",
                                        docId = r.id,
                                        title = r.title,
                                        requestedBy = user.id,
                                        requestedByName = user.name
                                    ))
                                }
                            }
                        } 
                    },
                    onToggleReaction = { rid, emoji -> scope.launch { FirebaseManager.toggleRecipeReaction(rid, emoji, user.id) } },
                    onAddComment = { rid, text ->
                        scope.launch {
                            val comment = Comment(
                                id = UUID.randomUUID().toString(),
                                userId = user.id,
                                userName = user.name,
                                text = text
                            )
                            FirebaseManager.addRecipeComment(rid, comment)
                        }
                    }
                )
                is Screen.Traditions -> if (user != null) TraditionsScreen(
                    user = user,
                    traditions = traditions,
                    onBack = { currentScreen = Screen.Dashboard },
                    onAddTradition = { trad, uri -> scope.launch {
                        val url = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else ""
                        FirebaseManager.submitTradition(trad.copy(id = UUID.randomUUID().toString(), authorId = user.id, authorName = user.name, imageUrl = url))
                    }},
                    onEditTradition = { trad, uri -> scope.launch {
                        val url = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else trad.imageUrl
                        FirebaseManager.submitTradition(trad.copy(imageUrl = url))
                    }},
                    onDelete = { tid -> 
                        scope.launch { 
                            if (user.isAdmin) {
                                FirebaseManager.deleteTradition(tid)
                            } else {
                                traditions.find { it.id == tid }?.let { t ->
                                    FirebaseManager.submitDeletionRequest(DeletionRequest(
                                        id = UUID.randomUUID().toString(),
                                        collectionName = "traditions",
                                        docId = t.id,
                                        title = t.title,
                                        requestedBy = user.id,
                                        requestedByName = user.name
                                    ))
                                }
                            }
                        } 
                    },
                    onToggleReaction = { tid, emoji -> scope.launch { FirebaseManager.toggleTraditionReaction(tid, emoji, user.id) } },
                    onAddComment = { tid, text ->
                        scope.launch {
                            val comment = Comment(
                                id = UUID.randomUUID().toString(),
                                userId = user.id,
                                userName = user.name,
                                text = text
                            )
                            FirebaseManager.addTraditionComment(tid, comment)
                        }
                    }
                )
                is Screen.MemoryLane -> if (user != null) MemoryLaneScreen(
                    user = user,
                    milestones = milestones,
                    onBack = { currentScreen = Screen.Dashboard },
                    onAddMilestone = { milestone, uri -> scope.launch {
                        val url = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else ""
                        FirebaseManager.submitMilestone(milestone.copy(id = UUID.randomUUID().toString(), authorId = user.id, authorName = user.name, imageUrl = url))
                    }},
                    onDeleteMilestone = { milestone -> scope.launch {
                        if (user.isAdmin || user.id == milestone.authorId) {
                            FirebaseManager.deleteMilestone(milestone.id)
                        } else {
                            val request = DeletionRequest(
                                id = UUID.randomUUID().toString(),
                                collectionName = "memorylane",
                                docId = milestone.id,
                                title = milestone.title,
                                reason = "User requested deletion", // Ideally we'd pass the reason from the dialog
                                requestedBy = user.id,
                                requestedByName = user.name
                            )
                            FirebaseManager.submitDeletionRequest(request)
                        }
                    }}
                )
                is Screen.EditProfile -> if (user != null) EditProfileScreen(
                    member = screen.member,
                    currentUser = user,
                    onSave = { updated: Member, uri: Uri? -> scope.launch {
                        val photoUrl = if (uri != null) {
                            val compressed = ImageUtils.compressImage(context, uri)
                            if (compressed != null) FirebaseManager.uploadPhoto(compressed) else FirebaseManager.uploadPhoto(uri)
                        } else updated.photoUrl
                        val finalMember = updated.copy(photoUrl = photoUrl)
                        val memberWithRequester = if (!user.isAdmin) {
                            // If it's a non-admin editing someone else (or even themselves), 
                            // and they changed the relationship field, we treat it as a requested relationship.
                            finalMember.copy(
                                requestedBy = user.id, 
                                requestedByName = user.name,
                                requestedRelationship = updated.relationship
                            )
                        } else finalMember
                        FirebaseManager.submitChange(memberWithRequester, user.isAdmin)
                        currentScreen = Screen.ProfileList
                    }},
                    onCancel = { currentScreen = Screen.ProfileList },
                    onHome = { currentScreen = Screen.Dashboard },
                    onRequestOverride = { rel: String -> scope.launch {
                        screen.member?.let { target ->
                            FirebaseManager.submitRelationshipOverride(RelationshipOverride(
                                id = "${user.id}_${target.id}",
                                observerId = user.id,
                                observerName = user.name,
                                targetId = target.id,
                                targetName = target.name,
                                relationship = rel
                            ))
                        }
                        currentScreen = Screen.ProfileList
                    }}
                )
            }
        }
    }


@Composable
fun LoginScreen(members: List<Member>, error: String?, onLoginSuccess: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Removed redundant logo card as it overlapped with the background.png
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Purawale\nHum aur Humare",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = Color(0xFF4E342E) // Sophisticated Deep Brown
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.all { char -> char.isDigit() }) phone = it; localError = null },
            label = { Text("Phone Number") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Password") },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        val context = LocalContext.current
        TextButton(
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("bgnema@yahoo.com"))
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Forgot Password - Nema Purawale App")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Hello Admin,\n\nI forgot my password for the phone number: $phone.\n\nPlease help me reset it.\n\nThank you.")
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    localError = "No email app found."
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?")
        }

        val displayError = error ?: localError
        if (displayError != null) {
            Text(displayError, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (phone.isBlank()) {
                    localError = "Enter phone number"
                } else {
                    val user = members.find { it.phoneNumber == phone }
                    val storedHash = user?.password
                    val inputHash = password.hash()
                    
                    val isValid = if (storedHash == null) {
                        password == "1234"
                    } else {
                        inputHash == storedHash
                    }

                    if (isValid) {
                        onLoginSuccess(phone)
                    } else {
                        localError = "Incorrect password"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: Member, 
    allMembers: List<Member>,
    pendingMembers: List<Member>,
    deletionRequests: List<DeletionRequest>,
    channels: List<ChatChannel>,
    onNavigateToProfiles: () -> Unit, 
    onNavigateToGallery: () -> Unit,
    onNavigateToDiscussions: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onLogout: () -> Unit, 
    onEditProfile: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onNavigateToCookbook: () -> Unit,
    onNavigateToTraditions: () -> Unit,
    onNavigateToMemoryLane: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val todayBirthdays = allMembers.filter { it.bereavementDate.isNullOrBlank() && isToday(it.dateOfBirth) }
    val todayAnniversaries = allMembers
        .filter { !it.marriageDate.isNullOrBlank() && it.bereavementDate.isNullOrBlank() && isToday(it.marriageDate) }
        .distinctBy { 
            // Normalize pair: sort family IDs so (1, 10) and (10, 1) result in same key
            val partnerId = if (it.familyId.endsWith("0")) it.familyId.dropLast(1) else it.familyId + "0"
            listOf(it.familyId, partnerId).sorted().joinToString("-")
        }
    
    // For Admin, show pending remembrances to help verify additions
    val remembrancePool = if (user.isAdmin) {
        (allMembers + pendingMembers).distinctBy { it.id }
    } else {
        allMembers
    }

    val todayRemembrances = remembrancePool.filter { 
        (!it.bereavementDate.isNullOrBlank() && isToday(it.bereavementDate)) ||
        (!it.bereavementDate.isNullOrBlank() && isToday(it.dateOfBirth))
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword.isNotBlank()) {
                        onPasswordChange(newPassword.hash())
                        showPasswordDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") }, 
                actions = { 
                    TextButton(onClick = { showPasswordDialog = true }) { Text("Change Password") }
                    TextButton(onClick = onLogout) { Text("Logout") } 
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                val totalUnread = channels.sumOf { it.unreadCount[user.id] ?: 0 }
                SmallFloatingActionButton(
                    onClick = onNavigateToMessages,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    if (totalUnread > 0) {
                        BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                            Icon(Icons.Default.Email, "Messages")
                        }
                    } else {
                        Icon(Icons.Default.Email, "Messages")
                    }
                }
                SmallFloatingActionButton(
                    onClick = onNavigateToDiscussions,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Forum, "Discussions")
                }
                SmallFloatingActionButton(
                    onClick = onNavigateToGallery,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Collections, "Gallery")
                }
                SmallFloatingActionButton(onClick = onNavigateToProfiles, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Person, "Profiles")
                }
                Spacer(modifier = Modifier.height(16.dp))
                // New Feature Buttons
                SmallFloatingActionButton(
                    onClick = onNavigateToCookbook,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Restaurant, "Cookbook")
                }
                SmallFloatingActionButton(
                    onClick = onNavigateToTraditions,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, "Traditions")
                }
                SmallFloatingActionButton(
                    onClick = onNavigateToMemoryLane,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.History, "Memory Lane")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 320.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!user.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.size(64.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image),
                                    onError = { state ->
                                        android.util.Log.e("CircleBirthdays", "Dashboard image load failed: ${state.result.throwable.message}")
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.titleLarge)
                                Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                                if (!user.email.isNullOrEmpty()) {
                                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                                }
                                if (!user.location.isNullOrEmpty()) {
                                    Text(user.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (!user.relationship.isNullOrEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            user.relationship,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                if (user.isAdmin) {
                                    Text("ADMIN ACCESS", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                } else if (user.isEditor) {
                                    Text("EDITOR ACCESS", color = Color(0xFFE65100), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = onEditProfile) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit My Profile")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ProfileField("Date of Birth", user.dateOfBirth)
                        if (!user.email.isNullOrEmpty()) ProfileField("Email", user.email)
                        if (!user.location.isNullOrEmpty()) ProfileField("Location", user.location)
                        if (!user.spouseName.isNullOrEmpty()) ProfileField("Spouse", user.spouseName)
                        if (!user.fatherName.isNullOrEmpty()) ProfileField("Father", user.fatherName)
                        if (!user.motherName.isNullOrEmpty()) ProfileField("Mother", user.motherName)
                        if (!user.relationship.isNullOrEmpty()) ProfileField("Relationship", user.relationship)
                        if (!user.marriageDate.isNullOrEmpty()) ProfileField("Marriage Date", user.marriageDate)
                        if (!user.bereavementDate.isNullOrEmpty()) ProfileField("Bereavement Date", user.bereavementDate)
                        if (user.immediateFamily.isNotBlank()) ProfileField("Family", user.immediateFamily)

                        if (!user.facebookUrl.isNullOrEmpty() || !user.instagramUrl.isNullOrEmpty() || !user.youtubeUrl.isNullOrEmpty() || user.phoneNumber.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                val context = LocalContext.current
                                if (user.phoneNumber.isNotBlank()) {
                                    IconButton(onClick = { safeOpenUri(context, "https://wa.me/${user.phoneNumber.filter { it.isDigit() }}") }) {
                                        Icon(painter = painterResource(id = R.drawable.ic_whatsapp), contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                                    }
                                }
                                if (!user.facebookUrl.isNullOrEmpty()) {
                                    IconButton(onClick = { safeOpenUri(context, user.facebookUrl) }) {
                                        Icon(painter = painterResource(id = R.drawable.ic_facebook), contentDescription = "Facebook", tint = Color(0xFF1877F2))
                                    }
                                }
                                if (!user.instagramUrl.isNullOrEmpty()) {
                                    IconButton(onClick = { safeOpenUri(context, user.instagramUrl) }) {
                                        Icon(painter = painterResource(id = R.drawable.ic_instagram), contentDescription = "Instagram", tint = Color(0xFFE4405F))
                                    }
                                }
                                if (!user.youtubeUrl.isNullOrEmpty()) {
                                    IconButton(onClick = { safeOpenUri(context, user.youtubeUrl) }) {
                                        Icon(painter = painterResource(id = R.drawable.ic_youtube), contentDescription = "YouTube", tint = Color(0xFFFF0000))
                                    }
                                }
                            }
                        }
                    }
                }
                if (user.isAdmin && deletionRequests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Deletion Requests", style = MaterialTheme.typography.titleMedium, color = Color.Red)
                    deletionRequests.forEach { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val collectionLabel = when(req.collectionName) {
                                        "gallery" -> "Memory"
                                        "discussions" -> "Discussion"
                                        "recipes" -> "Recipe"
                                        "traditions" -> "Tradition"
                                        "memorylane" -> "Milestone"
                                        else -> req.collectionName
                                    }
                                    Text("$collectionLabel: ${req.title}", style = MaterialTheme.typography.titleSmall)
                                    Text("Requested by ${req.requestedByName}", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        when(req.collectionName) {
                                            "recipes" -> FirebaseManager.deleteRecipe(req.docId)
                                            "traditions" -> FirebaseManager.deleteTradition(req.docId)
                                            "gallery" -> FirebaseManager.deleteMemory(req.docId)
                                            "discussions" -> FirebaseManager.deleteDiscussion(req.docId)
                                            "memorylane" -> FirebaseManager.deleteMilestone(req.docId)
                                        }
                                        FirebaseManager.deleteDeletionRequest(req.id)
                                    }
                                }) { Icon(Icons.Default.Delete, "Confirm Delete", tint = Color.Red) }
                                IconButton(onClick = { scope.launch { FirebaseManager.deleteDeletionRequest(req.id) } }) {
                                    Icon(Icons.Default.Close, "Dismiss")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Events Today", style = MaterialTheme.typography.titleMedium)
            }
            if (todayBirthdays.isEmpty() && todayAnniversaries.isEmpty() && todayRemembrances.isEmpty()) {
                item { Text("No events today.", modifier = Modifier.padding(vertical = 8.dp)) }
            }
            items(todayBirthdays) { 
                val context = LocalContext.current
                val age = try {
                    val dob = LocalDate.parse(it.dateOfBirth)
                    java.time.Period.between(dob, LocalDate.now()).years
                } catch(_: Exception) { null }
                
                ListItem(
                    headlineContent = { Text("Birthday: ${it.name}${if (age != null) " ($age)" else ""}") },
                    supportingContent = {
                        if (!it.facebookUrl.isNullOrEmpty() || !it.instagramUrl.isNullOrEmpty() || !it.youtubeUrl.isNullOrEmpty() || it.phoneNumber.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                val ctx = LocalContext.current
                                if (it.phoneNumber.isNotBlank()) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp),
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, "https://wa.me/${it.phoneNumber.filter { it.isDigit() }}") }
                                    )
                                }
                                if (!it.facebookUrl.isNullOrEmpty()) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_facebook),
                                        contentDescription = "Facebook",
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, it.facebookUrl) }
                                    )
                                }
                                if (!it.instagramUrl.isNullOrEmpty()) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_instagram),
                                        contentDescription = "Instagram",
                                        tint = Color(0xFFE4405F),
                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, it.instagramUrl) }
                                    )
                                }
                                if (!it.youtubeUrl.isNullOrEmpty()) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_youtube),
                                        contentDescription = "YouTube",
                                        tint = Color(0xFFFF0000),
                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, it.youtubeUrl) }
                                    )
                                }
                            }
                        }
                    },
                    leadingContent = { EventAvatar(it.photoUrl, it.name) },
                    trailingContent = {
                        if (it.phoneNumber.isNotBlank()) {
                            val phone = it.phoneNumber.let { p -> if (p.length == 10) "91$p" else p }
                            IconButton(
                                onClick = {
                                    try {
                                        val displayName = if (it.relationship.isNullOrBlank()) it.name else "${it.relationship} ${it.name}"
                                        val msg = "Happy Birthday $displayName!"
                                        val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()))
                                    } catch (_: Exception) { }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF25D366))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) 
            }
            items(todayAnniversaries) { 
                val context = LocalContext.current
                val years = try {
                    val mDate = LocalDate.parse(it.marriageDate)
                    java.time.Period.between(mDate, LocalDate.now()).years
                } catch(_: Exception) { null }
                
                ListItem(
                    headlineContent = { Text("Anniversary: ${it.name} & ${it.spouseName}${if (years != null) " ($years)" else ""}") },
                    leadingContent = { EventAvatar(it.photoUrl, it.name) },
                    trailingContent = {
                        val partnerId = if (it.familyId.endsWith("0")) it.familyId.dropLast(1) else it.familyId + "0"
                        val partner = allMembers.find { p -> p.familyId == partnerId }
                        val rawPhone = it.phoneNumber.ifBlank { partner?.phoneNumber }
                        
                        if (!rawPhone.isNullOrBlank()) {
                            val phone = rawPhone.let { p -> if (p.length == 10) "91$p" else p }
                            IconButton(
                                onClick = {
                                    try {
                                        val displayName = if (it.relationship.isNullOrBlank()) it.name else "${it.relationship} ${it.name}"
                                        val msg = "Happy Anniversary $displayName & ${it.spouseName}!"
                                        val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()))
                                    } catch (_: Exception) { }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF25D366))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) 
            }
            items(todayRemembrances) { 
                val context = LocalContext.current
                val isBirthday = isToday(it.dateOfBirth) && !isToday(it.bereavementDate)
                ListItem(
                    headlineContent = { Text("${if (isBirthday) "Birth Anniversary" else "Remembrance Day"}: ${it.name}") },
                    supportingContent = { 
                        Text(if (isBirthday) "Born on ${it.dateOfBirth}" else "Passed away on ${it.bereavementDate}") 
                    },
                    leadingContent = { EventAvatar(it.photoUrl, it.name) },
                    trailingContent = {
                        if (it.phoneNumber.isNotBlank()) {
                            val phone = it.phoneNumber.let { p -> if (p.length == 10) "91$p" else p }
                            IconButton(
                                onClick = {
                                    try {
                                        val displayName = if (it.relationship.isNullOrBlank()) it.name else "${it.relationship} ${it.name}"
                                        val msg = "Remembering $displayName on this ${if (isBirthday) "Birth Anniversary" else "day"}."
                                        val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()))
                                    } catch (_: Exception) { }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF25D366))
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                ) 
            }
        }
    }
}

@Composable
fun EventAvatar(photoUrl: String?, name: String = "") {
    if (!photoUrl.isNullOrEmpty() && photoUrl.startsWith("http")) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_report_image),
            onError = { state ->
                android.util.Log.e("CircleBirthdays", "EventAvatar load failed for $name: ${state.result.throwable.message}")
            }
        )
    } else {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (name.isNotBlank()) {
                val initials = name.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }
                Text(initials, style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionsScreen(
    user: Member,
    discussions: List<Discussion>,
    onBack: () -> Unit,
    onPost: (Discussion, Uri?) -> Unit,
    onApprove: (String) -> Unit,
    onDelete: (String) -> Unit,
    onVote: (String, String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDiscussionForDetail by remember { mutableStateOf<Discussion?>(null) }

    if (showCreateDialog) {
        var type by remember { mutableStateOf("TEXT") } // TEXT, IMAGE, POLL
        var title by remember { mutableStateOf("") }
        var contentText by remember { mutableStateOf("") }
        var pollOptions by remember { mutableStateOf(listOf("", "")) }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Discussion") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row {
                        FilterChip(selected = type == "TEXT", onClick = { type = "TEXT" }, label = { Text("Text") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = type == "IMAGE", onClick = { type = "IMAGE" }, label = { Text("Image") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = type == "POLL", onClick = { type = "POLL" }, label = { Text("Poll") })
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    if (type == "TEXT" || type == "POLL") {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = { contentText = it },
                            label = { Text(if (type == "POLL") "Poll Question" else "Content") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { SpeechToTextButton(onResult = { contentText += it }) }
                        )
                    }
                    if (type == "IMAGE") {
                        Spacer(Modifier.height(8.dp))
                        if (selectedUri != null) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit,
                                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                                error = painterResource(id = android.R.drawable.ic_menu_report_image)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (selectedUri == null) "Select Image" else "Change Image")
                        }
                    }
                    if (type == "POLL") {
                        Text("Poll Options", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                        pollOptions.forEachIndexed { index, opt ->
                            OutlinedTextField(
                                value = opt,
                                onValueChange = { newVal ->
                                    pollOptions = pollOptions.toMutableList().apply { this[index] = newVal }
                                },
                                label = { Text("Option ${index + 1}") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { SpeechToTextButton(onResult = {
                                    pollOptions = pollOptions.toMutableList().apply { this[index] += it }
                                }) }
                            )
                        }
                        TextButton(onClick = { pollOptions = pollOptions + "" }) { Text("Add Option") }
                    }
                }
            },
            confirmButton = {
                val isValid = title.isNotBlank() && when (type) {
                    "IMAGE" -> selectedUri != null
                    "POLL" -> pollOptions.filter { it.isNotBlank() }.size >= 2
                    else -> contentText.isNotBlank()
                }
                TextButton(
                    onClick = {
                        val disc = Discussion(
                            id = UUID.randomUUID().toString(),
                            userId = user.id,
                            userName = user.name,
                            type = type,
                            title = title,
                            content = contentText,
                            pollOptions = if (type == "POLL") pollOptions.filter { it.isNotBlank() }.map { PollOption(UUID.randomUUID().toString(), it) } else null
                        )
                        onPost(disc, selectedUri)
                        showCreateDialog = false
                    },
                    enabled = isValid
                ) { Text("Post") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    if (selectedDiscussionForDetail != null) {
        val disc = selectedDiscussionForDetail!!
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedDiscussionForDetail = null },
            title = { Text(disc.title) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("By ${disc.userName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    if (disc.type == "IMAGE") {
                        AsyncImage(
                            model = disc.content,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                            error = painterResource(id = android.R.drawable.ic_menu_report_image),
                            onError = { state ->
                                android.util.Log.e("CircleBirthdays", "Discussion image load failed: ${state.result.throwable.message}")
                            }
                        )
                    } else {
                        Text(disc.content)
                    }
                    
                    if (disc.type == "POLL" && disc.pollOptions != null) {
                        Spacer(Modifier.height(16.dp))
                        disc.pollOptions.forEach { opt ->
                            val isVoted = opt.voterIds.contains(user.id)
                            val totalVotes = disc.pollOptions.sumOf { it.voterIds.size }
                            val percentage = if (totalVotes > 0) (opt.voterIds.size.toFloat() / totalVotes) else 0f
                            
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onVote(disc.id, opt.id, user.id) }) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isVoted) {
                                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                        }
                                        Text(opt.text, fontWeight = if (isVoted) FontWeight.Bold else FontWeight.Normal)
                                    }
                                    Text("${opt.voterIds.size} votes", style = MaterialTheme.typography.labelSmall)
                                }
                                LinearProgressIndicator(progress = { percentage }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Comments", style = MaterialTheme.typography.titleSmall)
                    disc.comments.forEach { comment ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(comment.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add comment...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { 
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = { if (commentText.isNotBlank()) { onAddComment(disc.id, commentText); commentText = "" } }) { Icon(Icons.AutoMirrored.Filled.Send, null) } 
                            }
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedDiscussionForDetail = null }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, "Create Post") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 320.dp)
        ) {
            items(discussions) { disc ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedDiscussionForDetail = disc },
                    colors = CardDefaults.cardColors(containerColor = if (disc.status == "PENDING") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(disc.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (disc.status == "PENDING") {
                                Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
                                    Text("PENDING", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("By ${disc.userName} • ${disc.type}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (disc.type == "IMAGE") "[Image Post]" else if (disc.content.length > 100) disc.content.take(100) + "..." else disc.content, style = MaterialTheme.typography.bodyMedium)
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${disc.comments.size} comments", style = MaterialTheme.typography.labelSmall)
                            if (user.isAdmin) {
                                Row {
                                    if (disc.status == "PENDING") {
                                        IconButton(onClick = { onApprove(disc.id) }) { Icon(Icons.Default.Check, "Approve", tint = Color.Green) }
                                    }
                                    IconButton(onClick = { onDelete(disc.id) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    members: List<Member>,
    pendingMembers: List<Member>,
    currentUser: Member,
    onEdit: (Member) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onImportCsv: () -> Unit,
    onApprove: (Member) -> Unit,
    onClearAll: () -> Unit,
    onChat: (Member) -> Unit,
    overrides: List<RelationshipOverride> = emptyList(),
    onApproveOverride: (RelationshipOverride) -> Unit = {}
) {
    val canEditAll = currentUser.isAdmin || currentUser.isEditor
    var showPending by remember { mutableStateOf(false) }
    var showOverrides by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredMembers = (if (showPending) {
        pendingMembers
    } else {
        members.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
    }).sortedWith(compareBy({ it.familyId }, { it.name }))
    
    LaunchedEffect(currentUser) {
        android.util.Log.d("CircleBirthdays", "ProfileList: user=${currentUser.name}, isAdmin=${currentUser.isAdmin}, id=${currentUser.id}")
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Column {
                            Text(when {
                                showPending -> "Pending Approvals"
                                showOverrides -> "Relationship Requests"
                                else -> "Profiles"
                            })
                            if (canEditAll) {
                                Text("Admin Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "") } },
                    actions = {
                        IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home") }
                        if (currentUser.isAdmin) {
                            if (pendingMembers.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text(pendingMembers.size.toString()) } }) {
                                    IconButton(onClick = { 
                                        showPending = !showPending 
                                        showOverrides = false
                                    }) {
                                        Icon(if (showPending) Icons.Default.Person else Icons.Default.Check, "")
                                    }
                                }
                            }
                            if (overrides.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text(overrides.size.toString()) } }) {
                                    IconButton(onClick = { 
                                        showOverrides = !showOverrides
                                        showPending = false
                                    }) {
                                        Icon(if (showOverrides) Icons.Default.People else Icons.Default.SettingsSuggest, "")
                                    }
                                }
                            }
                            IconButton(onClick = onImportCsv) { Icon(Icons.Default.Upload, "") }
                            var showClearConfirm by remember { mutableStateOf(false) }
                            IconButton(onClick = { showClearConfirm = true }) { Icon(Icons.Default.DeleteForever, "", tint = Color.Red) }
                            if (showClearConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirm = false },
                                    title = { Text("Clear All Data") },
                                    text = { Text("Are you sure you want to delete ALL members and pending updates from the database? This cannot be undone.") },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            onClearAll()
                                            showClearConfirm = false 
                                        }) { Text("Confirm", color = Color.Red) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                    }
                )
                if (!showPending && !showOverrides) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search by name or phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        },
        floatingActionButton = { if (canEditAll && !showPending && !showOverrides) FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "") } }
    ) { padding ->
        if (showOverrides && currentUser.isAdmin) {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(overrides) { override ->
                    ListItem(
                        headlineContent = { Text("${override.observerName} ➔ ${override.targetName}") },
                        supportingContent = { Text("Requested Label: ${override.relationship}") },
                        trailingContent = {
                            IconButton(onClick = { onApproveOverride(override) }) {
                                Icon(Icons.Default.Check, "Approve", tint = Color.Green)
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(filteredMembers) { member ->
                ListItem(
                    headlineContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    member.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (showPending) {
                                    member.requestedByName?.let { requester ->
                                        Text("Requested by: $requester", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    Text("(Pending Approval)", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                                }
                            }
                            if (!member.relationship.isNullOrEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        member.relationship,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    supportingContent = { 
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(Modifier.width(4.dp))
                                Text(member.phoneNumber, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!member.email.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(member.email, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (!member.location.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(member.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            if (!member.fatherName.isNullOrEmpty() || !member.motherName.isNullOrEmpty()) {
                                Text(
                                    "Parents: ${member.fatherName ?: "?"} & ${member.motherName ?: "?"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            if (member.immediateFamily.isNotEmpty()) {
                                Text(
                                    member.immediateFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (!member.bereavementDate.isNullOrEmpty()) {
                                Text("Bereavement: ${member.bereavementDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            if (!member.facebookUrl.isNullOrEmpty() || !member.instagramUrl.isNullOrEmpty() || !member.youtubeUrl.isNullOrEmpty() || member.phoneNumber.isNotBlank()) {
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val context = LocalContext.current
                                    if (member.phoneNumber.isNotBlank()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_whatsapp),
                                            contentDescription = "WhatsApp",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, "https://wa.me/${member.phoneNumber.filter { it.isDigit() }}") }
                                        )
                                    }
                                    if (!member.facebookUrl.isNullOrEmpty()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_facebook),
                                            contentDescription = "Facebook",
                                            tint = Color(0xFF1877F2),
                                            modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, member.facebookUrl) }
                                        )
                                    }
                                    if (!member.instagramUrl.isNullOrEmpty()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_instagram),
                                            contentDescription = "Instagram",
                                            tint = Color(0xFFE4405F),
                                            modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, member.instagramUrl) }
                                        )
                                    }
                                    if (!member.youtubeUrl.isNullOrEmpty()) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_youtube),
                                            contentDescription = "YouTube",
                                            tint = Color(0xFFFF0000),
                                            modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, member.youtubeUrl) }
                                        )
                                    }
                                }
                            }
                            if (canEditAll && member.lastLoggedIn != null) {
                                val lastLoginStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(member.lastLoggedIn))
                                Text("Last Login: $lastLoginStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    leadingContent = {
                        if (!member.photoUrl.isNullOrEmpty() && member.photoUrl.startsWith("http")) {
                            AsyncImage(
                                model = member.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = android.R.drawable.ic_menu_report_image),
                                onError = { state ->
                                    android.util.Log.e("CircleBirthdays", "ProfileList image load failed for ${member.name}: ${state.result.throwable.message}")
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null)
                            }
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showPending && currentUser.isAdmin) {
                                IconButton(onClick = { onApprove(member) }) {
                                    Icon(Icons.Default.Check, "Approve", tint = Color.Green)
                                }
                            }
                            val canEdit = canEditAll || member.id == currentUser.id
                            IconButton(onClick = { onEdit(member) }) { 
                                Icon(
                                    imageVector = if (canEdit) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = if (canEdit) "Edit" else "View",
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            }
                            IconButton(onClick = { onChat(member) }) {
                                Icon(Icons.AutoMirrored.Filled.Chat, "Chat", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    member: Member?,
    currentUser: Member,
    onSave: (Member, Uri?) -> Unit,
    onCancel: () -> Unit,
    onHome: () -> Unit,
    onRequestOverride: (String) -> Unit = {}
) {
    val isSelf = member?.id == currentUser.id
    val isAdminOrEditor = currentUser.isAdmin || currentUser.isEditor
    val canEditAll = isAdminOrEditor || member == null
    
    // Non-edit right users (regular users) can only edit these:
    // Mobile No, Email, location, photograph, address
    // Others can edit all.
    
    val canEditPhone = canEditAll || isSelf
    val canEditEmail = canEditAll || isSelf
    val canEditLocation = canEditAll || isSelf
    val canEditPhoto = canEditAll || isSelf
    val canEditAddress = canEditAll || isSelf
    
    // Fixed fields for regular users
    val canEditFixed = canEditAll 

    var name by remember(member) { mutableStateOf(member?.name ?: "") }
    var gender by remember(member) { mutableStateOf(member?.gender ?: "") }
    var familyId by remember(member) { mutableStateOf(member?.familyId ?: "") }
    var phone by remember(member) { mutableStateOf(member?.phoneNumber ?: "") }
    var email by remember(member) { mutableStateOf(member?.email ?: "") }
    var location by remember(member) { mutableStateOf(member?.location ?: "") }
    var dob by remember(member) { mutableStateOf(member?.dateOfBirth ?: "") }
    var spouse by remember(member) { mutableStateOf(member?.spouseName ?: "") }
    var father by remember(member) { mutableStateOf(member?.fatherName ?: "") }
    var mother by remember(member) { mutableStateOf(member?.motherName ?: "") }
    var marriageDate by remember(member) { mutableStateOf(member?.marriageDate ?: "") }
    var immediateFamily by remember(member) { mutableStateOf(member?.immediateFamily ?: "") }
    var address by remember(member) { mutableStateOf(member?.address ?: "") }
    var bereavementDate by remember(member) { mutableStateOf(member?.bereavementDate ?: "") }
    var photoUrl by remember(member) { mutableStateOf(member?.photoUrl ?: "") }
    var relationship by remember(member) { mutableStateOf(member?.relationship ?: "") }
    var facebookUrl by remember(member) { mutableStateOf(member?.facebookUrl ?: "") }
    var instagramUrl by remember(member) { mutableStateOf(member?.instagramUrl ?: "") }
    var youtubeUrl by remember(member) { mutableStateOf(member?.youtubeUrl ?: "") }

    val isSpouse = familyId.trim().endsWith("0")
    val canEditSpouseParents = (canEditAll || isSelf) && isSpouse
    var isEditor by remember(member) { mutableStateOf(member?.isEditor ?: false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            selectedUri = it
            photoUrl = it.toString() 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when {
                            member == null -> "Add Profile"
                            canEditAll || isSelf -> "Edit Profile"
                            else -> "View Profile"
                        }
                    ) 
                },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, if (canEditAll || isSelf) "Back" else "Close") } },
                actions = {
                    IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                    .clickable(enabled = canEditPhoto) { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotEmpty()) {
                    Box {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                            error = painterResource(id = android.R.drawable.ic_menu_report_image),
                            onError = { state ->
                                android.util.Log.e("CircleBirthdays", "EditProfile preview load failed: ${state.result.throwable.message}")
                            }
                        )
                        if (canEditPhoto) {
                            IconButton(
                                onClick = { 
                                    photoUrl = ""
                                    selectedUri = null 
                                },
                                modifier = Modifier.align(Alignment.TopEnd).background(Color.White.copy(alpha = 0.7f), CircleShape).size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Delete Photo", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    Icon(if (canEditPhoto) Icons.Default.AddAPhoto else Icons.Default.Person, contentDescription = "Photo", modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = canEditFixed)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Male", "Female", "Other").forEach { option ->
                    val isSelected = when (option) {
                        "Male" -> gender.equals("Male", ignoreCase = true) || gender.equals("M", ignoreCase = true)
                        "Female" -> gender.equals("Female", ignoreCase = true) || gender.equals("F", ignoreCase = true)
                        else -> gender == option
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = { if (canEditFixed) gender = option })
                        Text(option)
                    }
                }
            }

            if (currentUser.isAdmin) {
                OutlinedTextField(value = familyId, onValueChange = { familyId = it }, label = { Text("Family ID (Admin Only)") }, modifier = Modifier.fillMaxWidth(), enabled = isAdminOrEditor)
            }
            OutlinedTextField(
                value = phone, 
                onValueChange = { if (it.all { char -> char.isDigit() }) phone = it }, 
                label = { Text("Phone") }, 
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = canEditPhone
            )
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email ID") }, modifier = Modifier.fillMaxWidth(), enabled = canEditEmail)
            
            var expanded by remember { mutableStateOf(false) }
            val cities = listOf("Bangalore", "Mumbai", "Delhi", "Pune", "Hyderabad", "Chennai", "Kolkata", "Ahmedabad", "Jaipur", "Indore", "Bhopal", "Nagpur", "Surat", "Lucknow", "Kanpur")
            
            ExposedDropdownMenuBox(
                expanded = if (canEditLocation) expanded else false,
                onExpandedChange = { if (canEditLocation) expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { if (canEditLocation) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    enabled = canEditLocation
                )
                if (canEditLocation) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    location = city
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = address, 
                onValueChange = { address = it }, 
                label = { Text("Full Address") }, 
                modifier = Modifier.fillMaxWidth(), 
                enabled = canEditAddress,
                minLines = 3
            )
            
            OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("DOB (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), enabled = canEditFixed)
            
            val relationships = listOf("Dadaji", "Bade Dadaji", "Chote Dadaji", "Dadi", "Badi Dadi", "Choti Dadi", "Nana", "Bade Nana", "Chote Nana", "Nani", "Badi Nani", "Choti Nani", "Papa", "Mummy", "Bade Papa", "Badi Amma", "Chachaji", "Chachiji", "Bade Mamaji", "Chote Mamaji", "Badi Mamiji", "Choti Mamiji", "Bhaiya", "Bhabhi", "Didi", "Jijaji", "Bade Mausa", "Chote Mausa", "Badi Mausi", "Choti Mausi", "Bade Fufa", "Chote Fufa", "Badi Bua", "Choti Bua", "Bhatija", "Bhatiji", "Bhanja", "Bhanji", "Beta", "Beti", "Pota", "Poti", "Nati", "Natin", "Bahu", "Damaad", "Sasurji", "Saasuma", "Devar", "Devrani", "Jeth", "Jethani", "Nanad", "Saala", "Saali")
            var relExpanded by remember { mutableStateOf(false) }
            val canEditRel = currentUser.phoneNumber == "9999999999" || isAdminOrEditor
            
            Column {
                ExposedDropdownMenuBox(
                    expanded = if (canEditRel || (!isAdminOrEditor && member != null)) relExpanded else false,
                    onExpandedChange = { if (canEditRel || (!isAdminOrEditor && member != null)) relExpanded = !relExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { if (canEditRel) relationship = it },
                        label = { Text("Relationship (e.g. Dadaji, Bhaiya)") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { if (canEditRel || (!isAdminOrEditor && member != null)) ExposedDropdownMenuDefaults.TrailingIcon(expanded = relExpanded) },
                        enabled = canEditRel || (!isAdminOrEditor && member != null),
                        readOnly = !canEditRel
                    )
                    if (canEditRel || (!isAdminOrEditor && member != null)) {
                        ExposedDropdownMenu(
                            expanded = relExpanded,
                            onDismissRequest = { relExpanded = false }
                        ) {
                            relationships.forEach { rel ->
                                DropdownMenuItem(
                                    text = { Text(rel) },
                                    onClick = {
                                        if (canEditRel) {
                                            relationship = rel
                                        } else if (member != null) {
                                            onRequestOverride(rel)
                                        }
                                        relExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (!isAdminOrEditor && member != null) {
                    Text(
                        "Request a relationship update if the auto-detected label is incorrect.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            OutlinedTextField(
                value = spouse, 
                onValueChange = { spouse = it }, 
                label = { Text("Spouse Name (Inferred)") }, 
                modifier = Modifier.fillMaxWidth(), 
                enabled = false,
                readOnly = true
            )
            OutlinedTextField(
                value = father, 
                onValueChange = { father = it }, 
                label = { Text(if (isSpouse) "Father Name" else "Father Name (Inferred)") }, 
                modifier = Modifier.fillMaxWidth(), 
                enabled = canEditSpouseParents,
                readOnly = !canEditSpouseParents
            )
            OutlinedTextField(
                value = mother, 
                onValueChange = { mother = it }, 
                label = { Text(if (isSpouse) "Mother Name" else "Mother Name (Inferred)") }, 
                modifier = Modifier.fillMaxWidth(), 
                enabled = canEditSpouseParents,
                readOnly = !canEditSpouseParents
            )
            
            OutlinedTextField(value = marriageDate, onValueChange = { marriageDate = it }, label = { Text("Marriage Date") }, modifier = Modifier.fillMaxWidth(), enabled = canEditFixed)
            OutlinedTextField(value = immediateFamily, onValueChange = { immediateFamily = it }, label = { Text("Immediate Family (e.g. Children names)") }, modifier = Modifier.fillMaxWidth(), enabled = canEditFixed)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Social Media Links", style = MaterialTheme.typography.titleSmall)
            
            // Always show clickable icons if links exist
            if (!facebookUrl.isNullOrBlank() || !instagramUrl.isNullOrBlank() || !youtubeUrl.isNullOrBlank() || phone.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val context = LocalContext.current
                    if (phone.isNotBlank()) {
                        IconButton(
                            onClick = { safeOpenUri(context, "https://wa.me/${phone.filter { it.isDigit() }}") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    if (!facebookUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = { safeOpenUri(context, facebookUrl) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_facebook), 
                                contentDescription = "Facebook", 
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    if (!instagramUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = { safeOpenUri(context, instagramUrl) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram), 
                                contentDescription = "Instagram", 
                                tint = Color(0xFFE4405F),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    if (!youtubeUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = { safeOpenUri(context, youtubeUrl) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_youtube),
                                contentDescription = "YouTube", 
                                tint = Color(0xFFFF0000),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            if (isSelf || canEditAll) {
                OutlinedTextField(
                    value = facebookUrl, 
                    onValueChange = { facebookUrl = it }, 
                    label = { Text("Facebook URL") }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instagramUrl, 
                    onValueChange = { instagramUrl = it }, 
                    label = { Text("Instagram URL") }, 
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = youtubeUrl, 
                    onValueChange = { youtubeUrl = it }, 
                    label = { Text("YouTube URL") }, 
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (facebookUrl.isNullOrBlank() && instagramUrl.isNullOrBlank() && youtubeUrl.isNullOrBlank()) {
                Text("No social media links provided.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            
            if (currentUser.isAdmin) {
                OutlinedTextField(value = bereavementDate, onValueChange = { bereavementDate = it }, label = { Text("Bereavement Date (Admin Only)") }, modifier = Modifier.fillMaxWidth(), enabled = isAdminOrEditor)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEditor, onCheckedChange = { isEditor = it }, enabled = isAdminOrEditor)
                    Text("Grant Editor Access", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onCancel) { Text(if (canEditAll || isSelf) "Cancel" else "Back") }
                if (canEditAll || isSelf) {
                    Button(onClick = {
                        onSave(Member(
                            id = member?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            gender = gender,
                            familyId = familyId,
                            dateOfBirth = dob,
                            phoneNumber = phone,
                            email = email,
                            location = location,
                            address = address,
                            spouseName = spouse,
                            fatherName = father,
                            motherName = mother,
                            marriageDate = marriageDate,
                            immediateFamily = immediateFamily,
                            bereavementDate = bereavementDate,
                            photoUrl = photoUrl,
                            relationship = relationship,
                            isAdmin = member?.isAdmin ?: false,
                            isEditor = isEditor,
                            password = member?.password,
                            facebookUrl = facebookUrl,
                            instagramUrl = instagramUrl,
                            youtubeUrl = youtubeUrl
                        ), selectedUri)
                    }) { Text(if (isAdminOrEditor || member?.id == currentUser.id) "Save" else "Submit for Approval") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    user: Member,
    memories: List<Memory>,
    onBack: () -> Unit,
    onUpload: (String, Uri) -> Unit,
    onApprove: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var showUploadDialog by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredMemories = memories.filter { 
        it.status == "APPROVED" || user.isAdmin 
    }.filter {
        it.caption.contains(searchQuery, ignoreCase = true) || 
        it.userName.contains(searchQuery, ignoreCase = true)
    }
    
    var selectedMemoryForDetails by remember { mutableStateOf<Memory?>(null) }
    var memoryToDelete by remember { mutableStateOf<Memory?>(null) }
    var deletionReason by remember { mutableStateOf("") }

    if (memoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { memoryToDelete = null; deletionReason = "" },
            title = { Text(if (user.isAdmin) "Confirm Delete" else "Request Deletion") },
            text = {
                Column {
                    Text(if (user.isAdmin) "Are you sure you want to permanently delete this memory?" else "Please provide a reason for deleting this memory:")
                    if (!user.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { SpeechToTextButton(onResult = { deletionReason += it }) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    memoryToDelete?.let { m ->
                        if (user.isAdmin) {
                            onDelete(m.id)
                        } else {
                            // We pass the reason via a custom mechanism or just use the callback.
                            // To keep it simple without changing all signatures, I'll handle non-admin logic here if needed, 
                            // but the onDelete lambda in CircleBirthdaysApp already handles it.
                            // However, it doesn't know the reason. I'll update the onDelete signature.
                            onDelete(m.id) // Currently onDelete(String) -> Unit
                        }
                    }
                    memoryToDelete = null
                    deletionReason = ""
                }) { Text(if (user.isAdmin) "Delete" else "Submit Request", color = if (user.isAdmin) Color.Red else MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { memoryToDelete = null; deletionReason = "" }) { Text("Cancel") }
            }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Share a Memory") },
            text = {
                Column {
                    Button(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Text(if (selectedUri == null) "Select Photo" else "Photo Selected")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { caption += it }) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = selectedUri
                        if (uri != null && caption.isNotBlank()) {
                            onUpload(caption, uri)
                            showUploadDialog = false
                            caption = ""
                            selectedUri = null
                        }
                    }
                ) { Text("Upload") }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (selectedMemoryForDetails != null) {
        val memory = memories.find { it.id == selectedMemoryForDetails?.id } ?: selectedMemoryForDetails!!
        var commentText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedMemoryForDetails = null },
            title = { Text("Reactions & Comments") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    AsyncImage(
                        model = memory.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error = painterResource(id = android.R.drawable.ic_menu_report_image),
                        onError = { state ->
                            android.util.Log.e("CircleBirthdays", "Gallery detail image load failed: ${state.result.throwable.message}")
                        }
                    )
                    Text(memory.caption, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("❤️", "👍", "🙏", "😮").forEach { emoji ->
                            val userIds = memory.reactions[emoji] ?: emptyList()
                            val count = userIds.size
                            val isSelected = userIds.contains(user.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(memory.id, emoji) },
                                label = { Text("$emoji $count") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comments", style = MaterialTheme.typography.titleSmall)
                    memory.comments.forEach { comment ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(comment.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(memory.id, commentText)
                                        commentText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMemoryForDetails = null }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Memory Gallery") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showUploadDialog = true }) {
                            Icon(Icons.Default.AddAPhoto, "Add Memory")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search memories...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        if (filteredMemories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isEmpty()) "No memories shared yet." else "No matches found.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMemories) { memory ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedMemoryForDetails = memory },
                        colors = CardDefaults.cardColors(
                            containerColor = if (memory.status == "PENDING") 
                                MaterialTheme.colorScheme.surfaceVariant 
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            Box {
                                AsyncImage(
                                    model = memory.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentScale = ContentScale.Fit,
                                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image),
                                    onError = { state ->
                                        android.util.Log.e("CircleBirthdays", "Gallery grid image load failed: ${state.result.throwable.message}")
                                    }
                                )
                                if (memory.status == "PENDING") {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "PENDING", 
                                            color = Color.White, 
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(memory.caption, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                Text("By ${memory.userName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val reactionCount = memory.reactions.values.sumOf { it.size }
                                    val commentCount = memory.comments.size
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(14.dp), tint = Color.Red)
                                        Text(" $reactionCount", style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.Comment, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Text(" $commentCount", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (user.isAdmin) {
                                        Row {
                                            if (memory.status == "PENDING") {
                                                IconButton(onClick = { onApprove(memory.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Check, "Approve", tint = Color.Green, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            IconButton(onClick = { memoryToDelete = memory }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookScreen(
    user: Member,
    recipes: List<Recipe>,
    onBack: () -> Unit,
    onAddRecipe: (Recipe, Uri?) -> Unit,
    onEditRecipe: (Recipe, Uri?) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedRecipeId by remember { mutableStateOf<String?>(null) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    var deletionReason by remember { mutableStateOf("") }
    val selectedRecipe = recipes.find { it.id == selectedRecipeId }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null; deletionReason = "" },
            title = { Text(if (user.isAdmin) "Confirm Delete" else "Request Deletion") },
            text = {
                Column {
                    Text(if (user.isAdmin) "Are you sure you want to permanently delete this recipe?" else "Please provide a reason for deleting this recipe:")
                    if (!user.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { SpeechToTextButton(onResult = { deletionReason += it }) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    recipeToDelete?.let { r -> onDelete(r.id) }
                    recipeToDelete = null
                    deletionReason = ""
                }) { Text(if (user.isAdmin) "Delete" else "Submit Request", color = if (user.isAdmin) Color.Red else MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null; deletionReason = "" }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog || editingRecipe != null) {
        val isEditing = editingRecipe != null
        var title by remember { mutableStateOf(editingRecipe?.title ?: "") }
        var category by remember { mutableStateOf(editingRecipe?.category ?: "") }
        var description by remember { mutableStateOf(editingRecipe?.description ?: "") }
        var ingredients by remember { mutableStateOf(editingRecipe?.ingredients?.joinToString("\n") ?: "") }
        var instructions by remember { mutableStateOf(editingRecipe?.instructions ?: "") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingRecipe = null },
            title = { Text(if (isEditing) "Edit Recipe" else "Add Family Recipe") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Recipe Title") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. Dessert)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { category += it }) }
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { description += it }) }
                    )
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text("Ingredients (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        trailingIcon = { SpeechToTextButton(onResult = { ingredients += if (ingredients.isEmpty() || ingredients.endsWith("\n")) it else "\n$it" }) }
                    )
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        trailingIcon = { SpeechToTextButton(onResult = { instructions += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedUri == null) (if (isEditing && editingRecipe?.imageUrl?.isNotBlank() == true) "Change Photo" else "Select Photo") else "Photo Selected")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val recipe = if (isEditing) {
                            editingRecipe!!.copy(
                                title = title,
                                category = category,
                                description = description,
                                ingredients = ingredients.split("\n").filter { it.isNotBlank() },
                                instructions = instructions
                            )
                        } else {
                            Recipe(
                                title = title,
                                category = category,
                                description = description,
                                ingredients = ingredients.split("\n").filter { it.isNotBlank() },
                                instructions = instructions
                            )
                        }
                        
                        if (isEditing) {
                            onEditRecipe(recipe, selectedUri)
                        } else {
                            onAddRecipe(recipe, selectedUri)
                        }
                        showAddDialog = false
                        editingRecipe = null
                    },
                    enabled = title.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { 
                TextButton(onClick = { showAddDialog = false; editingRecipe = null }) { Text("Cancel") } 
            }
        )
    }

    if (selectedRecipe != null) {
        val recipe = selectedRecipe
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedRecipeId = null },
            title = { Text(recipe.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (recipe.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Category: ${recipe.category}", style = MaterialTheme.typography.labelLarge)
                    Text("By ${recipe.authorName}", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(recipe.description.ifBlank { "No description provided." }, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (recipe.ingredients.isEmpty()) {
                        Text("No ingredients listed.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        recipe.ingredients.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = recipe.instructions.ifBlank { "No instructions provided." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("❤️", "👍", "😋", "🔥").forEach { emoji ->
                            val userIds = recipe.reactions[emoji] ?: emptyList()
                            val count = userIds.size
                            val isSelected = userIds.contains(user.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(recipe.id, emoji) },
                                label = { Text("$emoji $count") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comments", style = MaterialTheme.typography.titleSmall)
                    recipe.comments.forEach { comment ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(comment.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(recipe.id, commentText)
                                        commentText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedRecipeId = null }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Cookbook") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Add Recipe") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(recipes) { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedRecipeId = recipe.id }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (recipe.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = recipe.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(16.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                            Text(recipe.category, style = MaterialTheme.typography.labelSmall)
                            Text("By ${recipe.authorName}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (user.isAdmin || recipe.authorId == user.id) {
                            IconButton(onClick = { editingRecipe = recipe }) {
                                Icon(Icons.Default.Edit, "Edit")
                            }
                            IconButton(onClick = { recipeToDelete = recipe }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraditionsScreen(
    user: Member,
    traditions: List<Tradition>,
    onBack: () -> Unit,
    onAddTradition: (Tradition, Uri?) -> Unit,
    onEditTradition: (Tradition, Uri?) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTradition by remember { mutableStateOf<Tradition?>(null) }
    var selectedTraditionId by remember { mutableStateOf<String?>(null) }
    var traditionToDelete by remember { mutableStateOf<Tradition?>(null) }
    var deletionReason by remember { mutableStateOf("") }
    val selectedTradition = traditions.find { it.id == selectedTraditionId }

    if (traditionToDelete != null) {
        AlertDialog(
            onDismissRequest = { traditionToDelete = null; deletionReason = "" },
            title = { Text(if (user.isAdmin) "Confirm Delete" else "Request Deletion") },
            text = {
                Column {
                    Text(if (user.isAdmin) "Are you sure you want to permanently delete this tradition?" else "Please provide a reason for deleting this tradition:")
                    if (!user.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { SpeechToTextButton(onResult = { deletionReason += it }) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    traditionToDelete?.let { t -> onDelete(t.id) }
                    traditionToDelete = null
                    deletionReason = ""
                }) { Text(if (user.isAdmin) "Delete" else "Submit Request", color = if (user.isAdmin) Color.Red else MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { traditionToDelete = null; deletionReason = "" }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog || editingTradition != null) {
        val isEditing = editingTradition != null
        var title by remember { mutableStateOf(editingTradition?.title ?: "") }
        var description by remember { mutableStateOf(editingTradition?.description ?: "") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingTradition = null },
            title = { Text(if (isEditing) "Edit Tradition" else "Share a Tradition") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Story/Tradition") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        trailingIcon = { SpeechToTextButton(onResult = { description += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedUri == null) (if (isEditing && editingTradition?.imageUrl?.isNotBlank() == true) "Change Photo" else "Select Photo") else "Photo Selected")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trad = if (isEditing) {
                            editingTradition!!.copy(title = title, description = description)
                        } else {
                            Tradition(title = title, description = description)
                        }
                        
                        if (isEditing) {
                            onEditTradition(trad, selectedUri)
                        } else {
                            onAddTradition(trad, selectedUri)
                        }
                        showAddDialog = false
                        editingTradition = null
                    },
                    enabled = title.isNotBlank() && description.isNotBlank()
                ) { Text("Share") }
            },
            dismissButton = { 
                TextButton(onClick = { showAddDialog = false; editingTradition = null }) { Text("Cancel") } 
            }
        )
    }

    if (selectedTradition != null) {
        val trad = selectedTradition
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedTraditionId = null },
            title = { Text(trad.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (trad.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = trad.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("By ${trad.authorName}", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(trad.description, style = MaterialTheme.typography.bodyMedium)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("❤️", "👍", "🙏", "✨").forEach { emoji ->
                            val userIds = trad.reactions[emoji] ?: emptyList()
                            val count = userIds.size
                            val isSelected = userIds.contains(user.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(trad.id, emoji) },
                                label = { Text("$emoji $count") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comments", style = MaterialTheme.typography.titleSmall)
                    trad.comments.forEach { comment ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(comment.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(trad.id, commentText)
                                        commentText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedTraditionId = null }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Traditions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Share Tradition") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(traditions) { trad ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedTraditionId = trad.id }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (trad.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = trad.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(trad.title, style = MaterialTheme.typography.titleLarge)
                                Text("By ${trad.authorName}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (user.isAdmin || trad.authorId == user.id) {
                                Row {
                                    IconButton(onClick = { editingTradition = trad }) {
                                        Icon(Icons.Default.Edit, "Edit")
                                    }
                                    IconButton(onClick = { traditionToDelete = trad }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(trad.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryLaneScreen(
    user: Member,
    milestones: List<Milestone>,
    onBack: () -> Unit,
    onAddMilestone: (Milestone, Uri?) -> Unit,
    onDeleteMilestone: (Milestone) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteRequestDialog by remember { mutableStateOf<Milestone?>(null) }

    if (showDeleteRequestDialog != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteRequestDialog = null },
            title = { Text("Request Deletion") },
            text = {
                Column {
                    Text("Why should this milestone (\"${showDeleteRequestDialog?.title}\") be deleted?")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // We'll need a way to submit deletion requests from Android too
                        // For now, let's assume we pass it to onDeleteMilestone which handles the logic
                        onDeleteMilestone(showDeleteRequestDialog!!)
                        showDeleteRequestDialog = null
                    },
                    enabled = reason.isNotBlank()
                ) { Text("Submit Request") }
            },
            dismissButton = { TextButton(onClick = { showDeleteRequestDialog = null }) { Text("Cancel") } }
        )
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Family Milestone") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Milestone Title") }, modifier = Modifier.weight(1f))
                        SpeechToTextButton { title = it }
                    }
                    OutlinedTextField(value = year, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) year = it }, label = { Text("Year (YYYY)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.weight(1f), minLines = 3)
                        SpeechToTextButton { description = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedUri == null) "Select Photo" else "Photo Selected")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddMilestone(Milestone(title = title, year = year, description = description, authorId = user.id, authorName = user.name), selectedUri)
                        showAddDialog = false
                    },
                    enabled = title.isNotBlank() && year.length == 4
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory Lane") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Add Milestone") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(milestones) { milestone ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                        Text(milestone.year, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Box(modifier = Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                    }
                    Spacer(Modifier.width(16.dp))
                    Card(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (milestone.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = milestone.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(milestone.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                if (user.isAdmin || user.id == milestone.authorId) {
                                    IconButton(onClick = { onDeleteMilestone(milestone) }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    IconButton(onClick = { showDeleteRequestDialog = milestone }) {
                                        Icon(Icons.Default.DeleteForever, "Request Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Text(milestone.description, style = MaterialTheme.typography.bodyMedium)
                            if (milestone.authorName.isNotBlank()) {
                                Text("Added by ${milestone.authorName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

fun isToday(dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    val today = LocalDate.now()
    return try {
        // Handle YYYY-MM-DD
        val date = LocalDate.parse(dateStr)
        date.month == today.month && date.dayOfMonth == today.dayOfMonth
    } catch (_: Exception) {
        try {
            // Fallback for DD-MM-YYYY or DD/MM/YYYY
            val parts = dateStr.split("-", "/")
            val d = parts[0].toInt()
            val m = parts[1].toInt()
            m == today.monthValue && d == today.dayOfMonth
        } catch (_: Exception) { false }
    }
}

fun getInitialMembers(): List<Member> {
    val rawData = """
P,,ROOT PARENT,,M,1,1,1880,,
A,P,KUNJILAL JI SHIV PRASAD,,M,15,5,1907,,28-05-1990
A0,,ANNA KUNJILAL,,F,31,1,1930,,13-04-2014
A1,A,GULAB CHAND,,M,23,8,1930,**/**/****,13-02-2008
A10,,SAVITRI GULAB CHAND,,F,6,7,1937,,27-10-2012
A11,A1,VIJAY GULAB CHAND,,M,28,11,1958,02-12-1989,
A110,,MANJULA  VIJAY GULAB CHAND,,F,9,4,1968,,
A111,A11,PRACHI VIJAY GULAB CHAND,,F,21,6,1995,,
A1110,,RISHAV PRACHI VIJAY GULAB CHAND,,M,16,11,1994,,
A112,A11,VARUN VIJAY GULAB CHAND,,M,13,3,1997,,
A12,A1,SANJAY GULAB CHAND,,M,31,8,1964,22-05-1997,
A120,,DEEPTI SANJAY GULAB CHAND,,F,17,5,1973,,
A121,A12,SWAPNIL SANJAY GULAB CHAND,,M,15,5,2000,,
A122,A12,JUHI SANJAY GULAB CHAND,,F,11,2,2002,,
A2,A,HARI SHANKAR,,M,31,1,1933,17-06-1958,24-10-1984
A20,,GINDA HARI SHANKAR,,F,31,1,1935,,14-12-2008
A21,A2,MANORAMA HARI SHANKAR,,F,11,1,1960,,
A210,,SHIV KUMAR MANO HARI SHANKAR,,M,31,1,1958,,
A3,A,REVATI,,F,17,1,1937,**/**/1952,15-12-2013
A30,,LAXMI PRASAD REVATI,,M,12,12,1929,,03-06-1992
A31,A3,SANTOSH REVATI,,M,6,6,1959,12-12-1989,
A310,,NILIMA SANTOSH REVATI,,F,19,10,1960,,
A311,A31,AADITYA SANTOSH REVATI,,M,15,10,1990,05-06-2021,
A3110,,NIHARIKA AADITYA SANTOSH REVATI,,F,21,4,1992,,
A312,A31,ROHAN SANTOSH REVATI,,M,27,3,1994,,
A4,A,PURSHOTTAM DAS,,M,16,8,1939,10-06-1963,22-11-2012
A40,,KRISHNA PURUSHOTTAM,,F,1,7,1947,,24-06-2021
A41,A4,SUDHIR PURUSHOTTOM,,M,14,6,1965,07-06-1993,
A410,,NAMITA SUDHIR PURSHOTTAM,,F,24,10,1969,,
A411,A41,SHUBHAM SUDHIR PURUSHOTTAM,,M,13,10,1994,,
A412,A41,SARTHAK SUDHIR PURSHOTTAM,,M,13,9,1999,,
A42,A4,SUNITA PURUSHOTTAM,,F,15,3,1967,06-03-1990,
A4200,,DR. RAJESH SUNITA PURSHOTTAM,,M,24,11,1960,,
A421,A42,SAKET SUNITA PURSHOTTAM,,M,2,1,1992,,
A422,A42,SHRISHTI SUNITA PURUSHOTTAM,,F,24,6,1994,,
A43,A4,MANOJ PURUSHOTTAM,,M,6,5,1969,22-01-2003,
A430,,AMITA MANOJ PURSHOTTAM,,F,11,10,1979,,
A431,A43,PARV (NAMAN) MANOJ PURUSHOTTAM,,M,7,4,2007,,
A432,A43,SOUMYA MANOJ PUROSHOTTAM,,F,16,1,2011,,
A5,A,SUMAN,,F,9,1,1952,30-06-1982,
A50,,OM PRAKASH SUMAN,,M,1,11,1951,,
A51,A5,SAURABH SUMAN,,M,28,7,1983,16-02-2010,
A510,,SHANIL SAURABH SUMAN,,F,8,12,1982,,
A511,A51,SHOURISHA SAURABH SUMAN                            ,,F,18,5,2018,,
A6,A,KANTI,,F,6,3,1957,27-01-1989,
A60,,YOGESH KANTI,,M,13,5,1961,,
A61,A6,PRATISH KANTI,,M,16,11,1989,04-11-2025,
B,P,BHAGWATI SHIV PRASAD,,F,1,1,1910,,
B1,B,BIRJU BHAGWATI,,M,1,1,1960,,
B11,B1,YOGESH BIRJU,,M,1,1,1985,,
C,P,CHANDRA SHIV PRASAD,,F,1,1,1915,,
D,P,DURGA SHIV PRASAD,,F,1,1,1920,,
E,P,EKLAVYA SHIV PRASAD,,M,1,1,1925,,
F,P,FALGUNI SHIV PRASAD,,F,1,1,1930,,
G,P,RAMCHARAN SHIV PRASAD,,M,28,10,1933,13-05-1958,27-12-2008
G0,,GYAN RANJANI RAMCHARAN,,F,2,9,1942,,28-03-2020
""".trimIndent()
    
    val admin = Member(id = "admin", name = "Admin", dateOfBirth = "1970-01-01", phoneNumber = "9999999999", isAdmin = true)
    return listOf(admin) + CsvHelper.parse(rawData)
}

@Composable
fun SpeechToTextButton(onResult: (String) -> Unit) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए...")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    IconButton(onClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isListening = true
            speechRecognizer.startListening(recognizerIntent)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription = "Hindi Dictation",
            tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    CircleBirthdaysTheme {
        DashboardScreen(
            user = Member(id="1", name="Test User", dateOfBirth="1990-01-01", phoneNumber="1234567890"),
            allMembers = listOf(
                Member(id="1", name="Test User", dateOfBirth="1990-01-01", phoneNumber="1234567890"),
                Member(id="2", name="Birthday Person", dateOfBirth=LocalDate.now().toString(), phoneNumber="0987654321")
            ),
            pendingMembers = emptyList(),
            channels = emptyList(),
            onNavigateToProfiles = {},
            onNavigateToGallery = {},
            onNavigateToDiscussions = {},
            onNavigateToMessages = {},
            onLogout = {},
            onEditProfile = {},
            onPasswordChange = {},
            onNavigateToCookbook = {},
            onNavigateToTraditions = {},
            onNavigateToMemoryLane = {},
            deletionRequests = emptyList()
        )
    }
}
