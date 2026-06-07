package com.purawale.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.purawale.app.*
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.DatePickerField
import com.purawale.app.ui.components.ScreenContainer
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    user: Member,
    achievements: List<Achievement>,
    onBack: () -> Unit,
    onAddAchievement: (Achievement, Uri?) -> Unit,
    onDeleteAchievement: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = t("Purawale Achievements", "पुरवाले उपलब्धियां"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = t("Add Achievement", "उपलब्धि जोड़ें"), tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (achievements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(t("No achievements found", "कोई उपलब्धियां नहीं मिलीं"), color = Color.White.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(achievements) { achievement ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                containerColor = Color(0xFF1A1C1E),
                                shape = RoundedCornerShape(28.dp),
                                title = { Text(t("Delete Achievement", "उपलब्धि हटाएं"), color = Color.White, fontWeight = FontWeight.Bold) },
                                text = { Text(t("Are you sure you want to delete this achievement?", "क्या आप वाकई इस उपलब्धि को हटाना चाहते हैं?"), color = Color.White.copy(alpha = 0.8f)) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onDeleteAchievement(achievement.id)
                                            showDeleteConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.Black),
                                        shape = RoundedCornerShape(24.dp)
                                    ) { Text(t("Delete", "हटाएं"), fontWeight = FontWeight.Bold) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.7f)) }
                                }
                            )
                        }

                        AchievementCard(achievement, user.isAdmin || achievement.addedBy == user.id) {
                            showDeleteConfirm = true
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddAchievementDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { achievement, uri ->
                    onAddAchievement(achievement.copy(addedBy = user.id), uri)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, canDelete: Boolean, onDelete: () -> Unit) {
    val context = LocalContext.current
    val gold = Color(0xFFFFC857)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            if (achievement.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = achievement.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            achievement.title, 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            achievement.memberName, 
                            style = MaterialTheme.typography.labelLarge, 
                            color = gold
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    achievement.description, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = gold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(achievement.date, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }

                if (achievement.location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = gold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(achievement.location, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                if (achievement.mapsLink.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(achievement.mapsLink))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = gold
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t("View on Map", "मैप पर देखें"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddAchievementDialog(onDismiss: () -> Unit, onConfirm: (Achievement, Uri?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mapsLink by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val editPhotoTitle = t("Crop Photo", "फोटो काटें")
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) selectedUri = result.uriContent
    }
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

    val gold = Color(0xFFFFC857)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        shape = RoundedCornerShape(28.dp),
        title = { Text(t("Add Achievement", "उपलब्धि जोड़ें"), color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(t("Achievement Title", "उपलब्धि का शीर्षक")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text(t("Member Name", "सदस्य का नाम")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(t("Description", "विवरण")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                DatePickerField(
                    label = t("Date", "तारीख"),
                    selectedDate = date,
                    onDateSelected = { date = it }
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(t("Location", "स्थान")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = mapsLink,
                    onValueChange = { mapsLink = it },
                    label = { Text(t("Maps Link", "मैप्स लिंक")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri == null) t("Add Photo", "फोटो जोड़ें") else t("Photo Selected", "फोटो चुनी गई"), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && memberName.isNotBlank()) {
                        onConfirm(Achievement(title = title, memberName = memberName, description = description, date = date, location = location, mapsLink = mapsLink), selectedUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp)
            ) { Text(t("Add", "जोड़ें"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.7f)) }
        }
    )
}
