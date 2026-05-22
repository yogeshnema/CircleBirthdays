package com.purawale.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purawale.app.GameSession
import com.purawale.app.Member
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaupadScreen(
    user: Member,
    otherPlayer: Member?,
    otherPlayerName: String,
    session: GameSession?,
    onBack: () -> Unit,
    onUpdateState: (Map<String, Any>, String?, String?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Royal Chaupad", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B0000),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFFF5E1)
    ) { padding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF8B0000))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game Status Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (otherPlayer != null) {
                            if (otherPlayer.photoUrl != null) {
                                AsyncImage(
                                    model = otherPlayer.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFB8860B), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFB8860B))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        otherPlayerName.take(1),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                        }

                        val currentTurnName = if (session.currentTurn == user.id) "Your Turn" else "$otherPlayerName's Turn"
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (session.winnerId != null) "Game Finished" else currentTurnName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B0000)
                            )
                            val lastRoll = (session.gameState["lastRoll"] as? Number)?.toInt() ?: 0
                            if (lastRoll > 0) {
                                Text("Last Roll: $lastRoll", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        if (session.currentTurn == user.id && session.winnerId == null) {
                            Button(
                                onClick = {
                                    val roll = Random.nextInt(1, 7) // Simple dice for now
                                    val newState = session.gameState.toMutableMap()
                                    newState["lastRoll"] = roll
                                    
                                    val nextTurn = session.players.find { it != user.id } ?: user.id
                                    onUpdateState(newState, nextTurn, null)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8860B))
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Roll")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Chaupad Board
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .background(Color(0xFFFDF5E6))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val gameState = session.gameState
                    val p1Pieces = (gameState["p1_pieces"] as? List<Int>) ?: listOf(0, 0, 0, 0)
                    val p2Pieces = (gameState["p2_pieces"] as? List<Int>) ?: listOf(0, 0, 0, 0)
                    
                    ChaupadBoardDrawing(
                        p1Pieces = p1Pieces,
                        p2Pieces = p2Pieces,
                        p1Color = Color(0xFFD32F2F),
                        p2Color = Color(0xFF1976D2)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Tradition meets Strategy",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}

@Composable
fun ChaupadBoardDrawing(
    p1Pieces: List<Int>,
    p2Pieces: List<Int>,
    p1Color: Color,
    p2Color: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val armWidth = size.width / 3.5f
        val cellSize = armWidth / 3
        
        // Colors
        val boardColor = Color(0xFFF5DEB3)
        val pathColor = Color(0xFFEEDC82)
        val strokeColor = Color(0xFF3E2723)
        val homeColor = Color(0xFFFFFFFF)

        // Draw the four arms (Pathways)
        val arms = listOf(
            Offset(center.x - armWidth / 2, 0f) to Size(armWidth, center.y - armWidth / 2), // Top
            Offset(center.x - armWidth / 2, center.y + armWidth / 2) to Size(armWidth, center.y - armWidth / 2), // Bottom
            Offset(0f, center.y - armWidth / 2) to Size(center.x - armWidth / 2, armWidth), // Left
            Offset(center.x + armWidth / 2, center.y - armWidth / 2) to Size(center.x - armWidth / 2, armWidth) // Right
        )

        arms.forEach { (pos, dim) ->
            drawRect(color = boardColor, topLeft = pos, size = dim)
            drawRect(color = strokeColor, topLeft = pos, size = dim, style = Stroke(2f))
        }

        // Draw center square (Home/Char-Koni)
        drawRect(
            color = homeColor,
            topLeft = Offset(center.x - armWidth / 2, center.y - armWidth / 2),
            size = Size(armWidth, armWidth)
        )
        drawRect(
            color = strokeColor,
            topLeft = Offset(center.x - armWidth / 2, center.y - armWidth / 2),
            size = Size(armWidth, armWidth),
            style = Stroke(4f)
        )

        // Grid lines and cells for arms
        for (i in 1..2) {
            val offset = i * cellSize
            // Vertical arms
            drawLine(strokeColor, Offset(center.x - armWidth / 2 + offset, 0f), Offset(center.x - armWidth / 2 + offset, center.y - armWidth / 2), 2f)
            drawLine(strokeColor, Offset(center.x - armWidth / 2 + offset, center.y + armWidth / 2), Offset(center.x - armWidth / 2 + offset, size.height), 2f)
            // Horizontal arms
            drawLine(strokeColor, Offset(0f, center.y - armWidth / 2 + offset), Offset(center.x - armWidth / 2, center.y - armWidth / 2 + offset), 2f)
            drawLine(strokeColor, Offset(center.x + armWidth / 2, center.y - armWidth / 2 + offset), Offset(size.width, center.y - armWidth / 2 + offset), 2f)
        }

        for (j in 1..7) {
            val offset = j * ( (center.y - armWidth/2) / 8)
            // Top/Bottom arm horizontal dividers
            drawLine(strokeColor, Offset(center.x - armWidth / 2, offset), Offset(center.x + armWidth / 2, offset), 1f)
            drawLine(strokeColor, Offset(center.x - armWidth / 2, center.y + armWidth / 2 + offset), Offset(center.x + armWidth / 2, center.y + armWidth / 2 + offset), 1f)
            
            val hOffset = j * ( (center.x - armWidth/2) / 8)
            // Left/Right arm vertical dividers
            drawLine(strokeColor, Offset(hOffset, center.y - armWidth / 2), Offset(hOffset, center.y + armWidth / 2), 1f)
            drawLine(strokeColor, Offset(center.x + armWidth / 2 + hOffset, center.y - armWidth / 2), Offset(center.x + armWidth / 2 + hOffset, center.y + armWidth / 2), 1f)
        }

        // Draw Pieces (Simulated positions)
        // Player 1 Pieces
        p1Pieces.forEachIndexed { index, pos ->
            val pieceOffset = when(index) {
                0 -> Offset(center.x - armWidth/4, center.y - armWidth/4)
                1 -> Offset(center.x + armWidth/4, center.y - armWidth/4)
                2 -> Offset(center.x - armWidth/4, center.y + armWidth/4)
                else -> Offset(center.x + armWidth/4, center.y + armWidth/4)
            }
            drawCircle(color = p1Color, radius = cellSize / 3, center = pieceOffset)
            drawCircle(color = Color.White, radius = cellSize / 3, center = pieceOffset, style = Stroke(2f))
        }

        // Player 2 Pieces (Slightly offset)
        p2Pieces.forEachIndexed { index, pos ->
            val pieceOffset = when(index) {
                0 -> Offset(center.x - armWidth/6, center.y - armWidth/6)
                1 -> Offset(center.x + armWidth/6, center.y - armWidth/6)
                2 -> Offset(center.x - armWidth/6, center.y + armWidth/6)
                else -> Offset(center.x + armWidth/6, center.y + armWidth/6)
            }
            drawCircle(color = p2Color, radius = cellSize / 3, center = pieceOffset)
            drawCircle(color = Color.White, radius = cellSize / 3, center = pieceOffset, style = Stroke(2f))
        }

        // Safe spots (Traditional X marks)
        val xColor = Color(0xFF8B0000).copy(alpha = 0.5f)
        drawSafeX(center.x, cellSize * 4f, cellSize / 2, xColor)
        drawSafeX(center.x, size.height - cellSize * 4f, cellSize / 2, xColor)
        drawSafeX(cellSize * 4f, center.y, cellSize / 2, xColor)
        drawSafeX(size.width - cellSize * 4f, center.y, cellSize / 2, xColor)
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSafeX(x: Float, y: Float, radius: Float, color: Color) {
    drawLine(color, Offset(x - radius, y - radius), Offset(x + radius, y + radius), 4f)
    drawLine(color, Offset(x + radius, y - radius), Offset(x - radius, y + radius), 4f)
}
