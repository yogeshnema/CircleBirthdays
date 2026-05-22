package com.purawale.app.ui.screens

import android.widget.Toast
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
fun CalendarScreen(allMembers: List<Member>, currentUser: Member, onBack: () -> Unit) {
    val context = LocalContext.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }

    val brown900 = Color(0xFF3E2723)
    val brown800 = Color(0xFF5D4037)
    val brown50 = Color(0xFFEFEBE9)
    
    // Filter members based on role
    val visibleMembers = remember(allMembers, currentUser) {
        if (currentUser.isAdmin) allMembers else allMembers.filter { !it.isAdmin || it.id == currentUser.id }
    }

    val isHindi = LocalLanguage.current
    var panchangMap by remember { mutableStateOf(generatePanchangForMonth(currentMonth, isHindi)) }
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentMonth, isHindi) {
        panchangMap = generatePanchangForMonth(currentMonth, isHindi)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("Hindu Calendar", "हिंदू कैलेंडर"), color = brown900, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brown900)
                    }
                },
                actions = {
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
                                Toast.makeText(context, "Calendar synced successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No festival data found for this month", Toast.LENGTH_SHORT).show()
                            }
                            isSyncing = false
                        }
                    }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = brown900, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = brown900)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
            )
        },
        containerColor = brown50
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // TOP HALF: Calendar Grid (Squeezed)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Month Selector - Very Compact
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = brown800, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        color = brown900,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ChevronRight, null, tint = brown800, modifier = Modifier.size(20.dp))
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
                            color = brown800.copy(alpha = 0.7f),
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
                                    val birthdays = visibleMembers.filter { isSameDay(it.dateOfBirth, date) }
                                    val anniversaries = visibleMembers.filter { isSameDay(it.marriageDate, date) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) brown800
                                                else if (p?.isPanchak == true) Color(0xFFFFEBEE)
                                                else if (isToday) brown800.copy(alpha = 0.1f)
                                                else Color.White
                                            )
                                            .border(
                                                width = if (isToday) 1.dp else 0.dp,
                                                color = if (isToday) brown800 else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                                            Text(
                                                text = day.toString(),
                                                modifier = Modifier.align(Alignment.TopStart).padding(1.dp),
                                                color = if (isSelected) Color.White else if (p?.isPanchak == true) Color(0xFFC62828) else brown900,
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
                                                        .background(if (isSelected) Color.White else Color(0xFFD32F2F), CircleShape)
                                                )
                                            }

                                            if (birthdays.isNotEmpty() || anniversaries.isNotEmpty()) {
                                                Icon(
                                                    if (birthdays.isNotEmpty()) Icons.Default.Cake else Icons.Default.Favorite,
                                                    null,
                                                    modifier = Modifier.align(Alignment.BottomEnd).size(8.dp).padding(1.dp),
                                                    tint = if (isSelected) Color.White else Color(0xFFE91E63)
                                                )
                                            }

                                            if (p != null) {
                                                Text(
                                                    text = p.tithiShort,
                                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else if (p.isPanchak) Color(0xFFC62828) else brown800,
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

            HorizontalDivider(color = brown800.copy(alpha = 0.1f), thickness = 1.dp)

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
                    val dayBirthdays = visibleMembers.filter { isSameDay(it.dateOfBirth, selectedDate) }
                    val dayAnniversaries = visibleMembers.filter { isSameDay(it.marriageDate, selectedDate) }
                    
                    if (dayBirthdays.isNotEmpty() || dayAnniversaries.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    t("Family Events", "पारिवारिक कार्यक्रम"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                                dayBirthdays.forEach { member ->
                                    EventItem(member.name, t("Birthday", "जन्मदिन"), Icons.Default.Cake)
                                }
                                dayAnniversaries.forEach { member ->
                                    EventItem(member.name, t("Anniversary", "वर्षगांठ"), Icons.Default.Favorite)
                                }
                            }
                        }
                    }

                    val p = panchangMap[selectedDate]
                    if (p != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = brown900,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = brown50)
                                
                                DetailItem(t("Tithi", "तिथि"), p.tithi, Icons.Default.BrightnessLow)
                                DetailItem(t("Nakshatra", "नक्षत्र"), p.nakshatra, Icons.Default.Star)
                                DetailItem(t("Yoga", "योग"), p.yoga, Icons.Default.Sync)
                                DetailItem(t("Karana", "करण"), p.karana, Icons.Default.Info)
                                DetailItem(t("Muhurat", "मुहूर्त"), p.muhurat, Icons.Default.AccessTime)
                                DetailItem(t("Sunrise", "सूर्योदय"), p.sunrise, Icons.Default.WbSunny)
                                DetailItem(t("Sunset", "सूर्यास्त"), p.sunset, Icons.Default.BrightnessLow)
                                
                                if (p.isPanchak || !p.note.isNullOrBlank()) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                                    ) {
                                        Text(
                                            text = p.note ?: t("Panchak", "पञ्चक"),
                                            modifier = Modifier.padding(4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                
                                if (p.festivals.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(t("Festivals", "त्योहार"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                    p.festivals.forEach { festival ->
                                        Text("• $festival", style = MaterialTheme.typography.labelSmall, color = brown800, fontSize = 9.sp)
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
                    shape = RoundedCornerShape(4.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    AsyncImage(
                        model = "https://circlebirthdays.web.app/calendar/${currentMonth.monthValue}.jpg",
                        contentDescription = "Panchang Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = Color(0xFF5D4037).copy(alpha = 0.6f))
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D6E63), fontSize = 9.sp)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3E2723), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun EventItem(name: String, label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color(0xFFE65100))
        Spacer(Modifier.width(4.dp))
        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color(0xFF3E2723), fontSize = 10.sp)
        Spacer(Modifier.width(2.dp))
        Text("($label)", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
    }
}
