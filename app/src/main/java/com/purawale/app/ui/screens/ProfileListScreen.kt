package com.purawale.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purawale.app.LocalLanguage
import com.purawale.app.Member
import com.purawale.app.R
import com.purawale.app.RelationshipOverride
import com.purawale.app.calculateAge
import com.purawale.app.formatDateToDDMMM
import com.purawale.app.safeOpenUri
import com.purawale.app.t
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer
import com.purawale.app.ui.theme.LightGolden
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    members: List<Member>,
    pendingMembers: List<Member>,
    currentUser: Member,
    currentTreeId: String = "primary",
    onView: (Member) -> Unit,
    onEdit: (Member) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onImportCsv: (android.net.Uri) -> Unit,
    onApprove: (Member) -> Unit,
    onClearAll: () -> Unit,
    onChat: (Member) -> Unit,
    overrides: List<RelationshipOverride> = emptyList(),
    onApproveOverride: (RelationshipOverride) -> Unit = {},
    onRejectOverride: (String) -> Unit = {},
    onResetPassword: (Member) -> Unit = {},
    onRemovePhoto: (Member) -> Unit = {},
    onRejectPending: (Member) -> Unit = {}
) {
    val isBranchModerator = currentTreeId != "primary" && currentTreeId == currentUser.id
    val canEditAll = currentUser.isAdmin || currentUser.isEditor || isBranchModerator
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
                AppTopBar(
                    title = when {
                        showPending -> t("Pending Approvals", "लंबित अनुमोदन")
                        showOverrides -> t("Relationship Requests", "रिश्ते के अनुरोध")
                        else -> t("Profiles", "प्रोफाइल")
                    },
                    subtitle = if (canEditAll) t("Admin Mode", "एडमिन मोड") else null,
                    onBack = onBack,
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
                            IconButton(onClick = { /* Trigger file picker */ }) { Icon(Icons.Default.Upload, "") }
                            var showClearConfirm by remember { mutableStateOf(false) }
                            IconButton(onClick = { showClearConfirm = true }) { Icon(Icons.Default.DeleteForever, "", tint = Color(0xFFFF5252)) }
                            if (showClearConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showClearConfirm = false },
                                    shape = RoundedCornerShape(28.dp),
                                    containerColor = Color(0xFF1A1C1E),
                                    title = { Text(t("Clear All Data", "सारा डेटा मिटाएं"), color = Color.White, fontWeight = FontWeight.Bold) },
                                    text = { Text(t("Are you sure you want to delete ALL members and pending updates? This cannot be undone.", "क्या आप वाकई सभी सदस्यों और लंबित अपडेट को हटाना चाहते हैं? इसे वापस नहीं लिया जा सकता।"), color = Color.White.copy(alpha = 0.7f)) },
                                    confirmButton = {
                                        Button(
                                            onClick = { 
                                                onClearAll()
                                                showClearConfirm = false 
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                                            shape = RoundedCornerShape(24.dp)
                                        ) { Text(t("Confirm", "पुष्टि करें"), fontWeight = FontWeight.Bold) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showClearConfirm = false }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) }
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
                        placeholder = { Text(t("Search by name or phone...", "नाम या फोन से खोजें..."), color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC857),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFFFFC857),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        floatingActionButton = { 
            if (canEditAll && !showPending && !showOverrides) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = Color(0xFFFFC857),
                    contentColor = Color(0xFF080B14),
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Add, "") }
            }
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) { innerPadding ->
            if (showOverrides && currentUser.isAdmin) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(overrides) { override ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            ListItem(
                                headlineContent = { Text("${override.observerName} ➔ ${override.targetName}", color = Color.White, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(t("Requested Label: ${override.relationship}", "अनुरोधित लेबल: ${override.relationship}"), color = Color.White.copy(alpha = 0.7f)) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { onApproveOverride(override) }) {
                                            Icon(Icons.Default.Check, "Approve", tint = Color(0xFF4CAF50))
                                        }
                                        IconButton(onClick = { onRejectOverride(override.id) }) {
                                            Icon(Icons.Default.Close, "Reject", tint = Color(0xFFFF5252))
                                        }
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMembers) { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!member.photoUrl.isNullOrEmpty() && member.photoUrl.startsWith("http")) {
                                        AsyncImage(
                                            model = member.photoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(60.dp).clip(CircleShape).border(1.5.dp, Color(0xFFFFC857), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(60.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.5f))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        if (!member.relationship.isNullOrEmpty()) {
                                            Text(member.relationship, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
                                        }
                                        if (showPending) {
                                            Text(t("(Pending Approval)", "(अनुमोदन लंबित)"), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF5252))
                                        }
                                    }
                                    Row {
                                        if (showPending && currentUser.isAdmin) {
                                            IconButton(onClick = { onApprove(member) }) {
                                                Icon(Icons.Default.Check, "Approve", tint = Color(0xFF4CAF50))
                                            }
                                            IconButton(onClick = { onRejectPending(member) }) {
                                                Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFFF5252))
                                            }
                                        }
                                        if (currentUser.isAdmin && !showPending && !showOverrides) {
                                            IconButton(onClick = { onResetPassword(member) }) {
                                                Icon(Icons.Default.LockReset, "Reset Password", tint = Color.White.copy(alpha = 0.7f))
                                            }
                                            if (!member.photoUrl.isNullOrEmpty()) {
                                                IconButton(onClick = { onRemovePhoto(member) }) {
                                                    Icon(Icons.Default.NoPhotography, "Remove Photo", tint = Color(0xFFFF5252))
                                                }
                                            }
                                        }
                                        val canEdit = canEditAll || member.id == currentUser.id
                                        IconButton(onClick = { onView(member) }) { 
                                            Icon(Icons.Default.Visibility, "View", tint = Color.White.copy(alpha = 0.7f))
                                        }
                                        if (canEdit) {
                                            IconButton(onClick = { onEdit(member) }) { 
                                                Icon(Icons.Default.Edit, "Edit", tint = Color.White.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC857))
                                    Spacer(Modifier.width(8.dp))
                                    Text(member.phoneNumber, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.Cake, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC857))
                                    Spacer(Modifier.width(8.dp))
                                    val age = calculateAge(member.dateOfBirth)
                                    val ageSuffix = if (age != null) " (Age $age)" else ""
                                    Text(formatDateToDDMMM(member.dateOfBirth) + ageSuffix, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                }

                                if (!member.marriageDate.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Default.Celebration, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC857))
                                        Spacer(Modifier.width(8.dp))
                                        val years = calculateAge(member.marriageDate)
                                        val yearsSuffix = if (years != null && years > 0) " (Age $years)" else ""
                                        Text(formatDateToDDMMM(member.marriageDate) + yearsSuffix, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                                
                                if (!member.location.isNullOrEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC857))
                                        Spacer(Modifier.width(8.dp))
                                        Text(member.location, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                val hasSocial = member.phoneNumber.isNotBlank() || !member.facebookUrl.isNullOrEmpty() || !member.instagramUrl.isNullOrEmpty() || !member.youtubeUrl.isNullOrEmpty()
                                val hasChat = member.id != currentUser.id

                                if (hasSocial || hasChat) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        val context = LocalContext.current
                                        if (member.phoneNumber.isNotBlank()) {
                                            IconButton(
                                                onClick = { 
                                                    val phone = member.phoneNumber.filter { it.isDigit() }
                                                    val finalPhone = if (phone.length == 10) "91$phone" else phone
                                                    safeOpenUri(context, "https://wa.me/$finalPhone") 
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(painterResource(R.drawable.ic_whatsapp), null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        if (!member.facebookUrl.isNullOrEmpty()) {
                                            IconButton(onClick = { safeOpenUri(context, member.facebookUrl) }, modifier = Modifier.size(32.dp)) {
                                                Icon(painterResource(R.drawable.ic_facebook), null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        if (!member.instagramUrl.isNullOrEmpty()) {
                                            IconButton(onClick = { safeOpenUri(context, member.instagramUrl) }, modifier = Modifier.size(32.dp)) {
                                                Icon(painterResource(R.drawable.ic_instagram), null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        if (!member.youtubeUrl.isNullOrEmpty()) {
                                            IconButton(onClick = { safeOpenUri(context, member.youtubeUrl) }, modifier = Modifier.size(32.dp)) {
                                                Icon(painterResource(R.drawable.ic_youtube), null, tint = Color(0xFFFF0000), modifier = Modifier.size(20.dp))
                                            }
                                        }

                                        if (hasChat) {
                                            Spacer(Modifier.weight(1f))
                                            Button(
                                                onClick = { onChat(member) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFC857))
                                                Spacer(Modifier.width(8.dp))
                                                Text(t("Chat", "चैट"), style = MaterialTheme.typography.labelLarge, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                if (currentUser.isAdmin && member.id != currentUser.id) {
                                    val context = LocalContext.current
                                    val isHindi = LocalLanguage.current
                                    Button(
                                        onClick = { 
                                            val inviteMsg = if (isHindi) {
                                                "🌟 *पुरावले - हम और हमारे* 🌟\n\nनमस्ते ${member.name}! आपको हमारे विशेष समुदाय ऐप में शामिल होने के लिए आमंत्रित किया गया है।\n\n📲 *Android App:* https://play.google.com/store/apps/details?id=com.purawale.app\n🌐 *Web Access:* https://circlebirthdays.web.app/\n\nकृपया अपनी *Email ID* या *Phone Number* के साथ उत्तर दें ताकि हम आपका लॉगिन बना सकें और आपको एक्सेस दे सकें!\n\nजुड़े रहें! ❤️"
                                            } else {
                                                "🌟 *Purawale - Hum aur Humare* 🌟\n\nHello ${member.name}! You're invited to join our exclusive community app.\n\n📲 *Android App:* https://play.google.com/store/apps/details?id=com.purawale.app\n🌐 *Web Access:* https://circlebirthdays.web.app/\n\nPlease reply with your *Email ID* or *Phone Number* so we can create your login and give you access!\n\nLet's stay connected! ❤️"
                                            }
                                            val encodedMsg = android.net.Uri.encode(inviteMsg)
                                            val phone = member.phoneNumber.filter { it.isDigit() }
                                            val finalPhone = if (phone.length == 10) "91$phone" else phone
                                            val url = if (finalPhone.isNotBlank()) {
                                                "https://wa.me/$finalPhone?text=$encodedMsg"
                                            } else {
                                                "https://wa.me/?text=$encodedMsg"
                                            }
                                            safeOpenUri(context, url)
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f))
                                    ) {
                                        Icon(painterResource(R.drawable.ic_whatsapp), null, modifier = Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                                        Spacer(Modifier.width(12.dp))
                                        Text(t("Invite via WhatsApp", "व्हाट्सएप से आमंत्रित करें"), color = Color.White)
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
