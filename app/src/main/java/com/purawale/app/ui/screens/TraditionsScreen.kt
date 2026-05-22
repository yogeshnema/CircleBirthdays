package com.purawale.app.ui.screens

import com.purawale.app.ui.components.SpeechToTextButton
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.purawale.app.Member
import com.purawale.app.Tradition
import com.purawale.app.t
import com.purawale.app.ui.theme.LightGolden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraditionsScreen(
    user: Member,
    traditions: List<Tradition>,
    allMembers: List<Member>,
    onBack: () -> Unit,
    onAddTradition: (Tradition, Uri?) -> Unit,
    onEditTradition: (Tradition, Uri?) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTradition by remember { mutableStateOf<Tradition?>(null) }
    var selectedTraditionId by remember { mutableStateOf<String?>(null) }
    var traditionToDelete by remember { mutableStateOf<Tradition?>(null) }
    var deletionReason by remember { mutableStateOf("") }
    val selectedTradition = traditions.find { it.id == selectedTraditionId }

    if (traditionToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                traditionToDelete = null
                deletionReason = "" 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(if (user.isAdmin) "Confirm Delete" else "Request Deletion", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        if (user.isAdmin) "Are you sure you want to permanently delete this tradition?" else "Please provide a reason for deleting this tradition:",
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
                        traditionToDelete?.let { t -> onDelete(t.id) }
                        traditionToDelete = null
                        deletionReason = ""
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (user.isAdmin) Color.Red else Color(0xFF5D4037))
                ) { Text(if (user.isAdmin) "Delete" else "Submit Request", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        traditionToDelete = null
                        deletionReason = "" 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog || editingTradition != null) {
        val isEditing = editingTradition != null
        var title by remember { mutableStateOf(editingTradition?.title ?: "") }
        var description by remember { mutableStateOf(editingTradition?.description ?: "") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
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
                showAddDialog = false
                editingTradition = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(if (isEditing) "Edit Tradition" else "Share a Tradition", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5D4037),
                            focusedLabelColor = Color(0xFF5D4037)
                        ),
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Story/Tradition") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5D4037),
                            focusedLabelColor = Color(0xFF5D4037)
                        ),
                        trailingIcon = { SpeechToTextButton(onResult = { description += it }) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                    ) {
                        Text(if (selectedUri == null) (if (isEditing && editingTradition?.imageUrl?.isNotBlank() == true) "Change Photo" else "Select Photo") else "Photo Selected", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trad = if (isEditing) {
                            editingTradition!!.copy(title = title, description = description)
                        } else {
                            Tradition(title = title, description = description)
                        }
                        
                        if (isEditing) {
                            onEditTradition(trad, selectedUri)
                        } else {
                            onAddTradition(trad, selectedUri)
                        }
                        showAddDialog = false
                        editingTradition = null
                    },
                    enabled = title.isNotBlank() && description.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text(if (isEditing) "Save" else "Share", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { 
                TextButton(
                    onClick = { 
                        showAddDialog = false
                        editingTradition = null 
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Cancel") } 
            }
        )
    }

    if (selectedTradition != null) {
        val trad = selectedTradition
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { 
                selectedTraditionId = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(trad.title, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (trad.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = trad.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(t("By ${trad.authorName}", "${trad.authorName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Text(trad.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF5D4037).copy(alpha = 0.1f))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (emoji in listOf("❤️", "👍", "🙏", "✨")) {
                            val userIds = trad.reactions[emoji] ?: emptyList()
                            val count = userIds.size
                            val isSelected = userIds.contains(user.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(trad.id, emoji) },
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
                    val visibleComments = trad.comments
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
                                        onAddComment(trad.id, commentText)
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
                    onClick = { selectedTraditionId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                ) { Text("Close", fontWeight = FontWeight.Bold) } 
            }
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.White.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = { Text(t("Family Traditions", "पारिवारिक परंपराएं"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF3E2723)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF5D4037),
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp)
            ) { Icon(Icons.Default.Add, "Share Tradition") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(t("Search traditions...", "परंपराएं खोजें..."), color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
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

            if (traditions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF5D4037).copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            t("No family traditions shared yet.", "अभी तक कोई पारिवारिक परंपरा साझा नहीं की गई।"),
                            color = Color(0xFF5D4037).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                val filteredTraditions = traditions.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true) ||
                            it.authorName.contains(searchQuery, ignoreCase = true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredTraditions) { trad ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedTraditionId = trad.id }
                                .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (trad.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = trad.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(24.dp))
                                                    .background(Color(0xFFEFEBE9)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(Modifier.height(12.dp))
                                        }
                                        Text(trad.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                                        Text(t("By ${if (trad.authorName == "Admin") "Admin" else trad.authorName}", "${if (trad.authorName == "Admin") "Admin" else trad.authorName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                                    }
                                    if (user.isAdmin || trad.authorId == user.id) {
                                        Row {
                                            IconButton(onClick = { editingTradition = trad }) {
                                                Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF5D4037))
                                            }
                                            IconButton(onClick = { traditionToDelete = trad }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F))
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(trad.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, color = Color(0xFF3E2723).copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }
        }
    }
}
