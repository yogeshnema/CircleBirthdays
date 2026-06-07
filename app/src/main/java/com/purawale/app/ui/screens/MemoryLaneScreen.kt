package com.purawale.app.ui.screens

import com.purawale.app.ui.components.AddressPickerModal
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer
import com.purawale.app.ui.components.SpeechToTextButton
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.purawale.app.*
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
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

    val userBaseId = if (user.familyId.endsWith("0")) user.familyId.dropLast(1) else user.familyId

    val filteredMilestones = milestones.filter { m ->
        when (m.visibilityType) {
            "GLOBAL" -> true
            "PRIVATE_FAMILY" -> {
                if (m.familyContextId.isEmpty()) false
                else userBaseId.startsWith(m.familyContextId) || m.familyContextId.startsWith(userBaseId)
            }
            "OLD_IS_GOLD" -> {
                if (m.familyContextId.isEmpty()) false
                else userBaseId.startsWith(m.familyContextId) || m.familyContextId.startsWith(userBaseId)
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

    if (fullScreenImageUri != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullScreenImageUri = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenImageUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullScreenImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenImageUri = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }
        }
    }

    if (showDeleteRequestDialog != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { 
                showDeleteRequestDialog = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF1A1C1E),
            title = { Text(t("Request Deletion", "हटाने का अनुरोध"), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(t("Why should this milestone (\"${showDeleteRequestDialog?.title}\") be deleted?", "यह मील का पत्थर (\"${showDeleteRequestDialog?.title}\") क्यों हटाया जाना चाहिए?"), color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(t("Reason", "कारण")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFC857),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFFFFC857),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        trailingIcon = { SpeechToTextButton(onResult = { reason += it }) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMilestone(showDeleteRequestDialog!!)
                        showDeleteRequestDialog = null
                    },
                    enabled = reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC857),
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("Submit Request", "अनुरोध भेजें"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showDeleteRequestDialog = null 
                }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) } 
            }
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
        val editPhotoTitle = t("Crop Photo", "फोटो काटें")
        val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { 
                cropLauncher.launch(
                    CropImageContractOptions(
                        it, 
                        CropImageOptions(
                            guidelines = CropImageView.Guidelines.ON,
                            showProgressBar = true,
                            activityTitle = editPhotoTitle,
                            activityMenuIconColor = android.graphics.Color.WHITE
                        )
                    )
                ) 
            }
        }
        val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedAudioUri = it }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF1A1C1E),
            title = { Text(t("Add Memory", "याद जोड़ें"), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFC857),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFFFC857),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color(0xFFFFC857)
                    )
                    val textFieldShape = RoundedCornerShape(12.dp)

                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text(t("Title", "शीर्षक")) }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = year, 
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) year = it }, 
                        label = { Text(t("Year (YYYY)", "वर्ष (YYYY)")) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = textFieldShape,
                        colors = textFieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = location, 
                        onValueChange = { location = it }, 
                        label = { Text(t("Location", "स्थान")) }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showLocationPicker = true }) {
                                    Icon(Icons.Default.LocationOn, "Location", tint = Color(0xFFFFC857))
                                }
                            }
                        }
                    )

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
                    
                    Text(t("Visibility", "दृश्यता"), style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC857), modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("GLOBAL" to t("Global", "सभी"), "PRIVATE_FAMILY" to t("Private", "निजी"), "OLD_IS_GOLD" to t("Heritage", "धरोहर")).forEach { (type, label) ->
                            FilterChip(
                                selected = visibilityType == type,
                                onClick = { visibilityType = type },
                                label = { Text(label) },
                                shape = RoundedCornerShape(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFC857),
                                    selectedLabelColor = Color(0xFF080B14),
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { description = it }, 
                        label = { Text(t("Description", "विवरण")) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        minLines = 3,
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { description += it }) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(t("Tag Members", "सदस्यों को टैग करें"), style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    var tagSearch by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = tagSearch,
                        onValueChange = { tagSearch = it },
                        placeholder = { Text(t("Search and add tags...", "खोजें और टैग जोड़ें..."), color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = textFieldShape,
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFFC857)) },
                        colors = textFieldColors
                    )
                    
                    if (tagSearch.isNotEmpty()) {
                        val searchResults = allMembers.filter { 
                            it.name.contains(tagSearch, ignoreCase = true) && 
                            !selectedTaggedIds.contains(it.id) &&
                            (!it.isAdmin || user.isAdmin || it.id == user.id)
                        }.take(5)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                searchResults.forEach { member ->
                                    TextButton(
                                        onClick = {
                                            selectedTaggedIds = selectedTaggedIds + member.id
                                            tagSearch = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(member.name, color = Color.White, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                                    }
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
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = InputChipDefaults.inputChipColors(selectedContainerColor = Color(0xFFFFC857), selectedLabelColor = Color(0xFF080B14))
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857))
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(18.dp), tint = Color(0xFF080B14))
                            Spacer(Modifier.width(8.dp))
                            Text(if (selectedUri == null) t("Photo", "फोटो") else t("Selected", "चुनी गई"), color = Color(0xFF080B14), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") }, 
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857))
                        ) {
                            Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp), tint = Color(0xFF080B14))
                            Spacer(Modifier.width(8.dp))
                            Text(if (selectedAudioUri == null) t("Audio", "ऑडियो") else t("Selected", "चुना गया"), color = Color(0xFF080B14), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                val isValid = title.isNotBlank() && year.length == 4
                Button(
                    onClick = {
                        val contextId = when(visibilityType) {
                            "PRIVATE_FAMILY" -> if (userBaseId.isEmpty()) "P" else userBaseId
                            "OLD_IS_GOLD" -> if (userBaseId.isEmpty()) "P" else userBaseId
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
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC857),
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("Save", "सहेजें"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showAddDialog = false 
                }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) } 
            }
        )
    }

    ScreenContainer { paddingValues ->
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    AppTopBar(
                        title = t("Memory Lane", "यादों की गली"),
                        onBack = onBack
                    )
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = Color(0xFFFFC857)
                                )
                            }
                        },
                        divider = {}
                    ) {
                        listOf(t("Family", "परिवार"), t("Private", "निजी"), t("Heritage", "धरोहर")).forEachIndexed { index, label ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFFFC857),
                    contentColor = Color(0xFF080B14),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 70.dp) // Move it up to avoid AI Chat overlap
                ) { Icon(Icons.Default.Add, "Add Milestone") }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (filteredMilestones.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(80.dp), tint = Color.White.copy(alpha = 0.1f))
                            Spacer(Modifier.height(16.dp))
                            Text(t("No memories found here.", "यहाँ कोई यादें नहीं मिलीं।"), color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredMilestones) { milestone ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                                    Text(milestone.year, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857))
                                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().padding(vertical = 8.dp).background(Color(0xFFFFC857).copy(alpha = 0.2f)))
                                }
                                Spacer(Modifier.width(16.dp))
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        if (milestone.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = milestone.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .clickable { fullScreenImageUri = milestone.imageUrl },
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
                                                Text(milestone.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                if (milestone.location.isNotBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFFC857), modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(milestone.location, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                                    }
                                                }
                                            }
                                            if (user.isAdmin || user.id == milestone.authorId) {
                                                IconButton(onClick = { onDeleteMilestone(milestone) }) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                                }
                                            } else {
                                                IconButton(onClick = { showDeleteRequestDialog = milestone }) {
                                                    Icon(Icons.Default.DeleteForever, "Request Delete", tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(milestone.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                        
                                        if (milestone.taggedMemberIds.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(t("Tagged:", "टैग किया गया:"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857))
                                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                milestone.taggedMemberIds.forEach { id ->
                                                    val name = allMembers.find { it.id == id }?.name ?: "Unknown"
                                                    AssistChip(
                                                        onClick = { },
                                                        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                                        shape = RoundedCornerShape(28.dp),
                                                        colors = AssistChipDefaults.assistChipColors(
                                                            containerColor = Color.White.copy(alpha = 0.05f),
                                                            labelColor = Color.White.copy(alpha = 0.9f)
                                                        ),
                                                        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = Color.White.copy(alpha = 0.1f))
                                                    )
                                                }
                                            }
                                        }

                                        if (milestone.authorName.isNotBlank()) {
                                            Text(t("Added by ${milestone.authorName}", "${milestone.authorName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(top = 8.dp))
                                        }

                                        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))

                                        // Reactions
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("❤️", "👍", "🙏", "✨").forEach { emoji ->
                                                val usersWhoReacted = milestone.reactions[emoji] ?: emptyList()
                                                val hasReacted = usersWhoReacted.contains(user.id)
                                                FilterChip(
                                                    selected = hasReacted,
                                                    onClick = { onToggleReaction(milestone.id, emoji) },
                                                    label = { Text("$emoji ${if(usersWhoReacted.isNotEmpty()) usersWhoReacted.size else ""}") },
                                                    shape = RoundedCornerShape(28.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFFFFC857),
                                                        selectedLabelColor = Color(0xFF080B14),
                                                        containerColor = Color.White.copy(alpha = 0.05f),
                                                        labelColor = Color.White
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = hasReacted,
                                                        borderColor = Color.White.copy(alpha = 0.1f),
                                                        selectedBorderColor = Color(0xFFFFC857),
                                                        borderWidth = 1.dp,
                                                        selectedBorderWidth = 1.dp
                                                    )
                                                )
                                            }
                                        }

                                        // Comments Section
                                        val visibleComments = milestone.comments
                                        if (visibleComments.isNotEmpty()) {
                                            Spacer(Modifier.height(12.dp))
                                            visibleComments.takeLast(3).forEach { comment ->
                                                val displayName = if (comment.userName == "Admin") "Admin" else comment.userName
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(displayName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857))
                                                        Text(comment.text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                                                    }
                                                }
                                            }
                                        }

                                        var commentText by remember { mutableStateOf("") }
                                        Spacer(Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = commentText,
                                            onValueChange = { commentText = it },
                                            placeholder = { Text(t("Add comment...", "टिप्पणी जोड़ें..."), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(28.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFFFFC857),
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    if (commentText.isNotBlank()) {
                                                        onAddComment(milestone.id, commentText)
                                                        commentText = ""
                                                    }
                                                }) {
                                                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFFFFC857), modifier = Modifier.size(20.dp))
                                                }
                                            }
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
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
                tint = Color(0xFFFFC857)
            )
        }
        Text(
            text = if (isPlaying) t("Playing voice memory...", "वॉइस मेमोरी चल रही है...") else t("Voice Memory", "वॉइस मेमोरी"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
