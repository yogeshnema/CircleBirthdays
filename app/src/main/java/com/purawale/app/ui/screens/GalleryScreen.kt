package com.purawale.app.ui.screens

import com.purawale.app.ui.components.SpeechToTextButton
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.purawale.app.Member
import com.purawale.app.Memory
import com.purawale.app.t
import com.purawale.app.ui.theme.LightGolden

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    user: Member,
    memories: List<Memory>,
    allMembers: List<Member>,
    onBack: () -> Unit,
    onUpload: (String, Uri, List<String>) -> Unit,
    onApprove: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onImageClick: (Memory) -> Unit
) {
    var showUploadDialog by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTaggedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredMemories = memories.filter { 
        (it.status == "APPROVED" || user.isAdmin)
    }.filter {
        it.caption.contains(searchQuery, ignoreCase = true) || 
        it.userName.contains(searchQuery, ignoreCase = true)
    }
    
    var selectedMemoryForDetails by remember { mutableStateOf<Memory?>(null) }
    var memoryToDelete by remember { mutableStateOf<Memory?>(null) }
    var deletionReason by remember { mutableStateOf("") }

    if (memoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                memoryToDelete = null
                deletionReason = "" 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(if (user.isAdmin) "Confirm Delete" else "Request Deletion", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        if (user.isAdmin) "Are you sure you want to permanently delete this memory?" else "Please provide a reason for deleting this memory:",
                        color = Color(0xFF3E2723)
                    )
                    if (!user.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5D4037),
                                focusedLabelColor = Color(0xFF5D4037)
                            ),
                            trailingIcon = { SpeechToTextButton(onResult = { deletionReason += it }) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        memoryToDelete?.let { m ->
                            onDelete(m.id)
                        }
                        memoryToDelete = null
                        deletionReason = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (user.isAdmin) Color.Red else Color(0xFF5D4037))
                ) { Text(if (user.isAdmin) "Delete" else "Submit Request", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        memoryToDelete = null
                        deletionReason = "" 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Cancel") }
            }
        )
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            selectedUri = result.uriContent
        } else {
            val exception = result.error
            Log.e("CircleBirthdays", "Crop failed", exception)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            val cropOptions = CropImageContractOptions(
                uri = it,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    fixAspectRatio = false,
                    imageSourceIncludeCamera = false
                )
            )
            cropLauncher.launch(cropOptions)
        }
    }

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUploadDialog = false 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text("Share a Memory", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                    ) {
                        Text(if (selectedUri == null) "Select Photo" else "Photo Selected", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5D4037),
                            focusedLabelColor = Color(0xFF5D4037)
                        ),
                        trailingIcon = { SpeechToTextButton(onResult = { caption += it }) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(t("Tag Members", "सदस्यों को टैग करें"), style = MaterialTheme.typography.labelMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    var tagSearch by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = tagSearch,
                        onValueChange = { tagSearch = it },
                        placeholder = { Text(t("Search and add tags...", "खोजें और टैग जोड़ें...")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = Color(0xFF5D4037)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5D4037),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f)
                        )
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
                            colors = CardDefaults.cardColors(containerColor = LightGolden.copy(alpha = 0.2f))
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
                                        Text(member.name, color = Color(0xFF3E2723), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
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
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = LightGolden,
                                        selectedLabelColor = Color(0xFF5D4037)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = selectedUri
                        if (uri != null && caption.isNotBlank()) {
                            onUpload(caption, uri, selectedTaggedIds)
                            showUploadDialog = false
                            caption = ""
                            selectedUri = null
                            selectedTaggedIds = emptyList()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Upload", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showUploadDialog = false 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Cancel") }
            }
        )
    }

    if (selectedMemoryForDetails != null) {
        val memory = memories.find { it.id == selectedMemoryForDetails?.id } ?: selectedMemoryForDetails!!
        var commentText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { 
                selectedMemoryForDetails = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text("Reactions & Comments", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    AsyncImage(
                        model = memory.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp))
                            .clickable { 
                                onImageClick(memory)
                                selectedMemoryForDetails = null 
                            },
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error = painterResource(id = android.R.drawable.ic_menu_report_image),
                        onError = { state ->
                            Log.e("CircleBirthdays", "Gallery detail image load failed: ${state.result.throwable.message}")
                        }
                    )
                    Text(
                        memory.caption, 
                        style = MaterialTheme.typography.bodyLarge, 
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    
                    if (memory.taggedMemberIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tagged Members:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            memory.taggedMemberIds.forEach { id ->
                                val name = allMembers.find { it.id == id }?.name ?: "Unknown"
                                AssistChip(
                                    onClick = { },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF5D4037))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (emoji in listOf("❤️", "👍", "🙏", "😮")) {
                            val userIds = memory.reactions[emoji] ?: emptyList()
                            val count = userIds.size
                            val isSelected = userIds.contains(user.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(memory.id, emoji) },
                                label = { Text("$emoji $count") },
                                shape = RoundedCornerShape(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LightGolden,
                                    selectedLabelColor = Color(0xFF5D4037)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Comments", style = MaterialTheme.typography.titleSmall, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    val visibleComments = memory.comments
                    for (comment in visibleComments) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(if (comment.userName == "Admin") "Admin" else comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                                Text(comment.text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5D4037),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f)
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(memory.id, commentText)
                                        commentText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFF5D4037))
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedMemoryForDetails = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Close", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Scaffold(
        containerColor = Color.White.copy(alpha = 0.3f),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(t("Memory Gallery", "यादों की गैलरी"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF3E2723))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showUploadDialog = true }) {
                            Icon(Icons.Default.AddAPhoto, "Add Memory", tint = Color(0xFF5D4037))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color.White, RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(t("Search memories...", "यादें खोजें..."), color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF5D4037)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF3E2723),
                            unfocusedTextColor = Color(0xFF3E2723),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF3E2723)
                        )
                    )
                }
            }
        }
    ) { padding ->
        if (filteredMemories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF5D4037).copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isEmpty()) t("No memories shared yet.", "अभी तक कोई यादें साझा नहीं की गईं।") else t("No matches found.", "कोई मेल नहीं मिला।"),
                        color = Color(0xFF5D4037).copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMemories) { memory ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMemoryForDetails = memory }
                            .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (memory.status == "PENDING") 
                                Color.White.copy(alpha = 0.7f) 
                            else Color.White
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                AsyncImage(
                                    model = memory.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                                    error = painterResource(id = android.R.drawable.ic_menu_report_image),
                                    onError = { state ->
                                        Log.e("CircleBirthdays", "Gallery grid image load failed: ${state.result.throwable.message}")
                                    }
                                )
                                // Add a subtle dark overlay at the bottom for text legibility
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                            )
                                        )
                                )
                                if (memory.status == "PENDING") {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            t("PENDING", "लंबित"), 
                                            color = Color.White, 
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(memory.caption, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color(0xFF3E2723))
                                Text(t("By ${if (memory.userName == "Admin") "Admin" else memory.userName}", "${if (memory.userName == "Admin") "Admin" else memory.userName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val reactionCount = memory.reactions.values.sumOf { it.size }
                                    val commentCount = memory.comments.size
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(14.dp), tint = Color(0xFFD32F2F))
                                        Text(" $reactionCount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.8f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.Comment, null, modifier = Modifier.size(14.dp), tint = Color(0xFF5D4037).copy(alpha = 0.6f))
                                        Text(" $commentCount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.8f))
                                    }

                                    if (user.isAdmin) {
                                        Row {
                                            if (memory.status == "PENDING") {
                                                IconButton(onClick = { onApprove(memory.id) }, modifier = Modifier.size(28.dp)) {
                                                    Icon(Icons.Default.Check, "Approve", tint = Color(0xFF388E3C), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            IconButton(onClick = { memoryToDelete = memory }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
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
}
