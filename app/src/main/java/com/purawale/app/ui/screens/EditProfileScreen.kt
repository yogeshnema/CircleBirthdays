package com.purawale.app.ui.screens

import com.purawale.app.ui.components.AddressPickerModal
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.purawale.app.*
import com.purawale.app.R
import com.purawale.app.ui.theme.LightGolden
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    member: Member?,
    currentUser: Member,
    isReadOnly: Boolean = false,
    onSave: (Member, Uri?) -> Unit,
    onCancel: () -> Unit,
    onHome: () -> Unit,
    onRequestOverride: (String) -> Unit = {}
) {
    val isSelf = member?.id == currentUser.id
    val isAdminOrEditor = currentUser.isAdmin || currentUser.isEditor
    
    // Parent check: Father/Mother can edit their children's data
    val isParent = member?.let { m ->
        val targetBaseId = if (m.familyId.endsWith("0")) m.familyId.dropLast(1) else m.familyId
        val parentBaseId = if (targetBaseId.length > 1) targetBaseId.dropLast(1)
                           else if (targetBaseId.length == 1 && targetBaseId != "P") "P"
                           else ""
        parentBaseId.isNotEmpty() && (currentUser.familyId == parentBaseId || currentUser.familyId == parentBaseId + "0")
    } ?: false

    val canEditAll = !isReadOnly && (isAdminOrEditor || member == null || isParent)
    
    val canEditPhone = !isReadOnly && (canEditAll || isSelf)
    val canEditEmail = !isReadOnly && (canEditAll || isSelf)
    val canEditLocation = !isReadOnly && (canEditAll || isSelf)
    val canEditPhoto = !isReadOnly && (canEditAll || isSelf)
    val canEditAddress = !isReadOnly && (canEditAll || isSelf)
    val canEditFixed = !isReadOnly && canEditAll
    val canEditSpouseParents = !isReadOnly && (canEditAll || isSelf)
    val canEditSocials = !isReadOnly && (canEditAll || isSelf)

    var name by remember(member) { mutableStateOf(member?.name ?: "") }
    var gender by remember(member) { mutableStateOf(member?.gender ?: "") }
    var familyId by remember(member) { mutableStateOf(member?.familyId ?: "") }
    var phone by remember(member) { mutableStateOf(member?.phoneNumber ?: "") }
    var email by remember(member) { mutableStateOf(member?.email ?: "") }
    var location by remember(member) { mutableStateOf(member?.location ?: "") }
    var dob by remember(member) { mutableStateOf(member?.dateOfBirth ?: "") }
    var spouse by remember(member) { mutableStateOf(member?.spouseName ?: "") }
    var father by remember(member) { mutableStateOf(member?.fatherName ?: "") }
    var mother by remember(member) { mutableStateOf(member?.motherName ?: "") }
    var marriageDate by remember(member) { mutableStateOf(member?.marriageDate ?: "") }
    var immediateFamily by remember(member) { mutableStateOf(member?.immediateFamily ?: "") }
    var address by remember(member) { mutableStateOf(member?.address ?: "") }
    var latitude by remember(member) { mutableStateOf(member?.latitude) }
    var longitude by remember(member) { mutableStateOf(member?.longitude) }
    var flatNumber by remember(member) { mutableStateOf(member?.flatNumber ?: "") }
    var floor by remember(member) { mutableStateOf(member?.floor ?: "") }
    var landmark by remember(member) { mutableStateOf(member?.landmark ?: "") }
    var bereavementDate by remember(member) { mutableStateOf(member?.bereavementDate ?: "") }
    var photoUrl by remember(member) { mutableStateOf(member?.photoUrl ?: "") }
    var relationship by remember(member) { mutableStateOf(member?.relationship ?: "") }
    var facebookUrl by remember(member) { mutableStateOf(member?.facebookUrl ?: "") }
    var instagramUrl by remember(member) { mutableStateOf(member?.instagramUrl ?: "") }
    var youtubeUrl by remember(member) { mutableStateOf(member?.youtubeUrl ?: "") }

    val isSpouse = familyId.trim().endsWith("0")
    var isEditor by remember(member) { mutableStateOf(member?.isEditor ?: false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val editPhotoTitle = t("Edit Photo", "फोटो संपादित करें")

    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            selectedUri = result.uriContent
            photoUrl = result.uriContent.toString()
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
                    cropShape = CropImageView.CropShape.OVAL,
                    fixAspectRatio = true,
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    imageSourceIncludeCamera = false,
                    showProgressBar = true,
                    activityMenuIconColor = android.graphics.Color.BLACK,
                    activityTitle = editPhotoTitle
                )
            )
            cropLauncher.launch(cropOptions)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        when {
                            isReadOnly -> t("View Profile", "प्रोफ़ाइल देखें")
                            member == null -> t("Add Profile", "प्रोफ़ाइल जोड़ें")
                            canEditAll || isSelf -> t("Edit Profile", "प्रोफ़ाइल संपादित करें")
                            else -> t("View Profile", "प्रोफ़ाइल देखें")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF3E2723),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onCancel) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, t("Back", "पीछे"), tint = Color(0xFF3E2723)) 
                    } 
                },
                actions = {
                    IconButton(onClick = onHome) { 
                        Icon(Icons.Default.Home, t("Home", "मुख्य पृष्ठ"), tint = Color(0xFF3E2723)) 
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LightGolden)
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Profile Photo Card
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9)),
                border = BorderStroke(2.dp, Color(0xFF5D4037)),
                modifier = Modifier.align(Alignment.CenterHorizontally).size(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = canEditPhoto) { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUrl.isNotEmpty()) {
                        Box {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = t("Profile Photo", "प्रोफ़ाइल फोटो"),
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = android.R.drawable.ic_menu_report_image)
                            )
                            if (canEditPhoto) {
                                Row(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            val cropOptions = CropImageContractOptions(
                                                uri = photoUrl.toUri(),
                                                cropImageOptions = CropImageOptions(
                                                    guidelines = CropImageView.Guidelines.ON,
                                                    cropShape = CropImageView.CropShape.OVAL,
                                                    fixAspectRatio = true,
                                                    aspectRatioX = 1,
                                                    aspectRatioY = 1,
                                                    imageSourceIncludeCamera = false,
                                                    showProgressBar = true,
                                                    activityMenuIconColor = android.graphics.Color.BLACK,
                                                    activityTitle = editPhotoTitle
                                                )
                                            )
                                            cropLauncher.launch(cropOptions)
                                        },
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Icon(Icons.Default.Edit, t("Edit", "संपादित करें"), tint = Color.White, modifier = Modifier.padding(6.dp))
                                    }
                                    Surface(
                                        onClick = { photoUrl = ""; selectedUri = null },
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Icon(Icons.Default.Delete, t("Delete", "हटाएं"), tint = Color.White, modifier = Modifier.padding(6.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (canEditPhoto) Icons.Default.AddAPhoto else Icons.Default.Person, 
                                contentDescription = "Photo", 
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF5D4037).copy(alpha = 0.5f)
                            )
                            if (canEditPhoto) {
                                Text(t("Add Photo", "फोटो जोड़ें"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D4037).copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = name, onValueChange = { name = it }, label = t("Full Name", "पूरा नाम"), enabled = canEditFixed, icon = Icons.Default.Person)
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wc, null, tint = Color(0xFF5D4037), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        for (option in listOf("Male", "Female", "Other")) {
                            val isSelected = when (option) {
                                "Male" -> gender.equals("Male", ignoreCase = true) || gender.equals("M", ignoreCase = true)
                                "Female" -> gender.equals("Female", ignoreCase = true) || gender.equals("F", ignoreCase = true)
                                else -> gender == option
                            }
                            val displayOption = when(option) {
                                "Male" -> t("Male", "पुरुष")
                                "Female" -> t("Female", "महिला")
                                else -> t("Other", "अन्य")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { if (canEditFixed) gender = option }) {
                                RadioButton(
                                    selected = isSelected, 
                                    onClick = { if (canEditFixed) gender = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF5D4037), unselectedColor = Color(0xFF5D4037).copy(alpha = 0.4f))
                                )
                                Text(displayOption, color = Color(0xFF3E2723), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (currentUser.isAdmin) {
                        ModernTextField(value = familyId, onValueChange = { familyId = it }, label = t("Family ID (Admin Only)", "फैमिली आईडी (केवल एडमिन)"), enabled = isAdminOrEditor, icon = Icons.Default.Badge)
                    }
                    
                    ModernTextField(
                        value = phone, 
                        onValueChange = { if (it.all { char -> char.isDigit() }) phone = it }, 
                        label = t("Phone Number", "फ़ोन नंबर"), 
                        enabled = canEditPhone,
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Number
                    )
                    
                    ModernTextField(value = email, onValueChange = { email = it }, label = t("Email Address", "ईमेल पता"), enabled = canEditEmail, icon = Icons.Default.Email)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = dob, onValueChange = { dob = it }, label = t("Date of Birth (YYYY-MM-DD)", "जन्म तिथि (YYYY-MM-DD)"), enabled = canEditFixed, icon = Icons.Default.Cake)
                    ModernTextField(value = spouse, onValueChange = { spouse = it }, label = t("Spouse Name", "जीवनसाथी का नाम"), enabled = canEditFixed, icon = Icons.Default.Favorite)
                    ModernTextField(value = father, onValueChange = { father = it }, label = if (isSpouse) t("Father Name", "पिता का नाम") else t("Father Name (Inferred)", "पिता का नाम (अनुमानित)"), enabled = canEditSpouseParents, icon = Icons.Default.Person)
                    ModernTextField(value = mother, onValueChange = { mother = it }, label = if (isSpouse) t("Mother Name", "माता का नाम") else t("Mother Name (Inferred)", "माता का नाम (अनुमानित)"), enabled = canEditSpouseParents, icon = Icons.Default.Person)
                    ModernTextField(value = marriageDate, onValueChange = { marriageDate = it }, label = t("Marriage Anniversary (YYYY-MM-DD)", "शादी की सालगिरह (YYYY-MM-DD)"), enabled = canEditFixed, icon = Icons.Default.Celebration)
                    ModernTextField(value = location, onValueChange = { location = it }, label = t("Location (City)", "स्थान (शहर)"), enabled = canEditLocation, icon = Icons.Default.LocationOn)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernTextField(
                            value = address, 
                            onValueChange = { address = it }, 
                            label = t("Address", "पता"), 
                            enabled = canEditAddress, 
                            icon = Icons.Default.Home
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                ModernTextField(value = flatNumber, onValueChange = { flatNumber = it }, label = t("Flat/House No.", "फ्लैट/घर नंबर"), enabled = canEditAddress)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ModernTextField(value = floor, onValueChange = { floor = it }, label = t("Floor", "मंजिल"), enabled = canEditAddress)
                            }
                        }
                        ModernTextField(value = landmark, onValueChange = { landmark = it }, label = t("Landmark", "लैंडमार्क"), enabled = canEditAddress, icon = Icons.Default.Flag)

                        if (canEditAddress || address.isNotBlank() || location.isNotBlank() || (latitude != null && longitude != null)) {
                            var showMap by remember { mutableStateOf(false) }
                            val context = LocalContext.current
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { 
                                        if (canEditAddress) {
                                            showMap = true 
                                        } else {
                                            val query = address.ifBlank { location }
                                            val uriString = if (latitude != null && longitude != null) {
                                                "geo:$latitude,$longitude?q=" + Uri.encode(query)
                                            } else if (query.isNotBlank()) {
                                                "geo:0,0?q=" + Uri.encode(query)
                                            } else null

                                            uriString?.let {
                                                val gmmIntentUri = Uri.parse(it)
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                context.startActivity(mapIntent)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF5D4037))
                                ) {
                                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (canEditAddress) t("Locate on Map", "मैप पर खोजें") 
                                        else t("View on Map", "मैप पर देखें"), 
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            
                            if (showMap) {
                                AddressPickerModal(
                                    initialAddress = address,
                                    onAddressSelected = { selectedAddress, lat, lng ->
                                        address = selectedAddress
                                        latitude = lat
                                        longitude = lng
                                        showMap = false
                                    },
                                    onDismiss = { showMap = false }
                                )
                            }
                        }
                    }

                    ModernTextField(value = immediateFamily, onValueChange = { immediateFamily = it }, label = t("Immediate Family (e.g. Children)", "निकटतम परिवार (जैसे बच्चे)"), enabled = canEditFixed, icon = Icons.Default.Groups)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val relationships = listOf("Dadaji", "Bade Dadaji", "Chote Dadaji", "Dadi", "Badi Dadi", "Choti Dadi", "Nana", "Bade Nana", "Chote Nana", "Nani", "Badi Nani", "Choti Nani", "Papa", "Mummy", "Bade Papa", "Badi Amma", "Chachaji", "Chachiji", "Bade Mamaji", "Chote Mamaji", "Badi Mamiji", "Choti Mamiji", "Bhaiya", "Bhabhi", "Didi", "Jijaji", "Bade Mausa", "Chote Mausa", "Badi Mausi", "Choti Mausi", "Bade Fufa", "Chote Fufa", "Badi Bua", "Choti Bua", "Bhatija", "Bhatiji", "Bhanja", "Bhanji", "Beta", "Beti", "Pota", "Poti", "Nati", "Natin", "Bahu", "Damaad", "Sasurji", "Saasuma", "Devar", "Devrani", "Jeth", "Jethani", "Nanad", "Saala", "Saali")
            var relExpanded by remember { mutableStateOf(false) }
            val canEditRel = currentUser.phoneNumber == AppConfig.ADMIN_PHONE || isAdminOrEditor

            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("Relationship", "संबंध"), style = MaterialTheme.typography.labelMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(
                        expanded = if ((canEditRel || (!isAdminOrEditor && member != null)) && !isReadOnly) relExpanded else false,
                        onExpandedChange = { if ((canEditRel || (!isAdminOrEditor && member != null)) && !isReadOnly) relExpanded = !relExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = relationship,
                            onValueChange = { if (canEditRel && !isReadOnly) relationship = it },
                            label = { Text(t("Select Relationship", "संबंध चुनें"), color = Color(0xFF5D4037).copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { if ((canEditRel || (!isAdminOrEditor && member != null)) && !isReadOnly) ExposedDropdownMenuDefaults.TrailingIcon(expanded = relExpanded) },
                            enabled = (canEditRel || (!isAdminOrEditor && member != null)) && !isReadOnly,
                            readOnly = !canEditRel || isReadOnly,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF3E2723),
                                unfocusedTextColor = Color(0xFF3E2723),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color(0xFF5D4037),
                                unfocusedIndicatorColor = Color(0xFF5D4037).copy(alpha = 0.3f)
                            )
                        )
                        if (canEditRel || (!isAdminOrEditor && member != null)) {
                            ExposedDropdownMenu(
                                expanded = relExpanded,
                                onDismissRequest = { relExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                for (rel in relationships) {
                                    DropdownMenuItem(
                                        text = { Text(rel, color = Color(0xFF3E2723)) },
                                        onClick = {
                                            if (canEditRel) {
                                                relationship = rel
                                            } else if (member != null && !isReadOnly) {
                                                onRequestOverride(rel)
                                            }
                                            relExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (!isAdminOrEditor && member != null && !isReadOnly) {
                        Text(
                            t("Request a relationship update if the auto-detected label is incorrect.", "यदि स्वतः पहचाना गया लेबल गलत है तो संबंध अपडेट के लिए अनुरोध करें।"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5D4037).copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ThemedInputContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModernTextField(value = facebookUrl, onValueChange = { facebookUrl = it }, label = t("Facebook Profile URL", "फेसबुक प्रोफाइल लिंक"), enabled = canEditSocials, icon = Icons.Default.Link)
                    ModernTextField(value = instagramUrl, onValueChange = { instagramUrl = it }, label = t("Instagram Username", "इंस्टाग्राम यूजरनाम"), enabled = canEditSocials, icon = Icons.Default.Link)
                    ModernTextField(value = youtubeUrl, onValueChange = { youtubeUrl = it }, label = t("YouTube Channel URL", "यूट्यूब चैनल लिंक"), enabled = canEditSocials, icon = Icons.Default.Link)
                    
                    // Social Icons Preview
                    if (facebookUrl.isNotBlank() || instagramUrl.isNotBlank() || youtubeUrl.isNotBlank() || phone.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val context = LocalContext.current
                            if (phone.isNotBlank()) {
                                IconButton(onClick = { safeOpenUri(context, "https://wa.me/${phone.filter { it.isDigit() }}") }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_whatsapp), contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(28.dp))
                                }
                            }
                            if (facebookUrl.isNotBlank()) {
                                IconButton(onClick = { safeOpenUri(context, facebookUrl) }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_facebook), contentDescription = "Facebook", tint = Color(0xFF1877F2), modifier = Modifier.size(28.dp))
                                }
                            }
                            if (instagramUrl.isNotBlank()) {
                                IconButton(onClick = { safeOpenUri(context, instagramUrl) }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_instagram), contentDescription = "Instagram", tint = Color(0xFFE4405F), modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (currentUser.isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                ThemedInputContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ModernTextField(value = bereavementDate, onValueChange = { bereavementDate = it }, label = t("Bereavement Date (Admin Only)", "शोक की तिथि (केवल एडमिन)"), enabled = isAdminOrEditor, icon = Icons.Default.History)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isEditor, 
                                onCheckedChange = { isEditor = it }, 
                                enabled = isAdminOrEditor,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5D4037), uncheckedColor = Color(0xFF5D4037).copy(alpha = 0.4f))
                            )
                            Text(t("Grant Editor Access", "संपादक पहुंच प्रदान करें"), color = Color(0xFF3E2723), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isReadOnly) {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val updatedMember = (member ?: Member(id = UUID.randomUUID().toString())).copy(
                            name = name, gender = gender, familyId = familyId, phoneNumber = phone, email = email,
                            location = location, dateOfBirth = dob, spouseName = spouse, marriageDate = marriageDate,
                            fatherName = father, motherName = mother, immediateFamily = immediateFamily,
                            address = address, latitude = latitude, longitude = longitude,
                            flatNumber = flatNumber, floor = floor, landmark = landmark,
                            bereavementDate = bereavementDate, photoUrl = photoUrl,
                            relationship = relationship, facebookUrl = facebookUrl, instagramUrl = instagramUrl,
                            youtubeUrl = youtubeUrl, isEditor = isEditor
                        )
                        onSave(updatedMember, selectedUri)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037), contentColor = Color.White)
                ) {
                    Text(t("SAVE PROFILE", "प्रोफ़ाइल सहेजें"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ThemedInputContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEBE9).copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, Color(0xFF5D4037).copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, color = Color(0xFF5D4037).copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = icon?.let { { Icon(it, null, tint = Color(0xFF5D4037).copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) } },
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color(0xFF3E2723),
            unfocusedTextColor = Color(0xFF3E2723),
            disabledTextColor = Color(0xFF3E2723).copy(alpha = 0.5f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = Color(0xFF5D4037),
            focusedIndicatorColor = Color(0xFF5D4037),
            unfocusedIndicatorColor = Color(0xFF5D4037).copy(alpha = 0.1f),
            disabledIndicatorColor = Color(0xFF5D4037).copy(alpha = 0.05f)
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    )
}