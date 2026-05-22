package com.purawale.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.purawale.app.GameSession
import com.purawale.app.Member
import com.purawale.app.t
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntakshariScreen(
    user: Member,
    otherPlayer: Member?,
    otherPlayerName: String,
    session: GameSession?,
    onBack: () -> Unit,
    onSendRecording: (File) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

    mediaPlayer.setOnCompletionListener {
        isPlaying = false
        playingUrl = null
    }

    var recordingStartTime by remember { mutableLongStateOf(0L) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.let {
                    try { it.stop() } catch (e: Exception) {}
                    it.release()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(t("Antakshari", "अंताक्षरी"), fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Turn Info
                val isMyTurn = session?.currentTurn == user.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMyTurn) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isMyTurn) t("Your Turn!", "आपकी बारी!") else t("Waiting for $otherPlayerName...", "$otherPlayerName का इंतज़ार कर रहे हैं..."),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isMyTurn) Color(0xFF2E7D32) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isMyTurn) {
                            Text(
                                t("Sing a song starting with the last letter!", "पिछले अक्षर से शुरू होने वाला गाना गाएं!"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Last Letter / Hint
                val lastLetter = session?.gameState?.get("lastLetter") as? String
                if (lastLetter != null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t("Last letter: ", "पिछला अक्षर: "), fontWeight = FontWeight.Medium)
                            Text(
                                lastLetter,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Recordings List
                val recordings = session?.gameState?.get("recordings") as? List<Map<String, String>> ?: emptyList()
                val listState = rememberLazyListState()
                
                LaunchedEffect(recordings.size) {
                    if (recordings.isNotEmpty()) {
                        listState.animateScrollToItem(0)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(recordings.asReversed()) { rec ->
                        val isMe = rec["senderId"] == user.id
                        val senderName = if (isMe) t("You", "आप") else otherPlayerName
                        val senderPhoto = if (isMe) user.photoUrl else otherPlayer?.photoUrl
                        val isThisPlaying = playingUrl == rec["url"]

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isMe) {
                                UserAvatar(senderPhoto)
                                Spacer(Modifier.width(8.dp))
                            }
                            
                            Card(
                                modifier = Modifier.widthIn(max = 280.dp),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            val url = rec["url"] ?: return@IconButton
                                            if (isThisPlaying && isPlaying) {
                                                mediaPlayer.pause()
                                                isPlaying = false
                                            } else if (isThisPlaying && !isPlaying) {
                                                mediaPlayer.start()
                                                isPlaying = true
                                            } else {
                                                try {
                                                    mediaPlayer.reset()
                                                    mediaPlayer.setDataSource(url)
                                                    mediaPlayer.prepareAsync()
                                                    mediaPlayer.setOnPreparedListener { 
                                                        it.start()
                                                        isPlaying = true
                                                        playingUrl = url
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = if (isThisPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Icon(
                                            if (isThisPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = if (isThisPlaying) Color.White else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Text(senderName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(rec["timestamp"] ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }

                            if (isMe) {
                                Spacer(Modifier.width(8.dp))
                                UserAvatar(senderPhoto)
                            }
                        }
                    }
                }
            }

            // Bottom Controls for recording
            val isMyTurn = session?.currentTurn == user.id
            if (isMyTurn) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isRecording) {
                            Text(
                                t("Recording...", "रिकॉर्डिंग..."),
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.2f))
                                )
                            }
                            
                            FloatingActionButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@FloatingActionButton
                                    }

                                    try {
                                        if (!isRecording) {
                                            val file = File(context.cacheDir, "antakshari_${System.currentTimeMillis()}.mp4")
                                            audioFile = file
                                            
                                            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                MediaRecorder(context)
                                            } else {
                                                MediaRecorder()
                                            }
                                            
                                            recorder.apply {
                                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                setOutputFile(file.absolutePath)
                                                prepare()
                                                start()
                                            }
                                            mediaRecorder = recorder
                                            isRecording = true
                                            recordingStartTime = System.currentTimeMillis()
                                        } else {
                                            val duration = System.currentTimeMillis() - recordingStartTime
                                            mediaRecorder?.let {
                                                if (duration > 1000) {
                                                    try { it.stop() } catch (e: Exception) { e.printStackTrace() }
                                                }
                                                it.release()
                                            }
                                            mediaRecorder = null
                                            isRecording = false
                                            if (duration > 1000) {
                                                audioFile?.let { onSendRecording(it) }
                                            } else {
                                                Toast.makeText(context, "Recording too short", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Recording failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        mediaRecorder?.let { try { it.release() } catch (ex: Exception) {} }
                                        mediaRecorder = null
                                        isRecording = false
                                    }
                                },
                                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Record",
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        
                        Text(
                            if (isRecording) t("Tap to stop", "रोकने के लिए दबाएं") else t("Tap to start singing", "गाने के लिए दबाएं"),
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserAvatar(photoUrl: String?) {
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, Color.LightGray, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
        }
    }
}
