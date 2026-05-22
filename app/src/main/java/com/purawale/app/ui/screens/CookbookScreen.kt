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
import com.purawale.app.Recipe
import com.purawale.app.t
import com.purawale.app.ui.theme.LightGolden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookScreen(
    user: Member,
    recipes: List<Recipe>,
    allMembers: List<Member>,
    onBack: () -> Unit,
    onAddRecipe: (Recipe, Uri?) -> Unit,
    onEditRecipe: (Recipe, Uri?) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecipe by remember { mutableStateOf<Recipe?>(null) }
    var selectedRecipeId by remember { mutableStateOf<String?>(null) }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    var deletionReason by remember { mutableStateOf("") }
    val selectedRecipe = recipes.find { it.id == selectedRecipeId }

    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                recipeToDelete = null
                deletionReason = "" 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(if (user.isAdmin) "Confirm Delete" else "Request Deletion", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(if (user.isAdmin) "Are you sure you want to permanently delete this recipe?" else "Please provide a reason for deleting this recipe:", color = Color(0xFF5D4037))
                    if (!user.isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5D4037),
                                unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f)
                            ),
                            trailingIcon = { SpeechToTextButton(onResult = { deletionReason += it }) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    recipeToDelete?.let { r -> onDelete(r.id) }
                    recipeToDelete = null
                    deletionReason = ""
                }) { Text(if (user.isAdmin) "Delete" else "Submit Request", color = if (user.isAdmin) Color.Red else Color(0xFF5D4037), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    recipeToDelete = null
                    deletionReason = "" 
                }) { Text("Cancel", color = Color(0xFF5D4037).copy(alpha = 0.7f)) }
            }
        )
    }

    if (showAddDialog || editingRecipe != null) {
        val isEditing = editingRecipe != null
        var title by remember { mutableStateOf(editingRecipe?.title ?: "") }
        var category by remember { mutableStateOf(editingRecipe?.category ?: "") }
        var description by remember { mutableStateOf(editingRecipe?.description ?: "") }
        var ingredients by remember { mutableStateOf(editingRecipe?.ingredients?.joinToString("\n") ?: "") }
        var instructions by remember { mutableStateOf(editingRecipe?.instructions ?: "") }
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
                editingRecipe = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(if (isEditing) "Edit Recipe" else "Add Family Recipe", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5D4037),
                        unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f)
                    )
                    val textFieldShape = RoundedCornerShape(28.dp)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Recipe Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. Dessert)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { category += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { description += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text("Ingredients (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { ingredients += if (ingredients.isEmpty() || ingredients.endsWith("\n")) it else "\n$it" }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { instructions += it }) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                    ) {
                        Text(if (selectedUri == null) (if (isEditing && editingRecipe?.imageUrl?.isNotBlank() == true) "Change Photo" else "Select Photo") else "Photo Selected")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val recipe = if (isEditing) {
                            editingRecipe!!.copy(
                                title = title,
                                category = category,
                                description = description,
                                ingredients = ingredients.split("\n").filter { it.isNotBlank() },
                                instructions = instructions
                            )
                        } else {
                            Recipe(
                                title = title,
                                category = category,
                                description = description,
                                ingredients = ingredients.split("\n").filter { it.isNotBlank() },
                                instructions = instructions
                            )
                        }
                        
                        if (isEditing) {
                            onEditRecipe(recipe, selectedUri)
                        } else {
                            onAddRecipe(recipe, selectedUri)
                        }
                        showAddDialog = false
                        editingRecipe = null
                    },
                    enabled = title.isNotBlank()
                ) { Text("Save", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    showAddDialog = false
                    editingRecipe = null 
                }) { Text("Cancel", color = Color(0xFF5D4037).copy(alpha = 0.7f)) } 
            }
        )
    }

    if (selectedRecipe != null) {
        val recipe = selectedRecipe
        var commentText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { 
                selectedRecipeId = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text(recipe.title, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (recipe.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(28.dp)).border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("Category: ${recipe.category}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF5D4037))
                    Text("By ${recipe.authorName}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))
                    Text(recipe.description.ifBlank { "No description provided." }, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3E2723))
                    Spacer(Modifier.height(16.dp))
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    if (recipe.ingredients.isEmpty()) {
                        Text("No ingredients listed.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723))
                    } else {
                        for (ingredient in recipe.ingredients) {
                            Text("• $ingredient", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    Text(
                        text = recipe.instructions.ifBlank { "No instructions provided." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3E2723)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFF5D4037).copy(alpha = 0.2f))
                    Text("Reactions", style = MaterialTheme.typography.titleSmall, color = Color(0xFF5D4037))
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (emoji in listOf("❤️", "👍", "😋", "🔥")) {
                            val userIds = recipe.reactions[emoji] ?: emptyList()
                            val count = userIds.size
                            val isSelected = userIds.contains(user.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleReaction(recipe.id, emoji) },
                                label = { Text("$emoji $count") },
                                shape = RoundedCornerShape(28.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LightGolden,
                                    selectedLabelColor = Color(0xFF5D4037)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF5D4037),
                                    selectedBorderColor = Color(0xFF5D4037)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Comments", style = MaterialTheme.typography.titleSmall, color = Color(0xFF5D4037))
                    val visibleComments = recipe.comments
                    for (comment in visibleComments) {
                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                            Text(if (comment.userName == "Admin") "Admin" else comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                            Text(comment.text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...", color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5D4037),
                            unfocusedBorderColor = Color(0xFF5D4037).copy(alpha = 0.5f)
                        ),
                        trailingIcon = {
                            Row {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(recipe.id, commentText)
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
            confirmButton = { TextButton(onClick = { selectedRecipeId = null }) { Text("Close", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold) } }
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Family Cookbook", "पारिवारिक रसोइया"), fontWeight = FontWeight.Bold, color = Color(0xFF3E2723)) },
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
            ) { Icon(Icons.Default.Add, "Add Recipe") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White.copy(alpha = 0.3f))
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
                        placeholder = { Text(t("Search recipes...", "व्यंजन खोजें..."), color = Color(0xFF5D4037).copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF5D4037).copy(alpha = 0.5f)) },
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

                if (recipes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.RestaurantMenu,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF5D4037).copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                t("No family recipes yet.", "अभी तक कोई पारिवारिक व्यंजन नहीं।"),
                                color = Color(0xFF5D4037).copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    val filteredRecipes = recipes.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true) ||
                                it.authorName.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredRecipes) { recipe ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedRecipeId = recipe.id }
                                    .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (recipe.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = recipe.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEFEBE9)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(16.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                                        Text(recipe.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.6f))
                                        Text(t("By ${if (recipe.authorName == "Admin") "Admin" else recipe.authorName}", "${if (recipe.authorName == "Admin") "Admin" else recipe.authorName} द्वारा"), style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037).copy(alpha = 0.8f))
                                    }
                                    if (user.isAdmin || recipe.authorId == user.id) {
                                        IconButton(onClick = { editingRecipe = recipe }) {
                                            Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF5D4037))
                                        }
                                        IconButton(onClick = { recipeToDelete = recipe }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFD32F2F))
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
