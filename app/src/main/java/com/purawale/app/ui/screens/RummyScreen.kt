package com.purawale.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.purawale.app.GameSession
import com.purawale.app.Member
import com.purawale.app.PlayingCard
import com.purawale.app.Suit

fun createDeck(): List<PlayingCard> {
    val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    return Suit.entries.flatMap { suit ->
        ranks.mapIndexed { index, rank ->
            PlayingCard(suit, rank, index + 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RummyScreen(
    user: Member,
    otherPlayer: Member?,
    otherPlayerName: String,
    session: GameSession?,
    onBack: () -> Unit,
    onUpdateState: (Map<String, Any>, String?, String?) -> Unit
) {
    var selectedCard by remember { mutableStateOf<PlayingCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Indian Rummy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF2E7D32)
    ) { padding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            val gameState = session.gameState
            val myHandStrings = (gameState["hand_${user.id}"] as? List<String>) ?: emptyList()
            val discardPileStrings = (gameState["discardPile"] as? List<String>) ?: emptyList()
            val deckCount = (gameState["deckCount"] as? Number)?.toInt() ?: 0
            
            val myHand = myHandStrings.mapNotNull { parseCard(it) }
            val topDiscard = discardPileStrings.lastOrNull()?.let { parseCard(it) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (otherPlayer != null) {
                                if (otherPlayer.photoUrl != null) {
                                    AsyncImage(
                                        model = otherPlayer.photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color(0xFF1B5E20), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1B5E20)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(otherPlayerName.take(1), color = Color.White)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                            
                            Column {
                                Text(
                                    text = when {
                                        session.winnerId != null -> if (session.winnerId == user.id) "Victory! 🏆" else "Game Over"
                                        session.status == "WAITING" -> "Waiting for Players..."
                                        session.currentTurn == user.id -> "Your Turn"
                                        else -> "$otherPlayerName's Turn"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                if (session.status == "ACTIVE") {
                                    Text("Remaining in Deck: $deckCount", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Board (Deck and Discard Pile)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deck
                    CardView(
                        card = null,
                        isBack = true,
                        onClick = {
                            if (session.currentTurn == user.id && session.winnerId == null) {
                                // Draw from deck logic
                                drawCard(session, user, fromDeck = true, onUpdateState)
                            }
                        }
                    )
                    
                    Spacer(Modifier.width(32.dp))
                    
                    // Discard Pile
                    CardView(
                        card = topDiscard,
                        onClick = {
                            if (session.currentTurn == user.id && session.winnerId == null && topDiscard != null) {
                                // Draw from discard pile logic
                                drawCard(session, user, fromDeck = false, onUpdateState)
                            }
                        }
                    )
                }

                Spacer(Modifier.weight(1f))

                // My Hand
                Text(
                    "Your Hand",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy((-20).dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    items(myHand) { card ->
                        CardView(
                            card = card,
                            isSelected = selectedCard == card,
                            onClick = {
                                if (session.currentTurn == user.id) {
                                    if (selectedCard == card) {
                                        // Discard this card
                                        discardCard(session, user, card, onUpdateState)
                                        selectedCard = null
                                    } else {
                                        selectedCard = card
                                    }
                                }
                            }
                        )
                    }
                }
                
                if (session.currentTurn == user.id && selectedCard != null && myHand.size == 11) {
                    Button(
                        onClick = { 
                            val newState = session.gameState.toMutableMap()
                            val finalHand = myHandStrings.toMutableList()
                            if (finalHand.remove(selectedCard?.toString())) {
                                newState["hand_${user.id}"] = finalHand
                                val discardPile = (newState["discardPile"] as? List<String>)?.toMutableList() ?: mutableListOf()
                                discardPile.add(selectedCard!!.toString())
                                newState["discardPile"] = discardPile
                                // Declare winner and set turn to null
                                onUpdateState(newState, null, user.id)
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                    ) {
                        Text("DECLARE & WIN")
                    }
                }
            }
        }
    }
}

@Composable
fun CardView(
    card: PlayingCard?,
    isBack: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val modifier = Modifier
        .size(width = 80.dp, height = 120.dp)
        .offset(y = if (isSelected) (-20).dp else 0.dp)
        .background(
            if (isBack) Color(0xFFB71C1C) else Color.White,
            RoundedCornerShape(8.dp)
        )
        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
        .clickable { onClick() }
        .padding(8.dp)

    Box(modifier = modifier) {
        if (!isBack && card != null) {
            Text(
                text = card.rank,
                color = card.suit.color,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = card.suit.symbol,
                color = card.suit.color,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = card.rank,
                color = card.suit.color,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        } else if (isBack) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🂠", color = Color.White, fontSize = 40.sp)
            }
        }
    }
}

fun parseCard(cardStr: String): PlayingCard? {
    if (cardStr.length < 2) return null
    val suitChar = cardStr.last()
    val rank = cardStr.substring(0, cardStr.length - 1)
    val suit = Suit.entries.find { it.symbol == suitChar.toString() } ?: return null
    val value = when(rank) {
        "A" -> 1
        "J" -> 11
        "Q" -> 12
        "K" -> 13
        else -> rank.toIntOrNull() ?: 0
    }
    return PlayingCard(suit, rank, value)
}

fun drawCard(
    session: GameSession,
    user: Member,
    fromDeck: Boolean,
    onUpdateState: (Map<String, Any>, String?, String?) -> Unit
) {
    val newState = session.gameState.toMutableMap()
    val deck = (newState["deck"] as? List<String>)?.toMutableList() ?: createDeck().map { it.toString() }.shuffled().toMutableList()
    val discardPile = (newState["discardPile"] as? List<String>)?.toMutableList() ?: mutableListOf()
    val myHand = (newState["hand_${user.id}"] as? List<String>)?.toMutableList() ?: mutableListOf()

    if (myHand.size >= 11) return // Already drawn (10 initial + 1 drawn)

    val card = if (fromDeck) {
        if (deck.isEmpty()) return
        deck.removeAt(0)
    } else {
        if (discardPile.isEmpty()) return
        discardPile.removeAt(discardPile.size - 1)
    }

    myHand.add(card)
    newState["deck"] = deck
    newState["deckCount"] = deck.size
    newState["discardPile"] = discardPile
    newState["hand_${user.id}"] = myHand
    
    // In Rummy, you draw THEN discard. So we don't change turn yet.
    onUpdateState(newState, session.currentTurn, null)
}

fun discardCard(
    session: GameSession,
    user: Member,
    card: PlayingCard,
    onUpdateState: (Map<String, Any>, String?, String?) -> Unit
) {
    val newState = session.gameState.toMutableMap()
    val myHand = (newState["hand_${user.id}"] as? List<String>)?.toMutableList() ?: return
    val discardPile = (newState["discardPile"] as? List<String>)?.toMutableList() ?: mutableListOf()

    val cardStr = card.toString()
    if (myHand.remove(cardStr)) {
        discardPile.add(cardStr)
        newState["hand_${user.id}"] = myHand
        newState["discardPile"] = discardPile
        
        val nextTurn = session.players.find { it != user.id } ?: user.id
        onUpdateState(newState, nextTurn, null)
    }
}
