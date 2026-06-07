package com.purawale.app.ui.screens

import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.ScreenContainer
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
            containerColor = Color(0xFF1A1C1E),
            title = { Text(if (user.isAdmin) t("Confirm Delete", "हटाने की पुष्टि करें") else t("Request Deletion", "हटाने का अनुरोध करें"), color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(if (user.isAdmin) t("Are you sure you want to permanently delete this recipe?", "क्या आप वाकई इस रेसिपी को स्थायी रूप से हटाना चाहते हैं?") else t("Please provide a reason for deleting this recipe:", "कृपया इस रेसिपी को हटाने का कारण बताएं:"), color = Color.White.copy(alpha = 0.7f))
                    if (!user.isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = deletionReason,
                            onValueChange = { deletionReason = it },
                            label = { Text(t("Reason", "कारण")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFC857),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFFFC857),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                cursorColor = Color(0xFFFFC857)
                            ),
                            trailingIcon = { SpeechToTextButton(onResult = { deletionReason += it }) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        recipeToDelete?.let { r -> onDelete(r.id) }
                        recipeToDelete = null
                        deletionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.isAdmin) Color(0xFFFF5252) else Color(0xFFFFC857),
                        contentColor = if (user.isAdmin) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(if (user.isAdmin) t("Delete", "हटाएं") else t("Submit Request", "अनुरोध भेजें"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    recipeToDelete = null
                    deletionReason = "" 
                }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) }
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

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editingRecipe = null 
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color(0xFF1A1C1E),
            title = { Text(if (isEditing) t("Edit Recipe", "रेसिपी संपादित करें") else t("Add Family Recipe", "पारिवारिक रेसिपी जोड़ें"), color = Color.White, fontWeight = FontWeight.Bold) },
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
                        label = { Text(t("Recipe Title", "रेसिपी का शीर्षक")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { title += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(t("Category (e.g. Dessert)", "श्रेणी (जैसे मिठाई)")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { category += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(t("Description", "विवरण")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = textFieldShape,
                        colors = textFieldColors,
                        trailingIcon = { SpeechToTextButton(onResult = { description += it }) }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text(t("Ingredients (one per line)", "सामग्री (प्रति पंक्ति एक)")) },
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
                        label = { Text(t("Instructions", "निर्देश")) },
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857))
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(20.dp), tint = Color(0xFF080B14))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedUri == null) (if (isEditing && editingRecipe?.imageUrl?.isNotBlank() == true) t("Change Photo", "फोटो बदलें") else t("Select Photo", "फोटो चुनें")) else t("Photo Selected", "फोटो चुनी गई"),
                            color = Color(0xFF080B14),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
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
                    enabled = title.isNotBlank(),
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
                    editingRecipe = null 
                }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) } 
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
            containerColor = Color(0xFF1A1C1E),
            title = { Text(recipe.title, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (recipe.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = recipe.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(t("Category: ${recipe.category}", "श्रेणी: ${recipe.category}"), style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFC857))
                    Text(t("By ${recipe.authorName}", "${recipe.authorName} द्वारा"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(recipe.description.ifBlank { t("No description provided.", "कोई विवरण प्रदान नहीं किया गया।") }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    Spacer(Modifier.height(20.dp))
                    Text(t("Ingredients", "सामग्री"), style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    if (recipe.ingredients.isEmpty()) {
                        Text(t("No ingredients listed.", "कोई सामग्री सूचीबद्ध नहीं है।"), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    } else {
                        recipe.ingredients.forEach { ingredient ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFC857), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ingredient, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(t("Instructions", "निर्देश"), style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    Text(
                        text = recipe.instructions.ifBlank { t("No instructions provided.", "कोई निर्देश प्रदान नहीं किया गया।") },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.White.copy(alpha = 0.1f))
                    Text(t("Reactions", "प्रतिक्रियाएं"), style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    selectedContainerColor = Color(0xFFFFC857),
                                    selectedLabelColor = Color(0xFF080B14),
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.White.copy(alpha = 0.1f),
                                    selectedBorderColor = Color(0xFFFFC857),
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(t("Comments", "टिप्पणियाँ"), style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    recipe.comments.forEach { comment ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (comment.userName == "Admin") "Admin" else comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857))
                                Text(comment.text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(t("Add a comment...", "एक टिप्पणी जोड़ें..."), color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFC857),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFFFFC857)
                        ),
                        trailingIcon = {
                            Row(modifier = Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SpeechToTextButton(onResult = { commentText += it })
                                IconButton(onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(recipe.id, commentText)
                                        commentText = ""
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFFFFC857))
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = { 
                Button(
                    onClick = { selectedRecipeId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color.Black),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("Close", "बंद करें"), fontWeight = FontWeight.Bold) }
            }
        )
    }

    var searchQuery by remember { mutableStateOf("") }

    ScreenContainer { paddingValues ->
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AppTopBar(
                    title = t("Family Cookbook", "पारिवारिक रसोइया"),
                    onBack = onBack
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFFFC857),
                    contentColor = Color(0xFF080B14),
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Add, "Add Recipe") }
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
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(t("Search recipes...", "व्यंजन खोजें..."), color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFFFFC857).copy(alpha = 0.8f)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFFFFC857)
                        )
                    )
                }

                if (recipes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.RestaurantMenu,
                                null,
                                modifier = Modifier.size(80.dp),
                                tint = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                t("No family recipes yet.", "अभी तक कोई पारिवारिक व्यंजन नहीं।"),
                                color = Color.White.copy(alpha = 0.5f),
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
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRecipes) { recipe ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRecipeId = recipe.id },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (recipe.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = recipe.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(16.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(recipe.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
                                        Text(t("By ${if (recipe.authorName == "Admin") "Admin" else recipe.authorName}", "${if (recipe.authorName == "Admin") "Admin" else recipe.authorName} द्वारा"), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                                    }
                                    if (user.isAdmin || recipe.authorId == user.id) {
                                        IconButton(onClick = { editingRecipe = recipe }) {
                                            Icon(Icons.Default.Edit, "Edit", tint = Color.White.copy(alpha = 0.7f))
                                        }
                                        IconButton(onClick = { recipeToDelete = recipe }) {
                                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.8f))
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
