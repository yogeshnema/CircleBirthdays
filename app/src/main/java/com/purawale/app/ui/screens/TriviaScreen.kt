package com.purawale.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purawale.app.TriviaQuestion
import com.purawale.app.ui.theme.LightGolden
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaScreen(
    questions: List<TriviaQuestion>,
    onBack: () -> Unit,
    onComplete: (Int) -> Unit
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var timeLeft by remember { mutableStateOf(15) }
    var isFinished by remember { mutableStateOf(false) }

    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    LaunchedEffect(currentQuestionIndex, isFinished) {
        if (!isFinished && currentQuestion != null) {
            timeLeft = 15
            while (timeLeft > 0 && selectedAnswer == null) {
                delay(1000)
                timeLeft--
            }
            if (selectedAnswer == null) {
                // Time's up
                selectedAnswer = -1
                delay(2000)
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    selectedAnswer = null
                } else {
                    isFinished = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Trivia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightGolden
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White.copy(alpha = 0.3f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isFinished || questions.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Trivia Finished!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Your Score: $score / ${questions.size}", fontSize = 20.sp)
                        Text("Points Earned: ${score * 10}", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { onComplete(score) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037))
                        ) {
                            Text("Claim Points", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else if (currentQuestion != null) {
                Text(
                    "Question ${currentQuestionIndex + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF5D4037)
                )
                LinearProgressIndicator(
                    progress = { timeLeft / 15f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = Color(0xFF5D4037),
                    trackColor = LightGolden
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = currentQuestion.question,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5D4037),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        currentQuestion.options.forEachIndexed { index, option ->
                            val color = when {
                                selectedAnswer == null -> Color.White
                                index == currentQuestion.correctAnswerIndex -> Color(0xFFA5D6A7) // Light Green
                                index == selectedAnswer -> Color(0xFFEF9A9A) // Light Red
                                else -> Color.White
                            }

                            OutlinedButton(
                                onClick = {
                                    if (selectedAnswer == null) {
                                        selectedAnswer = index
                                        if (index == currentQuestion.correctAnswerIndex) {
                                            score++
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = color),
                                border = BorderStroke(1.dp, Color(0xFF5D4037)),
                                enabled = selectedAnswer == null
                            ) {
                                Text(
                                    option, 
                                    color = Color(0xFF5D4037),
                                    fontWeight = if (selectedAnswer != null && (index == currentQuestion.correctAnswerIndex || index == selectedAnswer)) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                if (selectedAnswer != null) {
                    LaunchedEffect(selectedAnswer) {
                        delay(2000)
                        if (currentQuestionIndex < questions.size - 1) {
                            currentQuestionIndex++
                            selectedAnswer = null
                        } else {
                            isFinished = true
                        }
                    }
                }
            }
        }
    }
}
