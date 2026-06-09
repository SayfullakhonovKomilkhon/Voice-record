package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeetingStatus
import com.example.ui.viewmodel.MeetingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    meetingId: Long,
    viewModel: MeetingViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val meetings by viewModel.allMeetings.collectAsState()
    val activeStates by viewModel.activeProcessingStates.collectAsState()

    val currentMeeting = remember(meetings) { meetings.find { it.id == meetingId } }
    val isDark = false

    // Retrieve active background service progress state
    val state = activeStates[meetingId]
    val progress = state?.progress ?: 0.15f
    val activePhaseText = state?.phaseText ?: "Подготовка ресурсов..."

    // Visual pulse animation for glowing effects
    val infiniteTransition = rememberInfiniteTransition(label = "processing_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radialPulse"
    )

    // Handle auto-completion redirect
    LaunchedEffect(currentMeeting?.status) {
        if (currentMeeting?.status == MeetingStatus.COMPLETED) {
            onNavigateToDetail(meetingId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "ИИ-Анализ встречи",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
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
                // Topic metadata block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentMeeting?.title ?: "Обработка аудиозаписи...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Собеседники: ${currentMeeting?.participantA ?: "Участник A"} • ${currentMeeting?.participantB ?: "Участник B"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Glowing animated circular indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .padding(20.dp)
                ) {
                    // Pulsing ambient background radial
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Track rings & circular loaders
                    CircularProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        strokeCap = strokeCapAndStyle()
                    )

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ОБРАБОТКА",
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Stage Checklist representing our 4 robust phases of backend AI analysis!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E2330) else Color(0xFFF1F3F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProcessingStageRow(
                            title = "Загрузка фонограммы и дешумация",
                            subtext = "Удаление отголосков, очистка аудиопотока",
                            isActive = progress >= 0.10f && progress < 0.35f,
                            isCompleted = progress >= 0.35f
                        )
                        ProcessingStageRow(
                            title = "Калибровка и сопоставление голосов",
                            subtext = "Профилирование спикеров по биометрии",
                            isActive = progress >= 0.35f && progress < 0.60f,
                            isCompleted = progress >= 0.60f
                        )
                        ProcessingStageRow(
                            title = "Транскрибирование речи (Gemini AI)",
                            subtext = "Дешифровка диалога по ролям участников",
                            isActive = progress >= 0.60f && progress < 0.85f,
                            isCompleted = progress >= 0.85f
                        )
                        ProcessingStageRow(
                            title = "Формирование выводов и задач",
                            subtext = "Синтез расширенного ИИ-протокола решений",
                            isActive = progress >= 0.85f && progress < 1.0f,
                            isCompleted = currentMeeting?.status == MeetingStatus.COMPLETED
                        )
                    }
                }

                // Context message layout letting user close app safely
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Вы можете безопасно закрыть приложение. Обработка полностью выполняется в фоновом режиме на сервере, а мы отправим вам уведомление, когда расшифровка закончится.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Button triggers
                    if (currentMeeting?.status == MeetingStatus.FAILED) {
                        Button(
                            onClick = { viewModel.retryTranscription(currentMeeting) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("retry_processing_btn"),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Повторить отправку запроса", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onNavigateToList,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("go_to_list_btn"),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Icon(imageVector = Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Вернуться к списку встреч", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// Custom Helper to keep import clean
private fun strokeCapAndStyle() = androidx.compose.ui.graphics.StrokeCap.Round

@Composable
fun ProcessingStageRow(
    title: String,
    subtext: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon state bubble switcher
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        isActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    }
                )
        ) {
            when {
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Пройдено",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                isActive -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Detailed textual metadata labels
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.onSurface
                    isActive -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
            )
            if (isActive) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}
