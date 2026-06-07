package com.purawale.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purawale.app.Member
import com.purawale.app.t
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginLogScreen(
    members: List<Member>,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val sortedMembers = remember(members) {
        members.filter { it.lastLoggedIn != null }
            .sortedByDescending { it.lastLoggedIn }
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.systemDefault())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = t("Login Log", "लॉगिन लॉग"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onHome) { Icon(Icons.Default.Home, "Home") }
                }
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedMembers) { member ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!member.photoUrl.isNullOrEmpty() && member.photoUrl.startsWith("http")) {
                                AsyncImage(
                                    model = member.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(CircleShape).border(1.5.dp, Color(0xFFFFC857), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(50.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(member.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                member.lastLoggedIn?.let { timestamp ->
                                    val date = dateFormatter.format(Instant.ofEpochMilli(timestamp))
                                    Text(
                                        t("Last Login: $date", "पिछला लॉगिन: $date"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFC857)
                                    )
                                }
                                Text(member.phoneNumber, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                
                if (sortedMembers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(t("No login logs found", "कोई लॉगिन लॉग नहीं मिला"), color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
