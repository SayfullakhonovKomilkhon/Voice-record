package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MeetingViewModel
import kotlinx.coroutines.delay
import kotlin.math.log10
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: MeetingViewModel,
    onNavigateBack: () -> Unit,
    onStopAndStartProcessing: (Long) -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val rawAmplitude by viewModel.amplitude.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isDark = isSystemInDarkTheme()

    val title by viewModel.meetingTitle.collectAsState()
    val partA by viewModel.participantA.collectAsState()
    val partB by viewModel.participantB.collectAsState()

    // Smooth amplitude transition for the visualizer
    val animatedAmplitude = animateFloatAsState(
        targetValue = max(0f, rawAmplitude.toFloat()),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "amplitudeAnimator"
    )

    // Format seconds to HH:MM:SS
    val timerText = remember(recordingSeconds) {
        val hours = recordingSeconds / 3600
        val minutes = (recordingSeconds % 3600) / 60
        val seconds = recordingSeconds % 60
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Capture amplitude values over time for waveform movement
    val waveBars = remember { mutableStateListOf<Float>().apply { repeat(32) { add(0.1f) } } }
    
    LaunchedEffect(rawAmplitude, isPaused) {
        if (!isPaused && isRecording) {
            // Generate standard scaled fraction
            val fraction = (rawAmplitude.toFloat() / 32768f).coerceIn(0.01f, 1f)
            waveBars.add(fraction)
            if (waveBars.size > 32) {
                waveBars.removeAt(0)
            }
        } else {
            // Flatten slightly when paused or stopped
            waveBars.add(0.02f)
            if (waveBars.size > 32) {
                waveBars.removeAt(0)
            }
        }
    }

    // Infinite pulse scale animation for buttons & indicators
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Протокол встречи",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // If user goes back, recording stays active in foreground! Let them know.
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("recording_back")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topic card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E2330) else Color(0xFFF1F3F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title.ifBlank { "Переговоры без названия" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$partA • $partB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Waveform Indicator & Timer layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pulse LED indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(if (!isPaused && isRecording) pulseScale else 1.0f)
                                .clip(CircleShape)
                                .background(
                                    if (isPaused) MaterialTheme.colorScheme.secondary
                                    else if (isRecording) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                        Text(
                            text = if (isPaused) "ЗАПИСЬ ПРИОСТАНОВЛЕНА" else "ИДЕТ ЗАПИСЬ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (isPaused) MaterialTheme.colorScheme.secondary else if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Huge Digital Timer
                    Text(
                        text = timerText,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Sound waves indicator (Ripple Wave canvas)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 8.dp.toPx()
                            val cornerSize = 4.dp.toPx()
                            val spacing = 6.dp.toPx()
                            val barsCount = waveBars.size
                            
                            val startX = (size.width - (barsCount * (barWidth + spacing))) / 2f
                            val centerY = size.height / 2f

                            waveBars.forEachIndexed { index, fraction ->
                                // Standard dynamic height based on amplitude fraction
                                val height = max(4.dp.toPx(), fraction * size.height * 0.9f)
                                val x = startX + index * (barWidth + spacing)
                                val y = centerY - height / 2f

                                // Beautiful glowing color gradient logic matching Material design
                                val colorBrush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3),
                                        Color(0xFF8E24AA)
                                    )
                                )

                                drawRoundRect(
                                    brush = colorBrush,
                                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerSize, cornerSize)
                                )
                            }
                        }
                    }
                }

                // Control Triggers Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause Button Layout
                    IconButton(
                        onClick = {
                            if (isPaused) {
                                viewModel.resumeRecording()
                            } else {
                                viewModel.pauseRecording()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2A2E3D) else Color(0xFFE3E8F3))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .testTag("pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Возобновить" else "Пауза",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Large Stop circular slider action button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(92.dp)
                            .scale(if (!isPaused && isRecording) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .clickable {
                                // Stop and complete meeting recording
                                viewModel.stopRecording { newId ->
                                    onStopAndStartProcessing(newId)
                                }
                            }
                            .testTag("stop_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Остановить",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}
