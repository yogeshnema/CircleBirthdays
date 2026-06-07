package com.purawale.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalLanguage = staticCompositionLocalOf { false } // false for English, true for Hindi

fun String.hash(): String {
    val bytes = this.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

fun safeOpenUri(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    try {
        val uri = if (!uriString.startsWith("http://") && !uriString.startsWith("https://")) {
            Uri.parse("https://$uriString")
        } else {
            Uri.parse(uriString)
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("CircleBirthdays", "Failed to open URI: $uriString", e)
    }
}

@Composable
fun t(en: String, hi: String): String {
    return if (LocalLanguage.current) hi else en
}

fun isToday(dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    val today = LocalDate.now()
    return try {
        // Handle YYYY-MM-DD
        val date = LocalDate.parse(dateStr)
        date.month == today.month && date.dayOfMonth == today.dayOfMonth
    } catch (_: Exception) {
        try {
            // Fallback for DD-MM-YYYY or DD/MM/YYYY
            val parts = dateStr.split("-", "/")
            val d = parts[0].toInt()
            val m = parts[1].toInt()
            m == today.monthValue && d == today.dayOfMonth
        } catch (_: Exception) { false }
    }
}

fun isTimestampToday(timestamp: Long?): Boolean {
    if (timestamp == null) return false
    val today = LocalDate.now()
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.isEqual(today)
}

fun formatDateToDDMMM(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val date = try {
            LocalDate.parse(dateStr)
        } catch (_: Exception) {
            val parts = dateStr.split("-", "/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = if (parts.size > 2) parts[2].toInt() else LocalDate.now().year
            LocalDate.of(year, month, day)
        }
        val day = date.dayOfMonth
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        val month = date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
        "$day$suffix $month"
    } catch (_: Exception) {
        dateStr
    }
}

fun calculateAge(dateStr: String?): Int? {
    if (dateStr.isNullOrBlank()) return null
    return try {
        val dob = try {
            LocalDate.parse(dateStr)
        } catch (_: Exception) {
            val parts = dateStr.split("-", "/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = if (parts.size > 2) parts[2].toInt() else return null
            LocalDate.of(year, month, day)
        }
        java.time.Period.between(dob, LocalDate.now()).years
    } catch (_: Exception) {
        null
    }
}

fun isMemberAdmin(memberId: String?, allMembers: List<Member>): Boolean {
    if (memberId == null) return false
    return allMembers.find { it.id == memberId }?.let { it.isAdmin || it.phoneNumber == AppConfig.ADMIN_PHONE } ?: false
}

fun isWithinSevenDays(dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    val today = LocalDate.now()
    return try {
        val date = try {
            LocalDate.parse(dateStr)
        } catch (_: Exception) {
            val parts = dateStr.split("-", "/")
            LocalDate.of(today.year, parts[1].toInt(), parts[0].toInt())
        }
        
        val thisYearEvent = date.withYear(today.year)
        val eventDate = if (thisYearEvent.isBefore(today)) {
            thisYearEvent.withYear(today.year + 1)
        } else {
            thisYearEvent
        }
        
        val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
        daysUntil in 1..7
    } catch (_: Exception) {
        false
    }
}

fun getInitialMembers(): List<Member> {
    val rawData = """
P,,ROOT PARENT,,M,1,1,1880,,
A,P,KUNJILAL JI SHIV PRASAD,,M,15,5,1907,,28-05-1990
A0,,ANNA KUNJILAL,,F,31,1,1930,,13-04-2014
A1,A,GULAB CHAND,,M,23,8,1930,**/**/****,13-02-2008
A10,,SAVITRI GULAB CHAND,,F,6,7,1937,,27-10-2012
A11,A1,VIJAY GULAB CHAND,,M,28,11,1958,02-12-1989,
A110,,MANJULA  VIJAY GULAB CHAND,,F,9,4,1968,,
A111,A11,PRACHI VIJAY GULAB CHAND,,F,21,6,1995,,
A1110,,RISHAV PRACHI VIJAY GULAB CHAND,,M,16,11,1994,,
A112,A11,VARUN VIJAY GULAB CHAND,,M,13,3,1997,,
A12,A1,SANJAY GULAB CHAND,,M,31,8,1964,22-05-1997,
A120,,DEEPTI SANJAY GULAB CHAND,,F,17,5,1973,,
A121,A12,SWAPNIL SANJAY GULAB CHAND,,M,15,5,2000,,
A122,A12,JUHI SANJAY GULAB CHAND,,F,11,2,2002,,
A2,A,HARI SHANKAR,,M,31,1,1933,17-06-1958,24-10-1984
A20,,GINDA HARI SHANKAR,,F,31,1,1935,,14-12-2008
A21,A2,MANORAMA HARI SHANKAR,,F,11,1,1960,,
A210,,SHIV KUMAR MANO HARI SHANKAR,,M,31,1,1958,,
A3,A,REVATI,,F,17,1,1937,**/**/1952,15-12-2013
A30,,LAXMI PRASAD REVATI,,M,12,12,1929,,03-06-1992
A31,A3,SANTOSH REVATI,,M,6,6,1959,12-12-1989,
A310,,NILIMA SANTOSH REVATI,,F,19,10,1960,,
A311,A31,AADITYA SANTOSH REVATI,,M,15,10,1990,05-06-2021,
A3110,,NIHARIKA AADITYA SANTOSH REVATI,,F,21,4,1992,,
A312,A31,ROHAN SANTOSH REVATI,,M,27,3,1994,,
A4,A,PURSHOTTAM DAS,,M,16,8,1939,10-6-1963,22-11-2012
A40,,KRISHNA PURUSHOTTAM,,F,1,7,1947,,24-06-2021
A41,A4,SUDHIR PURUSHOTTOM,,M,14,6,1965,07-06-1993,
A410,,NAMITA SUDHIR PURSHOTTAM,,F,24,10,1969,,
A411,A41,SHUBHAM SUDHIR PURUSHOTTAM,,M,13,10,1994,,
A412,A41,SARTHAK SUDHIR PURSHOTTAM,,M,13,9,1999,,
A42,A4,SUNITA PURUSHOTTAM,,F,15,3,1967,06-03-1990,
A4200,,DR. RAJESH SUNITA PURSHOTTAM,,M,24,11,1960,,
A421,A42,SAKET SUNITA PURSHOTTAM,,M,2,1,1992,,
A422,A42,SHRISHTI SUNITA PURUSHOTTAM,,F,24,6,1994,,
A43,A4,MANOJ PURUSHOTTAM,,M,6,5,1969,22-01-2003,
A430,,AMITA MANOJ PURSHOTTAM,,F,11,10,1979,,
A431,A43,PARV (NAMAN) MANOJ PURUSHOTTAM,,M,7,4,2007,,
A432,A43,SOUMYA MANOJ PUROSHOTTAM,,F,16,1,2011,,
A5,A,SUMAN,,F,9,1,1952,30-06-1982,
A50,,OM PRAKASH SUMAN,,M,1,11,1951,,
A51,A5,SAURABH SUMAN,,M,28,7,1983,16-02-2010,
A510,,SHANIL SAURABH SUMAN,,F,8,12,1982,,
A511,A51,SHOURISHA SAURABH SUMAN                            ,,F,18,5,2018,,
A6,A,KANTI,,F,6,3,1957,27-01-1989,
A60,,YOGESH KANTI,,M,13,5,1961,,
A61,A6,PRATISH KANTI,,M,16,11,1989,04-11-2025,
B,P,BHAGWATI SHIV PRASAD,,F,1,1,1910,,
B1,B,BIRJU BHAGWATI,,M,1,1,1960,,
B11,B1,YOGESH BIRJU,,M,1,1,1985,,
C,P,CHANDRA SHIV PRASAD,,F,1,1,1915,,
D,P,DURGA SHIV PRASAD,,F,1,1,1920,,
E,P,EKLAVYA SHIV PRASAD,,M,1,1,1925,,
F,P,FALGUNI SHIV PRASAD,,F,1,1,1930,,
G,P,RAMCHARAN SHIV PRASAD,,M,28,10,1933,13-05-1958,27-12-2008
G0,,GYAN RANJANI RAMCHARAN,,F,2,9,1942,,28-03-2020
""".trimIndent()
    
    val admin = Member(id = "admin", name = AppConfig.ADMIN_NAME, dateOfBirth = "1970-01-01", phoneNumber = AppConfig.ADMIN_PHONE, isAdmin = true)
    return listOf(admin) + CsvHelper.parse(rawData)
}

data class DayPanchang(
    val tithi: String,
    val tithiShort: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String,
    val muhurat: String,
    val festivals: List<String> = emptyList(),
    val isPanchak: Boolean = false,
    val note: String? = null,
    val sunrise: String = "06:15 AM",
    val sunset: String = "06:45 PM"
)

fun generatePanchangForMonth(month: YearMonth, isHindi: Boolean = false): Map<LocalDate, DayPanchang> {
    val result = mutableMapOf<LocalDate, DayPanchang>()
    
    val tithisEn = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashti", "Saptami", "Ashtami", "Navami", "Dashami", 
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima", 
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami", "Shashti", "Saptami", "Ashtami", "Navami", "Dashami", 
        "Ekadashi", "Dwadashi", "Trayodashi", "Chaturdashi", "Amavasya"
    )
    val tithisHi = listOf(
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पंचमी", "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी", 
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "पूर्णिमा", 
        "प्रतिपदा", "द्वितीया", "तृतीया", "चतुर्थी", "पंचमी", "षष्ठी", "सप्तमी", "अष्टमी", "नवमी", "दशमी", 
        "एकादशी", "द्वादशी", "त्रयोदशी", "चतुर्दशी", "अमावस्या"
    )
    
    val nakshatrasEn = listOf("Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashirsha", "Ardra", "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati")
    val nakshatrasHi = listOf("अश्विनी", "भरणी", "कृत्तिका", "रोहिणी", "मृगशिरा", "आर्द्रा", "पुनर्वसु", "पुष्य", "अश्लेषा", "मघा", "पूर्वा फाल्गुनी", "उत्तरा फाल्गुनी", "हस्त", "चित्रा", "स्वाती", "विशाखा", "अनुराधा", "ज्येष्ठा", "मूल", "पूर्वाषाढ़ा", "उत्तराषाढ़ा", "श्रवण", "धनिष्ठा", "शतभिषा", "पूर्वाभाद्रपद", "उत्तराभाद्रपद", "रेवती")

    val yogasEn = listOf("Vishkumbha", "Priti", "Ayushman", "Saubhagya", "Shobhana", "Atiganda", "Sukarma", "Dhriti", "Shula", "Ganda", "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra", "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva", "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma", "Indra", "Vaidhriti")
    val yogasHi = listOf("विष्कुम्भ", "प्रीति", "आयुष्मान्", "सौभाग्य", "शोभन", "अतिगण्ड", "सुकर्मा", "धृति", "शूल", "गण्ड", "वृद्धि", "ध्रुव", "व्याघात", "हर्षण", "वज्र", "सिद्धि", "व्यतीपात", "वरीयान्", "परिघ", "शिव", "सिद्ध", "साध्य", "शुभ", "शुक्ल", "ब्रह्म", "ऐन्द्र", "वैधृति")

    val karanasEn = listOf("Bava", "Balava", "Kaulava", "Taitila", "Gara", "Vanija", "Vishti", "Shakuni", "Chatushpada", "Naga", "Kinstughna")
    val karanasHi = listOf("बव", "बालव", "कौलव", "तैतिल", "गर", "वणिज", "विष्टि", "शकुनि", "चतुष्पाद", "नाग", "किंस्तुघ्न")

    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day)
        
        // Use more accurate Tithi calculation for 2025-2026 based on reference data
        val tithiIndex = if (month.year == 2026) {
            val startTithi = PanchangData.startingTithi2026[month.monthValue] ?: 0
            (startTithi + day - 1) % 30
        } else if (month.year == 2025) {
            val startTithi = PanchangData.startingTithi2025[month.monthValue] ?: 0
            (startTithi + day - 1) % 30
        } else {
            (day + month.monthValue * 2 + month.year % 30) % 30
        }
        
        // Nakshatra and Yoga are still approximations but shifted for better alignment
        val nakshatraIndex = (day + month.monthValue + (if(month.year == 2026) 5 else 0)) % 27
        val yogaIndex = (day + month.monthValue * 3 + (if(month.year == 2026) 10 else 0)) % 27
        val karanaIndex = (day * 2) % 11
        
        val tithiNameEn = tithisEn[tithiIndex]
        val tithiNameHi = tithisHi[tithiIndex]
        val pakshaEn = if (tithiIndex < 15) "Shukla Paksha" else "Krishna Paksha"
        val pakshaHi = if (tithiIndex < 15) "शुक्ल पक्ष" else "कृष्ण पक्ष"
        
        val festivals = mutableListOf<String>()
        var isPanchak = false
        var note: String? = null
        
        // Data from reference Panchang data (2025-2026)
        if (month.year == 2026) {
            PanchangData.festivals2026[month.monthValue]?.get(day)?.let {
                festivals.add(it)
            }
            
            // Panchak data
            PanchangData.panchak2026[month.monthValue]?.forEach { range ->
                if (day in range) isPanchak = true
            }

            // Muhurat data
            val shubhMuhurats = mutableListOf<String>()
            PanchangData.muhurats2026[month.monthValue]?.forEach { (type, days) ->
                if (day in days) {
                    val translatedType = if (isHindi) {
                        when (type) {
                            "Vivah" -> "विवाह"
                            "Namkaran" -> "नामकरण"
                            "Vyapar Prarambh" -> "व्यापार प्रारम्भ"
                            "Annaprashan" -> "अन्नप्राशन"
                            "Griharambh" -> "गृहआरम्भ"
                            else -> type
                        }
                    } else type
                    shubhMuhurats.add(translatedType)
                }
            }
            
            if (shubhMuhurats.isNotEmpty()) {
                val muhuratStr = shubhMuhurats.joinToString(", ")
                note = if (isHindi) "शुभ: $muhuratStr" else "Shubh: $muhuratStr"
            }
        } else if (month.year == 2025) {
            // Panchak data for 2025
            PanchangData.panchak2025[month.monthValue]?.forEach { range ->
                if (day in range) isPanchak = true
            }
        }

        // Standard logic for other years/months
        if (month.year != 2026 || month.monthValue != 1) {
            if (tithiNameEn == "Ekadashi") festivals.add(if (isHindi) "एकादशी व्रत" else "Ekadashi Vrat")
            if (tithiNameEn == "Chaturthi") festivals.add(if (isHindi) "संकष्टी चतुर्थी" else "Sankashti Chaturthi")
            if (tithiNameEn == "Purnima") festivals.add(if (isHindi) "पूर्णिमा व्रत" else "Purnima Vrat")
        }
        
        val defaultMuhurat = if (day % 3 == 0) (if (isHindi) "शुभ: 09:30-11:00 AM" else "Shubh: 09:30-11:00 AM") else (if (isHindi) "अभिजीत: 11:45-12:30 PM" else "Abhijit: 11:45-12:30 PM")

        result[date] = DayPanchang(
            tithi = if (isHindi) "$tithiNameHi ($pakshaHi)" else "$tithiNameEn ($pakshaEn)",
            tithiShort = if (isHindi) tithiNameHi else tithiNameEn.take(6),
            nakshatra = if (isHindi) nakshatrasHi[nakshatraIndex] else nakshatrasEn[nakshatraIndex],
            yoga = if (isHindi) yogasHi[yogaIndex] else yogasEn[yogaIndex],
            karana = if (isHindi) karanasHi[karanaIndex] else karanasEn[karanaIndex],
            muhurat = note ?: defaultMuhurat,
            festivals = festivals.distinct(),
            isPanchak = isPanchak,
            note = note,
            sunrise = if (day % 2 == 0) "06:12 AM" else "06:14 AM",
            sunset = if (day % 2 == 0) "18:48 PM" else "18:47 PM"
        )
    }
    return result
}

suspend fun scrapePanchangData(month: YearMonth): Map<LocalDate, DayPanchang> {
    return withContext(Dispatchers.IO) {
        val result = mutableMapOf<LocalDate, DayPanchang>()
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
        val year = month.year
        
        // Try multiple common URL patterns used by this site
        val urlPatterns = listOf(
            "https://lalaramswaroopcalendar.com/lala-ramswaroop-calendar-$monthName-$year/",
            "https://lalaramswaroopcalendar.com/$monthName-$year-calendar/",
            "https://lalaramswaroopcalendar.com/lala-ramswaroop-panchang-$monthName-$year/"
        )

        for (urlString in urlPatterns) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode != 200) continue

                val html = connection.inputStream.bufferedReader().use { it.readText() }
                
                val monthLabel = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                // More robust regex: matches day, optional month/year, and the festival name after a colon or within tags
                val pattern = """<li>(?:\s*<strong>)?\s*(\d{1,2})(?:\s+$monthLabel)?(?:\s+$year)?.*?(?::|</strong>)\s*([^<]+)</li>"""
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                
                val matches = regex.findAll(html)
                if (matches.none()) continue // Try next URL if no matches found

                matches.forEach { match ->
                    val day = match.groupValues[1].toIntOrNull() ?: return@forEach
                    if (day < 1 || day > month.lengthOfMonth()) return@forEach
                    
                    val festivalName = match.groupValues[2].trim()
                        .replace("&nbsp;", " ")
                        .replace(Regex("""\s+"""), " ")
                    
                    val date = month.atDay(day)
                    val existing = result[date]
                    if (existing != null) {
                        if (!existing.festivals.contains(festivalName)) {
                            result[date] = existing.copy(festivals = (existing.festivals + festivalName).distinct())
                        }
                    } else {
                        // Use a dummy date to generate base data so we don't have empty strings
                        val basePanchang = generatePanchangForMonth(month, false)[date]
                        result[date] = basePanchang?.copy(festivals = listOf(festivalName)) ?: DayPanchang(
                            tithi = "Tithi", tithiShort = "", nakshatra = "", yoga = "", karana = "", muhurat = "",
                            festivals = listOf(festivalName)
                        )
                    }
                }
                
                if (result.isNotEmpty()) break // Success!
            } catch (e: Exception) {
                Log.e("PanchangScraper", "Error scraping $urlString: ${e.message}")
            }
        }
        result
    }
}
