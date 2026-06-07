package com.purawale.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purawale.app.ActivityLog
import com.purawale.app.Member
import com.purawale.app.t
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    logs: List<ActivityLog>,
    members: List<Member>,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    val activityLogs = remember(logs) {
        logs.filter { it.action != "LOGIN" }
    }
    val elevatedMembers = remember(members) {
        members.filter { it.isAdmin || it.isEditor || it.secondaryTreeEnabled }
            .sortedWith(compareByDescending<Member> { it.isAdmin }.thenByDescending { it.isEditor }.thenBy { it.name })
    }
    val branchEnabledMembers = remember(members) {
        members.filter { it.secondaryTreeEnabled }.sortedBy { it.name }
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.systemDefault())
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = t("Activity Log", "गतिविधि लॉग"),
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
                item {
                    AdminAccessSummary(
                        elevatedMembers = elevatedMembers,
                        branchEnabledMembers = branchEnabledMembers
                    )
                }

                items(activityLogs) { log ->
                    val member = members.find { it.id == log.userId }
                    ActivityLogItem(log, member, dateFormatter)
                }

                if (activityLogs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(t("No activities found", "कोई गतिविधि नहीं मिली"), color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAccessSummary(
    elevatedMembers: List<Member>,
    branchEnabledMembers: List<Member>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = Color(0xFFFFC857), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    t("Access Overview", "एक्सेस अवलोकन"),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AccessCountChip(t("Elevated", "विशेष"), elevatedMembers.size.toString(), Modifier.weight(1f))
                AccessCountChip(t("Branches", "शाखाएं"), branchEnabledMembers.size.toString(), Modifier.weight(1f))
            }
            AccessMemberList(
                title = t("Current Elevated Users", "वर्तमान विशेष उपयोगकर्ता"),
                members = elevatedMembers,
                emptyText = t("No elevated access enabled", "कोई विशेष एक्सेस सक्रिय नहीं")
            ) { member ->
                buildList {
                    if (member.isAdmin) add(t("Admin", "एडमिन"))
                    if (member.isEditor) add(t("Editor", "एडिटर"))
                    if (member.secondaryTreeEnabled) add(t("Branch", "शाखा"))
                }.joinToString(" / ")
            }
            AccessMemberList(
                title = t("Secondary Branches Enabled", "सेकेंडरी शाखाएं सक्रिय"),
                members = branchEnabledMembers,
                emptyText = t("No secondary branches enabled", "कोई सेकेंडरी शाखा सक्रिय नहीं")
            ) { member ->
                t("Branch ID: ${member.id}", "शाखा ID: ${member.id}")
            }
        }
    }
}

@Composable
fun AccessCountChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccessMemberList(
    title: String,
    members: List<Member>,
    emptyText: String,
    roleText: @Composable (Member) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
        if (members.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
        } else {
            members.forEach { member ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.55f))
                    Spacer(Modifier.width(8.dp))
                    Text(member.name, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
                    Text(roleText(member), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: ActivityLog, member: Member?, dateFormatter: DateTimeFormatter) {
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
            if (member?.photoUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = member.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color(0xFFFFC857), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = Color.White.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        log.userName.ifEmpty { "A user" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    ActivityIcon(log.action)
                }

                val targetType = log.targetType?.lowercase()?.replaceFirstChar { it.uppercase() } ?: t("item", "आइटम")
                val targetName = log.targetName?.takeIf { it.isNotBlank() } ?: targetType
                val actionText = when (log.action) {
                    "CREATE" -> when (log.targetType) {
                        "MEMORY" -> t("added a gallery photo: $targetName", "गैलरी फोटो जोड़ी: $targetName")
                        "RECIPE" -> t("added a cookbook recipe: $targetName", "कुकबुक रेसिपी जोड़ी: $targetName")
                        "MILESTONE" -> t("added a milestone: $targetName", "माइलस्टोन जोड़ा: $targetName")
                        else -> t("added $targetType: $targetName", "$targetType जोड़ा: $targetName")
                    }
                    "REACTION" -> t("reacted ${log.details ?: ""} to ${targetType.lowercase()}: $targetName", "${targetType.lowercase()} पर ${log.details ?: ""} प्रतिक्रिया दी: $targetName")
                    "COMMENT" -> t("commented on ${targetType.lowercase()}: $targetName", "${targetType.lowercase()} पर टिप्पणी की: $targetName")
                    "CHANGE_PROPOSED" -> t("proposed a profile change for $targetName", "$targetName के लिए प्रोफ़ाइल परिवर्तन प्रस्तावित किया")
                    "CHANGE_APPLIED" -> t("updated profile: $targetName", "प्रोफ़ाइल अपडेट की: $targetName")
                    "CHANGE_APPROVED" -> t("approved profile change for $targetName", "$targetName का प्रोफ़ाइल परिवर्तन मंजूर किया")
                    "DELETE" -> t("deleted ${targetType.lowercase()}: $targetName", "${targetType.lowercase()} हटाया: $targetName")
                    else -> log.action
                }

                Text(
                    actionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                if (log.action == "COMMENT" && !log.details.isNullOrEmpty()) {
                    Text(
                        "\"${log.details}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFC857).copy(alpha = 0.9f),
                        fontStyle = FontStyle.Italic
                    )
                }

                Text(
                    dateFormatter.format(Instant.ofEpochMilli(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun ActivityIcon(action: String) {
    val icon: ImageVector = when (action) {
        "CREATE" -> Icons.Default.AddCircle
        "REACTION" -> Icons.Default.Favorite
        "COMMENT" -> Icons.Default.Comment
        "CHANGE_PROPOSED" -> Icons.Default.Edit
        "CHANGE_APPLIED" -> Icons.Default.Edit
        "CHANGE_APPROVED" -> Icons.Default.CheckCircle
        "DELETE" -> Icons.Default.Delete
        else -> Icons.Default.Info
    }
    Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFC857))
}
