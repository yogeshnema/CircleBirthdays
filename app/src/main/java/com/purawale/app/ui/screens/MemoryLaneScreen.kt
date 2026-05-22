package com.purawale.app.ui.screens

import com.purawale.app.ui.components.AddressPickerModal
import com.purawale.app.ui.components.SpeechToTextButton
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.purawale.app.*
import com.purawale.app.ui.theme.LightGolden
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryLaneScreen(
    user: Member,
    milestones: List<Milestone>,
    allMembers: List<Member>,
    onBack: () -> Unit,
    onAddMilestone: (Milestone, Uri?, Uri?, List<String>) -> Unit,
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
                // Visible to the couple (baseId) and their children (starts with baseId and length + 1)
                userBaseId == m.familyContextId || (userBaseId.startsWith(m.familyContextId) && userBaseId.length == m.familyContextId.length + 1)
            }
            "OLD_IS_GOLD" -> {
                // Visible to the branch owner (parent), siblings and all their descendants
                if (m.familyContextId.isEmpty()) false
                else userBaseId.startsWith(m.familyContextId)
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
            onDismissRequest = { 
                showDeleteRequestDialog = null 
            },
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
            dismissButton = { 
                TextButton(onClick = { 
                    showDeleteRequestDialog = null 
                }) { Text(t("Cancel", "रद्द करें"), color = brown800) } 
            },
            containerColor = Color.White
        )
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var showLocationPicker by remember { mutableStateOf(false) }
        var visibilityType by remember { mutableStateOf(if (selectedTab == 1) "PRIVATE_FAMILY" else if (selectedTab == 2) "OLD_IS_GOLD" else "GLOBAL") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
        var selectedTaggedIds by remember { mutableStateOf<List<String>>(emptyList()) }
        val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) selectedUri = result.uriContent
        }
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { cropLauncher.launch(CropImageContractOptions(it, CropImageOptions(guidelines = CropImageView.Guidelines.ON))) }
        }
        val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedAudioUri = it }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false 
            },
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
                        SpeechToTextButton(onResult = { title += it })
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
                        IconButton(onClick = { showLocationPicker = true }) {
                            Icon(Icons.Default.LocationOn, "Location", tint = brown800)
                        }
                    }

                    if (showLocationPicker) {
                        AddressPickerModal(
                            initialAddress = location,
                            onAddressSelected = { address, _, _ ->
                                location = address
                                showLocationPicker = false
                            },
                            onDismiss = { showLocationPicker = false }
                        )
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
                        SpeechToTextButton(onResult = { description += it })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(t("Tag Members", "सदस्यों को टैग करें"), style = MaterialTheme.typography.labelMedium, color = brown800)
                    var tagSearch by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = tagSearch,
                        onValueChange = { tagSearch = it },
                        placeholder = { Text(t("Search and add tags...", "खोजें और टैग जोड़ें...")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = brown800)
                    )
                    
                    if (tagSearch.isNotEmpty()) {
                        val searchResults = allMembers.filter { 
                            it.name.contains(tagSearch, ignoreCase = true) && 
                            !selectedTaggedIds.contains(it.id) &&
                            (!it.isAdmin || user.isAdmin || it.id == user.id)
                        }.take(5)
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            searchResults.forEach { member ->
                                TextButton(
                                    onClick = {
                                        selectedTaggedIds = selectedTaggedIds + member.id
                                        tagSearch = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(member.name, color = brown900, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }

                    if (selectedTaggedIds.isNotEmpty()) {
                        FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            selectedTaggedIds.forEach { id ->
                                val member = allMembers.find { it.id == id }
                                InputChip(
                                    selected = true,
                                    onClick = { selectedTaggedIds = selectedTaggedIds - id },
                                    label = { Text(member?.name ?: "Unknown", style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") }, 
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = brown800)
                        ) {
                            Text(if (selectedUri == null) t("Select Photo", "फोटो चुनें") else t("Photo Selected", "फोटो चुनी गई"))
                        }
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") }, 
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = brown800)
                        ) {
                            Text(if (selectedAudioUri == null) t("Select Audio", "ऑडियो चुनें") else t("Audio Selected", "ऑडियो चुना गया"))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(t("Tag Members", "सदस्यों को टैग करें"), style = MaterialTheme.typography.labelMedium, color = brown800)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        allMembers.filter { 
                            it.id != user.id &&
                            (!it.isAdmin || user.isAdmin || it.id == user.id)
                        }.forEach { member ->
                            val isSelected = selectedTaggedIds.contains(member.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTaggedIds = if (isSelected) selectedTaggedIds - member.id else selectedTaggedIds + member.id
                                },
                                label = { Text(member.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
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
                            familyContextId = contextId,
                            taggedMemberIds = selectedTaggedIds
                        ), selectedUri, selectedAudioUri, selectedTaggedIds)
                        showAddDialog = false
                    },
                    enabled = isValid
                ) { Text(t("Save", "सहेजें"), color = if (isValid) brown800 else Color.Gray) }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showAddDialog = false 
                }) { Text(t("Cancel", "रद्द करें"), color = brown800) } 
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(t("Memory Lane", "यादों की गली"), fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            if (filteredMilestones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(t("No memories found here.", "यहाँ कोई यादें नहीं मिलीं।"), color = brown800.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(filteredMilestones) { milestone ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                                Text(milestone.year, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = brown900)
                                Box(modifier = Modifier.width(2.dp).weight(1f).background(brown800.copy(alpha = 0.2f)))
                            }
                            Spacer(Modifier.width(16.dp))
                            Card(
                                modifier = Modifier.weight(1f)
                                    .border(1.dp, brown800, RoundedCornerShape(28.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(28.dp),
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

                                    if (milestone.audioUrl.isNotBlank()) {
                                        AudioPlayerWidget(milestone.audioUrl)
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
                                    
                                    if (milestone.taggedMemberIds.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(t("Tagged:", "टैग किया गया:"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = brown800)
                                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            milestone.taggedMemberIds.forEach { id ->
                                                val name = allMembers.find { it.id == id }?.name ?: "Unknown"
                                                AssistChip(
                                                    onClick = { },
                                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                                    colors = AssistChipDefaults.assistChipColors(labelColor = brown900)
                                                )
                                            }
                                        }
                                    }

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
                                    val visibleComments = milestone.comments
                                    if (visibleComments.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        visibleComments.takeLast(3).forEach { comment ->
                                            val displayName = if (comment.userName == "Admin") "Admin" else comment.userName
                                            Text(
                                                text = buildAnnotatedString {
                                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = brown900)) { append("$displayName: ") }
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
}

@Composable
fun AudioPlayerWidget(url: String) {
    val context = LocalContext.current
    val mediaPlayer = remember { android.media.MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }

    DisposableEffect(url) {
        try {
            mediaPlayer.apply {
                reset()
                setDataSource(context, Uri.parse(url))
                setOnPreparedListener {
                    isPrepared = true
                }
                setOnCompletionListener {
                    isPlaying = false
                }
                setOnErrorListener { _, _, _ ->
                    isPrepared = false
                    isPlaying = false
                    false
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error preparing media player", e)
        }

        onDispose {
            mediaPlayer.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFEBE9))
            .padding(8.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else if (isPrepared) {
                    mediaPlayer.start()
                    isPlaying = true
                }
            },
            enabled = isPrepared
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFF5D4037)
            )
        }
        Text(
            text = if (isPlaying) t("Playing voice memory...", "वॉइस मेमोरी चल रही है...") else t("Voice Memory", "वॉइस मेमोरी"),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF3E2723),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
