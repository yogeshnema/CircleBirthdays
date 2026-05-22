package com.purawale.app.ui.components

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun EventAvatar(photoUrl: String?, name: String = "") {
    if (!photoUrl.isNullOrEmpty() && photoUrl.startsWith("http")) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(CircleShape).border(1.5.dp, Color(0xFF5D4037), CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = android.R.drawable.ic_menu_report_image),
            onError = { state ->
                Log.e("CircleBirthdays", "EventAvatar load failed for $name: ${state.result.throwable.message}")
            }
        )
    } else {
        Box(
            modifier = Modifier.size(44.dp).background(Color(0xFFEFEBE9), CircleShape).border(1.5.dp, Color(0xFF5D4037), CircleShape),
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

@Composable
fun ProfileField(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
    }
}
