package com.purawale.app.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.google.ai.client.generativeai.GenerativeModel
import com.purawale.app.AppConfig
import com.purawale.app.FamilyUtils
import com.purawale.app.FirebaseManager
import com.purawale.app.LocalLanguage
import com.purawale.app.Member
import com.purawale.app.Message
import com.purawale.app.R
import com.purawale.app.t
import com.purawale.app.ui.theme.PlayfairFontFamily
import com.purawale.app.ui.theme.ScriptFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import org.json.JSONObject

data class AIDesignSpec(
    val message: String = "",
    val primaryColor: Color = Color(0xFFD4AF37),
    val secondaryColor: Color = Color(0xFF8B4513),
    val backgroundColor: Color = Color(0xFFFFF8E7),
    val accentColor: Color = Color(0xFF6D2E46),
    val motif: String = "floral",
    val fontVibe: String = "serif",
    val layout: String = "portrait-frame",
    val headline: String = "",
    val subheadline: String = "",
    val photoTreatment: String = "ornate-frame"
)

data class CardFrameStyle(
    val name: String,
    val drawableRes: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val photoBorderColor: Color,
    val plateColor: Color
)

data class CardPhotoAdjustments(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
)

data class CardTextSettings(
    val fontIndex: Int = 0,
    val sizeScale: Float = 1f,
    val colorIndex: Int = 0
)

private val cardFrameStyles = listOf(
    CardFrameStyle(
        name = "Ivory",
        drawableRes = R.drawable.ai_card_floral_frame,
        primaryColor = Color(0xFF7A121C),
        secondaryColor = Color(0xFF3F2417),
        accentColor = Color(0xFFC08A1A),
        photoBorderColor = Color(0xFFC08A1A),
        plateColor = Color(0xFFFFF1D2).copy(alpha = 0.90f)
    ),
    CardFrameStyle(
        name = "Burgundy",
        drawableRes = R.drawable.ai_card_frame_burgundy,
        primaryColor = Color(0xFFFFD37A),
        secondaryColor = Color(0xFFFFF3D4),
        accentColor = Color(0xFFD4A238),
        photoBorderColor = Color(0xFFD4A238),
        plateColor = Color(0xFF5A1015).copy(alpha = 0.70f)
    ),
    CardFrameStyle(
        name = "Navy",
        drawableRes = R.drawable.ai_card_frame_navy,
        primaryColor = Color(0xFFFFD37A),
        secondaryColor = Color(0xFFFFF3D4),
        accentColor = Color(0xFFD6A94F),
        photoBorderColor = Color(0xFFD6A94F),
        plateColor = Color(0xFF092844).copy(alpha = 0.74f)
    ),
    CardFrameStyle(
        name = "Blush",
        drawableRes = R.drawable.ai_card_frame_blush,
        primaryColor = Color(0xFF7A121C),
        secondaryColor = Color(0xFF4B2A22),
        accentColor = Color(0xFFC08A1A),
        photoBorderColor = Color(0xFFC08A1A),
        plateColor = Color(0xFFFFE0E8).copy(alpha = 0.86f)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICardGeneratorScreen(
    member: Member,
    eventType: String,
    currentUser: Member,
    allMembers: List<Member>,
    onBack: () -> Unit,
    aiDesignCache: Map<String, Any>,
    onUpdateCache: (Map<String, Any>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Initialize from cache if available
    val cacheKey = "${member.id}_$eventType"
    val cachedSpec = aiDesignCache[cacheKey] as? AIDesignSpec
    
    var aiDesign by remember { mutableStateOf(cachedSpec ?: AIDesignSpec()) }
    var generatedMessage by remember { mutableStateOf(cachedSpec?.message ?: "") }
    var isGenerating by remember { mutableStateOf(false) }
    var isImageLoading by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var selectedFrameIndex by remember(cacheKey) { mutableStateOf(0) }
    var selectedPhotoUri by remember(cacheKey) { mutableStateOf<Uri?>(null) }
    var photoAdjustments by remember(cacheKey) { mutableStateOf(CardPhotoAdjustments()) }
    var textSettings by remember(cacheKey) { mutableStateOf(CardTextSettings()) }
    var nameLabelEdited by remember(cacheKey) { mutableStateOf(false) }
    var messageEdited by remember(cacheKey) { mutableStateOf(false) }
    
    val graphicsLayer = rememberGraphicsLayer()
    val isHindi = LocalLanguage.current

    val relationship = member.relationship ?: FamilyUtils.getRelationship(member, currentUser, allMembers)
    val supportedEvent = eventType == "Birthday" || eventType == "Anniversary"
    val defaultNameLabel = remember(member, eventType, allMembers) { defaultCardNameLabel(member, eventType, allMembers) }
    var editableNameLabel by remember(cacheKey) {
        mutableStateOf(cachedSpec?.subheadline?.ifBlank { defaultNameLabel } ?: defaultNameLabel)
    }
    var editableMessage by remember(cacheKey) {
        mutableStateOf(cachedSpec?.message ?: defaultWish(member, eventType))
    }
    var editableFromLabel by remember(cacheKey) { mutableStateOf("With love from") }
    var editableSenderLabel by remember(cacheKey) { mutableStateOf("Purawale - Hum aur Humare") }
    var editableClosingLabel by remember(cacheKey) { mutableStateOf("We love you $defaultNameLabel") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            photoAdjustments = CardPhotoAdjustments()
        }
    }

    val years = remember(member, eventType) {
        try {
            val dateStr = if (eventType == "Birthday") member.dateOfBirth else member.marriageDate
            if (!dateStr.isNullOrBlank()) {
                val parts = dateStr.split("-", "/")
                if (parts.size == 3) {
                    val day = parts[0].toIntOrNull() ?: 0
                    val month = parts[1].toIntOrNull() ?: 0
                    val year = parts[2].toIntOrNull() ?: 0
                    if (day > 0 && month > 0 && year > 0) {
                        val start = LocalDate.of(year, month, day)
                        ChronoUnit.YEARS.between(start, LocalDate.now()).toInt()
                    } else 0
                } else 0
            } else 0
        } catch (_: Exception) { 0 }
    }

    val generateAIContent = {
        scope.launch {
            if (!supportedEvent) {
                generatedMessage = "Cards are generated only for birthdays and anniversaries."
                return@launch
            }
            isGenerating = true
            try {
                val generativeModel = GenerativeModel(
                    modelName = AppConfig.GEMINI_MODEL,
                    apiKey = AppConfig.GEMINI_API_KEY
                )

                val partnerId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
                val partner = allMembers.firstOrNull { it.familyId == partnerId }
                val spouseName = member.spouseName ?: partner?.name ?: "Partner"

                val prompt = """
                    You are designing a complete premium keepsake greeting card for Purawale - Hum aur Humare.
                    The app will render the full card from your JSON. Use only these inputs: recipient name, event type, spouse name when relevant, and the recipient profile photo already available in the app.
                    Visual direction: vintage Indian family greeting poster, ivory paper, gold ornamental border, watercolor floral corners, central framed photo, elegant but readable typography. Never request huge text, diagonal text, slashes, or text over the photo.

                    Event: $eventType
                    Recipient: ${member.name}
                    Recipient relationship to current user: $relationship
                    ${if (eventType == "Anniversary") "Spouse name: $spouseName" else ""}
                    Sender brand: Purawale - Hum aur Humare

                    Return ONLY a JSON object with these fields:
                    {
                      "headline": "Short, e.g. Happy",
                      "subheadline": "Recipient first name, or couple names for anniversary",
                      "message": "Warm premium wish from Purawale - Hum aur Humare, max 22 words. Do not say AI.",
                      "primaryColor": "Deep readable maroon/brown hex for main text",
                      "secondaryColor": "Dark readable brown hex for message text",
                      "backgroundColor": "Warm ivory or parchment hex",
                      "accentColor": "Soft rose/gold hex for flowers and ornaments",
                      "motif": "One of: floral, sparkles, royal, festive",
                      "fontVibe": "One of: script, serif",
                      "layout": "portrait-frame",
                      "photoTreatment": "ornate-frame"
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val jsonStr = response.text?.substringAfter("{")?.substringBeforeLast("}")?.let { "{$it}" }
                
                if (jsonStr != null) {
                    val json = JSONObject(jsonStr)
                    val pColor = parseAiColor(json.optString("primaryColor"), Color(0xFFD4AF37))
                    val sColor = parseAiColor(json.optString("secondaryColor"), Color(0xFF6D2E46))
                    val bgColor = parseAiColor(json.optString("backgroundColor"), Color(0xFFFFF8E7))
                    val accentColor = parseAiColor(json.optString("accentColor"), Color(0xFFB76E79))
                    
                    val newDesign = AIDesignSpec(
                        message = json.optString("message", defaultWish(member, eventType)),
                        primaryColor = pColor,
                        secondaryColor = sColor,
                        backgroundColor = bgColor,
                        accentColor = accentColor,
                        motif = json.optString("motif", "floral"),
                        fontVibe = json.optString("fontVibe", "serif"),
                        layout = json.optString("layout", "portrait-frame"),
                        headline = json.optString("headline", "Happy $eventType"),
                        subheadline = json.optString("subheadline", member.name.split(" ").first()),
                        photoTreatment = json.optString("photoTreatment", "ornate-frame")
                    )
                    aiDesign = newDesign
                    generatedMessage = aiDesign.message
                    if (!nameLabelEdited) {
                        editableNameLabel = newDesign.subheadline.ifBlank { defaultNameLabel }
                        editableClosingLabel = "We love you ${editableNameLabel.ifBlank { defaultNameLabel }}"
                    }
                    if (!messageEdited) {
                        editableMessage = newDesign.message
                    }
                    
                    // Update cache
                    val newCache = aiDesignCache.toMutableMap()
                    newCache[cacheKey] = newDesign
                    onUpdateCache(newCache)
                }
            } catch (e: Exception) {
                val fallback = defaultAiDesign(member, eventType, allMembers)
                aiDesign = fallback
                generatedMessage = fallback.message
                if (!nameLabelEdited) editableNameLabel = fallback.subheadline.ifBlank { defaultNameLabel }
                if (!messageEdited) editableMessage = fallback.message
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, if (isHindi) "AI स्टाइलिंग त्रुटि: ${e.localizedMessage}" else "AI Styling Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isGenerating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (generatedMessage.isEmpty() && supportedEvent) {
            generateAIContent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("AI Card Generator", "AI कार्ड जेनरेटर")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { generateAIContent() }, enabled = !isGenerating) {
                        Icon(Icons.Default.Refresh, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!supportedEvent) {
                Text(
                    text = "Cards are available for birthdays and anniversaries only.",
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
            }

            if (isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text(t("Generating magical message...", "जादुई संदेश बनाया जा रहा है..."))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Preview
            Card(
                modifier = Modifier
                    .width(350.dp)
                    .aspectRatio(2f / 3f)
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                GreetingCardContent(
                    member = member,
                    eventType = eventType,
                    message = editableMessage.ifBlank { generatedMessage },
                    years = years,
                    allMembers = allMembers,
                    aiDesign = aiDesign,
                    frameStyle = cardFrameStyles[selectedFrameIndex],
                    customPhotoUri = selectedPhotoUri,
                    photoAdjustments = photoAdjustments,
                    onPhotoAdjustmentsChange = { photoAdjustments = it },
                    textSettings = textSettings,
                    nameLabel = editableNameLabel,
                    fromLabel = editableFromLabel,
                    senderLabel = editableSenderLabel,
                    closingLabel = editableClosingLabel,
                    onLoadingStateChange = { isImageLoading = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Frame", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                cardFrameStyles.forEachIndexed { index, frame ->
                    FrameChoice(
                        frameStyle = frame,
                        selected = selectedFrameIndex == index,
                        onClick = { selectedFrameIndex = index }
                    )
                }
            }

            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (selectedPhotoUri == null) "Add or replace card photo" else "Change card photo")
            }

            PhotoEditControls(
                adjustments = photoAdjustments,
                onChange = { photoAdjustments = it },
                onReset = { photoAdjustments = CardPhotoAdjustments() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editableNameLabel,
                        onValueChange = {
                            editableNameLabel = it
                            nameLabelEdited = true
                        },
                        label = { Text("Name label on card") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editableMessage,
                        onValueChange = {
                            editableMessage = it
                            messageEdited = true
                        },
                        label = { Text("Wish message") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editableFromLabel,
                        onValueChange = { editableFromLabel = it },
                        label = { Text("From label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editableSenderLabel,
                        onValueChange = { editableSenderLabel = it },
                        label = { Text("Sender / addressee") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editableClosingLabel,
                        onValueChange = { editableClosingLabel = it },
                        label = { Text("Closing line") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextStyleControls(
                        frameStyle = cardFrameStyles[selectedFrameIndex],
                        settings = textSettings,
                        onChange = { textSettings = it }
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            saveBitmapToGallery(context, bitmap, "${member.name}_${eventType}_Card")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, if (isHindi) "कार्ड गैलरी में सहेजा गया!" else "Card saved to gallery!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, if (isHindi) "सहेजने में विफल: ${e.message}" else "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isImageLoading && !isGenerating && !isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isImageLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Download, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isImageLoading) t("Loading images...", "तस्वीरें लोड हो रही हैं...") else t("Download Card", "कार्ड डाउनलोड करें"))
            }

            Button(
                onClick = {
                    scope.launch {
                        isSending = true
                        try {
                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                            val data = stream.toByteArray()
                            
                            val imageUrl = FirebaseManager.uploadPhoto(data)
                            
                            val message = Message(
                                senderId = currentUser.id,
                                senderName = currentUser.name,
                                receiverId = member.id,
                                text = "I generated a special $eventType card for you! ✨",
                                imageUrl = imageUrl,
                                timestamp = System.currentTimeMillis()
                            )
                            
                            FirebaseManager.sendMessage(message)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, if (isHindi) "कार्ड ${member.name} को भेज दिया गया!" else "Card sent to ${member.name}!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, if (isHindi) "भेजने में विफल: ${e.localizedMessage}" else "Failed to send: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        } finally {
                            isSending = false
                        }
                    }
                },
                enabled = !isImageLoading && !isGenerating && !isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSending) t("Sending...", "भेज रहा है...") else t("Send to ${member.name}", "${member.name} को भेजें"))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GreetingCardContent(
    member: Member,
    eventType: String,
    message: String,
    years: Int,
    allMembers: List<Member>,
    aiDesign: AIDesignSpec = AIDesignSpec(),
    frameStyle: CardFrameStyle = cardFrameStyles.first(),
    customPhotoUri: Uri? = null,
    photoAdjustments: CardPhotoAdjustments = CardPhotoAdjustments(),
    onPhotoAdjustmentsChange: (CardPhotoAdjustments) -> Unit = {},
    textSettings: CardTextSettings = CardTextSettings(),
    nameLabel: String = "",
    fromLabel: String = "With love from",
    senderLabel: String = "Purawale - Hum aur Humare",
    closingLabel: String = "",
    onLoadingStateChange: (Boolean) -> Unit = {}
) {
    var photoLoading by remember { mutableStateOf(false) }

    LaunchedEffect(photoLoading) {
        onLoadingStateChange(photoLoading)
    }

    val partnerId = remember(member) {
        if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
    }
    val partner = remember(allMembers, partnerId) { allMembers.firstOrNull { it.familyId == partnerId } }
    val displayName = remember(member, eventType, partner) {
        if (eventType == "Anniversary") {
            val firstName = member.name.split(" ").first()
            val partnerName = (member.spouseName ?: partner?.name ?: "Partner").split(" ").first()
            "$firstName & $partnerName"
        } else {
            member.name.split(" ").first()
        }
    }
    val headline = aiDesign.headline.ifBlank { "Happy" }
    val subheadline = nameLabel.ifBlank { aiDesign.subheadline.ifBlank { displayName } }
    val eventLabel = if (eventType == "Anniversary") "Anniversary" else "Birthday"
    val suffix = ordinalSuffix(years)
    val editableFont = cardTextFont(textSettings)
    val editableColor = cardTextColor(frameStyle, textSettings)
    val editableScale = textSettings.sizeScale.coerceIn(0.85f, 1.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        aiDesign.backgroundColor,
                        aiDesign.backgroundColor.copy(alpha = 0.88f),
                        aiDesign.accentColor.copy(alpha = 0.16f)
                    )
                )
            )
    ) {
        Image(
            painter = painterResource(id = frameStyle.drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 34.dp, end = 34.dp, top = 34.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = ScriptFontFamily,
                    fontSize = 25.sp,
                    color = frameStyle.primaryColor,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp
                ),
                maxLines = 1
            )

            if (years > 0) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(62.dp)
                ) {
                    Text(
                        text = years.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlayfairFontFamily,
                            fontSize = 58.sp,
                            lineHeight = 58.sp,
                            color = frameStyle.accentColor
                        )
                    )
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlayfairFontFamily,
                            fontSize = 17.sp,
                            color = frameStyle.primaryColor,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(top = 10.dp, start = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = eventLabel,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = ScriptFontFamily,
                    fontSize = 35.sp,
                    lineHeight = 36.sp,
                    color = frameStyle.primaryColor,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.offset(y = (-3).dp)
            )

            AICardOrnamentDivider(frameStyle)

            Surface(
                color = frameStyle.plateColor,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, frameStyle.accentColor.copy(alpha = 0.8f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .padding(top = 2.dp, bottom = 7.dp)
            ) {
                Text(
                    text = subheadline,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = editableFont,
                        fontSize = (27f * editableScale).sp,
                        lineHeight = (30f * editableScale).sp,
                        color = editableColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(width = 232.dp, height = 144.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, frameStyle.photoBorderColor.copy(alpha = 0.45f)),
                    shadowElevation = 1.dp,
                    color = Color.White
                ) {
                    val cardPhoto = customPhotoUri ?: member.photoUrl
                    if (cardPhoto != null && cardPhoto.toString().isNotBlank()) {
                        AsyncImage(
                            model = cardPhoto,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .photoTransformGestures(
                                    adjustments = photoAdjustments,
                                    onChange = onPhotoAdjustmentsChange
                                )
                                .graphicsLayer {
                                    scaleX = photoAdjustments.scale
                                    scaleY = photoAdjustments.scale
                                    translationX = photoAdjustments.offsetX
                                    translationY = photoAdjustments.offsetY
                                    rotationZ = photoAdjustments.rotation
                                },
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(photoColorMatrix(photoAdjustments)),
                            onState = { photoLoading = it is AsyncImagePainter.State.Loading }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                member.name.take(1),
                                style = MaterialTheme.typography.displayLarge.copy(fontFamily = PlayfairFontFamily),
                                color = frameStyle.primaryColor.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = editableFont,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = (15f * editableScale).sp,
                    fontSize = (12f * editableScale).sp,
                    color = editableColor
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp),
                maxLines = 3
            )

            AICardOrnamentDivider(frameStyle, modifier = Modifier.padding(top = 5.dp, bottom = 2.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fromLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = editableFont,
                        color = editableColor,
                        fontSize = (10f * editableScale).sp,
                        lineHeight = (11f * editableScale).sp
                    )
                )
                Text(
                    text = senderLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = editableFont,
                        fontSize = (13f * editableScale).sp,
                        lineHeight = (15f * editableScale).sp,
                        color = editableColor,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Text(
                    text = closingLabel.ifBlank { "Thank you for everything!" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = editableFont,
                        fontSize = (13f * editableScale).sp,
                        lineHeight = (15f * editableScale).sp,
                        color = editableColor
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AICardBackground(aiDesign: AIDesignSpec) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val inset = 12.dp.toPx()
        val gold = aiDesign.accentColor
        val rose = Color(0xFFC87575)
        val leaf = Color(0xFF6D7A45)

        drawRect(
            color = Color(0xFF8B6B2E).copy(alpha = 0.12f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, h)
        )

        drawFloralCorner(44.dp.toPx(), 52.dp.toPx(), 1f, 1f, rose, leaf, gold)
        drawFloralCorner(w - 44.dp.toPx(), 52.dp.toPx(), -1f, 1f, rose, leaf, gold)
        drawFloralCorner(48.dp.toPx(), h - 54.dp.toPx(), 1f, -1f, rose, leaf, gold)
        drawFloralCorner(w - 48.dp.toPx(), h - 54.dp.toPx(), -1f, -1f, rose, leaf, gold)

        val smallHearts = listOf(
            androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.08f),
            androidx.compose.ui.geometry.Offset(w * 0.74f, h * 0.08f),
            androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.52f),
            androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.52f)
        )
        smallHearts.forEach { pos ->
            drawCircle(rose.copy(alpha = 0.55f), 3.5.dp.toPx(), pos)
            drawCircle(rose.copy(alpha = 0.55f), 3.5.dp.toPx(), androidx.compose.ui.geometry.Offset(pos.x + 5.dp.toPx(), pos.y))
            drawCircle(rose.copy(alpha = 0.45f), 4.dp.toPx(), androidx.compose.ui.geometry.Offset(pos.x + 2.5.dp.toPx(), pos.y + 4.dp.toPx()))
        }

        drawRect(
            color = aiDesign.primaryColor,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(w - 2 * inset, h - 2 * inset),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = aiDesign.primaryColor.copy(alpha = 0.32f),
            topLeft = androidx.compose.ui.geometry.Offset(inset + 5.dp.toPx(), inset + 5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(w - 2 * (inset + 5.dp.toPx()), h - 2 * (inset + 5.dp.toPx())),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
        )

        drawLine(gold.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset(inset + 8.dp.toPx(), inset + 8.dp.toPx()), androidx.compose.ui.geometry.Offset(w - inset - 8.dp.toPx(), inset + 8.dp.toPx()), 0.8.dp.toPx())
        drawLine(gold.copy(alpha = 0.65f), androidx.compose.ui.geometry.Offset(inset + 8.dp.toPx(), h - inset - 8.dp.toPx()), androidx.compose.ui.geometry.Offset(w - inset - 8.dp.toPx(), h - inset - 8.dp.toPx()), 0.8.dp.toPx())
    }
}

@Composable
private fun AICardPhotoFrame(frameStyle: CardFrameStyle, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val frameColor = frameStyle.photoBorderColor
        val accent = frameStyle.accentColor
        val stroke = 3.dp.toPx()
        val notch = 12.dp.toPx()
        val frame = Path().apply {
            moveTo(notch, 0f)
            lineTo(size.width - notch, 0f)
            quadraticTo(size.width, 0f, size.width, notch)
            lineTo(size.width, size.height - notch)
            quadraticTo(size.width, size.height, size.width - notch, size.height)
            lineTo(notch, size.height)
            quadraticTo(0f, size.height, 0f, size.height - notch)
            lineTo(0f, notch)
            quadraticTo(0f, 0f, notch, 0f)
            close()
        }
        drawPath(frame, Color(0xFFFFF8E7).copy(alpha = 0.78f))
        drawPath(frame, frameColor.copy(alpha = 0.92f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawPath(frame, accent.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
    }
}

@Composable
private fun AICardOrnamentDivider(frameStyle: CardFrameStyle, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.62f)
            .height(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = frameStyle.accentColor.copy(alpha = 0.55f))
        Text(
            text = "♥",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                color = frameStyle.primaryColor,
                lineHeight = 12.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = frameStyle.accentColor.copy(alpha = 0.55f))
    }
}

@Composable
private fun FrameChoice(
    frameStyle: CardFrameStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 92.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    }
                )
        ) {
            Image(
                painter = painterResource(id = frameStyle.drawableRes),
                contentDescription = frameStyle.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = frameStyle.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun PhotoEditControls(
    adjustments: CardPhotoAdjustments,
    onChange: (CardPhotoAdjustments) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Photo edits", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onReset) { Text("Reset") }
            }
            Text(
                "Drag the photo to pan. Pinch inside the photo to zoom.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LabeledSlider(
                label = "Zoom",
                value = adjustments.scale,
                valueRange = 0.8f..3f,
                onValueChange = { onChange(adjustments.copy(scale = it)) }
            )
            LabeledSlider(
                label = "Rotation",
                value = adjustments.rotation,
                valueRange = -180f..180f,
                onValueChange = { onChange(adjustments.copy(rotation = it)) }
            )
            LabeledSlider(
                label = "Brightness",
                value = adjustments.brightness,
                valueRange = -0.35f..0.35f,
                onValueChange = { onChange(adjustments.copy(brightness = it)) }
            )
            LabeledSlider(
                label = "Contrast",
                value = adjustments.contrast,
                valueRange = 0.65f..1.65f,
                onValueChange = { onChange(adjustments.copy(contrast = it)) }
            )
            LabeledSlider(
                label = "Saturation",
                value = adjustments.saturation,
                valueRange = 0f..1.8f,
                onValueChange = { onChange(adjustments.copy(saturation = it)) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onChange(adjustments.copy(rotation = (adjustments.rotation - 90f).normalizeRotation())) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rotate left")
                }
                OutlinedButton(
                    onClick = { onChange(adjustments.copy(rotation = (adjustments.rotation + 90f).normalizeRotation())) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Rotate right")
                }
            }
        }
    }
}

@Composable
private fun TextStyleControls(
    frameStyle: CardFrameStyle,
    settings: CardTextSettings,
    onChange: (CardTextSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Editable text style", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("Script", "Serif", "Classic").forEachIndexed { index, label ->
                FilterChip(
                    selected = settings.fontIndex == index,
                    onClick = { onChange(settings.copy(fontIndex = index)) },
                    label = { Text(label) }
                )
            }
        }
        LabeledSlider(
            label = "Text size",
            value = settings.sizeScale,
            valueRange = 0.85f..1.2f,
            onValueChange = { onChange(settings.copy(sizeScale = it)) }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            cardTextColorOptions(frameStyle).forEachIndexed { index, option ->
                FilterChip(
                    selected = settings.colorIndex == index,
                    onClick = { onChange(settings.copy(colorIndex = index)) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(option.second, RoundedCornerShape(7.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(option.first)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(String.format("%.2f", value), style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

private fun Modifier.photoTransformGestures(
    adjustments: CardPhotoAdjustments,
    onChange: (CardPhotoAdjustments) -> Unit
): Modifier = pointerInput(adjustments) {
    detectTransformGestures { _, pan, zoom, rotation ->
        onChange(
            adjustments.copy(
                scale = (adjustments.scale * zoom).coerceIn(0.8f, 3f),
                offsetX = (adjustments.offsetX + pan.x).coerceIn(-220f, 220f),
                offsetY = (adjustments.offsetY + pan.y).coerceIn(-160f, 160f),
                rotation = (adjustments.rotation + rotation).normalizeRotation()
            )
        )
    }
}

private fun photoColorMatrix(adjustments: CardPhotoAdjustments): ColorMatrix {
    val c = adjustments.contrast
    val b = adjustments.brightness * 255f
    val translate = 128f * (1f - c) + b
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, translate,
            0f, c, 0f, 0f, translate,
            0f, 0f, c, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val saturationMatrix = ColorMatrix().apply { setToSaturation(adjustments.saturation) }
    contrastMatrix.timesAssign(saturationMatrix)
    return contrastMatrix
}

private fun cardTextFont(settings: CardTextSettings): FontFamily {
    return when (settings.fontIndex) {
        0 -> ScriptFontFamily
        1 -> PlayfairFontFamily
        else -> FontFamily.Serif
    }
}

private fun cardTextColor(frameStyle: CardFrameStyle, settings: CardTextSettings): Color {
    val options = cardTextColorOptions(frameStyle)
    return options.getOrElse(settings.colorIndex.coerceIn(0, options.lastIndex)) { options.first() }.second
}

private fun cardTextColorOptions(frameStyle: CardFrameStyle): List<Pair<String, Color>> {
    val isDarkFrame = frameStyle.name == "Burgundy" || frameStyle.name == "Navy"
    return listOf(
        "Default" to frameStyle.secondaryColor,
        "Main" to frameStyle.primaryColor,
        "Gold" to frameStyle.accentColor,
        if (isDarkFrame) "Ivory" to Color(0xFFFFF3D4) else "Maroon" to Color(0xFF7A121C)
    )
}

private fun Float.normalizeRotation(): Float {
    return when {
        this > 180f -> this - 360f
        this < -180f -> this + 360f
        else -> this
    }.coerceIn(-180f, 180f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFloralCorner(
    x: Float,
    y: Float,
    sx: Float,
    sy: Float,
    rose: Color,
    leaf: Color,
    gold: Color
) {
    fun ox(dp: Int) = x + sx * dp.dp.toPx()
    fun oy(dp: Int) = y + sy * dp.dp.toPx()

    val blooms = listOf(
        Triple(0, 0, 16),
        Triple(24, 16, 10),
        Triple(-18, 22, 9)
    )
    blooms.forEach { (dx, dy, radiusDp) ->
        val center = androidx.compose.ui.geometry.Offset(ox(dx), oy(dy))
        drawCircle(rose.copy(alpha = 0.28f), (radiusDp + 8).dp.toPx(), center)
        drawCircle(rose.copy(alpha = 0.68f), radiusDp.dp.toPx(), center)
        drawCircle(Color(0xFFFFC8B8).copy(alpha = 0.76f), (radiusDp * 0.58f).dp.toPx(), center)
        drawCircle(gold.copy(alpha = 0.72f), (radiusDp * 0.18f).dp.toPx(), center)
    }

    val leaves = listOf(
        16 to -22,
        34 to -6,
        -24 to -8,
        -34 to 13,
        8 to 31,
        36 to 34
    )
    leaves.forEach { (dx, dy) ->
        drawCircle(
            leaf.copy(alpha = 0.46f),
            5.dp.toPx(),
            androidx.compose.ui.geometry.Offset(ox(dx), oy(dy))
        )
    }

    drawLine(
        gold.copy(alpha = 0.35f),
        androidx.compose.ui.geometry.Offset(x, y),
        androidx.compose.ui.geometry.Offset(ox(42), oy(42)),
        1.dp.toPx()
    )
}

private fun ordinalSuffix(years: Int): String {
    return when {
        years <= 0 -> ""
        years % 10 == 1 && years % 100 != 11 -> "st"
        years % 10 == 2 && years % 100 != 12 -> "nd"
        years % 10 == 3 && years % 100 != 13 -> "rd"
        else -> "th"
    }
}

private fun parseAiColor(value: String?, fallback: Color): Color {
    return try {
        if (value.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(value.trim()))
    } catch (_: Exception) {
        fallback
    }
}

private fun defaultAiDesign(member: Member, eventType: String, allMembers: List<Member>): AIDesignSpec {
    val displayName = defaultCardNameLabel(member, eventType, allMembers)
    return AIDesignSpec(
        headline = "Happy $eventType",
        subheadline = displayName,
        message = defaultWish(member, eventType),
        primaryColor = if (eventType == "Birthday") Color(0xFF8B3A62) else Color(0xFF7A4B00),
        secondaryColor = Color(0xFF4A2D20),
        backgroundColor = Color(0xFFFFF8E7),
        accentColor = if (eventType == "Birthday") Color(0xFFD4AF37) else Color(0xFFB76E79),
        motif = if (eventType == "Birthday") "festive" else "floral",
        fontVibe = "script",
        layout = "portrait-frame",
        photoTreatment = "ornate-frame"
    )
}

private fun defaultCardNameLabel(member: Member, eventType: String, allMembers: List<Member>): String {
    val firstName = member.name.split(" ").first()
    return if (eventType == "Anniversary") {
        val partnerId = if (member.familyId.endsWith("0")) member.familyId.dropLast(1) else member.familyId + "0"
        val partner = allMembers.firstOrNull { it.familyId == partnerId }
        "$firstName & ${(member.spouseName ?: partner?.name ?: "Partner").split(" ").first()}"
    } else {
        firstName
    }
}

private fun defaultWish(member: Member, eventType: String): String {
    val firstName = member.name.split(" ").first()
    return if (eventType == "Anniversary") {
        "With warm wishes from Purawale - Hum aur Humare, may your togetherness keep glowing beautifully."
    } else {
        "Dear $firstName, Purawale - Hum aur Humare wishes you joy, health, laughter, and a beautiful year ahead."
    }
}

suspend fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap, fileName: String) {
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CircleBirthdays")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollection, contentValues)

        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
    }
}
