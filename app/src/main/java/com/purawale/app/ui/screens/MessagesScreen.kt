package com.purawale.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.purawale.app.ChatChannel
import com.purawale.app.Member
import com.purawale.app.R
import com.purawale.app.t
import com.purawale.app.ui.components.EventAvatar
import com.purawale.app.ui.theme.LightGolden
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    user: Member,
    channels: List<ChatChannel>,
    allMembers: List<Member>,
    onBack: () -> Unit,
    onNavigateToChat: (Member) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Combine existing channels with a way to start new chats
    val activeChannelsMembers = channels.mapNotNull { channel ->
        val otherUserId = channel.userIds.find { it != user.id }
        allMembers.find { it.id == otherUserId }?.let { it to channel }
    }

    val filteredMembers = if (searchQuery.isNotBlank()) {
        allMembers.filter { 
            it.id != user.id && 
            it.name.contains(searchQuery, ignoreCase = true) &&
            (!it.isAdmin || user.isAdmin || it.id == user.id)
        }
    } else {
        emptyList()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Messages", "संदेश"), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF080B14).copy(alpha = 0.90f)
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.75f), BlendMode.Darken)
            )

            Column(modifier = Modifier.padding(padding)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text(t("Search people...", "लोग खोजें..."), color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFC857),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFFFC857)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (searchQuery.isNotBlank()) {
                        item {
                            Text(
                                t("Search Results", "खोज के परिणाम"),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFC857),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(filteredMembers) { member ->
                            ListItem(
                                headlineContent = { Text(member.name, color = Color.White, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(member.relationship ?: "", color = Color.White.copy(alpha = 0.7f)) },
                                leadingContent = { EventAvatar(member.photoUrl, member.name) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onNavigateToChat(member) }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f)) }
                    }

                    item {
                        Text(
                            t("Recent Chats", "हाल की बातचीत"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFC857),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (activeChannelsMembers.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(t("No active chats", "कोई सक्रिय बातचीत नहीं"), color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        items(activeChannelsMembers) { (otherMember, channel) ->
                            val unreadCount = channel.unreadCount[user.id] ?: 0
                            ListItem(
                                headlineContent = {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text(otherMember.name, fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold, color = Color.White)
                                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(channel.lastTimestamp))
                                        Text(time, style = MaterialTheme.typography.labelSmall, color = if (unreadCount > 0) Color(0xFFFFC857) else Color.White.copy(alpha = 0.5f))
                                    }
                                },
                                supportingContent = {
                                    Text(
                                        channel.lastMessage, 
                                        maxLines = 1, 
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (unreadCount > 0) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                },
                                leadingContent = { EventAvatar(otherMember.photoUrl, otherMember.name) },
                                trailingContent = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = Color(0xFFFFC857), contentColor = Color.Black) {
                                            Text(unreadCount.toString())
                                        }
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onNavigateToChat(otherMember) }
                            )
                        }
                    }
                }
            }
        }
    }
}
