package com.purawale.app.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.purawale.app.ui.components.EventAvatar
import com.purawale.app.*
import com.purawale.app.R
import com.purawale.app.ui.theme.LightGolden
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class FeatureItem(val title: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: Member,
    allMembers: List<Member>,
    pendingMembers: List<Member>,
    deletionRequests: List<DeletionRequest>,
    channels: List<ChatChannel>,
    unreadNotificationsCount: Int,
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
    onNavigateToFamilyGames: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Filter members to hide admins from non-admin users
    val visibleMembers = if (user.isAdmin) allMembers else allMembers.filter { !it.isAdmin || it.id == user.id }

    val isHindi = LocalLanguage.current
    val todayPanchang = remember(isHindi) { generatePanchangForMonth(YearMonth.now(), isHindi)[LocalDate.now()] }

    val todayBirthdays = visibleMembers.filter { it.bereavementDate.isNullOrBlank() && isToday(it.dateOfBirth) }
    val todayAnniversaries = visibleMembers
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
        visibleMembers
    }

    val todayRemembrances = remembrancePool.filter {
        (!it.bereavementDate.isNullOrBlank() && isToday(it.bereavementDate)) ||
        (!it.bereavementDate.isNullOrBlank() && isToday(it.dateOfBirth))
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
            },
            title = { Text("Change Password") },
            text = {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword.isNotBlank()) {
                        onPasswordChange(newPassword.hash())
                        showPasswordDialog = false
                    }
                }) { Text("Save", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                }) { Text("Cancel", color = Color(0xFF5D4037)) }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Dashboard", "डैशबोर्ड"), color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold) },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge {
                                    Text(unreadNotificationsCount.toString())
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF5D4037))
                        }
                    }
                    IconButton(onClick = {
                        showPasswordDialog = true
                    }) {
                        Icon(Icons.Default.Password, contentDescription = t("Password", "पासवर्ड"), tint = Color(0xFF5D4037))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = t("Logout", "लॉगआउट"), tint = Color(0xFF5D4037))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
        ) {

            item {
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

                                if (user.isAdmin && user.lastLoggedIn != null) {
                                    val lastLoginStr = Instant.ofEpochMilli(user.lastLoggedIn)
                                        .atZone(ZoneId.systemDefault())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
                                    Text("Last Login: $lastLoginStr", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                }

                                if (!user.location.isNullOrEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color(0xFF8D6E63))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(user.location, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFC107)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Lvl ${user.level} • ${user.points} pts",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF3E2723),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = onViewProfile) {
                                Icon(Icons.Default.Visibility, contentDescription = "View My Profile", tint = Color(0xFF5D4037))
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
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFD7CCC8))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(t("Birthday", "जन्मदिन"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                val age = calculateAge(user.dateOfBirth)
                                val ageSuffix = if (age != null) " (Age $age)" else ""
                                Text(formatDateToDDMMM(user.dateOfBirth) + ageSuffix, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
                            }
                            if (!user.marriageDate.isNullOrBlank()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(t("Anniversary", "वर्षगांठ"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63), fontWeight = FontWeight.Bold)
                                    val years = calculateAge(user.marriageDate)
                                    val yearsSuffix = if (years != null && years > 0) " (Age $years)" else ""
                                    Text(formatDateToDDMMM(user.marriageDate) + yearsSuffix, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
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
                                val age = calculateAge(bDayMember.dateOfBirth)

                                ListItem(
                                    headlineContent = { Text("${t("Birthday", "जन्मदिन")}: ${bDayMember.name}${if (age != null) " (Age $age)" else ""}", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
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
                                val years = calculateAge(anniversaryMember.marriageDate)

                                ListItem(
                                    headlineContent = { Text("${t("Anniversary", "वर्षगांठ")}: ${anniversaryMember.name} & ${anniversaryMember.spouseName}${if (years != null && years > 0) " (Age $years)" else ""}", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
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
                                        Text(if (isBirthday) t("Born on", "जन्म") + " ${formatDateToDDMMM(remMember.dateOfBirth)}" else t("Passed away on", "स्वर्गवास") + " ${formatDateToDDMMM(remMember.bereavementDate)}", color = Color(0xFF5D4037), fontWeight = FontWeight.Medium)
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
                val upcomingEvents = visibleMembers.filter {
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
                                        val age = calculateAge(dateStr)
                                        val ageSuffix = if (age != null && age > 0) " (Age $age)" else ""
                                        Text("$eventType: ${formatDateToDDMMM(dateStr)}$ageSuffix", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
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
                if (user.isAdmin) {
                    val membersLoggedToday = allMembers.filter { isTimestampToday(it.lastLoggedIn) }
                    val loginsToday = membersLoggedToday.size
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color(0xFF2E7D32))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Logins Today: $loginsToday",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (membersLoggedToday.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    membersLoggedToday.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(start = 36.dp)
                                )
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
                    FeatureItem(t("Calendar", "कैलेंडर"), Icons.Default.CalendarMonth, Color(0xFF673AB7), onNavigateToCalendar),
                    FeatureItem(t("Family Games", "फैमिली गेम्स"), Icons.Default.SportsEsports, Color(0xFFFF9800), onNavigateToFamilyGames)
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
                // Today Panchang Tile - Moved to bottom as requested
                if (todayPanchang != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFDAA520)) // Golden border for Panchang
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFDAA520))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    t("Today Panchang", "आज का पंचांग"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF3E2723),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t("Tithi", "तिथि"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63))
                                    Text(todayPanchang.tithi, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t("Muhurat", "मुहूर्त"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63))
                                    Text(todayPanchang.muhurat.split(":").lastOrNull()?.trim() ?: todayPanchang.muhurat,
                                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }

                            if (todayPanchang.festivals.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(t("Festivals", "त्योहार"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                todayPanchang.festivals.forEach { festival ->
                                    Text("• $festival", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                                }
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
        Text(badgeCount.toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}, content = {
    Icon(icon, contentDescription = title, modifier = Modifier.size(32.dp), tint = Color(0xFF5D4037))
})
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

