package com.purawale.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purawale.app.FirebaseManager
import com.purawale.app.TriviaQuestion
import com.purawale.app.t
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTriviaScreen(
    onBack: () -> Unit
) {
    var questions by remember { mutableStateOf<List<TriviaQuestion>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        questions = FirebaseManager.getTriviaQuestions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Trivia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFFFC857),
                contentColor = Color(0xFF080B14),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = t("Add Question", "प्रश्न जोड़ें"))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(questions) { question ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(question.question, fontWeight = FontWeight.Bold)
                        question.options.forEachIndexed { index, option ->
                            Text(
                                text = "${index + 1}. $option",
                                color = if (index == question.correctAnswerIndex) Color(0xFF4CAF50) else Color.Unspecified,
                                fontWeight = if (index == question.correctAnswerIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTriviaDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newQuestion ->
                    scope.launch {
                        FirebaseManager.submitTriviaQuestion(newQuestion)
                        questions = FirebaseManager.getTriviaQuestions()
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun AddTriviaDialog(
    onDismiss: () -> Unit,
    onSave: (TriviaQuestion) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    var options = remember { mutableStateListOf("", "", "", "") }
    var correctAnswerIndex by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf("GENERAL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        shape = RoundedCornerShape(28.dp),
        title = { Text(t("Add Trivia Question", "सामान्य ज्ञान प्रश्न जोड़ें"), color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text(t("Question", "प्रश्न")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFC857),
                        focusedLabelColor = Color(0xFFFFC857),
                        cursorColor = Color(0xFFFFC857)
                    )
                )
                
                options.forEachIndexed { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = correctAnswerIndex == index,
                            onClick = { correctAnswerIndex = index },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFC857))
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { options[index] = it },
                            label = { Text(t("Option ${index + 1}", "विकल्प ${index + 1}")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFC857),
                                focusedLabelColor = Color(0xFFFFC857),
                                cursorColor = Color(0xFFFFC857)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(t("Category (e.g. HISTORY, BIRTHDAYS)", "श्रेणी (जैसे इतिहास, जन्मदिन)")) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFFC857),
                        focusedLabelColor = Color(0xFFFFC857),
                        cursorColor = Color(0xFFFFC857)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (questionText.isNotBlank() && options.all { it.isNotBlank() }) {
                        onSave(TriviaQuestion(
                            question = questionText,
                            options = options.toList(),
                            correctAnswerIndex = correctAnswerIndex,
                            category = category
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(t("Save", "सहेजें"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
