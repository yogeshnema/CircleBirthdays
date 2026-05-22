package com.purawale.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purawale.app.GameSession
import com.purawale.app.Member

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HangmanScreen(
    user: Member,
    otherPlayer: Member?,
    otherPlayerName: String,
    session: GameSession?,
    onBack: () -> Unit,
    onUpdateState: (Map<String, Any>, String?, String?) -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Hangman", fontWeight = FontWeight.ExtraBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(padding)
        ) {
            if (session == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (session.status == "WAITING") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(modifier = Modifier.padding(32.dp)) {
                        Text(
                            "Waiting for an opponent to join...",
                            modifier = Modifier.padding(24.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            } else {
                val word = (session.gameState["word"] as? String) ?: "FAMILY"
                val category = (session.gameState["category"] as? String) ?: "GENERAL"
                val guessedLetters = (session.gameState["guessedLetters"] as? List<String>) ?: emptyList()
                val wrongGuesses = guessedLetters.filter { !word.contains(it, ignoreCase = true) }.size
                val isGameOver = wrongGuesses >= 6
                val isVictory = word.all { char -> char.isWhitespace() || guessedLetters.contains(char.toString().uppercase()) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Category Hint
                    GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "HINT: $category",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Bigger Hangman Drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
                            .padding(24.dp)
                    ) {
                        HangmanDrawingAnimated(wrongGuesses)
                    }

                    Spacer(Modifier.height(32.dp))

                    // Word Display using FlowRow for responsiveness
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        maxItemsInEachRow = 8
                    ) {
                        word.forEach { char ->
                            val isRevealed = guessedLetters.contains(char.toString().uppercase()) || isGameOver
                            val displayedChar = if (char.isWhitespace()) " " else if (isRevealed) char.toString().uppercase() else ""
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp).width(30.dp)
                            ) {
                                Text(
                                    text = displayedChar,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        isGameOver && !guessedLetters.contains(char.toString().uppercase()) -> Color(0xFFFF5252)
                                        else -> Color.White
                                    },
                                    modifier = Modifier.height(36.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(
                                            if (char.isWhitespace()) Color.Transparent else Color.White.copy(alpha = 0.6f),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    if (isGameOver || isVictory) {
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = if (isVictory) "VICTORY! 🎉" else "GAME OVER!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (isVictory) Color(0xFF64FFDA) else Color(0xFFFF5252),
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (isVictory) "You saved them!" else "The word was $word",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.padding(top = 20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Text("Back to Lobby", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Keyboard
                        val alphabet = ('A'..'Z').map { it.toString() }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(alphabet) { letter ->
                                val isGuessed = guessedLetters.contains(letter)
                                val isCorrect = word.contains(letter, ignoreCase = true)
                                val isMyTurn = session.currentTurn == user.id

                                GlassKey(
                                    letter = letter,
                                    isGuessed = isGuessed,
                                    isCorrect = isCorrect,
                                    enabled = !isGuessed && isMyTurn && session.winnerId == null,
                                    onClick = {
                                        val newList = guessedLetters + letter
                                        val newState = session.gameState.toMutableMap()
                                        newState["guessedLetters"] = newList
                                        
                                        val won = word.all { char -> char.isWhitespace() || newList.contains(char.toString().uppercase()) }
                                        val lost = (newList.filter { !word.contains(it, ignoreCase = true) }.size) >= 6
                                        
                                        val winnerId = if (won) user.id else null
                                        val nextTurn = if (won || lost) null else (session.players.find { it != user.id } ?: user.id)
                                        
                                        onUpdateState(newState, nextTurn, winnerId)
                                    }
                                )
                            }
                        }
                        
                        Text(
                            text = if (session.currentTurn == user.id) "YOUR TURN" else "WAITING FOR ${otherPlayerName.uppercase()}...",
                            color = if (session.currentTurn == user.id) Color.Yellow else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GlassKey(
    letter: String,
    isGuessed: Boolean,
    isCorrect: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isGuessed && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.7f)
        isGuessed -> Color(0xFFF44336).copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.15f)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = if (isGuessed) Color.White else Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun HangmanDrawingAnimated(wrongGuesses: Int) {
    val color = Color.White
    val strokeWidth = 10f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Gallows (Static)
        drawLine(color, Offset(w * 0.1f, h * 0.95f), Offset(w * 0.9f, h * 0.95f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.95f), Offset(w * 0.2f, h * 0.05f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.2f, h * 0.05f), Offset(w * 0.7f, h * 0.05f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.7f, h * 0.05f), Offset(w * 0.7f, h * 0.15f), strokeWidth = 6f, cap = StrokeCap.Round)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedPart(visible = wrongGuesses >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color, radius = size.width * 0.12f, center = Offset(size.width * 0.7f, size.height * 0.27f), style = Stroke(width = 6f))
            }
        }
        AnimatedPart(visible = wrongGuesses >= 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color, Offset(size.width * 0.7f, size.height * 0.39f), Offset(size.width * 0.7f, size.height * 0.7f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
        }
        AnimatedPart(visible = wrongGuesses >= 3) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color, Offset(size.width * 0.7f, size.height * 0.45f), Offset(size.width * 0.55f, size.height * 0.6f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
        }
        AnimatedPart(visible = wrongGuesses >= 4) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color, Offset(size.width * 0.7f, size.height * 0.45f), Offset(size.width * 0.85f, size.height * 0.6f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
        }
        AnimatedPart(visible = wrongGuesses >= 5) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color, Offset(size.width * 0.7f, size.height * 0.7f), Offset(size.width * 0.58f, size.height * 0.88f), strokeWidth = 8f, cap = StrokeCap.Round)
            }
        }
        AnimatedPart(visible = wrongGuesses >= 6) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(color, Offset(size.width * 0.7f, size.height * 0.7f), Offset(size.width * 0.82f, size.height * 0.88f), strokeWidth = 8f, cap = StrokeCap.Round)
                
                // Eyes (Dead)
                val ex = size.width * 0.7f
                val ey = size.height * 0.27f
                val r = size.width * 0.03f
                drawLine(Color.Red, Offset(ex-r, ey-r), Offset(ex+r, ey+r), strokeWidth = 4f)
                drawLine(Color.Red, Offset(ex+r, ey-r), Offset(ex-r, ey+r), strokeWidth = 4f)
            }
        }
    }
}

@Composable
fun AnimatedPart(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.8f, animationSpec = tween(600))
    ) {
        content()
    }
}
