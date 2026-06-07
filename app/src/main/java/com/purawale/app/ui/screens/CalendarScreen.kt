package com.purawale.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purawale.app.*
import com.purawale.app.ui.components.AppTopBar
import com.purawale.app.ui.components.DatePickerField
import com.purawale.app.ui.components.ScreenContainer
import com.purawale.app.ui.theme.LightGolden
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

private fun isSameDay(dateStr: String?, targetDate: LocalDate): Boolean {
    if (dateStr.isNullOrBlank()) return false
    return try {
        if (dateStr.contains("-") || dateStr.contains("/")) {
            val parts = dateStr.split("-", "/")
            if (parts[0].length == 4) {
                val m = parts[1].toInt()
                val d = parts[2].toInt()
                m == targetDate.monthValue && d == targetDate.dayOfMonth
            } else {
                val d = parts[0].toInt()
                val m = parts[1].toInt()
                m == targetDate.monthValue && d == targetDate.dayOfMonth
            }
        } else {
            val date = LocalDate.parse(dateStr)
            date.monthValue == targetDate.monthValue && date.dayOfMonth == targetDate.dayOfMonth
        }
    } catch (_: Exception) { false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(allMembers: List<Member>, currentUser: Member, currentTreeId: String = "primary", onBack: () -> Unit) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }

    val gold = Color(0xFFFFC857)
    
    // Filter members based on role
    val visibleMembers = remember(allMembers, currentUser) {
        if (currentUser.isAdmin) allMembers else allMembers.filter { !it.isAdmin || it.id == currentUser.id }
    }

    val isHindi = LocalLanguage.current
    var panchangMap by remember { mutableStateOf(generatePanchangForMonth(currentMonth, isHindi)) }
    var isSyncing by remember { mutableStateOf(false) }
    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentMonth, isHindi) {
        panchangMap = generatePanchangForMonth(currentMonth, isHindi)
    }

    DisposableEffect(currentTreeId) {
        val listener = FirebaseManager.getCalendarEvents(currentTreeId) { events ->
            calendarEvents = events
        }
        onDispose { listener.remove() }
    }

    val calendarSyncedText = t("Calendar synced successfully", "कैलेंडर सफलतापूर्वक सिंक हो गया")
    val noFestivalDataText = t("No festival data found for this month", "इस महीने के लिए कोई त्योहार डेटा नहीं मिला")
    val invalidMapsLinkText = t("Invalid maps link", "अमान्य मैप लिंक")
    val invalidLinkText = t("Invalid link", "अमान्य लिंक")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = t("Hindu Calendar", "हिंदू कैलेंडर"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddEventDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = t("Add Event", "कार्यक्रम जोड़ें"), tint = Color.White)
                    }
                    IconButton(onClick = { 
                        scope.launch {
                            isSyncing = true
                            val scraped = scrapePanchangData(currentMonth)
                            if (scraped.isNotEmpty()) {
                                panchangMap = panchangMap.toMutableMap().apply {
                                    scraped.forEach { (date, p) ->
                                        this[date] = this[date]?.copy(festivals = p.festivals) ?: p
                                    }
                                }
                                Toast.makeText(context, calendarSyncedText, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, noFestivalDataText, Toast.LENGTH_SHORT).show()
                            }
                            isSyncing = false
                        }
                    }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = t("Sync", "सिंक करें"), tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = Color(0xFFFFC857),
                contentColor = Color(0xFF080B14),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 70.dp)
            ) { Icon(Icons.Default.Event, contentDescription = null) }
        }
    ) { padding ->
        ScreenContainer(modifier = Modifier.padding(padding)) {
            if (showAddEventDialog) {
                AddEventDialog(
                    selectedDate = selectedDate,
                    currentUser = currentUser,
                    onDismiss = { showAddEventDialog = false },
                    onConfirm = { event ->
                        scope.launch {
                            FirebaseManager.submitCalendarEvent(event, currentTreeId)
                            showAddEventDialog = false
                        }
                    }
                )
            }

            val gold = Color(0xFFFFC857)

            Column(modifier = Modifier.fillMaxSize()) {
                // TOP HALF: Calendar Grid (Squeezed)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Month Selector - Very Compact
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Days of the week headers
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Calendar Grid - Squeezed using weights to fill Top Half exactly
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value % 7
                    
                    Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        for (r in 0 until 6) { // Always show 6 rows for stability
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val day = cellIndex - firstDayOfMonth + 1
                                    if (day in 1..daysInMonth) {
                                        val date = currentMonth.atDay(day)
                                        val isSelected = date == selectedDate
                                        val isToday = date == LocalDate.now()
                                        val p = panchangMap[date]
                                        val birthdays = visibleMembers.filter { it.bereavementDate.isNullOrBlank() && isSameDay(it.dateOfBirth, date) }
                                        val anniversaries = visibleMembers.filter { it.bereavementDate.isNullOrBlank() && isSameDay(it.marriageDate, date) }
                                        val remembrances = visibleMembers.filter {
                                            !it.bereavementDate.isNullOrBlank() && (isSameDay(it.bereavementDate, date) || isSameDay(it.dateOfBirth, date))
                                        }
                                        val customEvents = calendarEvents.filter { it.date == date.toString() }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(1.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSelected) gold
                                                    else if (p?.isPanchak == true) Color(0xFFFF5252).copy(alpha = 0.15f)
                                                    else if (isToday) Color.White.copy(alpha = 0.15f)
                                                    else Color.White.copy(alpha = 0.05f)
                                                )
                                                .border(
                                                    width = if (isToday) 1.dp else 0.dp,
                                                    color = if (isToday) gold else Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable { selectedDate = date },
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                                                Text(
                                                    text = day.toString(),
                                                    modifier = Modifier.align(Alignment.TopStart).padding(1.dp),
                                                    color = if (isSelected) Color(0xFF080B14) else if (p?.isPanchak == true) Color(0xFFFF5252) else Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp
                                                )

                                                if (p?.festivals?.isNotEmpty() == true) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                            .size(3.dp)
                                                            .background(if (isSelected) Color(0xFF080B14) else Color(0xFFFF5252), CircleShape)
                                                    )
                                                }

                                                if (birthdays.isNotEmpty() || anniversaries.isNotEmpty() || customEvents.isNotEmpty() || remembrances.isNotEmpty()) {
                                                    Icon(
                                                        if (customEvents.isNotEmpty()) Icons.Default.Event
                                                        else if (birthdays.isNotEmpty()) Icons.Default.Cake 
                                                        else if (anniversaries.isNotEmpty()) Icons.Default.Favorite
                                                        else Icons.Default.History,
                                                        null,
                                                        modifier = Modifier.align(Alignment.BottomEnd).size(8.dp).padding(1.dp),
                                                        tint = if (isSelected) Color(0xFF080B14) else if (customEvents.isNotEmpty()) Color(0xFF64B5F6) else Color(0xFFF06292)
                                                    )
                                                }

                                                if (p != null) {
                                                    Text(
                                                        text = p.tithiShort,
                                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) Color(0xFF080B14).copy(alpha = 0.9f) else if (p.isPanchak) Color(0xFFFF5252) else gold,
                                                        fontSize = 6.sp,
                                                        maxLines = 1,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // BOTTOM HALF: Divided vertically into Left (Data) and Right (Image)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Left Column: Scrollable Events & Panchang Details
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        val dayBirthdays = visibleMembers.filter { it.bereavementDate.isNullOrBlank() && isSameDay(it.dateOfBirth, selectedDate) }
                        val dayAnniversaries = visibleMembers.filter { it.bereavementDate.isNullOrBlank() && isSameDay(it.marriageDate, selectedDate) }
                        val dayRemembrances = visibleMembers.filter {
                            !it.bereavementDate.isNullOrBlank() && (isSameDay(it.bereavementDate, selectedDate) || isSameDay(it.dateOfBirth, selectedDate))
                        }
                        val dayEvents = calendarEvents.filter { it.date == selectedDate.toString() }
                        
                        if (dayBirthdays.isNotEmpty() || dayAnniversaries.isNotEmpty() || dayRemembrances.isNotEmpty() || dayEvents.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = gold.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, gold.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        t("Family Events", "पारिवारिक कार्यक्रम"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = gold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    dayBirthdays.forEach { member ->
                                        val rel = member.relationship ?: FamilyUtils.getRelationship(member, currentUser, allMembers)
                                        EventItem(member.name, t("Birthday", "जन्मदिन"), Icons.Default.Cake, rel)
                                    }
                                    dayAnniversaries.forEach { member ->
                                        val rel = member.relationship ?: FamilyUtils.getRelationship(member, currentUser, allMembers)
                                        EventItem(member.name, t("Anniversary", "वर्षगांठ"), Icons.Default.Favorite, rel)
                                    }
                                    dayRemembrances.forEach { member ->
                                        val rel = member.relationship ?: FamilyUtils.getRelationship(member, currentUser, allMembers)
                                        EventItem(member.name, t("Remembrance", "पुण्यतिथि"), Icons.Default.History, rel)
                                    }
                                    dayEvents.forEach { event ->
                                        var showDetails by remember { mutableStateOf(false) }
                                        
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable { showDetails = !showDetails },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    EventItem(event.title, event.type, Icons.Default.Event)
                                                    if (event.location.isNotEmpty()) {
                                                        Text(
                                                            text = "@ ${event.location}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 8.sp,
                                                            modifier = Modifier.padding(start = 18.dp)
                                                        )
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (event.mapsLink.isNotEmpty() || event.inviteCardUrl.isNotEmpty() || event.imageUrl.isNotEmpty() || event.description.isNotEmpty()) {
                                                        Icon(
                                                            if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            null,
                                                            modifier = Modifier.size(20.dp),
                                                            tint = Color.White.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                    if (currentUser.isAdmin) {
                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    FirebaseManager.deleteCalendarEvent(event.id, currentUser.id, currentUser.name)
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252).copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            if (showDetails) {
                                                Column(modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 4.dp)) {
                                                    if (event.description.isNotEmpty()) {
                                                        Text(event.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                                    }
                                                    
                                                    Row(modifier = Modifier.padding(top = 8.dp)) {
                                                        if (event.mapsLink.isNotEmpty()) {
                                                            Button(
                                                                onClick = {
                                                                    try {
                                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.mapsLink))
                                                                        context.startActivity(intent)
                                                                    } catch (e: Exception) {
                                                                        Toast.makeText(context, invalidMapsLinkText, Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                                modifier = Modifier.height(32.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = gold)
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(t("View on Map", "मैप पर देखें"), fontSize = 10.sp, color = Color.White)
                                                            }
                                                        }
                                                        
                                                        if (event.inviteCardUrl.isNotEmpty()) {
                                                            Spacer(Modifier.width(8.dp))
                                                            Button(
                                                                onClick = {
                                                                    try {
                                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.inviteCardUrl))
                                                                        context.startActivity(intent)
                                                                    } catch (e: Exception) {
                                                                        Toast.makeText(context, invalidLinkText, Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                                modifier = Modifier.height(32.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Icon(Icons.Default.CardMembership, null, modifier = Modifier.size(14.dp), tint = gold)
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(t("Invite Card", "निमंत्रण पत्र"), fontSize = 10.sp, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                    
                                                    if (event.inviteCardUrl.isNotEmpty()) {
                                                        AsyncImage(
                                                            model = event.inviteCardUrl,
                                                            contentDescription = "Invite Card",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 8.dp)
                                                                .heightIn(max = 200.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.inviteCardUrl))
                                                                    context.startActivity(intent)
                                                                },
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }

                                                    if (event.imageUrl.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        AsyncImage(
                                                            model = event.imageUrl,
                                                            contentDescription = "Event Photo",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(max = 200.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.imageUrl))
                                                                    context.startActivity(intent)
                                                                },
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val p = panchangMap[selectedDate]
                        if (p != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = gold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.1f))
                                    
                                    DetailItem(t("Tithi", "तिथि"), p.tithi, Icons.Default.BrightnessLow)
                                    DetailItem(t("Nakshatra", "नक्षत्र"), p.nakshatra, Icons.Default.Star)
                                    DetailItem(t("Yoga", "योग"), p.yoga, Icons.Default.Sync)
                                    DetailItem(t("Karana", "करण"), p.karana, Icons.Default.Info)
                                    DetailItem(t("Muhurat", "मुहूर्त"), p.muhurat, Icons.Default.AccessTime)
                                    DetailItem(t("Sunrise", "सूर्योदय"), p.sunrise, Icons.Default.WbSunny)
                                    DetailItem(t("Sunset", "सूर्यास्त"), p.sunset, Icons.Default.BrightnessLow)
                                    
                                    if (p.isPanchak || !p.note.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0xFFFF5252).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                                        ) {
                                            Text(
                                                text = p.note ?: t("Panchak", "पञ्चक"),
                                                modifier = Modifier.padding(4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFF5252),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    
                                    if (p.festivals.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(t("Festivals", "त्योहार"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                                        p.festivals.forEach { festival ->
                                            Text("• $festival", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Panchang Image (Embedded)
                    Card(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(top = 4.dp, bottom = 4.dp, end = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        AsyncImage(
                            model = "https://circlebirthdays.web.app/calendar/${currentMonth.monthValue}.jpg",
                            contentDescription = "Panchang Page",
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: ImageVector) {
    val gold = Color(0xFFFFC857)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = gold.copy(alpha = 0.6f))
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun EventItem(name: String, label: String, icon: ImageVector, relationship: String? = null) {
    val gold = Color(0xFFFFC857)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = gold)
        Spacer(Modifier.width(4.dp))
        val displayName = if (!relationship.isNullOrEmpty()) "$name ($relationship)" else name
        Text(displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
        Spacer(Modifier.width(2.dp))
        Text("($label)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    selectedDate: LocalDate,
    currentUser: Member,
    onDismiss: () -> Unit,
    onConfirm: (CalendarEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mapsLink by remember { mutableStateOf("") }
    var inviteCardUrl by remember { mutableStateOf("") }
    var eventImageUrl by remember { mutableStateOf("") }
    var inviteCardUri by remember { mutableStateOf<Uri?>(null) }
    var eventImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("MARRIAGE") }
    var date by remember { mutableStateOf(selectedDate.toString()) }
    val types = listOf(
        "MARRIAGE" to t("Marriage Function", "Marriage Function"),
        "GET_TOGETHER" to t("Family Get-together", "Family Get-together"),
        "COMMUNITY_GATHERING" to t("Community Gathering", "Community Gathering"),
        "COMMUNITY_SERVICE" to t("Community Service", "Community Service"),
        "GENERAL" to t("Other Event", "Other Event")
    )
    val inviteCardPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        inviteCardUri = uri
        if (uri != null) inviteCardUrl = ""
    }
    val eventPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        eventImageUri = uri
        if (uri != null) eventImageUrl = ""
    }

    val gold = Color(0xFFFFC857)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        shape = RoundedCornerShape(28.dp),
        title = { Text(t("Add Calendar Event", "कैलेंडर कार्यक्रम जोड़ें"), color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                DatePickerField(
                    label = t("Date", "तारीख"),
                    selectedDate = date,
                    onDateSelected = { date = it }
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(t("Event Type", "कार्यक्रम का प्रकार"), style = MaterialTheme.typography.labelMedium, color = gold)
                types.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { type = value }
                    ) {
                        RadioButton(
                            selected = (type == value),
                            onClick = { type = value },
                            colors = RadioButtonDefaults.colors(selectedColor = gold, unselectedColor = Color.White.copy(alpha = 0.6f))
                        )
                        Text(label, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(t("Title", "शीर्षक")) },
                    placeholder = { Text(t("Example: Rohan & Priya wedding", "उदाहरण: रोहन और प्रिया की शादी")) },
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
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(t("Venue / Location Name", "स्थान का नाम")) },
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

                OutlinedTextField(
                    value = mapsLink,
                    onValueChange = { mapsLink = it },
                    label = { Text(t("Google Maps Venue Link", "गूगल मैप्स वेन्यू लिंक")) },
                    placeholder = { Text("https://maps.app.goo.gl/...") },
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

                OutlinedTextField(
                    value = inviteCardUrl,
                    onValueChange = { inviteCardUrl = it },
                    label = { Text(t("Invite Card Image URL", "आमंत्रण कार्ड इमेज URL")) },
                    placeholder = { Text("https://...") },
                    enabled = inviteCardUri == null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { inviteCardPicker.launch("image/*") },
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = gold)
                    ) {
                        Icon(Icons.Default.CardMembership, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(t("Upload Invite Card", "निमंत्रण पत्र अपलोड करें"))
                    }
                    if (inviteCardUri != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(t("Selected", "चयनित"), style = MaterialTheme.typography.labelSmall, color = gold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = eventImageUrl,
                    onValueChange = { eventImageUrl = it },
                    label = { Text(t("Event Photo URL", "कार्यक्रम फोटो URL")) },
                    placeholder = { Text("https://...") },
                    enabled = eventImageUri == null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gold,
                        focusedLabelColor = gold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = gold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { eventPhotoPicker.launch("image/*") },
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = gold)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(t("Upload Photo", "फोटो अपलोड करें"))
                    }
                    if (eventImageUri != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(t("Selected", "चयनित"), style = MaterialTheme.typography.labelSmall, color = gold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(t("Description, timings, notes", "विवरण, समय, नोट्स")) },
                    minLines = 3,
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && !isSaving) {
                        scope.launch {
                            isSaving = true
                            try {
                                val uploadedInviteUrl = inviteCardUri?.let { FirebaseManager.uploadPhoto(it) } ?: inviteCardUrl
                                val uploadedImageUrl = eventImageUri?.let { FirebaseManager.uploadPhoto(it) } ?: eventImageUrl
                                onConfirm(
                                    CalendarEvent(
                                        title = title,
                                        description = description,
                                        location = location,
                                        mapsLink = mapsLink,
                                        inviteCardUrl = uploadedInviteUrl,
                                        imageUrl = uploadedImageUrl,
                                        date = date,
                                        type = type,
                                        createdBy = currentUser.id,
                                        createdByName = currentUser.name
                                    )
                                )
                            } catch (e: Exception) {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = title.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold,
                    contentColor = Color.Black,
                    disabledContainerColor = gold.copy(alpha = 0.3f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(t("Saving", "सहेज रहे हैं"), fontWeight = FontWeight.Bold)
                } else {
                    Text(t("Add Event", "कार्यक्रम जोड़ें"), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
