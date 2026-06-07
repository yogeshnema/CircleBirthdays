package com.purawale.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purawale.app.*
import com.purawale.app.R
import com.purawale.app.ui.theme.LightGolden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGamesScreen(
    user: Member,
    onBack: () -> Unit,
    onStartGame: (String) -> Unit, // gameType
    onTakeTrivia: () -> Unit,
    onManageTrivia: () -> Unit,
    onQuestClick: (FamilyQuest) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.75f), BlendMode.Darken)
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t("Family Games", "फैमिली गेम्स"), color = Color.White, fontWeight = FontWeight.ExtraBold) },
                    actions = {
                        if (user.isAdmin) {
                            IconButton(onClick = onManageTrivia) {
                                Icon(Icons.Default.Settings, contentDescription = "Manage", tint = Color.White)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF080B14).copy(alpha = 0.90f)
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        text = t("Gamification Hub", "गेमिफिकेशन हब"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    GamificationStats(user)
                }

                item {
                    SectionHeader(t("Multiplayer Games", "मल्टीप्लेयर गेम्स"), Icons.Default.SportsEsports)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameTile(t("Snakes & Ladders", "सांप और सीढ़ी"), Icons.Default.Casino, Color(0xFF4CAF50), Modifier.weight(1f)) {
                            onStartGame("SNAKES_LADDERS")
                        }
                        GameTile(t("Chess", "शतरंज"), Icons.Default.Extension, Color(0xFF795548), Modifier.weight(1f)) {
                            onStartGame("CHESS")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameTile(t("Chaupad", "चौपड़"), Icons.Default.Token, Color(0xFFE91E63), Modifier.weight(1f)) {
                            onStartGame("CHAUPAD")
                        }
                        GameTile(t("Hangman", "हैंगमैन"), Icons.Default.Title, Color(0xFF2196F3), Modifier.weight(1f)) {
                            onStartGame("HANGMAN")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GameTile(t("Rummy", "रम्मी"), Icons.Default.Style, Color(0xFF9C27B0), Modifier.weight(1f)) {
                            onStartGame("RUMMY")
                        }
                        GameTile(t("Antakshari", "अंताक्षरी"), Icons.Default.MusicNote, Color(0xFFFF5722), Modifier.weight(1f)) {
                            onStartGame("ANTAKSHARI")
                        }
                    }
                }

                item {
                    SectionHeader(t("Trivia", "प्रश्नोत्तरी"), Icons.Default.Quiz)
                    TriviaCard(onTakeTrivia)
                }

                item {
                    SectionHeader(t("Active Quests", "सक्रिय मिशन"), Icons.AutoMirrored.Filled.Assignment)
                }

                // Mock Quests for now, in a real app these would come from a ViewModel
                val mockQuests = listOf(
                    FamilyQuest(id = "1", title = "Photo Master", description = "Upload 5 family photos", pointsReward = 50, type = "ADD_PHOTO"),
                    FamilyQuest(id = "2", title = "Chef in Training", description = "Post your first recipe", pointsReward = 30, type = "POST_RECIPE")
                )

                items(mockQuests) { quest ->
                    QuestItem(quest) { onQuestClick(quest) }
                }
            }
        }
    }
}

@Composable
fun GamificationStats(user: Member) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${t("Level", "स्तर")} ${user.level}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${user.points} ${t("Total Points", "कुल अंक")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color(0xFFFFC857)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFFFFC857))
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameTile(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.18f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun TriviaCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Quiz, null, tint = Color(0xFFFFC857), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t("Daily Trivia", "दैनिक प्रश्नोत्तरी"),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Text(
                    t("Test your family knowledge!", "अपने पारिवारिक ज्ञान का परीक्षण करें!"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(t("Play", "खेलें"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuestItem(quest: FamilyQuest, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quest.title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(quest.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Text(
                    "${quest.pointsReward} ${t("Points", "अंक")}",
                    color = Color(0xFFFFC857),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            IconButton(
                onClick = onComplete,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = Color.White)
            }
        }
    }
}

