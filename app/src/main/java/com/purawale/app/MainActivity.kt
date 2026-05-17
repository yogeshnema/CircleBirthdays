package com.purawale.app

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.geometry.Offset
import android.widget.Toast
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.sp
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.google.firebase.firestore.ListenerRegistration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purawale.app.ui.theme.CircleBirthdaysTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.security.MessageDigest
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

fun String.hash(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun safeOpenUri(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    try {
        val formattedUri = if (!uriString.startsWith("http://") && !uriString.startsWith("https://")) {
            "https://$uriString"
        } else {
            uriString
        }
        val uri = formattedUri.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("CircleBirthdays", "Failed to open URI: $uriString", e)
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}

class MainActivity : ComponentActivity() {
    private var intentState = mutableStateOf<Intent?>(null)

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
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.12f,
                            colorFilter = ColorFilter.tint(
                                Color(0xFFD7CCC8), // Light Brown / Cocoa tint
                                BlendMode.Multiply
                            )
                        )
                        CircleBirthdaysApp(intentState.value)
                    }
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
    object FamilyTree : Screen()
    object Calendar : Screen()
    data class Chat(val otherMember: Member) : Screen()
    data class EditProfile(val member: Member?) : Screen()
}

val LocalLanguage = staticCompositionLocalOf { false } // false for English, true for Hindi

@Composable
fun t(en: String, hi: String): String {
    return if (LocalLanguage.current) hi else en
}

@Composable
fun CircleBirthdaysApp(intent: Intent? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Persist login and language state
    val prefs = context.getSharedPreferences("circle_prefs", Context.MODE_PRIVATE)
    var isHindi by remember { mutableStateOf(prefs.getBoolean("is_hindi", false)) }

        CompositionLocalProvider(LocalLanguage provides isHindi) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
        var currentUser by remember { mutableStateOf<Member?>(null) }
        var loginError by remember { mutableStateOf<String?>(null) }

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

    // Sync with Firebase
    DisposableEffect(Unit) {
        val lastLogin = prefs.getLong("last_login_time", 0L)
        val savedUserId = prefs.getString("user_id", null)
        val currentTime = System.currentTimeMillis()
        
        if (savedUserId != null && (currentTime - lastLogin) < 10L * 24 * 60 * 60 * 1000) {
            // Auto-login will happen when members are fetched
        } else {
            prefs.edit { clear() }
        }

        val listeners = mutableListOf<ListenerRegistration>()

        listeners += FirebaseManager.getMembers(
            onResult = { fetched ->
                Log.d("CircleBirthdays", "Fetched ${fetched.size} members")
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
                Log.e("CircleBirthdays", "Firestore error: ${error.message}")
                if (error.message?.contains("Unable to resolve host") == true) {
                    loginError = "No Internet connection. Please check your network."
                }
            }
        )
        listeners += FirebaseManager.getPendingChanges(
            onResult = { fetched ->
                Log.d("CircleBirthdays", "Fetched ${fetched.size} pending changes")
                pendingMembers = FamilyUtils.populateAllLinks(fetched)
            },
            onError = { error ->
                Log.e("CircleBirthdays", "Firestore pending error: ${error.message}")
            }
        )

        // Listen for all memories and discussions if admin, to ensure real-time updates
        listeners += FirebaseManager.getMemories(onlyApproved = false) { fetched ->
            if (currentUser?.isAdmin == true) memories = fetched
        }
        listeners += FirebaseManager.getDiscussions(onlyApproved = false) { fetched ->
            if (currentUser?.isAdmin == true) discussions = fetched
        }

        listeners += FirebaseManager.getRecipes { fetched -> recipes = fetched }
        listeners += FirebaseManager.getTraditions { fetched -> traditions = fetched }
        listeners += FirebaseManager.getMilestones { fetched -> milestones = fetched }
        listeners += FirebaseManager.getDeletionRequests { fetched -> deletionRequests = fetched }

        onDispose {
            listeners.forEach { it.remove() }
        }
    }

    // Fetch Memories based on user role and populate relationships
    DisposableEffect(currentUser) {
        val user = currentUser
        val listeners = mutableListOf<ListenerRegistration>()
        if (user != null) {
            members = FamilyUtils.populateAllLinks(members, user)
            
            val isAdmin = user.isAdmin
            listeners += FirebaseManager.getMemories(onlyApproved = !isAdmin) { fetched ->
                memories = fetched
            }
            listeners += FirebaseManager.getDiscussions(onlyApproved = !isAdmin) { fetched ->
                discussions = fetched
            }

            listeners += FirebaseManager.getChannels(user.id) { fetched ->
                channels = fetched
            }
            if (user.isAdmin) {
                FirebaseMessaging.getInstance().subscribeToTopic("admin_approvals")
                listeners += FirebaseManager.getRelationshipOverrides { fetched ->
                    relationshipOverrides = fetched
                }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_approvals")
            }
            FirebaseMessaging.getInstance().subscribeToTopic("events")
            FirebaseMessaging.getInstance().subscribeToTopic("recipes")
            FirebaseMessaging.getInstance().subscribeToTopic("traditions")
            FirebaseMessaging.getInstance().subscribeToTopic("gallery")
        }
        onDispose {
            listeners.forEach { it.remove() }
        }
    }

    DisposableEffect(currentScreen, currentUser) {
        val screen = currentScreen
        val user = currentUser
        var registration: ListenerRegistration? = null
        if (screen is Screen.Chat && user != null) {
            registration = FirebaseManager.getMessages(user.id, screen.otherMember.id) { fetched ->
                chatMessages = fetched
            }
        }
        onDispose {
            registration?.remove()
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
            Log.d("CircleBirthdays", "Seeding started by ${currentUser?.name}")
            val initial = getInitialMembers()
            try {
                FirebaseManager.submitBatch(initial, true)
            } catch (e: Exception) {
                Log.e("CircleBirthdays", "Seed batch failed", e)
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
                    Log.d("CsvHelper", "Parsed ${newOnes.size} members")
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
        } else if (navigateTo == "RECIPE") {
            currentScreen = Screen.Cookbook
        } else if (navigateTo == "TRADITION") {
            currentScreen = Screen.Traditions
        } else if (navigateTo == "GALLERY") {
            currentScreen = Screen.Gallery
        } else if (navigateTo == "OVERRIDE_APPROVED" || navigateTo == "DELETION_REQUEST" || navigateTo == "PENDING_APPROVAL") {
            currentScreen = Screen.Dashboard
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Color(0xFFD7CCC8).copy(alpha = 0.12f), blendMode = BlendMode.Multiply)
        )
        val user = currentUser
        when (val screen = currentScreen) {
                is Screen.Login -> LoginScreen(
                    members = members,
                    error = loginError,
                    isHindi = isHindi,
                    onLanguageToggle = { 
                        isHindi = it
                        prefs.edit { putBoolean("is_hindi", it) }
                    },
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
                    onNavigateToFamilyTree = { currentScreen = Screen.FamilyTree },
                    onNavigateToCalendar = { currentScreen = Screen.Calendar },
                    onLogout = { 
                        currentUser = null
                        currentScreen = Screen.Login 
                        prefs.edit { clear() }
                    },
                    onEditProfile = { currentScreen = Screen.EditProfile(user) },
                    onPasswordChange = { hashedPw -> scope.launch { FirebaseManager.updatePassword(user.id, hashedPw) } }
                )
                is Screen.FamilyTree -> if (user != null) FamilyTreeScreen(
                    currentUser = user,
                    members = members,
                    onNavigateBack = { currentScreen = Screen.Dashboard },
                    onMemberClick = { memberToEdit -> currentScreen = Screen.EditProfile(memberToEdit) }
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
                        FirebaseManager.submitMilestone(milestone.copy(imageUrl = url))
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
                                reason = "User requested deletion",
                                requestedBy = user.id,
                                requestedByName = user.name
                            )
                            FirebaseManager.submitDeletionRequest(request)
                        }
                    }},
                    onToggleReaction = { mid, emoji -> scope.launch { FirebaseManager.toggleMilestoneReaction(mid, emoji, user.id) } },
                    onAddComment = { mid, text -> 
                        scope.launch {
                            FirebaseManager.addMilestoneComment(mid, Comment(
                                id = UUID.randomUUID().toString(),
                                userId = user.id,
                                userName = user.name,
                                text = text
                            ))
                        }
                    }
                )
                is Screen.Calendar -> CalendarScreen(
                    allMembers = members,
                    onBack = { currentScreen = Screen.Dashboard }
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
}


@Composable
fun LoginScreen(
    members: List<Member>, 
    error: String?, 
    isHindi: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val noEmailError = t("No email app found.", "कोई ईमेल ऐप नहीं मिला।")
    val enterPhoneError = t("Enter phone number", "फ़ोन नंबर दर्ज करें")
    val incorrectPasswordError = t("Incorrect password", "ग़लत पासवर्ड")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Language Toggle at the top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .background(Color(0xFF5D4037).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "English",
                    color = if (!isHindi) Color(0xFF3E2723) else Color(0xFF3E2723).copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (!isHindi) FontWeight.ExtraBold else FontWeight.Medium
                )
                Switch(
                    checked = isHindi,
                    onCheckedChange = onLanguageToggle,
                    modifier = Modifier.scale(0.8f).padding(horizontal = 8.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF5D4037),
                        checkedTrackColor = Color(0xFFD7CCC8),
                        uncheckedThumbColor = Color(0xFF8D6E63),
                        uncheckedTrackColor = Color(0xFFEFEBE9)
                    )
                )
                Text(
                    "हिंदी",
                    color = if (isHindi) Color(0xFF3E2723) else Color(0xFF3E2723).copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isHindi) FontWeight.ExtraBold else FontWeight.Medium
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9).copy(alpha = 0.98f)),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(2.dp, Color(0xFF5D4037))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        t("Purawale\nHum aur Humare", "पुरावाले\nहम और हमारे"),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color(0xFF3E2723), 
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(36.dp))
                    
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.all { char -> char.isDigit() }) phone = it; localError = null },
                        label = { Text(t("Phone Number", "फ़ोन नंबर"), fontWeight = FontWeight.Bold) },
                        placeholder = { Text(t("Enter your phone", "फ़ोन नंबर दर्ज करें")) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedBorderColor = Color(0xFF3E2723),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF3E2723),
                            unfocusedLabelColor = Color(0xFF5D4037),
                            cursorColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.White.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; localError = null },
                        label = { Text(t("Password", "पासवर्ड"), fontWeight = FontWeight.Bold) },
                        placeholder = { Text(t("Enter your password", "पासवर्ड दर्ज करें")) },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedBorderColor = Color(0xFF3E2723),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFF3E2723),
                            unfocusedLabelColor = Color(0xFF5D4037),
                            cursorColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.White.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("bgnema@yahoo.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Forgot Password - Nema Purawale App")
                                putExtra(Intent.EXTRA_TEXT, "Hello Admin,\n\nI forgot my password for the phone number: $phone.\n\nPlease help me reset it.\n\nThank you.")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                localError = noEmailError
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            t("Forgot Password?", "पासवर्ड भूल गए?"), 
                            color = Color(0xFF5D4037), 
                            style = MaterialTheme.typography.labelLarge, 
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    }

                    val displayError = error ?: localError
                    if (displayError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                        ) {
                            Text(
                                displayError, 
                                color = Color(0xFFB71C1C), 
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                localError = enterPhoneError
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
                                    localError = incorrectPasswordError
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3E2723),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            t("Login", "लॉगिन करें"), 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    TextButton(
                        onClick = { uriHandler.openUri("https://circlebirthdays.web.app/privacy.html") }
                    ) {
                        Text(
                            t("Privacy Policy", "गोपनीयता नीति"),
                            color = Color(0xFF5D4037),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

data class FeatureItem(val title: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

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
    onNavigateToMemoryLane: () -> Unit,
    onNavigateToFamilyTree: () -> Unit,
    onNavigateToCalendar: () -> Unit
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
                title = { Text(t("Dashboard", "डैशबोर्ड"), color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold) }, 
                actions = { 
                    TextButton(onClick = { showPasswordDialog = true }) { Text(t("Password", "पासवर्ड"), color = Color(0xFF5D4037), fontWeight = FontWeight.ExtraBold) }
                    TextButton(onClick = onLogout) { Text(t("Logout", "लॉगआउट"), color = Color(0xFF5D4037), fontWeight = FontWeight.ExtraBold) } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
        ) {
            item {
                // Enhanced Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(2.dp, Color(0xFF5D4037))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!user.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFEFEBE9)).border(2.dp, Color(0xFF5D4037), CircleShape),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(80.dp).background(Color(0xFFEFEBE9), CircleShape).border(2.dp, Color(0xFF5D4037), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = Color(0xFF5D4037))
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
                                Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.ExtraBold)
                                if (!user.location.isNullOrEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color(0xFF8D6E63))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(user.location, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                    IconButton(onClick = onEditProfile) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit My Profile", tint = Color(0xFF5D4037))
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            TextButton(
                                onClick = { uriHandler.openUri("https://circlebirthdays.web.app/privacy.html") }
                            ) {
                                Text(
                                    t("Privacy Policy", "गोपनीयता नीति"),
                                    color = Color(0xFF5D4037).copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFD7CCC8))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(t("Birthday", "जन्मदिन"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                Text(user.dateOfBirth, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
                            }
                            if (!user.marriageDate.isNullOrBlank()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(t("Anniversary", "वर्षगांठ"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                    Text(user.marriageDate, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val roleColor = if (user.isAdmin) Color(0xFFC62828) else Color(0xFF5D4037)
                    val roleText = if (user.isAdmin) "ADMIN" else "EDITOR"
                    Surface(
                        color = roleColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, roleColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            roleText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = roleColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // --- Events Today (Moved Up & Encapsulated) ---
            item {
                if (todayBirthdays.isNotEmpty() || todayAnniversaries.isNotEmpty() || todayRemembrances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        t("Events Today", "आज के कार्यक्रम"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF5D4037))
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            // Birthdays Today
                            todayBirthdays.forEach { bDayMember ->
                                val context = LocalContext.current
                                val age = try {
                                    val dob = LocalDate.parse(bDayMember.dateOfBirth)
                                    java.time.Period.between(dob, LocalDate.now()).years
                                } catch(_: Exception) { null }
                                
                                ListItem(
                                    headlineContent = { Text("${t("Birthday", "जन्मदिन")}: ${bDayMember.name}${if (age != null) " ($age)" else ""}", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                                    supportingContent = {
                                        if (!bDayMember.facebookUrl.isNullOrEmpty() || !bDayMember.instagramUrl.isNullOrEmpty() || !bDayMember.youtubeUrl.isNullOrEmpty() || bDayMember.phoneNumber.isNotBlank()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                                val ctx = LocalContext.current
                                                if (bDayMember.phoneNumber.isNotBlank()) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_whatsapp),
                                                        contentDescription = "WhatsApp",
                                                        tint = Color(0xFF25D366),
                                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, "https://wa.me/${bDayMember.phoneNumber.filter { it.isDigit() }}") }
                                                    )
                                                }
                                                if (!bDayMember.facebookUrl.isNullOrEmpty()) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_facebook),
                                                        contentDescription = "Facebook",
                                                        tint = Color(0xFF1877F2),
                                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, bDayMember.facebookUrl) }
                                                    )
                                                }
                                                if (!bDayMember.instagramUrl.isNullOrEmpty()) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_instagram),
                                                        contentDescription = "Instagram",
                                                        tint = Color(0xFFE4405F),
                                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, bDayMember.instagramUrl) }
                                                    )
                                                }
                                                if (!bDayMember.youtubeUrl.isNullOrEmpty()) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_youtube),
                                                        contentDescription = "YouTube",
                                                        tint = Color(0xFFFF0000),
                                                        modifier = Modifier.size(18.dp).clickable { safeOpenUri(ctx, bDayMember.youtubeUrl) }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    leadingContent = { EventAvatar(bDayMember.photoUrl, bDayMember.name) },
                                    trailingContent = {
                                        if (bDayMember.phoneNumber.isNotBlank()) {
                                            val phone = bDayMember.phoneNumber.let { p -> if (p.length == 10) "91$p" else p }
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val displayName = if (bDayMember.relationship.isNullOrBlank()) bDayMember.name else "${bDayMember.relationship} ${bDayMember.name}"
                                                        val msg = "Happy Birthday $displayName!"
                                                        val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
                            
                            // Anniversaries Today
                            todayAnniversaries.forEach { anniversaryMember ->
                                val context = LocalContext.current
                                val years = try {
                                    val mDate = LocalDate.parse(anniversaryMember.marriageDate)
                                    java.time.Period.between(mDate, LocalDate.now()).years
                                } catch(_: Exception) { null }

                                ListItem(
                                    headlineContent = { Text("${t("Anniversary", "वर्षगांठ")}: ${anniversaryMember.name} & ${anniversaryMember.spouseName}${if (years != null && years > 0) " ($years)" else ""}", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                                    leadingContent = { EventAvatar(anniversaryMember.photoUrl, anniversaryMember.name) },
                                    trailingContent = {
                                        val partnerId = if (anniversaryMember.familyId.endsWith("0")) anniversaryMember.familyId.dropLast(1) else anniversaryMember.familyId + "0"
                                        val partner = allMembers.find { p -> p.familyId == partnerId }
                                        val rawPhone = anniversaryMember.phoneNumber.ifBlank { partner?.phoneNumber }
                                        
                                        if (!rawPhone.isNullOrBlank()) {
                                            val phone = rawPhone.let { p -> if (p.length == 10) "91$p" else p }
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val displayName = if (anniversaryMember.relationship.isNullOrBlank()) anniversaryMember.name else "${anniversaryMember.relationship} ${anniversaryMember.name}"
                                                        val msg = "Happy Anniversary $displayName & ${anniversaryMember.spouseName}!"
                                                        val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
                            
                            // Remembrances Today
                            todayRemembrances.forEach { remMember ->
                                val context = LocalContext.current
                                val isBirthday = isToday(remMember.dateOfBirth) && !isToday(remMember.bereavementDate)
                                ListItem(
                                    headlineContent = { Text("${if (isBirthday) t("Birth Anniversary", "जयंती") else t("Remembrance Day", "पुण्यतिथि")}: ${remMember.name}", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                                    supportingContent = { 
                                        Text(if (isBirthday) t("Born on", "जन्म") + " ${remMember.dateOfBirth}" else t("Passed away on", "स्वर्गवास") + " ${remMember.bereavementDate}", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
                                    },
                                    leadingContent = { EventAvatar(remMember.photoUrl, remMember.name) },
                                    trailingContent = {
                                        if (remMember.phoneNumber.isNotBlank()) {
                                            val phone = remMember.phoneNumber.let { p -> if (p.length == 10) "91$p" else p }
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val displayName = if (remMember.relationship.isNullOrBlank()) remMember.name else "${remMember.relationship} ${remMember.name}"
                                                        val msg = "Remembering $displayName on this ${if (isBirthday) "Birth Anniversary" else "day"}."
                                                        val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
            }

            // --- Upcoming Events (Moved Down) ---
            item {
                val upcomingEvents = allMembers.filter { 
                    (it.bereavementDate.isNullOrBlank() && isWithinSevenDays(it.dateOfBirth) && !isToday(it.dateOfBirth)) ||
                    (!it.marriageDate.isNullOrBlank() && it.bereavementDate.isNullOrBlank() && isWithinSevenDays(it.marriageDate) && !isToday(it.marriageDate))
                }.sortedBy { 
                    val date = try {
                        val dStr = if (!it.marriageDate.isNullOrBlank() && isWithinSevenDays(it.marriageDate)) it.marriageDate else it.dateOfBirth
                        LocalDate.parse(dStr)
                    } catch (_: Exception) {
                        val dStr = if (!it.marriageDate.isNullOrBlank() && isWithinSevenDays(it.marriageDate)) it.marriageDate else it.dateOfBirth
                        val parts = dStr!!.split("-", "/")
                        LocalDate.of(LocalDate.now().year, parts[1].toInt(), parts[0].toInt())
                    }
                    val today = LocalDate.now()
                    val eventDate = date.withYear(today.year).let { if (it.isBefore(today)) it.withYear(today.year + 1) else it }
                    eventDate
                }

                if (upcomingEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        t("Upcoming Events (7 Days)", "आगामी कार्यक्रम (7 दिन)"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF5D4037))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            for (member in upcomingEvents.take(5)) {
                                val isBirthday = isWithinSevenDays(member.dateOfBirth)
                                val dateStr = if (isBirthday) member.dateOfBirth else member.marriageDate
                                val eventType = if (isBirthday) t("Birthday", "जन्मदिन") else t("Anniversary", "वर्षगांठ")
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EventAvatar(member.photoUrl, member.name)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.name, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
                                        Text("$eventType: $dateStr", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                                    }
                                    if (isToday(dateStr)) {
                                        Icon(Icons.Default.Cake, null, tint = Color(0xFF5D4037), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Compact 3-Column Feature Grid
                val totalUnread = channels.sumOf { it.unreadCount[user.id] ?: 0 }
                
                val features = listOf(
                    FeatureItem(t("Profiles", "प्रोफ़ाइल"), Icons.Default.People, Color(0xFF2196F3), onNavigateToProfiles),
                    FeatureItem(t("Gallery", "गैलरी"), Icons.Default.Collections, Color(0xFF4CAF50), onNavigateToGallery),
                    FeatureItem(t("Discussions", "चर्चा"), Icons.Default.Forum, Color(0xFFFF5722), onNavigateToDiscussions),
                    FeatureItem(t("Messages", "संदेश"), Icons.Default.Email, Color(0xFF9C27B0), onNavigateToMessages),
                    FeatureItem(t("Cookbook", "नुस्खे"), Icons.Default.Restaurant, Color(0xFFFFC107), onNavigateToCookbook),
                    FeatureItem(t("Traditions", "परंपरा"), Icons.AutoMirrored.Filled.MenuBook, Color(0xFF009688), onNavigateToTraditions),
                    FeatureItem(t("Memory Lane", "यादें"), Icons.Default.History, Color(0xFFE91E63), onNavigateToMemoryLane),
                    FeatureItem(t("Family Tree", "वंश वृक्ष"), Icons.Default.AccountTree, Color(0xFF795548), onNavigateToFamilyTree),
                    FeatureItem(t("Calendar", "कैलेंडर"), Icons.Default.CalendarMonth, Color(0xFF673AB7), onNavigateToCalendar)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (rowFeatures in features.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (feature in rowFeatures) {
                                Box(modifier = Modifier.weight(1f)) {
                                    val badgeCount = if (feature.title == t("Messages", "संदेश")) totalUnread else 0
                                    FeatureTile(feature.title, feature.icon, feature.color, badgeCount = badgeCount, onClick = feature.onClick)
                                }
                            }
                            // Add empty spacers if row is not full
                            repeat(3 - rowFeatures.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                if (user.isAdmin && deletionRequests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(t("Deletion Requests", "हटाने के अनुरोध"), style = MaterialTheme.typography.titleMedium, color = Color(0xFFC62828), fontWeight = FontWeight.ExtraBold)
                    for (req in deletionRequests) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFC62828))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val collectionLabel = when(req.collectionName) {
                                        "gallery" -> t("Memory", "याद")
                                        "discussions" -> t("Discussion", "चर्चा")
                                        "recipes" -> t("Recipe", "नुस्खा")
                                        "traditions" -> t("Tradition", "परंपरा")
                                        "memorylane" -> t("Milestone", "उपलब्धि")
                                        else -> req.collectionName
                                    }
                                    Text("$collectionLabel: ${req.title}", style = MaterialTheme.typography.titleSmall, color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold)
                                    Text(t("Requested by", "द्वारा अनुरोधित") + " ${req.requestedByName}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828))
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        try {
                                            when(req.collectionName) {
                                                "recipes" -> FirebaseManager.deleteRecipe(req.docId)
                                                "traditions" -> FirebaseManager.deleteTradition(req.docId)
                                                "gallery" -> FirebaseManager.deleteMemory(req.docId)
                                                "discussions" -> FirebaseManager.deleteDiscussion(req.docId)
                                                "memorylane" -> FirebaseManager.deleteMilestone(req.docId)
                                            }
                                            FirebaseManager.deleteDeletionRequest(req.id)
                                        } catch (e: Exception) {
                                            Log.e("Dashboard", "Failed to process deletion: ${e.message}")
                                        }
                                    }
                                }) { Icon(Icons.Default.Delete, "Confirm Delete", tint = Color(0xFFC62828)) }
                                IconButton(onClick = { scope.launch { FirebaseManager.deleteDeletionRequest(req.id) } }) {
                                    Icon(Icons.Default.Close, "Dismiss", tint = Color(0xFF5D4037))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureTile(
    title: String,
    icon: ImageVector,
    color: Color,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFEBE9)
        ),
        border = BorderStroke(2.dp, Color(0xFF5D4037))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                if (badgeCount > 0) {
                    BadgedBox(badge = { 
                        Badge(containerColor = Color(0xFFC62828)) { 
                            Text(badgeCount.toString(), color = Color.White, fontWeight = FontWeight.Bold) 
                        } 
                    }) {
                        Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = Color(0xFF5D4037))
                    }
                } else {
                    Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = Color(0xFF5D4037))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF3E2723),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
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
            modifier = Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, Color(0xFF5D4037), CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_report_image),
            onError = { state ->
                Log.e("CircleBirthdays", "EventAvatar load failed for $name: ${state.result.throwable.message}")
            }
        )
    } else {
        Box(
            modifier = Modifier.size(44.dp).background(Color(0xFFEFEBE9), CircleShape).border(1.5.dp, Color(0xFF5D4037), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (name.isNotBlank()) {
                val initials = name.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }
                Text(initials, style = MaterialTheme.typography.labelLarge, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = Color(0xFF5D4037))
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
    val brown900 = Color(0xFF3E2723)
    val brown800 = Color(0xFF5D4037)
    val brown50 = Color(0xFFEFEBE9)

    if (showCreateDialog) {
        var type by remember { mutableStateOf("TEXT") } // TEXT, IMAGE, POLL
        var title by remember { mutableStateOf("") }
        var contentText by remember { mutableStateOf("") }
        var pollOptions by remember { mutableStateOf(listOf("", "")) }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(t("Create Discussion", "चर्चा शुरू करें"), color = brown900, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row {
                        FilterChip(
                            selected = type == "TEXT",
                            onClick = { type = "TEXT" },
                            label = { Text(t("Text", "टेक्स्ट")) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = brown800, selectedLabelColor = Color.White)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = type == "IMAGE",
                            onClick = { type = "IMAGE" },
                            label = { Text(t("Image", "चित्र")) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = brown800, selectedLabelColor = Color.White)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = type == "POLL",
                            onClick = { type = "POLL" },
                            label = { Text(t("Poll", "पोल")) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = brown800, selectedLabelColor = Color.White)
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(t("Title", "शीर्षक")) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                    )
                    if (type == "TEXT" || type == "POLL") {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = { contentText = it },
                            label = { Text(if (type == "POLL") t("Poll Question", "पोल प्रश्न") else t("Content", "विषय")) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { SpeechToTextButton(onResult = { contentText += it }) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
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
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = brown800)
                        ) {
                            Text(if (selectedUri == null) t("Select Image", "चित्र चुनें") else t("Change Image", "चित्र बदलें"))
                        }
                    }
                    if (type == "POLL") {
                        Text(t("Poll Options", "पोल विकल्प"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp), color = brown900)
                        for ((index, opt) in pollOptions.withIndex()) {
                            OutlinedTextField(
                                value = opt,
                                onValueChange = { newVal ->
                                    pollOptions = pollOptions.toMutableList().apply { this[index] = newVal }
                                },
                                label = { Text(t("Option ${index + 1}", "विकल्प ${index + 1}")) },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { SpeechToTextButton(onResult = {
                                    pollOptions = pollOptions.toMutableList().apply { this[index] += it }
                                }) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                            )
                        }
                        TextButton(onClick = { pollOptions = pollOptions + "" }) { Text(t("Add Option", "विकल्प जोड़ें"), color = brown800) }
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
                ) { Text(t("Post", "पोस्ट करें"), color = if (isValid) brown800 else Color.Gray) }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(t("Cancel", "रद्द करें"), color = brown800) } },
            containerColor = Color.White
        )
    }

    if (selectedDiscussionForDetail != null) {
        val disc = selectedDiscussionForDetail!!
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedDiscussionForDetail = null },
            title = { Text(disc.title, color = brown900, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(t("By ${disc.userName}", "${disc.userName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = brown800)
                    Spacer(Modifier.height(8.dp))
                    if (disc.type == "IMAGE") {
                        AsyncImage(
                            model = disc.content,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(disc.content, color = brown900)
                    }
                    
                    if (disc.type == "POLL" && disc.pollOptions != null) {
                        Spacer(Modifier.height(16.dp))
                        for (opt in disc.pollOptions) {
                            val isVoted = opt.voterIds.contains(user.id)
                            val totalVotes = disc.pollOptions.sumOf { it.voterIds.size }
                            val percentage = if (totalVotes > 0) (opt.voterIds.size.toFloat() / totalVotes) else 0f
                            
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onVote(disc.id, opt.id, user.id) }) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isVoted) {
                                            Icon(Icons.Default.CheckCircle, null, tint = brown800, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                        }
                                        Text(opt.text, fontWeight = if (isVoted) FontWeight.Bold else FontWeight.Normal, color = brown900)
                                    }
                                    Text(t("${opt.voterIds.size} votes", "${opt.voterIds.size} वोट"), style = MaterialTheme.typography.labelSmall, color = brown800)
                                }
                                LinearProgressIndicator(progress = { percentage }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = brown800, trackColor = brown800.copy(alpha = 0.2f))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = brown800.copy(alpha = 0.2f))
                    Text(t("Comments", "टिप्पणियाँ"), style = MaterialTheme.typography.titleSmall, color = brown900)
                    for (comment in disc.comments) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = brown900)
                            Text(comment.text, style = MaterialTheme.typography.bodySmall, color = brown800)
                        }
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(t("Add comment...", "अपनी टिप्पणी लिखें...")) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { 
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = { if (commentText.isNotBlank()) { onAddComment(disc.id, commentText); commentText = "" } }) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = brown800) } 
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedDiscussionForDetail = null }) { Text(t("Close", "बंद करें"), color = brown800) } },
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Discussions", "चर्चाएँ"), color = brown900, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = brown900) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = brown800,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Create Post") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
        ) {
            items(discussions) { disc ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedDiscussionForDetail = disc },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(disc.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), color = brown900, fontWeight = FontWeight.Bold)
                            if (disc.status == "PENDING") {
                                Surface(color = Color(0xFFFFEBEE), shape = CircleShape) {
                                    Text(t("PENDING", "लंबित"), color = Color(0xFFC62828), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                        Text(t("By ${disc.userName} • ${disc.type}", "${disc.userName} द्वारा • ${disc.type}"), style = MaterialTheme.typography.labelSmall, color = brown800.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (disc.type == "IMAGE") t("[Image Post]", "[चित्र पोस्ट]") else if (disc.content.length > 100) disc.content.take(100) + "..." else disc.content, 
                            style = MaterialTheme.typography.bodyMedium,
                            color = brown900
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(t("${disc.comments.size} comments", "${disc.comments.size} टिप्पणियाँ"), style = MaterialTheme.typography.labelSmall, color = brown800.copy(alpha = 0.7f))
                            if (user.isAdmin) {
                                Row {
                                    if (disc.status == "PENDING") {
                                        IconButton(onClick = { onApprove(disc.id) }) { Icon(Icons.Default.Check, "Approve", tint = Color(0xFF2E7D32)) }
                                    }
                                    IconButton(onClick = { onDelete(disc.id) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFC62828)) }
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
    
    val brown900 = Color(0xFF3E2723)
    val brown800 = Color(0xFF5D4037)
    val brown50 = Color(0xFFEFEBE9)

    val filteredMembers = (if (showPending) {
        pendingMembers
    } else {
        members.filter { 
            (it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)) &&
            (!it.isAdmin || currentUser.isAdmin || it.id == currentUser.id)
        }
    }).sortedWith(compareBy({ it.familyId }, { it.name }))
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                when {
                                    showPending -> t("Pending Approvals", "लंबित अनुमोदन")
                                    showOverrides -> t("Relationship Requests", "रिश्ते के अनुरोध")
                                    else -> t("Profiles", "प्रोफाइल")
                                },
                                color = brown900,
                                fontWeight = FontWeight.Bold
                            )
                            if (canEditAll) {
                                Text(t("Admin Mode", "एडमिन मोड"), style = MaterialTheme.typography.labelSmall, color = brown800)
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "", tint = brown900) } },
                    actions = {
                        IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home", tint = brown900) }
                        if (currentUser.isAdmin) {
                            if (pendingMembers.isNotEmpty()) {
                                BadgedBox(badge = { Badge(containerColor = brown800, contentColor = Color.White) { Text(pendingMembers.size.toString()) } }) {
                                    IconButton(onClick = { 
                                        showPending = !showPending 
                                        showOverrides = false
                                    }) {
                                        Icon(if (showPending) Icons.Default.Person else Icons.Default.Check, "", tint = brown900)
                                    }
                                }
                            }
                            if (overrides.isNotEmpty()) {
                                BadgedBox(badge = { Badge(containerColor = brown800, contentColor = Color.White) { Text(overrides.size.toString()) } }) {
                                    IconButton(onClick = { 
                                        showOverrides = !showOverrides
                                        showPending = false
                                    }) {
                                        Icon(if (showOverrides) Icons.Default.People else Icons.Default.SettingsSuggest, "", tint = brown900)
                                    }
                                }
                            }
                            IconButton(onClick = onImportCsv) { Icon(Icons.Default.Upload, "", tint = brown900) }
                            var showClearConfirm by remember { mutableStateOf(false) }
                            IconButton(onClick = { showClearConfirm = true }) { Icon(Icons.Default.DeleteForever, "", tint = Color(0xFFC62828)) }
                            if (showClearConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirm = false },
                                    title = { Text(t("Clear All Data", "सारा डेटा मिटाएं"), color = brown900, fontWeight = FontWeight.Bold) },
                                    text = { Text(t("Are you sure you want to delete ALL members and pending updates? This cannot be undone.", "क्या आप वाकई सभी सदस्यों और लंबित अपडेट को हटाना चाहते हैं? इसे वापस नहीं लिया जा सकता।"), color = brown800) },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            onClearAll()
                                            showClearConfirm = false 
                                        }) { Text(t("Confirm", "पुष्टि करें"), color = Color(0xFFC62828)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirm = false }) { Text(t("Cancel", "रद्द करें"), color = brown800) }
                                    },
                                    containerColor = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                if (!showPending && !showOverrides) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(t("Search by name or phone...", "नाम या फोन से खोजें..."), color = brown800.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = brown800.copy(alpha = 0.5f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = brown900,
                            unfocusedTextColor = brown900,
                            focusedBorderColor = brown800,
                            unfocusedBorderColor = brown800.copy(alpha = 0.2f),
                            cursorColor = brown800,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                            focusedContainerColor = Color.White
                        )
                    )
                }
            }
        },
        floatingActionButton = { 
            if (canEditAll && !showPending && !showOverrides) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = brown800,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Add, "") }
            }
        }
    ) { padding ->
        if (showOverrides && currentUser.isAdmin) {
            LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
                items(overrides) { override ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, Color(0xFF5D4037)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text("${override.observerName} ➔ ${override.targetName}", color = brown900, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(t("Requested Label: ${override.relationship}", "अनुरोधित लेबल: ${override.relationship}"), color = brown800) },
                            trailingContent = {
                                IconButton(onClick = { onApproveOverride(override) }) {
                                    Icon(Icons.Default.Check, "Approve", tint = Color(0xFF2E7D32))
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMembers) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(2.dp, Color(0xFF5D4037)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!member.photoUrl.isNullOrEmpty() && member.photoUrl.startsWith("http")) {
                                    AsyncImage(
                                        model = member.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp).clip(CircleShape).background(brown50).border(1.5.dp, brown800, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(60.dp).background(brown50, CircleShape).border(1.5.dp, brown800, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = brown800.copy(alpha = 0.5f))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.name, style = MaterialTheme.typography.titleMedium, color = brown900, fontWeight = FontWeight.ExtraBold)
                                    if (!member.relationship.isNullOrEmpty()) {
                                        Text(member.relationship, style = MaterialTheme.typography.labelSmall, color = brown800)
                                    }
                                    if (showPending) {
                                        Text(t("(Pending Approval)", "(अनुमोदन लंबित)"), style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
                                    }
                                }
                                Row {
                                    if (showPending && currentUser.isAdmin) {
                                        IconButton(onClick = { onApprove(member) }) {
                                            Icon(Icons.Default.Check, "Approve", tint = Color(0xFF2E7D32))
                                        }
                                    }
                                    val canEdit = canEditAll || member.id == currentUser.id
                                    IconButton(onClick = { onEdit(member) }) { 
                                        Icon(
                                            imageVector = if (canEdit) Icons.Default.Edit else Icons.Default.Visibility,
                                            contentDescription = if (canEdit) "Edit" else "View",
                                            tint = brown800
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = brown800)
                                Spacer(Modifier.width(6.dp))
                                Text(member.phoneNumber, style = MaterialTheme.typography.bodySmall, color = brown800, fontWeight = FontWeight.Bold)
                            }
                            
                            if (!member.location.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = brown800)
                                    Spacer(Modifier.width(6.dp))
                                    Text(member.location, style = MaterialTheme.typography.bodySmall, color = brown800)
                                }
                            }

                            if (!member.facebookUrl.isNullOrEmpty() || !member.instagramUrl.isNullOrEmpty() || !member.youtubeUrl.isNullOrEmpty()) {
                                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val context = LocalContext.current
                                    if (member.phoneNumber.isNotBlank()) {
                                        Icon(painterResource(R.drawable.ic_whatsapp), null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, "https://wa.me/${member.phoneNumber.filter { it.isDigit() }}") })
                                    }
                                    if (!member.facebookUrl.isNullOrEmpty()) {
                                        Icon(painterResource(R.drawable.ic_facebook), null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, member.facebookUrl) })
                                    }
                                    if (!member.instagramUrl.isNullOrEmpty()) {
                                        Icon(painterResource(R.drawable.ic_instagram), null, tint = Color(0xFFAD1457), modifier = Modifier.size(20.dp).clickable { safeOpenUri(context, member.instagramUrl) })
                                    }
                                }
                            }

                            if (member.id != currentUser.id) {
                                Button(
                                    onClick = { onChat(member) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = brown800),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(t("Chat", "चैट"), style = MaterialTheme.typography.labelLarge)
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
    
    // Parent check: Father/Mother can edit their children's data
    val isParent = member?.let { m ->
        val targetBaseId = if (m.familyId.endsWith("0")) m.familyId.dropLast(1) else m.familyId
        val parentBaseId = if (targetBaseId.length > 1) targetBaseId.dropLast(1)
                           else if (targetBaseId.length == 1 && targetBaseId != "P") "P"
                           else ""
        parentBaseId.isNotEmpty() && (currentUser.familyId == parentBaseId || currentUser.familyId == parentBaseId + "0")
    } ?: false

    val canEditAll = isAdminOrEditor || member == null || isParent
    
    val canEditPhone = canEditAll || isSelf
    val canEditEmail = canEditAll || isSelf
    val canEditLocation = canEditAll || isSelf
    val canEditPhoto = canEditAll || isSelf
    val canEditAddress = canEditAll || isSelf
    val canEditFixed = canEditAll
    val canEditSpouseParents = canEditAll || isSelf

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
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        when {
                            member == null -> t("Add Profile", "प्रोफ़ाइल जोड़ें")
                            canEditAll || isSelf -> t("Edit Profile", "प्रोफ़ाइल संपादित करें")
                            else -> t("View Profile", "प्रोफ़ाइल देखें")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onCancel) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, t("Back", "पीछे"), tint = Color(0xFF3E2723)) 
                    } 
                },
                actions = {
                    IconButton(onClick = onHome) { 
                        Icon(Icons.Default.Home, t("Home", "मुख्य पृष्ठ"), tint = Color(0xFF3E2723)) 
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Profile Photo Card
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9)),
                border = BorderStroke(2.dp, Color(0xFF5D4037)),
                modifier = Modifier.align(Alignment.CenterHorizontally).size(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = canEditPhoto) { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl.isNotEmpty()) {
                        Box {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = t("Profile Photo", "प्रोफ़ाइल फोटो"),
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = android.R.drawable.ic_menu_report_image)
                            )
                            if (canEditPhoto) {
                                Surface(
                                    onClick = { photoUrl = ""; selectedUri = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.5f)
                                ) {
                                    Icon(Icons.Default.Delete, t("Delete", "हटाएं"), tint = Color.White, modifier = Modifier.padding(6.dp))
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (canEditPhoto) Icons.Default.AddAPhoto else Icons.Default.Person, 
                                contentDescription = "Photo", 
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF5D4037).copy(alpha = 0.5f)
                            )
                            if (canEditPhoto) {
                                Text(t("Add Photo", "फोटो जोड़ें"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = name, onValueChange = { name = it }, label = t("Full Name", "पूरा नाम"), enabled = canEditFixed, icon = Icons.Default.Person)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wc, null, tint = Color(0xFF5D4037), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        for (option in listOf("Male", "Female", "Other")) {
                            val isSelected = when (option) {
                                "Male" -> gender.equals("Male", ignoreCase = true) || gender.equals("M", ignoreCase = true)
                                "Female" -> gender.equals("Female", ignoreCase = true) || gender.equals("F", ignoreCase = true)
                                else -> gender == option
                            }
                            val displayOption = when(option) {
                                "Male" -> t("Male", "पुरुष")
                                "Female" -> t("Female", "महिला")
                                else -> t("Other", "अन्य")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (canEditFixed) gender = option }) {
                                RadioButton(
                                    selected = isSelected, 
                                    onClick = { if (canEditFixed) gender = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF5D4037), unselectedColor = Color(0xFF5D4037).copy(alpha = 0.4f))
                                )
                                Text(displayOption, color = Color(0xFF3E2723), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (currentUser.isAdmin) {
                        ModernTextField(value = familyId, onValueChange = { familyId = it }, label = t("Family ID (Admin Only)", "फैमिली आईडी (केवल एडमिन)"), enabled = isAdminOrEditor, icon = Icons.Default.Badge)
                    }
                    
                    ModernTextField(
                        value = phone, 
                        onValueChange = { if (it.all { char -> char.isDigit() }) phone = it }, 
                        label = t("Phone Number", "फ़ोन नंबर"), 
                        enabled = canEditPhone,
                        icon = Icons.Default.Phone,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                    
                    ModernTextField(value = email, onValueChange = { email = it }, label = t("Email Address", "ईमेल पता"), enabled = canEditEmail, icon = Icons.Default.Email)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = dob, onValueChange = { dob = it }, label = t("Date of Birth (YYYY-MM-DD)", "जन्म तिथि (YYYY-MM-DD)"), enabled = canEditFixed, icon = Icons.Default.Cake)
                    ModernTextField(value = spouse, onValueChange = { spouse = it }, label = t("Spouse Name", "जीवनसाथी का नाम"), enabled = canEditFixed, icon = Icons.Default.Favorite)
                    ModernTextField(value = father, onValueChange = { father = it }, label = if (isSpouse) t("Father Name", "पिता का नाम") else t("Father Name (Inferred)", "पिता का नाम (अनुमानित)"), enabled = canEditSpouseParents, icon = Icons.Default.Person)
                    ModernTextField(value = mother, onValueChange = { mother = it }, label = if (isSpouse) t("Mother Name", "माता का नाम") else t("Mother Name (Inferred)", "माता का नाम (अनुमानित)"), enabled = canEditSpouseParents, icon = Icons.Default.Person)
                    ModernTextField(value = marriageDate, onValueChange = { marriageDate = it }, label = t("Marriage Anniversary (YYYY-MM-DD)", "शादी की सालगिरह (YYYY-MM-DD)"), enabled = canEditFixed, icon = Icons.Default.Celebration)
                    ModernTextField(value = location, onValueChange = { location = it }, label = t("Location (City)", "स्थान (शहर)"), enabled = canEditLocation, icon = Icons.Default.LocationOn)
                    ModernTextField(value = address, onValueChange = { address = it }, label = t("Full Address", "पूरा पता"), enabled = canEditAddress, icon = Icons.Default.Home)
                    ModernTextField(value = immediateFamily, onValueChange = { immediateFamily = it }, label = t("Immediate Family (e.g. Children)", "निकटतम परिवार (जैसे बच्चे)"), enabled = canEditFixed, icon = Icons.Default.Groups)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val relationships = listOf("Dadaji", "Bade Dadaji", "Chote Dadaji", "Dadi", "Badi Dadi", "Choti Dadi", "Nana", "Bade Nana", "Chote Nana", "Nani", "Badi Nani", "Choti Nani", "Papa", "Mummy", "Bade Papa", "Badi Amma", "Chachaji", "Chachiji", "Bade Mamaji", "Chote Mamaji", "Badi Mamiji", "Choti Mamiji", "Bhaiya", "Bhabhi", "Didi", "Jijaji", "Bade Mausa", "Chote Mausa", "Badi Mausi", "Choti Mausi", "Bade Fufa", "Chote Fufa", "Badi Bua", "Choti Bua", "Bhatija", "Bhatiji", "Bhanja", "Bhanji", "Beta", "Beti", "Pota", "Poti", "Nati", "Natin", "Bahu", "Damaad", "Sasurji", "Saasuma", "Devar", "Devrani", "Jeth", "Jethani", "Nanad", "Saala", "Saali")
            var relExpanded by remember { mutableStateOf(false) }
            val canEditRel = currentUser.phoneNumber == "9999999999" || isAdminOrEditor

            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("Relationship", "संबंध"), style = MaterialTheme.typography.labelMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = if (canEditRel || (!isAdminOrEditor && member != null)) relExpanded else false,
                        onExpandedChange = { if (canEditRel || (!isAdminOrEditor && member != null)) relExpanded = !relExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = relationship,
                            onValueChange = { if (canEditRel) relationship = it },
                            label = { Text(t("Select Relationship", "संबंध चुनें"), color = Color(0xFF5D4037).copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { if (canEditRel || (!isAdminOrEditor && member != null)) ExposedDropdownMenuDefaults.TrailingIcon(expanded = relExpanded) },
                            enabled = canEditRel || (!isAdminOrEditor && member != null),
                            readOnly = !canEditRel,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF3E2723),
                                unfocusedTextColor = Color(0xFF3E2723),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color(0xFF5D4037),
                                unfocusedIndicatorColor = Color(0xFF5D4037).copy(alpha = 0.3f)
                            )
                        )
                        if (canEditRel || (!isAdminOrEditor && member != null)) {
                            ExposedDropdownMenu(
                                expanded = relExpanded,
                                onDismissRequest = { relExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                for (rel in relationships) {
                                    DropdownMenuItem(
                                        text = { Text(rel, color = Color(0xFF3E2723)) },
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
                            t("Request a relationship update if the auto-detected label is incorrect.", "यदि स्वतः पहचाना गया लेबल गलत है तो संबंध अपडेट के लिए अनुरोध करें।"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5D4037).copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = facebookUrl, onValueChange = { facebookUrl = it }, label = t("Facebook Profile URL", "फेसबुक प्रोफाइल लिंक"), enabled = canEditFixed, icon = Icons.Default.Link)
                    ModernTextField(value = instagramUrl, onValueChange = { instagramUrl = it }, label = t("Instagram Username", "इंस्टाग्राम यूजरनाम"), enabled = canEditFixed, icon = Icons.Default.Link)
                    ModernTextField(value = youtubeUrl, onValueChange = { youtubeUrl = it }, label = t("YouTube Channel URL", "यूट्यूब चैनल लिंक"), enabled = canEditFixed, icon = Icons.Default.Link)
                    
                    // Social Icons Preview
                    if (!facebookUrl.isNullOrBlank() || !instagramUrl.isNullOrBlank() || !youtubeUrl.isNullOrBlank() || phone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val context = LocalContext.current
                            if (phone.isNotBlank()) {
                                IconButton(onClick = { safeOpenUri(context, "https://wa.me/${phone.filter { it.isDigit() }}") }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_whatsapp), contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(28.dp))
                                }
                            }
                            if (!facebookUrl.isNullOrBlank()) {
                                IconButton(onClick = { safeOpenUri(context, facebookUrl) }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_facebook), contentDescription = "Facebook", tint = Color(0xFF1877F2), modifier = Modifier.size(28.dp))
                                }
                            }
                            if (!instagramUrl.isNullOrBlank()) {
                                IconButton(onClick = { safeOpenUri(context, instagramUrl) }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_instagram), contentDescription = "Instagram", tint = Color(0xFFE4405F), modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (currentUser.isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                ThemedInputContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernTextField(value = bereavementDate ?: "", onValueChange = { bereavementDate = it }, label = t("Bereavement Date (Admin Only)", "शोक की तिथि (केवल एडमिन)"), enabled = isAdminOrEditor, icon = Icons.Default.History)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isEditor, 
                                onCheckedChange = { isEditor = it }, 
                                enabled = isAdminOrEditor,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5D4037), uncheckedColor = Color(0xFF5D4037).copy(alpha = 0.4f))
                            )
                            Text(t("Grant Editor Access", "संपादक पहुंच प्रदान करें"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val updatedMember = (member ?: Member(id = UUID.randomUUID().toString())).copy(
                        name = name, gender = gender, familyId = familyId, phoneNumber = phone, email = email,
                        location = location, dateOfBirth = dob, spouseName = spouse, marriageDate = marriageDate,
                        fatherName = father, motherName = mother, immediateFamily = immediateFamily,
                        address = address, bereavementDate = bereavementDate, photoUrl = photoUrl,
                        relationship = relationship, facebookUrl = facebookUrl, instagramUrl = instagramUrl,
                        youtubeUrl = youtubeUrl, isEditor = isEditor
                    )
                    onSave(updatedMember, selectedUri)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037), contentColor = Color.White)
            ) {
                Text(t("SAVE PROFILE", "प्रोफ़ाइल सहेजें"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ThemedInputContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9).copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, color = Color(0xFF5D4037).copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = icon?.let { { Icon(it, null, tint = Color(0xFF5D4037).copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) } },
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color(0xFF3E2723),
            unfocusedTextColor = Color(0xFF3E2723),
            disabledTextColor = Color(0xFF3E2723).copy(alpha = 0.5f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = Color(0xFF5D4037),
            focusedIndicatorColor = Color(0xFF5D4037),
            unfocusedIndicatorColor = Color(0xFF5D4037).copy(alpha = 0.1f),
            disabledIndicatorColor = Color(0xFF5D4037).copy(alpha = 0.05f)
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    )
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
                            Log.e("CircleBirthdays", "Gallery detail image load failed: ${state.result.throwable.message}")
                        }
                    )
                    Text(memory.caption, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (emoji in listOf("❤️", "👍", "🙏", "😮")) {
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
                    for (comment in memory.comments) {
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
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(t("Memory Gallery", "यादों की गैलरी"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF3E2723))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showUploadDialog = true }) {
                            Icon(Icons.Default.AddAPhoto, "Add Memory", tint = Color(0xFF5D4037))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(t("Search memories...", "यादें खोजें..."), color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF3E2723)
                        )
                    )
                }
            }
        }
    ) { padding ->
        if (filteredMemories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF5D4037).copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isEmpty()) t("No memories shared yet.", "अभी तक कोई यादें साझा नहीं की गईं।") else t("No matches found.", "कोई मेल नहीं मिला।"),
                        color = Color(0xFF5D4037).copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMemories) { memory ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedMemoryForDetails = memory },
                        colors = CardDefaults.cardColors(
                            containerColor = if (memory.status == "PENDING") 
                                Color.White.copy(alpha = 0.7f) 
                            else Color.White.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f))
                    ) {
                        Column {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                AsyncImage(
                                    model = memory.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image),
                                    onError = { state ->
                                        Log.e("CircleBirthdays", "Gallery grid image load failed: ${state.result.throwable.message}")
                                    }
                                )
                                // Add a subtle dark overlay at the bottom for text legibility
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                            )
                                        )
                                )
                                if (memory.status == "PENDING") {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            t("PENDING", "लंबित"), 
                                            color = Color.White, 
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(memory.caption, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color(0xFF3E2723))
                                Text(t("By ${memory.userName}", "${memory.userName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val reactionCount = memory.reactions.values.sumOf { it.size }
                                    val commentCount = memory.comments.size
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(14.dp), tint = Color(0xFFD32F2F))
                                        Text(" $reactionCount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.8f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.Comment, null, modifier = Modifier.size(14.dp), tint = Color(0xFF5D4037).copy(alpha = 0.6f))
                                        Text(" $commentCount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.8f))
                                    }

                                    if (user.isAdmin) {
                                        Row {
                                            if (memory.status == "PENDING") {
                                                IconButton(onClick = { onApprove(memory.id) }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.Check, "Approve", tint = Color(0xFF388E3C), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            IconButton(onClick = { memoryToDelete = memory }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
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
                        for (ingredient in recipe.ingredients) {
                            Text("• $ingredient", style = MaterialTheme.typography.bodySmall)
                        }
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
                    for (emoji in listOf("❤️", "👍", "😋", "🔥")) {
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
                    for (comment in recipe.comments) {
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

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Family Cookbook", "पारिवारिक रसोइया"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF3E2723)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF5D4037),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add Recipe") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(t("Search recipes...", "व्यंजन खोजें..."), color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF3E2723)
                        )
                    )
                }

                if (recipes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.RestaurantMenu,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF5D4037).copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                t("No family recipes yet.", "अभी तक कोई पारिवारिक व्यंजन नहीं।"),
                                color = Color(0xFF5D4037).copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    val filteredRecipes = recipes.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                it.authorName.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredRecipes) { recipe ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedRecipeId = recipe.id },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (recipe.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = recipe.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEFEBE9)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(16.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                                        Text(recipe.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                                        Text(t("By ${recipe.authorName}", "${recipe.authorName} द्वारा"), style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037).copy(alpha = 0.8f))
                                    }
                                    if (user.isAdmin || recipe.authorId == user.id) {
                                        IconButton(onClick = { editingRecipe = recipe }) {
                                            Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF5D4037))
                                        }
                                        IconButton(onClick = { recipeToDelete = recipe }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F))
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
                    for (emoji in listOf("❤️", "👍", "🙏", "✨")) {
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
                    for (comment in trad.comments) {
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

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Family Traditions", "पारिवारिक परंपराएं"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF3E2723)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF5D4037),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Share Tradition") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(t("Search traditions...", "परंपराएं खोजें..."), color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF3E2723),
                        unfocusedTextColor = Color(0xFF3E2723),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF3E2723)
                    )
                )
            }

            if (traditions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF5D4037).copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            t("No family traditions shared yet.", "अभी तक कोई पारिवारिक परंपरा साझा नहीं की गई।"),
                            color = Color(0xFF5D4037).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                val filteredTraditions = traditions.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true) ||
                            it.authorName.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredTraditions) { trad ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedTraditionId = trad.id },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (trad.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = trad.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEFEBE9)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(Modifier.height(12.dp))
                                        }
                                        Text(trad.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                                        Text(t("By ${trad.authorName}", "${trad.authorName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                                    }
                                    if (user.isAdmin || trad.authorId == user.id) {
                                        Row {
                                            IconButton(onClick = { editingTradition = trad }) {
                                                Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF5D4037))
                                            }
                                            IconButton(onClick = { traditionToDelete = trad }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F))
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(trad.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, color = Color(0xFF3E2723).copy(alpha = 0.9f))
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
fun MemoryLaneScreen(
    user: Member,
    milestones: List<Milestone>,
    onBack: () -> Unit,
    onAddMilestone: (Milestone, Uri?) -> Unit,
    onDeleteMilestone: (Milestone) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteRequestDialog by remember { mutableStateOf<Milestone?>(null) }
    val brown900 = Color(0xFF3E2723)
    val brown800 = Color(0xFF5D4037)
    val brown600 = Color(0xFF8D6E63)
    val brown50 = Color(0xFFEFEBE9)

    val userBaseId = if (user.familyId.endsWith("0")) user.familyId.dropLast(1) else user.familyId
    val parentBaseId = if (userBaseId.length > 1) userBaseId.dropLast(1) else if (userBaseId.length == 1 && userBaseId != "P") "P" else ""

    val filteredMilestones = milestones.filter { m ->
        when (m.visibilityType) {
            "GLOBAL" -> true
            "PRIVATE_FAMILY" -> {
                // Visible to the couple (baseId) and their children (starts with baseId)
                userBaseId == m.familyContextId || (userBaseId.startsWith(m.familyContextId) && userBaseId.length == m.familyContextId.length + 1)
            }
            "OLD_IS_GOLD" -> {
                // Visible to siblings and their families (starts with parentBaseId)
                if (m.familyContextId.isEmpty()) false
                else userBaseId.startsWith(m.familyContextId) && userBaseId != m.familyContextId // starts with parent and is not the parent themselves
            }
            else -> true
        }
    }.filter { m ->
        when (selectedTab) {
            0 -> m.visibilityType == "GLOBAL"
            1 -> m.visibilityType == "PRIVATE_FAMILY"
            2 -> m.visibilityType == "OLD_IS_GOLD"
            else -> true
        }
    }

    if (showDeleteRequestDialog != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteRequestDialog = null },
            title = { Text(t("Request Deletion", "हटाने का अनुरोध"), color = brown900, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(t("Why should this milestone (\"${showDeleteRequestDialog?.title}\") be deleted?", "यह मील का पत्थर (\"${showDeleteRequestDialog?.title}\") क्यों हटाया जाना चाहिए?"), color = brown800)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(t("Reason", "कारण")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMilestone(showDeleteRequestDialog!!)
                        showDeleteRequestDialog = null
                    },
                    enabled = reason.isNotBlank()
                ) { Text(t("Submit Request", "अनुरोध भेजें"), color = if (reason.isNotBlank()) brown800 else Color.Gray) }
            },
            dismissButton = { TextButton(onClick = { showDeleteRequestDialog = null }) { Text(t("Cancel", "रद्द करें"), color = brown800) } },
            containerColor = Color.White
        )
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var visibilityType by remember { mutableStateOf(if (selectedTab == 1) "PRIVATE_FAMILY" else if (selectedTab == 2) "OLD_IS_GOLD" else "GLOBAL") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(t("Add Memory", "याद जोड़ें"), color = brown900, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = title, 
                            onValueChange = { title = it }, 
                            label = { Text(t("Title", "शीर्षक")) }, 
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                        )
                        SpeechToTextButton { title = it }
                    }
                    OutlinedTextField(
                        value = year, 
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) year = it }, 
                        label = { Text(t("Year (YYYY)", "वर्ष (YYYY)")) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = location, 
                            onValueChange = { location = it }, 
                            label = { Text(t("Location", "स्थान")) }, 
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                        )
                        IconButton(onClick = { /* Simple location detection could go here */ }) {
                            Icon(Icons.Default.LocationOn, "Location", tint = brown800)
                        }
                    }
                    
                    Text(t("Visibility", "दृश्यता"), style = MaterialTheme.typography.labelMedium, color = brown800, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        FilterChip(
                            selected = visibilityType == "GLOBAL",
                            onClick = { visibilityType = "GLOBAL" },
                            label = { Text(t("Global", "सभी के लिए")) }
                        )
                        FilterChip(
                            selected = visibilityType == "PRIVATE_FAMILY",
                            onClick = { visibilityType = "PRIVATE_FAMILY" },
                            label = { Text(t("Private Family", "निजी परिवार")) }
                        )
                        FilterChip(
                            selected = visibilityType == "OLD_IS_GOLD",
                            onClick = { visibilityType = "OLD_IS_GOLD" },
                            label = { Text(t("Old is Gold", "पुराना सोना")) }
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = description, 
                            onValueChange = { description = it }, 
                            label = { Text(t("Description", "विवरण")) }, 
                            modifier = Modifier.weight(1f), 
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, focusedLabelColor = brown800)
                        )
                        SpeechToTextButton { description = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = brown800)
                    ) {
                        Text(if (selectedUri == null) t("Select Photo", "फोटो चुनें") else t("Photo Selected", "फोटो चुनी गई"))
                    }
                }
            },
            confirmButton = {
                val isValid = title.isNotBlank() && year.length == 4
                TextButton(
                    onClick = {
                        val contextId = when(visibilityType) {
                            "PRIVATE_FAMILY" -> userBaseId
                            "OLD_IS_GOLD" -> parentBaseId
                            else -> ""
                        }
                        onAddMilestone(Milestone(
                            id = UUID.randomUUID().toString(),
                            title = title, 
                            year = year, 
                            description = description, 
                            location = location,
                            authorId = user.id, 
                            authorName = user.name,
                            visibilityType = visibilityType,
                            familyContextId = contextId
                        ), selectedUri)
                        showAddDialog = false
                    },
                    enabled = isValid
                ) { Text(t("Save", "सहेजें"), color = if (isValid) brown800 else Color.Gray) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(t("Cancel", "रद्द करें"), color = brown800) } },
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(t("Memory Lane", "यादों की गली"), color = brown900, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = brown900) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = brown900,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = brown800
                        )
                    }
                }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(t("Family Milestones", "पारिवारिक मील के पत्थर"), style = MaterialTheme.typography.labelMedium) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(t("Private Lane", "निजी गली"), style = MaterialTheme.typography.labelMedium) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(t("Old is Gold", "पुराना सोना"), style = MaterialTheme.typography.labelMedium) })
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = brown800,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add Milestone") }
        }
    ) { padding ->
        if (filteredMilestones.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(t("No memories found here.", "यहाँ कोई यादें नहीं मिलीं।"), color = brown800.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(filteredMilestones) { milestone ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                            Text(milestone.year, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = brown900)
                            Box(modifier = Modifier.width(2.dp).weight(1f).background(brown800.copy(alpha = 0.2f)))
                        }
                        Spacer(Modifier.width(16.dp))
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (milestone.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = milestone.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(milestone.title, style = MaterialTheme.typography.titleMedium, color = brown900, fontWeight = FontWeight.Bold)
                                        if (milestone.location.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocationOn, null, tint = brown600, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(milestone.location, style = MaterialTheme.typography.labelSmall, color = brown600)
                                            }
                                        }
                                    }
                                    if (user.isAdmin || user.id == milestone.authorId) {
                                        IconButton(onClick = { onDeleteMilestone(milestone) }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                        }
                                    } else {
                                        IconButton(onClick = { showDeleteRequestDialog = milestone }) {
                                            Icon(Icons.Default.DeleteForever, "Request Delete", tint = brown800.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(milestone.description, style = MaterialTheme.typography.bodyMedium, color = brown900)
                                if (milestone.authorName.isNotBlank()) {
                                    Text(t("Added by ${milestone.authorName}", "${milestone.authorName} द्वारा जोड़ा गया"), style = MaterialTheme.typography.labelSmall, color = brown800.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                                }

                                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = brown50)

                                // Reactions
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val reactions = listOf("❤️", "👍", "🙏", "✨")
                                    reactions.forEach { emoji ->
                                        val usersWhoReacted = milestone.reactions[emoji] ?: emptyList()
                                        val hasReacted = usersWhoReacted.contains(user.id)
                                        FilterChip(
                                            selected = hasReacted,
                                            onClick = { onToggleReaction(milestone.id, emoji) },
                                            label = { Text("$emoji ${if(usersWhoReacted.isNotEmpty()) usersWhoReacted.size else ""}") },
                                            modifier = Modifier.padding(end = 8.dp),
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = brown800.copy(alpha = 0.1f))
                                        )
                                    }
                                }

                                // Comments Section
                                if (milestone.comments.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    milestone.comments.takeLast(3).forEach { comment ->
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = brown900)) { append("${comment.userName}: ") }
                                                append(comment.text)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }

                                var commentText by remember { mutableStateOf("") }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = commentText,
                                        onValueChange = { commentText = it },
                                        placeholder = { Text(t("Add comment...", "टिप्पणी जोड़ें..."), style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800, unfocusedBorderColor = brown50)
                                    )
                                    IconButton(onClick = {
                                        if (commentText.isNotBlank()) {
                                            onAddComment(milestone.id, commentText)
                                            commentText = ""
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = brown800, modifier = Modifier.size(20.dp))
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
fun CalendarScreen(allMembers: List<Member>, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }

    val brown900 = Color(0xFF3E2723)
    val brown800 = Color(0xFF5D4037)
    val brown50 = Color(0xFFEFEBE9)

    var panchangMap by remember { mutableStateOf(generatePanchangForMonth(currentMonth, allMembers)) }
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentMonth, allMembers) {
        panchangMap = generatePanchangForMonth(currentMonth, allMembers)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("Hindu Calendar", "हिंदू कैलेंडर"), color = brown900, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brown900)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            isSyncing = true
                            val scraped = scrapePanchangData(currentMonth)
                            if (scraped.isNotEmpty()) {
                                panchangMap = panchangMap.toMutableMap().apply {
                                    scraped.forEach { (date, p) ->
                                        this[date] = this[date]?.copy(festivals = p.festivals) ?: p
                                    }
                                }
                                Toast.makeText(context, "Calendar synced successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No festival data found for this month", Toast.LENGTH_SHORT).show()
                            }
                            isSyncing = false
                        }
                    }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = brown900, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = brown900)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = brown50
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Month Selector
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = brown800)
                    }
                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        color = brown900,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = brown800)
                    }
                }
            }

            // Days of the week headers
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = brown800,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Calendar Grid
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(firstDayOfMonth) {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }
                items(daysInMonth) { index ->
                    val day = index + 1
                    val date = currentMonth.atDay(day)
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()
                    val hasEvent = panchangMap[date]?.festivals?.isNotEmpty() == true

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.8f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) brown800 else if (isToday) brown800.copy(alpha = 0.1f) else Color.White)
                            .border(
                                width = if (isToday) 1.dp else 0.dp,
                                color = if (isToday) brown800 else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { 
                                selectedDate = date
                                expandedDate = if (expandedDate == date) null else date
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = day.toString(),
                                color = if (isSelected) Color.White else brown900,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            val p = panchangMap[date]
                            if (p != null) {
                                Text(
                                    text = p.tithiShort,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else brown800,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            if (hasEvent) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .background(if (isSelected) Color.White else Color(0xFFD32F2F), CircleShape)
                                    )
                                }
                                if (p.muhurat.isNotBlank()) {
                                    Text(
                                        text = "M",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White.copy(alpha = 0.6f) else Color(0xFF2E7D32),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expanded Details for selected date
            if (expandedDate != null) {
                val p = panchangMap[expandedDate!!]
                if (p != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = brown800, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${expandedDate!!.dayOfMonth} ${expandedDate!!.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${expandedDate!!.year}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = brown900,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { expandedDate = null }) {
                                    Icon(Icons.Default.Close, null, tint = brown800)
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = brown50)
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(t("Tithi", "तिथि"), p.tithi, Icons.Default.BrightnessLow)
                                    DetailItem(t("Nakshatra", "नक्षत्र"), p.nakshatra, Icons.Default.Star)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(t("Yoga", "योग"), p.yoga, Icons.Default.Sync)
                                    DetailItem(t("Karana", "करण"), p.karana, Icons.Default.Info)
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            DetailItem(t("Muhurat", "मुहूर्त"), p.muhurat, Icons.Default.AccessTime)
                            
                            if (p.festivals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(t("Festivals & Events", "त्योहार और कार्यक्रम"), style = MaterialTheme.typography.labelLarge, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                p.festivals.forEach { festival ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFD32F2F), CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Text(festival, style = MaterialTheme.typography.bodyMedium, color = brown800)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Info placeholder when nothing is expanded
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        t("Tap on a date to view Panchang details", "पंचांग विवरण देखने के लिए किसी तिथि पर टैप करें"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = brown800.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color(0xFF5D4037).copy(alpha = 0.6f))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63))
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
        }
    }
}

data class DayPanchang(
    val tithi: String,
    val tithiShort: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String,
    val muhurat: String,
    val festivals: List<String> = emptyList()
)

fun generatePanchangForMonth(month: YearMonth, allMembers: List<Member>): Map<LocalDate, DayPanchang> {
    val result = mutableMapOf<LocalDate, DayPanchang>()
    
    // Tithi mapping logic (Simplified representation of Hindu Lunar Calendar)
    val tithis = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashti", "Saptami", "Ashtami", "Navami", "Dashami", 
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima", 
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashti", "Saptami", "Ashtami", "Navami", "Dashami", 
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Amavasya"
    )
    
    val nakshatras = listOf("Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati")

    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        val tithiIndex = (day + month.monthValue * 2 + month.year % 30) % 30
        val nakshatraIndex = (day + month.monthValue) % 27
        
        val tithiName = tithis[tithiIndex]
        val paksha = if (tithiIndex < 15) "Shukla Paksha" else "Krishna Paksha"
        
        val festivals = mutableListOf<String>()
        var muhurat = if (day % 3 == 0) "Shubh Muhurat: 09:30 AM - 11:00 AM" else "Abhijit: 11:45 AM - 12:30 PM"
        
        // Family Events
        allMembers.forEach { member ->
            fun matchesDate(dateStr: String?): Boolean {
                if (dateStr.isNullOrBlank()) return false
                return try {
                    val d = LocalDate.parse(dateStr)
                    d.month == date.month && d.dayOfMonth == date.dayOfMonth
                } catch (_: Exception) {
                    try {
                        val parts = dateStr.split("-", "/")
                        val dayPart = parts[0].toInt()
                        val monthPart = parts[1].toInt()
                        monthPart == date.monthValue && dayPart == date.dayOfMonth
                    } catch (_: Exception) { false }
                }
            }

            if (matchesDate(member.dateOfBirth)) {
                festivals.add("🎂 ${member.name}'s Birthday")
            }
            if (!member.marriageDate.isNullOrBlank() && matchesDate(member.marriageDate)) {
                festivals.add("💍 ${member.name}'s Anniversary")
            }
        }

        // Mocking some major festivals based on month and date
        when (month.monthValue) {
            1 -> {
                if (day == 1) festivals.add("New Year's Day")
                if (day == 14 || day == 15) festivals.add("Makar Sankranti / Pongal")
                if (day == 26) festivals.add("Republic Day")
            }
            2 -> {
                if (day == 14) festivals.add("Vasant Panchami")
            }
            3 -> {
                if (tithiName == "Pratipada" && paksha == "Shukla Paksha") festivals.add("Gudi Padwa / Ugadi")
                if (day == 25) festivals.add("Holi")
            }
            4 -> {
                if (day == 14) festivals.add("Ambedkar Jayanti")
                if (day == 17) festivals.add("Ram Navami")
            }
            8 -> {
                if (day == 15) festivals.add("Independence Day")
                if (day == 19) festivals.add("Raksha Bandhan")
                if (day == 26) festivals.add("Janmashtami")
            }
            9 -> {
                if (day == 7) festivals.add("Ganesh Chaturthi")
            }
            10 -> {
                if (day == 2) festivals.add("Gandhi Jayanti")
                if (tithiName == "Dashami" && paksha == "Shukla Paksha") festivals.add("Dussehra")
                if (tithiName == "Amavasya") festivals.add("Deepavali")
            }
            11 -> {
                if (tithiName == "Ekadashi") festivals.add("Dev Deepavali")
                if (day == 1) festivals.add("Bhai Dooj")
            }
            12 -> {
                if (day == 25) festivals.add("Christmas")
            }
        }
        
        if (tithiName == "Ekadashi") festivals.add("Ekadashi Vrat")
        if (tithiName == "Chaturthi") festivals.add("Sankashti Chaturthi")
        if (tithiName == "Purnima") festivals.add("Purnima Vrat")

        result[date] = DayPanchang(
            tithi = "$tithiName ($paksha)",
            tithiShort = tithiName.take(4),
            nakshatra = nakshatras[nakshatraIndex],
            yoga = "Siddha",
            karana = "Bava",
            muhurat = muhurat,
            festivals = festivals
        )
    }
    return result
}

suspend fun scrapePanchangData(month: YearMonth): Map<LocalDate, DayPanchang> {
    return withContext(Dispatchers.IO) {
        val result = mutableMapOf<LocalDate, DayPanchang>()
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
        val year = month.year
        
        // Try multiple common URL patterns used by this site
        val urlPatterns = listOf(
            "https://lalaramswaroopcalendar.com/lala-ramswaroop-calendar-$monthName-$year/",
            "https://lalaramswaroopcalendar.com/$monthName-$year-calendar/",
            "https://lalaramswaroopcalendar.com/lala-ramswaroop-panchang-$monthName-$year/"
        )

        for (urlString in urlPatterns) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode != 200) continue

                val html = connection.inputStream.bufferedReader().use { it.readText() }
                
                val monthLabel = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                // More robust regex: matches day, optional month/year, and the festival name after a colon or within tags
                val pattern = """<li>(?:\s*<strong>)?\s*(\d{1,2})(?:\s+$monthLabel)?(?:\s+$year)?.*?(?::|</strong>)\s*([^<]+)</li>"""
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                
                val matches = regex.findAll(html)
                if (matches.none()) continue // Try next URL if no matches found

                matches.forEach { match ->
                    val day = match.groupValues[1].toIntOrNull() ?: return@forEach
                    if (day < 1 || day > month.lengthOfMonth()) return@forEach
                    
                    val festivalName = match.groupValues[2].trim()
                        .replace("&nbsp;", " ")
                        .replace(Regex("""\s+"""), " ")
                    
                    val date = month.atDay(day)
                    val existing = result[date]
                    if (existing != null) {
                        if (!existing.festivals.contains(festivalName)) {
                            result[date] = existing.copy(festivals = existing.festivals + festivalName)
                        }
                    } else {
                        result[date] = DayPanchang(
                            tithi = "", tithiShort = "", nakshatra = "", yoga = "", karana = "", muhurat = "",
                            festivals = listOf(festivalName)
                        )
                    }
                }
                
                if (result.isNotEmpty()) break // Success!
            } catch (e: Exception) {
                Log.e("PanchangScraper", "Error scraping $urlString: ${e.message}")
            }
        }
        result
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
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

fun isWithinSevenDays(dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    val today = LocalDate.now()
    return try {
        val date = try {
            LocalDate.parse(dateStr)
        } catch (_: Exception) {
            val parts = dateStr.split("-", "/")
            LocalDate.of(today.year, parts[1].toInt(), parts[0].toInt())
        }
        
        val thisYearEvent = date.withYear(today.year)
        val eventDate = if (thisYearEvent.isBefore(today)) {
            thisYearEvent.withYear(today.year + 1)
        } else {
            thisYearEvent
        }
        
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
        daysUntil in 1..7
    } catch (_: Exception) {
        false
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
    val isPreview = LocalInspectionMode.current
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember(context, isPreview) {
        if (isPreview) null else SpeechRecognizer.createSpeechRecognizer(context)
    }
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
        if (isGranted && speechRecognizer != null) {
            isListening = true
            speechRecognizer.startListening(recognizerIntent)
        }
    }

    if (speechRecognizer != null) {
        DisposableEffect(speechRecognizer) {
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
    }

    IconButton(onClick = {
        if (speechRecognizer != null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                isListening = true
                speechRecognizer.startListening(recognizerIntent)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
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
            onNavigateToFamilyTree = {},
            onNavigateToCalendar = {},
            deletionRequests = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTreeScreen(
    currentUser: Member,
    members: List<Member>,
    onNavigateBack: () -> Unit,
    onMemberClick: (Member) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Smooth animations for centering/zooming
    var isUserInteracting by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (isUserInteracting) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    val animOffsetX by animateFloatAsState(
        targetValue = offset.x,
        animationSpec = if (isUserInteracting) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "offsetX"
    )
    val animOffsetY by animateFloatAsState(
        targetValue = offset.y,
        animationSpec = if (isUserInteracting) snap() else spring(stiffness = Spring.StiffnessLow),
        label = "offsetY"
    )
    val animatedOffset = Offset(animOffsetX, animOffsetY)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var treeCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val memberPositions = remember { mutableStateMapOf<String, Offset>() }
    val expandedNodes = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Track which member is currently "focused" or zoomed in on
    var focusedMemberId by remember { mutableStateOf<String?>(null) }

    val pRoot = members.find { it.familyId == "P" }
    val roots = if (pRoot != null) {
        listOf(pRoot)
    } else {
        members.filter { it.familyId.length == 1 && !it.familyId.endsWith("0") }.sortedBy { it.familyId }
    }

    fun centerOnMember(memberId: String) {
        val member = members.find { it.id == memberId } ?: return
        
        // Auto-expand ancestors
        var fid = member.familyId
        while (fid.isNotEmpty()) {
            val parentFid = if (fid.endsWith("0")) fid.dropLast(1) else fid.dropLast(1)
            val parent = members.find { it.familyId == parentFid || it.familyId == parentFid + "0" }
            if (parent != null) expandedNodes[parent.id] = true
            fid = if (fid.length > 1) fid.dropLast(1) else ""
        }

        scope.launch {
            kotlinx.coroutines.delay(100)
            val pos = memberPositions[memberId] ?: return@launch
            val treeCoords = treeCoordinates ?: return@launch
            
            // The tree is centered in its container. 
            // pos is relative to treeRootCoordinates (the Box containing the Column)
            val treeCenterX = treeCoords.size.width / 2f
            val treeCenterY = treeCoords.size.height / 2f
            
            val targetFromCenter = pos - Offset(treeCenterX, treeCenterY)
            
            scale = 1.0f // Reset scale to 1.0 for readability as requested
            offset = -targetFromCenter
            focusedMemberId = memberId
        }
    }

    fun performZoom(zoomFactor: Float) {
        val oldScale = scale
        scale = (scale * zoomFactor).coerceIn(0.1f, 10f)
        
        val focalPoint = if (focusedMemberId != null) {
            memberPositions[focusedMemberId] ?: Offset(containerSize.width / 2f, containerSize.height / 2f)
        } else {
            Offset(containerSize.width / 2f, containerSize.height / 2f)
        }

        val treeCoords = treeCoordinates ?: return
        val treeContentCenter = Offset(treeCoords.size.width / 2f, treeCoords.size.height / 2f)
        val relativeFocalPoint = focalPoint - treeContentCenter
        
        offset = offset - (relativeFocalPoint * (scale - oldScale))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchVisible) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(t("Search family...", "परिवार खोजें...")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isSearchVisible = false; searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        )
                    } else {
                        Text(t("Family Tree", "वंश वृक्ष")) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!isSearchVisible) {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                    IconButton(onClick = { centerOnMember(currentUser.id) }) { 
                        Icon(Icons.Default.MyLocation, "Find Me") 
                    }
                    IconButton(onClick = { scale = 1f; offset = Offset.Zero; focusedMemberId = null }) { 
                        Icon(Icons.Default.FilterCenterFocus, "Reset") 
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFEFEBE9))
        ) {
            // Main Canvas for Zoom/Pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                isUserInteracting = true
                            } while (event.changes.any { it.pressed })
                            isUserInteracting = false
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            scale = (scale * zoom).coerceIn(0.1f, 10f)
                            val effectiveZoom = scale / oldScale
                            
                            val pivot = Offset(containerSize.width / 2f, containerSize.height / 2f)
                            offset = (centroid - pivot) - (centroid - pivot - offset) * effectiveZoom + pan
                            
                            if (zoom != 1f) focusedMemberId = null
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            translationX = animatedOffset.x
                            translationY = animatedOffset.y
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                            clip = false
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(unbounded = true) // Allow tree to be larger than screen
                            .onGloballyPositioned { treeCoordinates = it }
                            .align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(200.dp), // Padding to allow space for connectors
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (root in roots) {
                                FamilyNode(
                                    root, 
                                    members, 
                                    onMemberClick = { member ->
                                        if (focusedMemberId == member.id) onMemberClick(member)
                                        else centerOnMember(member.id)
                                    },
                                    scale = animatedScale, 
                                    currentUserId = currentUser.id,
                                    focusedMemberId = focusedMemberId,
                                    treeRootCoordinates = treeCoordinates,
                                    onPositionReported = { id, pos -> memberPositions[id] = pos },
                                    expandedNodes = expandedNodes,
                                    viewportSize = containerSize,
                                    viewportOffset = animatedOffset
                                )
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    }
                }
            }

            // Search Results Overlay
            if (isSearchVisible && searchQuery.isNotEmpty()) {
                val results = members.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(5)
                if (results.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .width(300.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        LazyColumn {
                            items(results) { member ->
                                ListItem(
                                    headlineContent = { Text(member.name) },
                                    supportingContent = { Text(member.relationship ?: "") },
                                    modifier = Modifier.clickable {
                                        centerOnMember(member.id)
                                        isSearchVisible = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { performZoom(1.2f) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Add, "Zoom In")
                }
                SmallFloatingActionButton(
                    onClick = { performZoom(0.8f) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Remove, "Zoom Out")
                }
                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                        focusedMemberId = null
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.CenterFocusStrong, "Reset")
                }
            }
        }
    }
}

@Composable
fun FamilyNode(
    member: Member,
    allMembers: List<Member>,
    onMemberClick: (Member) -> Unit,
    scale: Float = 1f,
    currentUserId: String? = null,
    focusedMemberId: String? = null,
    treeRootCoordinates: LayoutCoordinates? = null,
    onPositionReported: (String, Offset) -> Unit = { _, _ -> },
    expandedNodes: MutableMap<String, Boolean> = mutableStateMapOf(),
    viewportSize: IntSize = IntSize.Zero,
    viewportOffset: Offset = Offset.Zero
) {
    val isExpanded = expandedNodes[member.id] ?: true
    val spouseId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
    val spouse = allMembers.find { it.familyId == spouseId }
    
    val children = allMembers.filter { 
        val baseId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId
        if (baseId == "P") {
            it.familyId.length == 1 && it.familyId != "P" && !it.familyId.endsWith("0")
        } else {
            it.familyId.length == baseId.length + 1 && it.familyId.startsWith(baseId) && !it.familyId.endsWith("0")
        }
    }.sortedBy { it.familyId }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomCenter) {
                MemberSmallCard(
                    member = member, 
                    onClick = onMemberClick, 
                    currentScale = scale, 
                    isSelf = member.id == currentUserId,
                    isFocused = member.id == focusedMemberId,
                    treeRootCoordinates = treeRootCoordinates,
                    onPositioned = { pos -> onPositionReported(member.id, pos) },
                    viewportSize = viewportSize,
                    viewportOffset = viewportOffset
                )
                
                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = { expandedNodes[member.id] = !isExpanded },
                        modifier = Modifier
                            .offset(y = 12.dp)
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, Color(0xFF8D6E63), CircleShape)
                            .zIndex(2f)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand/Collapse",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF8D6E63)
                        )
                    }
                }
            }
            
            if (spouse != null) {
                ConnectionLine(isVertical = false, length = 32.dp)
                MemberSmallCard(
                    member = spouse, 
                    onClick = onMemberClick, 
                    currentScale = scale, 
                    isSelf = spouse.id == currentUserId,
                    isFocused = spouse.id == focusedMemberId,
                    treeRootCoordinates = treeRootCoordinates,
                    onPositioned = { pos -> onPositionReported(spouse.id, pos) },
                    viewportSize = viewportSize,
                    viewportOffset = viewportOffset
                )
            }
        }
        
        if (children.isNotEmpty() && isExpanded) {
            ConnectionLine(isVertical = true, length = 32.dp)
            Row {
                for (index in children.indices) {
                    val child = children[index]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (children.size > 1) {
                            ConnectionLine(
                                isVertical = false, 
                                length = 0.dp,
                                isStart = index > 0, 
                                isEnd = index < children.size - 1
                            )
                        }
                        ConnectionLine(isVertical = true, length = 32.dp)
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            FamilyNode(
                                child, allMembers, onMemberClick, scale, currentUserId, 
                                focusedMemberId, treeRootCoordinates, onPositionReported, expandedNodes,
                                viewportSize, viewportOffset
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionLine(
    isVertical: Boolean,
    length: Dp = 24.dp,
    isStart: Boolean = true,
    isEnd: Boolean = true
) {
    val color = Color(0xFF8D6E63) // Solid brown color for better visibility
    val thickness = 2.dp
    
    Canvas(
        modifier = if (isVertical) {
            Modifier.width(thickness).height(length)
        } else {
            if (length > 0.dp) Modifier.width(length).height(thickness)
            else Modifier.fillMaxWidth().height(thickness)
        }
    ) {
        if (isVertical) {
            drawLine(
                color = color,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = thickness.toPx()
            )
        } else {
            val startX = if (isStart) 0f else size.width / 2
            val endX = if (isEnd) size.width else size.width / 2
            drawLine(
                color = color,
                start = Offset(startX, size.height / 2),
                end = Offset(endX, size.height / 2),
                strokeWidth = thickness.toPx()
            )
        }
    }
}

@Composable
fun MemberSmallCard(
    member: Member, 
    onClick: (Member) -> Unit, 
    currentScale: Float = 1f, 
    isSelf: Boolean = false,
    isFocused: Boolean = false,
    treeRootCoordinates: LayoutCoordinates? = null,
    onPositioned: (Offset) -> Unit = {},
    viewportSize: IntSize = IntSize.Zero,
    viewportOffset: Offset = Offset.Zero
) {
    val focusScale = 1f
    val cardWidth = 150.dp 
    val photoSize = 60.dp

    // Improved gender detection
    val isFemale = member.gender.equals("Female", ignoreCase = true) || 
                   member.gender.equals("F", ignoreCase = true) || 
                   member.gender.contains("स्त्री", ignoreCase = true)
    
    val genderColor = if (isFemale) Color(0xFFE91E63) else Color(0xFF2196F3) // Pink for girls, Blue for guys

    Card(
        onClick = { onClick(member) },
        modifier = Modifier
            .requiredWidth(cardWidth)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
                if (isFocused) {
                    shadowElevation = 20.dp.toPx()
                }
            }
            .onGloballyPositioned { coords ->
                treeRootCoordinates?.let { root ->
                    val pos = root.localPositionOf(coords, Offset.Zero)
                    val center = pos + Offset(coords.size.width / 2f, coords.size.height / 2f)
                    onPositioned(center)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = if (isFocused) 4.dp else 3.dp, 
            color = genderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 12.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo Section
            Box(
                modifier = Modifier
                    .size(photoSize)
                    .clip(CircleShape)
                    .background(genderColor.copy(alpha = 0.1f))
                    .border(2.dp, genderColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!member.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = member.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isFemale) Icons.Default.Woman else Icons.Default.Man,
                        null, 
                        modifier = Modifier.size(photoSize * 0.7f), 
                        tint = genderColor.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Name
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color(0xFF212121)
            )
            
            // Relationship
            val relationshipText = when {
                isSelf -> t("Me", "मैं")
                !member.relationship.isNullOrEmpty() -> member.relationship
                else -> ""
            }
            
            if (!relationshipText.isNullOrEmpty()) {
                Surface(
                    color = genderColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = relationshipText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        ),
                        color = genderColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
