package com.purawale.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
            alpha = 0.35f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080B14).copy(alpha = 0.72f))
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(gameLobbyTitle(gameType), color = Color.White, fontWeight = FontWeight.ExtraBold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF101522).copy(alpha = 0.94f))
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onCreateSession,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(t("Create Game", "गेम बनाएं"), fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFFFFC857),
                    contentColor = Color(0xFF101522),
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { padding ->
            if (activeSessions.isEmpty()) {
                EmptyLobbyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    onCreateSession = onCreateSession
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
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
private fun gameLobbyTitle(gameType: String): String {
    return when (gameType) {
        "CHESS" -> t("Chess Lobby", "शतरंज लॉबी")
        "SNAKES_LADDERS" -> t("Snakes & Ladders Lobby", "सांप-सीढ़ी लॉबी")
        "CHAUPAD" -> t("Chaupad Lobby", "चौपड़ लॉबी")
        "HANGMAN" -> t("Hangman Lobby", "हैंगमैन लॉबी")
        "RUMMY" -> t("Rummy Lobby", "रम्मी लॉबी")
        "ANTAKSHARI" -> t("Antakshari Lobby", "अंताक्षरी लॉबी")
        else -> t("Game Lobby", "गेम लॉबी")
    }
}

@Composable
private fun EmptyLobbyState(modifier: Modifier, onCreateSession: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(36.dp))
                Text(
                    t("No active games", "कोई सक्रिय गेम नहीं"),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    t("Create a table and invite someone to join.", "एक टेबल बनाएं और किसी को जुड़ने दें।"),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onCreateSession,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF101522)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t("Create Game", "गेम बनाएं"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: GameSession, onJoin: () -> Unit) {
    val canJoin = session.players.size < 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canJoin, onClick = onJoin),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape)
                    .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFFFFC857), modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                val hostName = session.playerNames.values.firstOrNull() ?: "Unknown"
                Text(
                    "${t("Host", "मेजबान")}: $hostName",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    "${t("Players", "खिलाड़ी")}: ${session.players.size}/2",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onJoin,
                enabled = canJoin,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF101522)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (canJoin) t("Join", "शामिल हों") else t("Full", "पूर्ण"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
