package com.purawale.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.purawale.app.*
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer
import com.purawale.app.ui.components.SpeechToTextButton
import com.purawale.app.ui.theme.LightGolden
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionsScreen(
    user: Member,
    discussions: List<Discussion>,
    allMembers: List<Member>,
    onBack: () -> Unit,
    onPost: (Discussion, Uri?) -> Unit,
    onApprove: (String) -> Unit,
    onDelete: (String) -> Unit,
    onVote: (String, String, String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onImageClick: (Memory) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDiscussionForDetail by remember { mutableStateOf<Discussion?>(null) }
    val gold = Color(0xFFFFC857)
    val cardBackground = Color.White.copy(alpha = 0.08f)

    val visibleDiscussions = discussions

    if (showCreateDialog) {
        var type by remember { mutableStateOf("TEXT") } // TEXT, IMAGE, POLL
        var title by remember { mutableStateOf("") }
        var contentText by remember { mutableStateOf("") }
        var pollOptions by remember { mutableStateOf(listOf("", "")) }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
        var selectedTaggedIds by remember { mutableStateOf<List<String>>(emptyList()) }
        val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) selectedUri = result.uriContent
        }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                cropLauncher.launch(CropImageContractOptions(it, CropImageOptions(guidelines = CropImageView.Guidelines.ON)))
            }
        }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
            },
            title = { Text(t("Create Discussion", "चर्चा शुरू करें"), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row {
                        FilterChip(
                            selected = type == "TEXT",
                            onClick = { type = "TEXT" },
                            label = { Text(t("Text", "टेक्स्ट")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = gold,
                                selectedLabelColor = Color(0xFF080B14),
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = type == "TEXT",
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = gold,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = type == "IMAGE",
                            onClick = { type = "IMAGE" },
                            label = { Text(t("Image", "चित्र")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = gold,
                                selectedLabelColor = Color(0xFF080B14),
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = type == "IMAGE",
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = gold,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = type == "POLL",
                            onClick = { type = "POLL" },
                            label = { Text(t("Poll", "पोल")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = gold,
                                selectedLabelColor = Color(0xFF080B14),
                                labelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = type == "POLL",
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = gold,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            )
                        )
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(t("Title", "शीर्षक")) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gold,
                            focusedLabelColor = gold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = gold
                        )
                    )
                    if (type == "TEXT" || type == "POLL") {
                        OutlinedTextField(
                            value = contentText,
                            onValueChange = { contentText = it },
                            label = { Text(if (type == "POLL") t("Poll Question", "पोल प्रश्न") else t("Content", "विषय")) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { SpeechToTextButton(onResult = { contentText += it }) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = gold,
                                focusedLabelColor = gold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = gold
                            )
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
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color(0xFF080B14))
                        ) {
                            Text(if (selectedUri == null) t("Select Image", "चित्र चुनें") else t("Change Image", "चित्र बदलें"))
                        }
                    }
                    if (type == "POLL") {
                        Text(t("Poll Options", "पोल विकल्प"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp), color = Color.White)
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = gold,
                                    focusedLabelColor = gold,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = gold
                                )
                            )
                        }
                        TextButton(onClick = { pollOptions = pollOptions + "" }) { Text(t("Add Option", "विकल्प जोड़ें"), color = gold) }
                    }
                }
            },
            confirmButton = {
                val isValid = title.isNotBlank() && when (type) {
                    "IMAGE" -> selectedUri != null
                    "POLL" -> pollOptions.filter { it.isNotBlank() }.size >= 2
                    else -> contentText.isNotBlank()
                }
                Button(
                    onClick = {
                        val disc = Discussion(
                            id = UUID.randomUUID().toString(),
                            userId = user.id,
                            userName = if (user.isAdmin || user.phoneNumber == AppConfig.ADMIN_PHONE) AppConfig.ADMIN_NAME else user.name,
                            type = type,
                            title = title,
                            content = contentText,
                            pollOptions = if (type == "POLL") pollOptions.filter { it.isNotBlank() }.map { PollOption(UUID.randomUUID().toString(), it) } else null
                        )
                        onPost(disc, selectedUri)
                        showCreateDialog = false
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color.Black,
                        disabledContainerColor = gold.copy(alpha = 0.3f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("Post", "पोस्ट करें"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                }) { Text(t("Cancel", "रद्द करें"), color = gold.copy(alpha = 0.7f)) }
            },
            containerColor = Color(0xFF1A1C1E),
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (selectedDiscussionForDetail != null) {
        val disc = selectedDiscussionForDetail!!
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                selectedDiscussionForDetail = null
            },
            title = { Text(disc.title, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(t("By ${disc.userName}", "${disc.userName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = gold)
                    Spacer(Modifier.height(8.dp))
                    if (disc.type == "IMAGE") {
                        AsyncImage(
                            model = disc.content,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onImageClick(Memory(
                                        id = disc.id,
                                        userId = disc.userId,
                                        userName = disc.userName,
                                        imageUrl = disc.content,
                                        caption = disc.title,
                                        timestamp = disc.timestamp
                                    ))
                                    selectedDiscussionForDetail = null
                                },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(disc.content, color = Color.White)
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
                                            Icon(Icons.Default.CheckCircle, null, tint = gold, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                        }
                                        Text(opt.text, fontWeight = if (isVoted) FontWeight.Bold else FontWeight.Normal, color = Color.White)
                                    }
                                    Text(t("${opt.voterIds.size} votes", "${opt.voterIds.size} वोट"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                }
                                LinearProgressIndicator(progress = { percentage }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = gold, trackColor = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))
                    Text(t("Comments", "टिप्पणियाँ"), style = MaterialTheme.typography.titleSmall, color = Color.White)
                    val visibleComments = disc.comments
                    for (comment in visibleComments) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(if (comment.userName == "Admin") "Admin" else comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = gold)
                            Text(comment.text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(t("Add comment...", "अपनी टिप्पणी लिखें..."), color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = { if (commentText.isNotBlank()) { onAddComment(disc.id, commentText); commentText = "" } }) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = gold) }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gold,
                            focusedLabelColor = gold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = gold
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDiscussionForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("Close", "बंद करें"), fontWeight = FontWeight.Bold) }
            },
            containerColor = Color(0xFF1A1C1E),
            shape = RoundedCornerShape(28.dp)
        )
    }

    ScreenContainer {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AppTopBar(
                    title = t("Discussions", "चर्चाएँ"),
                    onBack = onBack
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = gold,
                    contentColor = Color(0xFF080B14),
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Add, "Create Post") }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
            ) {
                items(visibleDiscussions) { disc ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedDiscussionForDetail = disc },
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(disc.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                                if (disc.status == "PENDING") {
                                    Surface(color = Color(0xFFFFEBEE).copy(alpha = 0.2f), shape = CircleShape) {
                                        Text(t("PENDING", "लंबित"), color = Color(0xFFFFCDD2), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            Text(t("By ${if (disc.userName == "Admin") "Admin" else disc.userName} • ${disc.type}", "${if (disc.userName == "Admin") "Admin" else disc.userName} द्वारा • ${disc.type}"), style = MaterialTheme.typography.labelSmall, color = gold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (disc.type == "IMAGE") t("[Image Post]", "[चित्र पोस्ट]") else if (disc.content.length > 100) disc.content.take(100) + "..." else disc.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(t("${disc.comments.size} comments", "${disc.comments.size} टिप्पणियाँ"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                if (user.isAdmin) {
                                    Row {
                                        if (disc.status == "PENDING") {
                                            IconButton(onClick = { onApprove(disc.id) }) { Icon(Icons.Default.Check, "Approve", tint = Color(0xFF81C784)) }
                                        }
                                        IconButton(onClick = { onDelete(disc.id) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFE57373)) }
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
