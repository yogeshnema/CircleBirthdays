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
import com.purawale.app.Member
import com.purawale.app.R
import com.purawale.app.RelationshipOverride
import com.purawale.app.calculateAge
import com.purawale.app.formatDateToDDMMM
import com.purawale.app.safeOpenUri
import com.purawale.app.t
import com.purawale.app.ui.theme.LightGolden
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    members: List<Member>,
    pendingMembers: List<Member>,
    currentUser: Member,
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
    onResetPassword: (Member) -> Unit = {},
    onRemovePhoto: (Member) -> Unit = {}
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
                            // Note: In a real app, this would trigger a file picker and then call onImportCsv(uri)
                            IconButton(onClick = { /* Trigger file picker here */ }) { Icon(Icons.Default.Upload, "", tint = brown900) }
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
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
                                    if (currentUser.isAdmin && !showPending && !showOverrides) {
                                        IconButton(onClick = { onResetPassword(member) }) {
                                            Icon(Icons.Default.LockReset, "Reset Password", tint = brown800)
                                        }
                                        if (!member.photoUrl.isNullOrEmpty()) {
                                            IconButton(onClick = { onRemovePhoto(member) }) {
                                                Icon(Icons.Default.NoPhotography, "Remove Photo", tint = Color(0xFFC62828))
                                            }
                                        }
                                    }
                                    val canEdit = canEditAll || member.id == currentUser.id
                                    IconButton(onClick = { onView(member) }) { 
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View",
                                            tint = brown800
                                        )
                                    }
                                    if (canEdit) {
                                        IconButton(onClick = { onEdit(member) }) { 
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = brown800
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = brown800)
                                Spacer(Modifier.width(6.dp))
                                Text(member.phoneNumber, style = MaterialTheme.typography.bodySmall, color = brown800, fontWeight = FontWeight.Bold)
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Icon(Icons.Default.Cake, null, modifier = Modifier.size(14.dp), tint = brown800)
                                Spacer(Modifier.width(6.dp))
                                val age = calculateAge(member.dateOfBirth)
                                val ageSuffix = if (age != null) " (Age $age)" else ""
                                Text(formatDateToDDMMM(member.dateOfBirth) + ageSuffix, style = MaterialTheme.typography.bodySmall, color = brown800, fontWeight = FontWeight.Bold)
                            }

                            if (!member.marriageDate.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Celebration, null, modifier = Modifier.size(14.dp), tint = brown800)
                                    Spacer(Modifier.width(6.dp))
                                    val years = calculateAge(member.marriageDate)
                                    val yearsSuffix = if (years != null && years > 0) " (Age $years)" else ""
                                    Text(formatDateToDDMMM(member.marriageDate) + yearsSuffix, style = MaterialTheme.typography.bodySmall, color = brown800, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (currentUser.isAdmin && member.lastLoggedIn != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = brown800)
                                    Spacer(Modifier.width(6.dp))
                                    val lastLoginStr = Instant.ofEpochMilli(member.lastLoggedIn)
                                        .atZone(ZoneId.systemDefault())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))
                                    Text("Last Login: $lastLoginStr", style = MaterialTheme.typography.bodySmall, color = brown800)
                                }
                            }

                            if (!member.location.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = brown800)
                                    Spacer(Modifier.width(6.dp))
                                    Text(member.location, style = MaterialTheme.typography.bodySmall, color = brown800)
                                }
                            }

                            val hasSocial = member.phoneNumber.isNotBlank() || !member.facebookUrl.isNullOrEmpty() || !member.instagramUrl.isNullOrEmpty() || !member.youtubeUrl.isNullOrEmpty()
                            val hasChat = member.id != currentUser.id

                            if (hasSocial || hasChat) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    val context = LocalContext.current
                                    if (member.phoneNumber.isNotBlank()) {
                                        IconButton(
                                            onClick = { safeOpenUri(context, "https://wa.me/${member.phoneNumber.filter { it.isDigit() }}") },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_whatsapp), null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (!member.facebookUrl.isNullOrEmpty()) {
                                        IconButton(
                                            onClick = { safeOpenUri(context, member.facebookUrl) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_facebook), null, tint = Color(0xFF1565C0), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (!member.instagramUrl.isNullOrEmpty()) {
                                        IconButton(
                                            onClick = { safeOpenUri(context, member.instagramUrl) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_instagram), null, tint = Color(0xFFAD1457), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (!member.youtubeUrl.isNullOrEmpty()) {
                                        IconButton(
                                            onClick = { safeOpenUri(context, member.youtubeUrl) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_youtube), null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    if (hasChat) {
                                        Spacer(Modifier.weight(1f))
                                        Button(
                                            onClick = { onChat(member) },
                                            colors = ButtonDefaults.buttonColors(containerColor = brown800),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(t("Chat", "चैट"), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            if (currentUser.isAdmin && member.phoneNumber.isBlank()) {
                                val context = LocalContext.current
                                Button(
                                    onClick = { 
                                        val inviteMsg = """
                                            🌟 *Purawale - Hum aur Humare* 🌟
                                            
                                            Hello ${member.name}! You're invited to join our exclusive community app.
                                            
                                            📲 *Android App:* https://play.google.com/store/apps/details?id=com.purawale.app
                                            🌐 *Web Access:* https://circlebirthdays.web.app/
                                            
                                            Please reply with your *Email ID* or *Phone Number* so we can create your login and give you access!
                                            
                                            Let's stay connected! ❤️
                                        """.trimIndent()
                                        
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, inviteMsg)
                                            setPackage("com.whatsapp")
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, inviteMsg)
                                            }
                                            context.startActivity(Intent.createChooser(genericIntent, "Invite via"))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(painterResource(R.drawable.ic_whatsapp), null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(t("Invite via WhatsApp", "व्हाट्सएप से आमंत्रित करें"), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
