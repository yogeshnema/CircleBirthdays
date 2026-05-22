package com.purawale.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purawale.app.GameSession
import com.purawale.app.Member

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(
    user: Member,
    otherPlayer: Member?,
    otherPlayerName: String,
    session: GameSession?,
    onBack: () -> Unit,
    onUpdateState: (Map<String, Any>, String?, String?) -> Unit
) {
    var selectedSquare by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chess Royale", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C3E50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1A1A1A)
    ) { padding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Other Player Info
                val opponentId = session.players.find { it != user.id }
                PlayerStatusCard(
                    name = otherPlayerName,
                    photoUrl = otherPlayer?.photoUrl,
                    isTurn = session.currentTurn == opponentId,
                    isBlack = true
                )

                Spacer(Modifier.height(24.dp))

                val board = session.gameState["board"] as? List<String> ?: initialChessBoard()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(16.dp, RoundedCornerShape(8.dp))
                        .background(Color(0xFF34495E))
                        .padding(8.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(64) { index ->
                            val row = index / 8
                            val col = index % 8
                            val isDark = (row + col) % 2 != 0
                            val piece = board[index]
                            
                            val squareColor = when {
                                selectedSquare == index -> Color(0xFFF1C40F).copy(alpha = 0.7f)
                                isDark -> Color(0xFF7F8C8D)
                                else -> Color(0xFFBDC3C7)
                            }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(squareColor)
                                    .clickable {
                                        if (session.currentTurn != user.id || session.winnerId != null) return@clickable
                                        
                                        if (selectedSquare == null) {
                                            if (piece.isNotEmpty() && piece.startsWith("W")) selectedSquare = index
                                        } else {
                                            val from = selectedSquare!!
                                            if (from == index) {
                                                selectedSquare = null
                                            } else {
                                                val newBoard = board.toMutableList()
                                                newBoard[index] = newBoard[from]
                                                newBoard[from] = ""
                                                
                                                val nextTurn = session.players.find { it != user.id } ?: user.id
                                                onUpdateState(mapOf("board" to newBoard), nextTurn, null)
                                                selectedSquare = null
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece.isNotEmpty()) {
                                    Text(
                                        text = getPieceSymbol(piece),
                                        fontSize = 32.sp,
                                        color = if (piece.startsWith("W")) Color.White else Color.Black,
                                        modifier = Modifier.shadow(if (piece.startsWith("W")) 2.dp else 0.dp)
                                    )
                                }
                                
                                // Coordinates labels
                                if (col == 0) {
                                    Text(
                                        "${8-row}", 
                                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                                        fontSize = 8.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                                    )
                                }
                                if (row == 7) {
                                    Text(
                                        "${'a' + col}", 
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                                        fontSize = 8.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // User Info
                PlayerStatusCard(
                    name = user.name,
                    photoUrl = user.photoUrl,
                    isTurn = session.currentTurn == user.id,
                    isBlack = false
                )

                Spacer(Modifier.weight(1f))
                
                if (session.winnerId == null && session.status == "ACTIVE" && session.currentTurn == user.id) {
                    OutlinedButton(
                        onClick = { onUpdateState(session.gameState, "", user.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resign / Claim Victory")
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerStatusCard(name: String, photoUrl: String?, isTurn: Boolean, isBlack: Boolean) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isTurn) 2.dp else 0.dp,
                brush = Brush.horizontalGradient(listOf(Color(0xFFF1C40F), Color(0xFFE67E22))),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isTurn) Color(0xFF2C3E50) else Color(0xFF34495E)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (isBlack) Color.Black else Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isBlack) Color.Black else Color.White)
                        .border(1.dp, Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isBlack) "B" else "W",
                        color = if (isBlack) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                if (isTurn) {
                    Text("THINKING...", color = Color(0xFFF1C40F), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

fun initialChessBoard(): List<String> {
    val board = MutableList(64) { "" }
    // Back ranks
    val pieces = listOf("R", "N", "B", "Q", "K", "B", "N", "R")
    for (i in 0..7) {
        board[i] = "B" + pieces[i]
        board[8 + i] = "BP"
        board[48 + i] = "WP"
        board[56 + i] = "W" + pieces[i]
    }
    return board
}

fun getPieceSymbol(piece: String): String {
    return when (piece) {
        "WK" -> "♔"
        "WQ" -> "♕"
        "WR" -> "♖"
        "WB" -> "♗"
        "WN" -> "♘"
        "WP" -> "♙"
        "BK" -> "♚"
        "BQ" -> "♛"
        "BR" -> "♜"
        "BB" -> "♝"
        "BN" -> "♞"
        "BP" -> "♟"
        else -> if (piece.length >= 2) piece.substring(1) else piece
    }
}
