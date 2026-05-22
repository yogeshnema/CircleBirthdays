package com.purawale.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.purawale.app.Member
import com.purawale.app.ChatChannel
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
                title = { Text("Messages", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF3E2723))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search people to chat...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            "Search Results",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredMembers) { member ->
                        ListItem(
                            headlineContent = { Text(member.name, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(member.relationship ?: "", color = Color(0xFF5D4037)) },
                            leadingContent = { EventAvatar(member.photoUrl, member.name) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onNavigateToChat(member) }
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                item {
                    Text(
                        "Recent Chats",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (activeChannelsMembers.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No active chats", color = Color.Gray)
                        }
                    }
                } else {
                    items(activeChannelsMembers) { (otherMember, channel) ->
                        val unreadCount = channel.unreadCount[user.id] ?: 0
                        ListItem(
                            headlineContent = {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(otherMember.name, fontWeight = if (unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold, color = Color(0xFF3E2723))
                                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(channel.lastTimestamp))
                                    Text(time, style = MaterialTheme.typography.labelSmall, color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else Color(0xFF5D4037))
                                }
                            },
                            supportingContent = {
                                Text(
                                    channel.lastMessage, 
                                    maxLines = 1, 
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (unreadCount > 0) Color.Black else Color(0xFF5D4037)
                                )
                            },
                            leadingContent = { EventAvatar(otherMember.photoUrl, otherMember.name) },
                            trailingContent = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(unreadCount.toString(), color = Color.White)
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
