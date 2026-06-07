package com.purawale.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.purawale.app.AppConfig
import com.purawale.app.Member
import com.purawale.app.formatDateToDDMMM
import com.purawale.app.t
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

private data class MemberMatch(
    val member: Member,
    val score: Int,
    val isFuzzy: Boolean = false
)

private val ignoredQuestionWords = setOf(
    "a", "an", "and", "are", "birth", "birthday", "bday", "dob", "date", "day", "of", "on", "the",
    "is", "when", "what", "whats", "who", "whose", "tell", "me", "show", "for", "ka", "ki", "ke",
    "hai", "kab", "kya", "please", "pls"
)

private val ignoredRelationshipWords = setOf(
    "bhai", "bhaiya", "bhaiyaa", "dada", "dadaji", "dadi", "dadiji", "didi", "behan", "bhabhi",
    "jijaji", "kaka", "kakaji", "kaki", "chacha", "chachaji", "chachi", "mama", "mamaji", "mami",
    "mausa", "mausaji", "mausi", "bua", "fufa", "fufaji", "papa", "mummy", "beta", "beti",
    "uncle", "aunty", "ji", "sir"
)

private fun searchableWords(text: String): List<String> {
    return text.lowercase(Locale.ENGLISH)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length > 1 && it !in ignoredQuestionWords && it !in ignoredRelationshipWords }
}

private fun isBirthdayQuestion(question: String): Boolean {
    val normalized = question.lowercase(Locale.ENGLISH)
    return listOf("birthday", "bday", "dob", "date of birth", "janam", "janmadin", "जन्म").any { normalized.contains(it) }
}

private fun editDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    val previous = IntArray(b.length + 1) { it }
    val current = IntArray(b.length + 1)

    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(
                current[j - 1] + 1,
                previous[j] + 1,
                previous[j - 1] + cost
            )
        }
        for (j in previous.indices) previous[j] = current[j]
    }

    return previous[b.length]
}

private fun fuzzyWordScore(query: String, candidate: String): Int {
    if (query.length < 3 || candidate.length < 3) return 0
    val distance = editDistance(query, candidate)
    val maxLength = maxOf(query.length, candidate.length)
    val similarity = 1.0 - (distance.toDouble() / maxLength.toDouble())

    return when {
        distance == 1 && maxLength >= 4 -> 68
        distance == 2 && maxLength >= 6 -> 58
        similarity >= 0.78 -> 58
        similarity >= 0.70 && maxLength >= 5 -> 45
        else -> 0
    }
}

private fun findMemberMatches(question: String, members: List<Member>): List<MemberMatch> {
    val queryWords = searchableWords(question)
    if (queryWords.isEmpty()) return emptyList()
    val joinedQuery = queryWords.joinToString(" ")

    return members.mapNotNull { member ->
        val nameWords = searchableWords(member.name)
        val searchableName = nameWords.joinToString(" ")
        if (searchableName.isBlank()) return@mapNotNull null

        val exactScore = when {
            searchableName == joinedQuery -> 120
            searchableName.contains(joinedQuery) -> 100 + joinedQuery.length
            queryWords.all { query -> nameWords.any { it == query } } -> 90 + queryWords.size
            queryWords.all { query -> nameWords.any { it.startsWith(query) } } -> 75 + queryWords.size
            queryWords.any { query -> nameWords.any { it == query } } -> 50
            queryWords.any { query -> nameWords.any { it.startsWith(query) } } -> 35
            else -> 0
        }

        val fuzzyScores = queryWords.map { query ->
            nameWords.maxOfOrNull { candidate -> fuzzyWordScore(query, candidate) } ?: 0
        }
        val fuzzyScore = if (fuzzyScores.isNotEmpty() && fuzzyScores.all { it > 0 }) {
            fuzzyScores.average().toInt() + queryWords.size
        } else {
            0
        }
        val score = maxOf(exactScore, fuzzyScore)

        if (score > 0) MemberMatch(member, score, isFuzzy = exactScore == 0 && fuzzyScore > 0) else null
    }
        .sortedWith(compareByDescending<MemberMatch> { it.score }.thenBy { it.member.name })
        .let { matches ->
            val bestScore = matches.firstOrNull()?.score ?: return emptyList()
            matches.filter { it.score >= bestScore - 12 && it.score >= 42 }.take(5)
        }
}

private fun memberPromptLine(member: Member): String {
    return listOfNotNull(
        "ID: ${member.id}",
        "Name: ${member.name}",
        "Gender: ${member.gender}",
        "DOB: ${member.dateOfBirth}",
        "FamilyID: ${member.familyId}",
        member.spouseName?.let { "Spouse: $it" },
        member.fatherName?.let { "Father: $it" },
        member.motherName?.let { "Mother: $it" },
        member.marriageDate?.let { "MarriageDate: $it" },
        member.bereavementDate?.let { "BereavementDate: $it" },
        member.relationship?.let { "Relationship: $it" }
    ).joinToString(", ")
}

private fun birthdayAnswer(question: String, matches: List<MemberMatch>): String? {
    if (!isBirthdayQuestion(question) || matches.isEmpty()) return null
    if (matches.size > 1) {
        val options = matches.joinToString("\n") { match ->
            val date = formatDateToDDMMM(match.member.dateOfBirth).ifBlank { "birthday not saved" }
            "- ${match.member.name} (${match.member.familyId}) - $date"
        }
        return "I found multiple matching people. Which one do you mean?\n$options"
    }

    val match = matches.first()
    val member = match.member
    if (member.dateOfBirth.isBlank()) {
        return "I found ${member.name}, but their birthday is not saved in the profile."
    }

    val prefix = if (match.isFuzzy) "Closest match: ${member.name}. " else ""
    return "$prefix${member.name}'s birthday is ${formatDateToDDMMM(member.dateOfBirth)}."
}

@Composable
fun AIChatAssistant(
    allMembers: List<Member>,
    currentUser: Member?
) {
    var isChatOpen by remember { mutableStateOf(false) }
    val initialMsg = t("Hello! I'm your Purawale AI. Ask me anything about our family directory.", "नमस्ते! मैं आपका Purawale AI हूँ। हमारे परिवार की जानकारी के बारे में कुछ भी पूछें।")
    var messages by remember { mutableStateOf(listOf(ChatMessage(initialMsg, false))) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Gemini Model
    val generativeModel = remember {
        GenerativeModel(
            modelName = AppConfig.GEMINI_MODEL,
            apiKey = AppConfig.GEMINI_API_KEY
        )
    }
    val compactMemberContext = remember(allMembers) {
        allMembers.joinToString("\n") { memberPromptLine(it) }.take(8000)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating Button
        if (!isChatOpen) {
            FloatingActionButton(
                onClick = { isChatOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = Color(0xFFFFC857),
                contentColor = Color(0xFF080B14),
                shape = CircleShape
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = t("AI Assistant", "AI सहायक"))
            }
        }

        // Chat Window
        AnimatedVisibility(
            visible = isChatOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .widthIn(max = 400.dp)
                .fillMaxHeight(0.6f)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t("Family AI Assistant", "फैमिली AI सहायक"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { isChatOpen = false }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }

                    // Messages Area
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        state = listState,
                        reverseLayout = false
                    ) {
                        items(messages) { msg ->
                            ChatBubble(msg)
                        }
                        if (isTyping) {
                            item {
                                Text(
                                    t("AI is thinking...", "AI सोच रहा है..."),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Input Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(t("Ask a question...", "सवाल पूछें...")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val userMsg = inputText
                                    messages = messages + ChatMessage(userMsg, true)
                                    inputText = ""
                                    isTyping = true
                                    
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size)
                                        
                                        try {
                                            val localMatches = findMemberMatches(userMsg, allMembers)
                                            birthdayAnswer(userMsg, localMatches)?.let { answer ->
                                                messages = messages + ChatMessage(answer, false)
                                                return@launch
                                            }

                                            val localMatchData = if (localMatches.isNotEmpty()) {
                                                localMatches.joinToString("\n") { match ->
                                                    "${memberPromptLine(match.member)}, MatchType: ${if (match.isFuzzy) "fuzzy suggestion" else "name match"}"
                                                }
                                            } else {
                                                "No confident local member match."
                                            }

                                            val directoryContext = if (localMatches.isNotEmpty()) {
                                                "Directory context limited to local matches to save tokens."
                                            } else {
                                                compactMemberContext
                                            }
                                            
                                            val prompt = """
                                                You are a Family Directory AI for the Purawale community.
                                                
                                                FAMILY ID SYSTEM EXPLANATION:
                                                - 'P' is the root/patriarch.
                                                - Single letters (A, B, C...) are children of P.
                                                - Multiple characters (A1, A12, A123) represent generations (A1 is child of A, A11 is child of A1).
                                                - A '0' suffix (e.g., A10, P0) indicates a SPOUSE of the person with that ID.
                                                
                                                RELATIONSHIP LOGIC:
                                                - Siblings: Same Father/Mother names or IDs like A1, A2, A3 (all children of A).
                                                - Cousins: Children of siblings (e.g., A11 and A21 are cousins because A1 and A2 are siblings).
                                                - Generations: Depth of ID (length) indicates generation.
                                                - Spouse Terminality: IDs ending in '0' are spouses and do NOT have children in the ID hierarchy. Children are listed under the main ID (e.g., children of A and A0 are A1, A2).
                                                - Paternal Focus: Branches A-G represent major family lines.
                                                
                                                 MEMBERS DATA:
                                                 $directoryContext
                                                 
                                                 LOCAL MEMBER MATCHES FROM USER QUESTION:
                                                 $localMatchData
                                                 
                                                 ---
                                                USER CONTEXT:
                                                Logged-in User Name: ${currentUser?.name ?: "Unknown"}
                                                Logged-in User ID: ${currentUser?.id ?: "Unknown"}
                                                Logged-in User DOB: ${currentUser?.dateOfBirth ?: "Unknown"}
                                                Logged-in User FamilyID: ${currentUser?.familyId ?: "Unknown"}
                                                Today's Date: ${LocalDate.now()}
                                                ---
                                                
                                                RULES:
                                                1. Priority: If a member's 'ManualRels' contains the Logged-in User ID as a key, use that specific label for the relationship.
                                                2. Only use the provided data. NEVER ask the user for their Name, DOB, or FamilyID.
                                                3. "My Generation": Use the Logged-in User's DOB. Find members born within +/- 10 years.
                                                4. "My Family": Use FamilyID and Father/Mother links.
                                                5. Distant Relatives: Trace IDs up/down. Anyone with an ID starting with the same letter as the user (A-G) is in the same major branch.
                                                6. Hindi Terms: Use specific terms like Bhabhi (brother's wife), Jijaji (sister's husband), Jeth/Devar (husband's elder/younger brother), Nanad (husband's sister), Devrani (husband's younger brother's wife), Jethani (husband's elder brother's wife), Sasurji/Saasuma (in-laws), Saala/Saali (wife's brother/sister).
                                                 7. If LOCAL MEMBER MATCHES contains one or more people, use those matched profiles first for name-specific questions. Do not say a person was not found unless both local matches and member data are empty or irrelevant.
                                                 8. If a local match has MatchType "fuzzy suggestion", mention that it is the closest match before answering.
                                                 9. Relationship words in user questions such as bhaiya, didi, bhabhi, kaka, mama, aunty, uncle, or ji are honorifics; match the actual name around them.
                                                 10. Responses: Be warm, concise, and support both English and Hindi.
                                                
                                                User Question: $userMsg
                                            """.trimIndent()

                                            val response = generativeModel.generateContent(prompt)
                                            messages = messages + ChatMessage(response.text ?: "I couldn't process that.", false)
                                        } catch (e: Exception) {
                                            val errorMsg = when {
                                                e.message?.contains("404") == true -> "AI Error: Model not found. This usually means your API Key in AppConfig is invalid or starts with the wrong characters (should start with AIza)."
                                                e.message?.contains("403") == true -> "AI Error: Permission denied. Ensure 'Generative Language API' is enabled in Google Cloud Console."
                                                e.message?.contains("deadline", ignoreCase = true) == true || e.message?.contains("timeout", ignoreCase = true) == true ->
                                                    "AI Error: Connection Timeout. Please check your internet connection."
                                                e.message?.contains("Unable to resolve host", ignoreCase = true) == true || e.message?.contains("No address associated with hostname", ignoreCase = true) == true ->
                                                    "AI Error: Network/DNS problem. Please check your internet or try again later."
                                                else -> "AI Error: ${e.localizedMessage}"
                                            }
                                            messages = messages + ChatMessage(errorMsg, false)
                                        } finally {
                                            isTyping = false
                                            listState.animateScrollToItem(messages.size)
                                        }
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !isTyping
                        ) {
                            Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F1F1)
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Surface(
            color = color,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}
