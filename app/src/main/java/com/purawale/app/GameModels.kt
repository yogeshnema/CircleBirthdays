package com.purawale.app

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.IgnoreExtraProperties

enum class Suit(val symbol: String, val color: Color) {
    SPADES("♠", Color(0xFF000000)),
    HEARTS("♥", Color(0xFFFF0000)),
    DIAMONDS("♦", Color(0xFFFF0000)),
    CLUBS("♣", Color(0xFF000000))
}

@Keep
data class PlayingCard(
    val suit: Suit,
    val rank: String,
    val value: Int
) {
    override fun toString(): String = "$rank${suit.symbol}"
}

@Keep
@IgnoreExtraProperties
data class GameSession(
    val id: String = "",
    val gameType: String = "", // "SNAKES_LADDERS", "CHESS", "TRIVIA"
    val players: List<String> = emptyList(), // Member IDs
    val playerNames: Map<String, String> = emptyMap(),
    val status: String = "WAITING", // "WAITING", "ACTIVE", "FINISHED"
    val currentTurn: String = "", // Member ID
    val gameState: Map<String, Any> = emptyMap(),
    val winnerId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class TriviaQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val category: String = "" // "HISTORY", "BIRTHDAYS", "RECIPES"
)

@Keep
@IgnoreExtraProperties
data class FamilyQuest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val pointsReward: Int = 0,
    val type: String = "" // "ADD_PHOTO", "FILL_DETAILS", "POST_RECIPE"
)
