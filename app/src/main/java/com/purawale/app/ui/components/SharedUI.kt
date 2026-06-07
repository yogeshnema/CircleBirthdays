package com.purawale.app.ui.components

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.purawale.app.t

@Composable
fun SpeechToTextButton(onResult: (String) -> Unit) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val recognizerIntent = remember {
        android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { isListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    IconButton(onClick = {
        if (isListening) {
            speechRecognizer.stopListening()
        } else {
            speechRecognizer.startListening(recognizerIntent)
        }
    }) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Speech to Text",
            tint = if (isListening) Color.Red else Color(0xFF5D4037)
        )
    }
}

@Composable
fun EventAvatar(photoUrl: String?, name: String = "", size: androidx.compose.ui.unit.Dp = 44.dp) {
    if (!photoUrl.isNullOrEmpty() && photoUrl.startsWith("http")) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape).border(1.5.dp, Color(0xFF5D4037), CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_report_image),
            onError = { state ->
                Log.e("CircleBirthdays", "EventAvatar load failed for $name: ${state.result.throwable.message}")
            }
        )
    } else {
        Box(
            modifier = Modifier.size(size).background(Color(0xFFEFEBE9), CircleShape).border(1.5.dp, Color(0xFF5D4037), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (name.isNotBlank()) {
                val initials = name.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }
                Text(initials, style = MaterialTheme.typography.labelLarge, color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = Color(0xFF5D4037))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF080B14).copy(alpha = 0.96f),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080B14), Color(0xFF121827), Color(0xFF241626))
                )
            )
    ) {
        content(PaddingValues(0.dp))
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val gold = Color(0xFFFFC857)

    TextField(
        value = selectedDate,
        onValueChange = { },
        readOnly = true,
        enabled = enabled,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { showDatePicker = true },
        trailingIcon = {
            IconButton(onClick = { if (enabled) showDatePicker = true }, enabled = enabled) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date", tint = gold.copy(alpha = 0.7f))
            }
        },
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.5f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = gold,
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
            disabledIndicatorColor = Color.White.copy(alpha = 0.05f)
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))
                            onDateSelected(date)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
                    shape = RoundedCornerShape(24.dp)
                ) { Text(t("OK", "ठीक है"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(t("Cancel", "रद्द करें"), color = Color.White.copy(alpha = 0.6f)) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF1A1C1E)
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF1A1C1E),
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.6f),
                    subheadContentColor = Color.White.copy(alpha = 0.6f),
                    yearContentColor = Color.White,
                    currentYearContentColor = gold,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = gold,
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = gold,
                    todayContentColor = gold,
                    todayDateBorderColor = gold
                )
            )
        }
    }
}
