package com.purawale.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.purawale.app.*
import com.purawale.app.R
import com.purawale.app.ui.components.EventAvatar
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireworksEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "fireworks")
    
    // Define particles once and just animate them.
    val particles = remember {
        List(5) { // 5 simultaneous fireworks
            val centerX = Random.nextFloat()
            val centerY = Random.nextFloat() * 0.4f + 0.1f
            val color = Color(
                red = Random.nextFloat().coerceIn(0.4f, 1f),
                green = Random.nextFloat().coerceIn(0.4f, 1f),
                blue = Random.nextFloat().coerceIn(0.4f, 1f),
                alpha = 1f
            )
            List(30) {
                val angle = Random.nextFloat() * 2 * PI
                val speed = Random.nextFloat() * 6f + 2f
                FireworkParticle(centerX, centerY, cos(angle).toFloat() * speed, sin(angle).toFloat() * speed, color)
            }
        }
    }

    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "explosion"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { list ->
            list.forEach { p ->
                val startX = p.x * w
                val startY = p.y * h
                // Explosion expansion + gravity
                val x = startX + p.vx * animValue * 150f
                val y = startY + p.vy * animValue * 150f + (animValue * animValue * 300f) 
                
                drawCircle(
                    color = p.color.copy(alpha = (1f - animValue).coerceIn(0f, 1f)),
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

data class FireworkParticle(val x: Float, val y: Float, val vx: Float, val vy: Float, val color: Color)

@Composable
fun CelebrationBanner(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "banner")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC857)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFFC857), Color(0xFFFFD54F), Color(0xFFFFC857))
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF101522),
                    letterSpacing = 1.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

data class FeatureItem(val title: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    user: Member,
    allMembers: List<Member>,
    pendingMembers: List<Member>,
    deletionRequests: List<DeletionRequest>,
    channels: List<ChatChannel>,
    unreadNotificationsCount: Int,
    currentTreeId: String = "primary",
    onSwitchTree: (String) -> Unit = {},
    onNavigateToProfiles: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToDiscussions: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onLogout: () -> Unit,
    onViewProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onNavigateToCookbook: () -> Unit,
    onNavigateToTraditions: () -> Unit,
    onNavigateToMemoryLane: () -> Unit,
    onNavigateToFamilyTree: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToEmergency: () -> Unit = {},
    onNavigateToFamilyGames: () -> Unit,
    onNavigateToBusinessDirectory: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToLoginLog: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onGenerateAICard: (Member, String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    DisposableEffect(Unit) {
        val listener = FirebaseManager.getCalendarEvents { calendarEvents = it }
        onDispose { listener.remove() }
    }

    val visibleMembers = if (user.isAdmin) allMembers else allMembers.filter { !it.isAdmin || it.id == user.id }
    val canSwitchTreeView = user.secondaryTreeEnabled
    val isBranchView = currentTreeId != "primary"

    val todayBirthdays = visibleMembers.filter { it.bereavementDate.isNullOrBlank() && isToday(it.dateOfBirth) }
    val todayAnniversaries = visibleMembers
        .filter { !it.marriageDate.isNullOrBlank() && it.bereavementDate.isNullOrBlank() && isToday(it.marriageDate) }
        .distinctBy {
            val partnerId = if (it.familyId.endsWith("0")) it.familyId.dropLast(1) else it.familyId + "0"
            listOf(it.familyId, partnerId).sorted().joinToString("-")
        }

    val remembrancePool = if (user.isAdmin) (allMembers + pendingMembers).distinctBy { it.id } else visibleMembers
    val todayRemembrances = remembrancePool.filter {
        !it.bereavementDate.isNullOrBlank() && (isToday(it.bereavementDate) || isToday(it.dateOfBirth))
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Color(0xFF1A1C1E),
            shape = RoundedCornerShape(28.dp),
            title = { Text(t("Change Password", "पासवर्ड बदलें"), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(t("New Password", "नया पासवर्ड")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFC857),
                        focusedLabelColor = Color(0xFFFFC857),
                        cursorColor = Color(0xFFFFC857),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isNotBlank()) {
                            onPasswordChange(newPassword.hash())
                            showPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(t("Save", "सहेजें"), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    val isCelebrationDay = remember(user) {
        isToday(user.dateOfBirth) || (!user.marriageDate.isNullOrBlank() && isToday(user.marriageDate))
    }
    val celebrationText = when {
        isToday(user.dateOfBirth) && !user.marriageDate.isNullOrBlank() && isToday(user.marriageDate) ->
            t("Double Celebration! Happy Birthday & Anniversary!", "दोहरा जश्न! जन्मदिन और सालगिरह मुबारक!")
        isToday(user.dateOfBirth) -> t("Happy Birthday, ${user.name.split(" ").first()}! 🎉", "जन्मदिन मुबारक, ${user.name.split(" ").first()}! 🎉")
        !user.marriageDate.isNullOrBlank() && isToday(user.marriageDate) ->
            t("Happy Anniversary! Wishing you togetherness forever! ❤️", "शादी की सालगिरह मुबारक! ❤️")
        else -> ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.75f), BlendMode.Darken)
        )

        if (isCelebrationDay) {
            FireworksEffect()
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                t("Purawale Hum aur Humare", "Purawale Hum aur Humare"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                t("Nema Sub- Community", "नेमा उप-समुदाय"),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFFC857)
                            )
                        }
                    },
                    actions = {
                        if (canSwitchTreeView) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                                    .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    "P",
                                    color = if (!isBranchView) Color.White else Color.White.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (!isBranchView) FontWeight.ExtraBold else FontWeight.Bold
                                )
                                Switch(
                                    checked = isBranchView,
                                    onCheckedChange = { useBranch -> onSwitchTree(if (useBranch) user.id else "primary") },
                                    modifier = Modifier.scale(0.68f),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF101522),
                                        checkedTrackColor = Color(0xFFFFC857),
                                        uncheckedThumbColor = Color(0xFFFFC857),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.20f)
                                    )
                                )
                                Text(
                                    "B",
                                    color = if (isBranchView) Color.White else Color.White.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isBranchView) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            }
                        }
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge { Text(unreadNotificationsCount.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = onNavigateToNotifications) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                        IconButton(onClick = { showPasswordDialog = true }) {
                            Icon(Icons.Default.Password, contentDescription = "Password")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color(0xFF080B14).copy(alpha = 0.90f),
                        scrolledContainerColor = Color(0xFF101522),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isCelebrationDay && celebrationText.isNotEmpty()) {
                    item {
                        CelebrationBanner(celebrationText)
                    }
                }

                // Profile Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.12f), Color(0xFFFFC857).copy(alpha = 0.08f))
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!user.photoUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp), tint = Color(0xFFFFC857))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(user.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f))

                                    if (!user.location.isNullOrEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC857))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(user.location, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
                                        }
                                    }
                                }
                                Column {
                                    IconButton(onClick = onViewProfile) { Icon(Icons.Default.Visibility, null) }
                                    IconButton(onClick = onEditProfile) { Icon(Icons.Default.Edit, null) }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DashboardInfoChip(label = t("Birthday", "जन्मदिन"), value = formatDateToDDMMM(user.dateOfBirth))
                                if (!user.marriageDate.isNullOrBlank()) {
                                    DashboardInfoChip(label = t("Anniversary", "वर्षगांठ"), value = formatDateToDDMMM(user.marriageDate))
                                }
                                DashboardInfoChip(label = "Points", value = user.points.toString(), icon = Icons.Default.Stars)
                            }

                            if (canSwitchTreeView && currentTreeId == "__legacy_card_switch__") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilterChip(
                                        selected = !isBranchView,
                                        onClick = { onSwitchTree("primary") },
                                        label = { Text(t("Primary", "à¤ªà¥à¤°à¤¾à¤¥à¤®à¤¿à¤•")) },
                                        leadingIcon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = isBranchView,
                                        onClick = { onSwitchTree(user.id) },
                                        label = { Text(t("My Branch", "à¤®à¥‡à¤°à¥€ à¤¶à¤¾à¤–à¤¾")) },
                                        leadingIcon = { Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Events Today
                val todayEvents = calendarEvents.filter { it.date == LocalDate.now().toString() }
                if (todayBirthdays.isNotEmpty() || todayAnniversaries.isNotEmpty() || todayRemembrances.isNotEmpty() || todayEvents.isNotEmpty()) {
                    item {
                        DashboardSectionHeader(t("Today's Events", "आज के कार्यक्रम"))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            todayBirthdays.forEach { member -> DashboardEventItem(member, "Birthday", onNavigateToMessages, allMembers, user, onGenerateAICard) }
                            todayAnniversaries.forEach { member -> DashboardEventItem(member, "Anniversary", onNavigateToMessages, allMembers, user, onGenerateAICard) }
                            todayRemembrances.forEach { member -> DashboardEventItem(member, "Remembrance", onNavigateToMessages, allMembers, user, onGenerateAICard) }
                            todayEvents.forEach { event ->
                                val context = LocalContext.current
                                Card(
                                    onClick = { onNavigateToCalendar() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("${event.type}${if (event.location.isNotEmpty()) " @ ${event.location}" else ""}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (event.mapsLink.isNotEmpty()) {
                                                IconButton(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, event.mapsLink.toUri())
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {}
                                                    }
                                                ) {
                                                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        if (event.description.isNotEmpty()) {
                                            Text(
                                                text = event.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 8.dp, start = 56.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Actions / Features Grid
                item {
                    DashboardSectionHeader(t("Explore", "एक्सप्लोर करें"))
                    val totalUnread = channels.sumOf { it.unreadCount[user.id] ?: 0 }
                    val features = listOf(
                        FeatureItem(t("Profiles", "प्रोफ़ाइल"), Icons.Default.People, MaterialTheme.colorScheme.primary, onNavigateToProfiles),
                        FeatureItem(t("Gallery", "गैलरी"), Icons.Default.Collections, MaterialTheme.colorScheme.secondary, onNavigateToGallery),
                        FeatureItem(t("Discussions", "चर्चा"), Icons.Default.Forum, MaterialTheme.colorScheme.tertiary, onNavigateToDiscussions),
                        FeatureItem(t("Messages", "संदेश"), Icons.Default.Email, MaterialTheme.colorScheme.primary, onNavigateToMessages),
                        FeatureItem(t("Cookbook", "नुस्खे"), Icons.Default.Restaurant, MaterialTheme.colorScheme.secondary, onNavigateToCookbook),
                        FeatureItem(t("Traditions", "परंपरा"), Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.tertiary, onNavigateToTraditions),
                        FeatureItem(t("Memory Lane", "यादें"), Icons.Default.History, MaterialTheme.colorScheme.primary, onNavigateToMemoryLane),
                        FeatureItem(t("Family Tree", "वंश वृक्ष"), Icons.Default.AccountTree, MaterialTheme.colorScheme.secondary, onNavigateToFamilyTree),
                        FeatureItem(t("Calendar", "कैलेंडर"), Icons.Default.CalendarMonth, MaterialTheme.colorScheme.tertiary, onNavigateToCalendar),
                        FeatureItem(t("Emergency", "आपातकाल"), Icons.Default.Emergency, Color(0xFFE53935), onNavigateToEmergency),
                        FeatureItem(t("Achievements", "उपलब्धियां"), Icons.Default.EmojiEvents, MaterialTheme.colorScheme.primary, onNavigateToAchievements),
                        FeatureItem(t("Family Games", "खेल"), Icons.Default.SportsEsports, MaterialTheme.colorScheme.secondary, onNavigateToFamilyGames),
                        FeatureItem(t("Business", "व्यवसाय"), Icons.Default.Business, MaterialTheme.colorScheme.tertiary, onNavigateToBusinessDirectory)
                    )

                    val adminFeatures = if (user.isAdmin) {
                        listOf(
                            FeatureItem(t("Activity Log", "गतिविधि लॉग"), Icons.Default.History, MaterialTheme.colorScheme.primary, onNavigateToActivityLog),
                            FeatureItem(t("Login Log", "लॉगिन लॉग"), Icons.Default.Login, MaterialTheme.colorScheme.secondary, onNavigateToLoginLog)
                        )
                    } else emptyList()

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2
                    ) {
                        (features + adminFeatures).forEach { feature ->
                            val badge = if (feature.title == t("Messages", "संदेश")) totalUnread else 0
                            DashboardFeatureCard(feature, badge, Modifier.weight(1f))
                        }
                    }
                }

                // Upcoming Events
                item {
                    val upcomingBirthdays = visibleMembers.filter {
                        it.bereavementDate.isNullOrBlank() && isWithinSevenDays(it.dateOfBirth) && !isToday(it.dateOfBirth)
                    }
                    val upcomingAnniversaries = visibleMembers
                        .filter { !it.marriageDate.isNullOrBlank() && it.bereavementDate.isNullOrBlank() && isWithinSevenDays(it.marriageDate) && !isToday(it.marriageDate) }
                        .distinctBy {
                            val partnerId = if (it.familyId.endsWith("0")) it.familyId.dropLast(1) else it.familyId + "0"
                            listOf(it.familyId, partnerId).sorted().joinToString("-")
                        }
                    val upcomingRemembrances = visibleMembers.filter {
                        !it.bereavementDate.isNullOrBlank() && ((isWithinSevenDays(it.bereavementDate) && !isToday(it.bereavementDate)) || (isWithinSevenDays(it.dateOfBirth) && !isToday(it.dateOfBirth)))
                    }

                    val combinedUpcoming = (upcomingBirthdays.map { it to "Birthday" } +
                            upcomingAnniversaries.map { it to "Anniversary" } +
                            upcomingRemembrances.map { it to "Remembrance" })
                        .sortedBy { (m, t) ->
                            val dateStr = when(t) {
                                "Birthday" -> m.dateOfBirth
                                "Anniversary" -> m.marriageDate
                                else -> if (isWithinSevenDays(m.bereavementDate)) m.bereavementDate else m.dateOfBirth
                            }
                            // Sort by day and month
                            val parts = dateStr?.split("-", "/") ?: listOf("01", "01")
                            if (parts.size >= 2) parts[1].padStart(2, '0') + parts[0].padStart(2, '0') else "9999"
                        }.take(5)

                    val upcomingCustom = calendarEvents.filter {
                        val date = try { LocalDate.parse(it.date) } catch(e: Exception) { null }
                        date != null && date.isAfter(LocalDate.now()) && date.isBefore(LocalDate.now().plusDays(8))
                    }

                    if (combinedUpcoming.isNotEmpty() || upcomingCustom.isNotEmpty()) {
                        DashboardSectionHeader(t("Coming Up", "आगामी"))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                combinedUpcoming.forEach { (member, type) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        EventAvatar(member.photoUrl, member.name)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val rel = member.relationship ?: (if (user != null) FamilyUtils.getRelationship(member, user, allMembers) else null)
                                            val displayName = if (!rel.isNullOrEmpty()) "${member.name} ($rel)" else member.name
                                            Text(displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)

                                            val eventLabel = when(type) {
                                                "Birthday" -> t("Birthday", "जन्मदिन")
                                                "Anniversary" -> t("Anniversary", "वर्षगांठ")
                                                else -> t("Remembrance", "पुण्यतिथि")
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    eventLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(0xFFFFC857)
                                                )
                                                if (type == "Anniversary") {
                                                    val partnerId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
                                                    val partner = allMembers.find { it.familyId == partnerId }
                                                    val spouseName = member.spouseName ?: partner?.name ?: ""
                                                    if (spouseName.isNotEmpty()) {
                                                        Text(
                                                            " & $spouseName",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = Color.White.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        val eventDate = when(type) {
                                            "Birthday" -> member.dateOfBirth
                                            "Anniversary" -> member.marriageDate
                                            else -> if (isWithinSevenDays(member.bereavementDate)) member.bereavementDate else member.dateOfBirth
                                        }

                                        Text(
                                            formatDateToDDMMM(eventDate),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                upcomingCustom.forEach { event ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Event, null, tint = Color(0xFFFFC857), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text(event.type, style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC857))
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            formatDateToDDMMM(event.date),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
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

@Composable
fun DashboardSectionHeader(title: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = Color(0xFFFFC857))
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFFFFC857), CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun DashboardInfoChip(label: String, value: String, icon: ImageVector? = null) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.68f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC857))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun DashboardEventItem(
    member: Member,
    type: String,
    onWish: () -> Unit,
    allMembers: List<Member> = emptyList(),
    currentUser: Member? = null,
    onGenerateAICard: (Member, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventAvatar(member.photoUrl, member.name)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val title = when(type) {
                    "Birthday" -> t("Birthday", "जन्मदिन")
                    "Anniversary" -> t("Anniversary", "वर्षगांठ")
                    else -> t("Remembrance", "पुण्यतिथि")
                }

                val rel = member.relationship ?: (if (currentUser != null) FamilyUtils.getRelationship(member, currentUser, allMembers) else null)
                val nameWithRel = if (!rel.isNullOrEmpty()) "${member.name} ($rel)" else member.name

                Text("$title: $nameWithRel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                if (type == "Anniversary") {
                    val partnerId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
                    val partner = allMembers.find { it.familyId == partnerId }
                    val partnerRel = partner?.relationship ?: (if (partner != null && currentUser != null) FamilyUtils.getRelationship(partner, currentUser, allMembers) else null)

                    val spouseName = member.spouseName ?: partner?.name ?: ""
                    val spouseText = if (!partnerRel.isNullOrEmpty()) "$spouseName ($partnerRel)" else spouseName

                    if (spouseName.isNotEmpty()) {
                        Text("with $spouseText", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.70f))
                    }
                }
            }

            if (type != "Remembrance") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onGenerateAICard(member, type) },
                        modifier = Modifier.background(Color(0xFFE1BEE7), CircleShape)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF4A148C), modifier = Modifier.size(20.dp))
                    }
                    if (member.phoneNumber.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val phone = member.phoneNumber.filter { it.isDigit() }
                                val finalPhone = if (phone.length == 10) "91$phone" else phone
                                val msg = "Happy $type ${member.name}!"
                                val url = "https://wa.me/$finalPhone?text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            },
                            modifier = Modifier.background(Color(0xFFFFC857), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF101522), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardFeatureCard(feature: FeatureItem, badgeCount: Int, modifier: Modifier = Modifier) {
    Card(
        onClick = feature.onClick,
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(feature.color.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BadgedBox(
                    badge = {
                        if (badgeCount > 0) Badge { Text(badgeCount.toString()) }
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            feature.icon,
                            contentDescription = null,
                            tint = Color(0xFFFFC857),
                            modifier = Modifier.padding(12.dp).size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(feature.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
