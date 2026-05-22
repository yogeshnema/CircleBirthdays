package com.purawale.app

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object ProfileList : Screen()
    object Gallery : Screen()
    object Discussions : Screen()
    object Messages : Screen()
    object Cookbook : Screen()
    object Traditions : Screen()
    object MemoryLane : Screen()
    object FamilyTree : Screen()
    object Calendar : Screen()
    object Notifications : Screen()
    object FamilyGames : Screen()
    object Trivia : Screen()
    object ManageTrivia : Screen()
    data class GameLobby(val gameType: String) : Screen()
    data class SnakesAndLadders(val sessionId: String) : Screen()
    data class Chess(val sessionId: String) : Screen()
    data class Chaupad(val sessionId: String) : Screen()
    data class Hangman(val sessionId: String) : Screen()
    data class Rummy(val sessionId: String) : Screen()

    data class Antakshari(val sessionId: String) : Screen()
    data class Chat(val otherMember: Member) : Screen()
    data class EditProfile(val member: Member?, val isReadOnly: Boolean = false) : Screen()

    companion object {
        fun fromGameType(gameType: String, sessionId: String): Screen {
            return when (gameType) {
                "SNAKES_LADDERS" -> SnakesAndLadders(sessionId)
                "CHESS" -> Chess(sessionId)
                "CHAUPAD" -> Chaupad(sessionId)
                "HANGMAN" -> Hangman(sessionId)
                "RUMMY" -> Rummy(sessionId)
                "ANTAKSHARI" -> Antakshari(sessionId)
                else -> FamilyGames
            }
        }
    }
}
