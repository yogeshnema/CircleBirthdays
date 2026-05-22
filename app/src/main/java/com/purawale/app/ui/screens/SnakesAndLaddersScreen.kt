package com.purawale.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
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
fun SnakesAndLaddersScreen(
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
                title = { Text("Snakes & Ladders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5DC),
                    titleContentColor = Color(0xFF3E2723)
                )
            )
        },
        containerColor = Color(0xFFFDF5E6)
    ) { padding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3E2723))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (otherPlayer != null) {
                                Box(modifier = Modifier.size(48.dp)) {
                                    if (otherPlayer.photoUrl != null) {
                                        AsyncImage(
                                            model = otherPlayer.photoUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .border(2.dp, Color(0xFFC2185B), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color(0xFFC2185B))
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
                                }
                                Spacer(Modifier.width(12.dp))
                            }

                            Column {
                                Text(
                                    text = when {
                                        session.winnerId != null -> if (session.winnerId == user.id) "Victory! 🎉" else "Game Over"
                                        session.status == "WAITING" -> "Waiting..."
                                        session.currentTurn == user.id -> "Your Turn"
                                        else -> "$otherPlayerName's Turn"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF3E2723)
                                )
                                val myPos = (session.gameState[user.id] as? Number)?.toInt() ?: 0
                                Text("Position: $myPos", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Icon(
                            Icons.Default.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF8D6E63)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(4.dp, Color(0xFF5D4037), RoundedCornerShape(16.dp))
                ) {
                    GameBoard(session, user)
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val roll = Random.nextInt(1, 7)
                        val currentPos = (session.gameState[user.id] as? Number)?.toInt() ?: 0
                        var nextPos = currentPos + roll
                        if (nextPos > 100) nextPos = 100
                        
                        nextPos = getNewPosition(nextPos)

                        val newState = session.gameState.toMutableMap()
                        newState[user.id] = nextPos
                        
                        val winnerId = if (nextPos == 100) user.id else null
                        val nextTurn = if (winnerId != null) "" else session.players.find { it != user.id } ?: user.id
                        
                        onUpdateState(newState, nextTurn, winnerId)
                    },
                    enabled = session.status == "ACTIVE" && session.currentTurn == user.id && session.winnerId == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723))
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Roll Dice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GameBoard(session: GameSession, currentUser: Member) {
    val snakes = mapOf(17 to 7, 54 to 34, 62 to 19, 98 to 79)
    val ladders = mapOf(3 to 38, 24 to 33, 42 to 93, 72 to 84)
    
    val infiniteTransition = rememberInfiniteTransition(label = "game_animations")
    val snakePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snake_phase"
    )
    
    val ladderGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ladder_glow"
    )

    val textMeasurer = rememberTextMeasurer()
    val cellColors = listOf(
        Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB), 
        Color(0xFFFFF9C4), Color(0xFFE1BEE7), Color(0xFFFFE0B2)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boardSize = constraints.maxWidth.toFloat()
        val cellSize = boardSize / 10

        // Track player positions with animation
        val playerOffsets = session.players.map { playerId ->
            val pos = (session.gameState[playerId] as? Number)?.toInt() ?: 0
            val target = if (pos > 0) getCenter(pos, cellSize) else Offset(-cellSize, -cellSize)
            animateOffsetAsState(
                targetValue = target,
                animationSpec = tween(800, easing = FastOutSlowInEasing),
                label = "player_pos_$playerId"
            ).value
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Draw Grid Background
            for (i in 0 until 100) {
                val row = i / 10
                val col = if (row % 2 == 0) i % 10 else 9 - (i % 10)
                val x = col * cellSize
                val y = (9 - row) * cellSize
                
                val color = cellColors[i % cellColors.size]
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize)
                )
                
                drawRect(
                    color = Color.Black.copy(alpha = 0.1f),
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 1f)
                )
                
                // Numbers
                val textResult = textMeasurer.measure(
                    text = (i + 1).toString(),
                    style = TextStyle(
                        color = Color.DarkGray.copy(alpha = 0.6f),
                        fontSize = (cellSize / 4).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(x + 6f, y + 6f)
                )
            }

            // 2. Draw Animated Ladders
            ladders.forEach { (start, end) ->
                val startPoint = getCenter(start, cellSize)
                val endPoint = getCenter(end, cellSize)
                
                val railColor = Color(0xFF5D4037)
                val rungColor = Color(0xFF8D6E63)
                val railOffset = cellSize / 6
                
                // Pulse effect on ladders
                val glowBrush = Brush.linearGradient(
                    colors = listOf(railColor, railColor.copy(alpha = ladderGlow), railColor),
                    start = startPoint,
                    end = endPoint
                )

                drawLine(
                    brush = glowBrush,
                    start = startPoint.copy(x = startPoint.x - railOffset),
                    end = endPoint.copy(x = endPoint.x - railOffset),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = glowBrush,
                    start = startPoint.copy(x = startPoint.x + railOffset),
                    end = endPoint.copy(x = endPoint.x + railOffset),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                
                val steps = 7
                for (j in 1 until steps) {
                    val t = j.toFloat() / steps
                    val rungCenter = startPoint + (endPoint - startPoint) * t
                    val rungStart = rungCenter.copy(x = rungCenter.x - railOffset)
                    val rungEnd = rungCenter.copy(x = rungCenter.x + railOffset)
                    drawLine(color = rungColor, start = rungStart, end = rungEnd, strokeWidth = 5f)
                }
            }

            // 3. Draw Animated Snakes (Slithering)
            snakes.forEach { (start, end) ->
                val head = getCenter(start, cellSize)
                val tail = getCenter(end, cellSize)
                
                val snakePath = Path().apply {
                    moveTo(head.x, head.y)
                    val segments = 10
                    for (k in 1..segments) {
                        val t = k.toFloat() / segments
                        val pointOnLine = head + (tail - head) * t
                        // Add slither wave
                        val wave = sin(t * 10f + snakePhase) * 20f
                        // Perpendicular offset for wave
                        val dir = (tail - head)
                        val length = dir.getDistance()
                        val perpX = -dir.y / length * wave
                        val perpY = dir.x / length * wave
                        lineTo(pointOnLine.x + perpX, pointOnLine.y + perpY)
                    }
                }
                
                drawPath(
                    path = snakePath,
                    color = Color(0xFF2E7D32),
                    style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                
                // Head Details
                drawCircle(Color(0xFF1B5E20), radius = 12f, center = head)
                drawCircle(Color.White, radius = 3f, center = head + Offset(-4f, -3f))
                drawCircle(Color.White, radius = 3f, center = head + Offset(4f, -3f))
                
                // Forked tongue animation
                val tongueLen = 15f + sin(snakePhase * 2f) * 5f
                drawLine(
                    color = Color.Red,
                    start = head,
                    end = head + (head - tail).let { it / it.getDistance() * tongueLen },
                    strokeWidth = 3f
                )
            }

            // 4. Draw Players with Smooth Offsets
            playerOffsets.forEachIndexed { index, offset ->
                if (offset.x > 0) {
                    val playerColor = if (index == 0) Color(0xFF1976D2) else Color(0xFFC2185B)
                    
                    // Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = cellSize / 2.5f,
                        center = offset + Offset(4f, 4f)
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(playerColor.copy(alpha = 0.7f), playerColor),
                            center = offset
                        ),
                        radius = cellSize / 3f,
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = cellSize / 3f,
                        center = offset,
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

fun getCenter(pos: Int, cellSize: Float): Offset {
    val i = pos - 1
    val row = i / 10
    val col = if (row % 2 == 0) i % 10 else 9 - (i % 10)
    return Offset(col * cellSize + cellSize / 2, (9 - row) * cellSize + cellSize / 2)
}

fun getNewPosition(pos: Int): Int {
    val snakes = mapOf(17 to 7, 54 to 34, 62 to 19, 98 to 79)
    val ladders = mapOf(3 to 38, 24 to 33, 42 to 93, 72 to 84)
    return ladders[pos] ?: snakes[pos] ?: pos
}
