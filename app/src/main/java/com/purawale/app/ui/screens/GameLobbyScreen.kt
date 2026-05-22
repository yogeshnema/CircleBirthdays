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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purawale.app.GameSession
import com.purawale.app.Member
import com.purawale.app.R
import com.purawale.app.t
import com.purawale.app.ui.theme.LightGolden

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    user: Member,
    gameType: String,
    activeSessions: List<GameSession>,
    onBack: () -> Unit,
    onCreateSession: () -> Unit,
    onJoinSession: (GameSession) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        val title = when(gameType) {
                            "CHESS" -> t("Chess Lobby", "शतरंज लॉबी")
                            "SNAKES_LADDERS" -> t("Snakes & Ladders Lobby", "सांप-सीढ़ी लॉबी")
                            "CHAUPAD" -> t("Chaupad Lobby", "चौपड़ लॉबी")
                            "HANGMAN" -> t("Hangman Lobby", "हैंगमैन लॉबी")
                            "RUMMY" -> t("Rummy Lobby", "रम्मी लॉबी")
                            "ANTAKSHARI" -> t("Antakshari Lobby", "अंताक्षरी लॉबी")
                            else -> t("Game Lobby", "गेम लॉबी")
                        }
                        Text(title, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF5D4037))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LightGolden)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreateSession,
                    containerColor = Color(0xFF5D4037),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Game")
                }
            }
        ) { padding ->
            if (activeSessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        t("No active games. Create one!", "कोई सक्रिय गेम नहीं। एक बनाएं!"),
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeSessions) { session ->
                        SessionCard(session, onJoin = { onJoinSession(session) })
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: GameSession, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onJoin),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, Color(0xFF5D4037))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEFEBE9), CircleShape)
                        .border(1.dp, Color(0xFF5D4037), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF5D4037), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    val hostName = session.playerNames.values.firstOrNull() ?: "Unknown"
                    Text(
                        "${t("Host", "मेजबान")}: $hostName",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3E2723),
                        fontSize = 16.sp
                    )
                    Text(
                        "${t("Players", "खिलाड़ी")}: ${session.players.size}/2",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5D4037),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Button(
                onClick = onJoin,
                enabled = session.players.size < 2,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
            ) {
                Text(
                    if (session.players.size < 2) t("Join", "शामिल हों") else t("Full", "पूर्ण"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

