package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.Meeting
import com.example.data.model.MeetingStatus
import com.example.data.model.MeetingTask
import com.example.data.model.Utterance
import com.example.ui.viewmodel.MeetingViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    meetingId: Long,
    viewModel: MeetingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val meetings by viewModel.allMeetings.collectAsState()
    val playingId by viewModel.currentPlayingMeetingId.collectAsState()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.data.local.SessionManager(context) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    val meeting = remember(meetings, meetingId) {
        meetings.find { it.id == meetingId }
    }

    if (meeting == null) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Детали встречи не найдены", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val isPlaying = playingId == meeting.id && isAudioPlaying
    var showExportMenu by remember { mutableStateOf(false) }

    val dateText = remember(meeting.date) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("ru")).format(Date(meeting.date))
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = meeting.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться / Экспорт")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Экспортировать в PDF") },
                                onClick = {
                                    showExportMenu = false
                                    if (!sessionManager.isProActive) {
                                        showUpgradeDialog = true
                                    } else {
                                        val uri = com.example.util.MeetingExportHelper.exportToPdf(context, meeting)
                                        if (uri != null) {
                                            com.example.util.MeetingExportHelper.shareFile(context, uri, "Поделиться протоколом PDF")
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Экспортировать в Word") },
                                onClick = {
                                    showExportMenu = false
                                    if (!sessionManager.isProActive) {
                                        showUpgradeDialog = true
                                    } else {
                                        val uri = com.example.util.MeetingExportHelper.exportToWord(context, meeting)
                                        if (uri != null) {
                                            com.example.util.MeetingExportHelper.shareFile(context, uri, "Поделиться протоколом Word")
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            )
                        }

                        if (showUpgradeDialog) {
                            AlertDialog(
                                onDismissRequest = { showUpgradeDialog = false },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                                        Text("Экспорт доступен в PRO", fontWeight = FontWeight.Bold)
                                    }
                                },
                                text = {
                                    Text("Экспорт протоколов в форматы PDF / Word доступен исключительно пользователям тарифа PRO.\n\nРазблокируйте безлимит и возможности экспорта прямо сейчас!")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showUpgradeDialog = false
                                            onNavigateToSubscription()
                                        }
                                    ) {
                                        Text("Перейти на PRO")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showUpgradeDialog = false }) {
                                        Text("Отмена")
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Player Card with Progress Slider
            PlayerCard(
                isPlaying = isPlaying,
                currentTimeMs = if (playingId == meeting.id) playbackPositionMs else 0L,
                totalDurationMs = meeting.durationMs,
                onPlayToggle = {
                    viewModel.playMeetingAudio(meeting)
                },
                onSeek = { seekTime ->
                    if (playingId != meeting.id) {
                        viewModel.playMeetingAudio(meeting)
                    }
                    viewModel.seekPlayingMeetingTo(seekTime)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Dynamic Tabs Header (5 main parts of a meeting)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                    text = { Text("Резюме") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Topic, contentDescription = null) },
                    text = { Text("Темы") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Gavel, contentDescription = null) },
                    text = { Text("Решения") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    text = { Text("Задачи") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null) },
                    text = { Text("Транскрипция") }
                )
            }

            // Tab contents
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> SummaryTab(meeting = meeting)
                    1 -> TopicsTab(meeting = meeting)
                    2 -> DecisionsTab(meeting = meeting)
                    3 -> TasksTab(meeting = meeting, onTaskToggle = { idx ->
                        viewModel.toggleTaskCompletion(meeting.id, idx)
                    })
                    4 -> TranscriptTab(
                        meeting = meeting,
                        playbackPositionMs = if (playingId == meeting.id) playbackPositionMs else -1L,
                        isAudioPlaying = isPlaying,
                        onUtteranceClick = { timestampMs ->
                            if (playingId != meeting.id) {
                                viewModel.playMeetingAudio(meeting)
                            }
                            viewModel.seekPlayingMeetingTo(timestampMs)
                        },
                        onEditUtterance = { index, speaker, text ->
                            viewModel.updateTranscriptUtterance(meeting.id, index, speaker, text)
                        }
                    )
                }
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun PlayerCard(
    isPlaying: Boolean,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onPlayToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var sliderPosition by remember(currentTimeMs) { mutableFloatStateOf(currentTimeMs.toFloat()) }
    val totalFloat = maxOf(totalDurationMs.toFloat(), 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Плей",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPlaying) "Воспроизведение записи..." else "Запись переговоров",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Длительность разговора: ${formatMs(totalDurationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = sliderPosition.coerceIn(0f, totalFloat),
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    onSeek(sliderPosition.toLong())
                },
                valueRange = 0f..totalFloat,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentTimeMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = formatMs(totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// TAB 0: Резюме (Summary, 3-5 sentences)
@Composable
fun SummaryTab(meeting: Meeting) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Умный ИИ проводит глубокий анализ встречи...")
        return
    }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Краткое резюме ИИ",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = meeting.summaryOverview ?: "Общая краткая сводка встреч формируется ИИ...",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                    )
                }
            }
        }
    }
}

// TAB 1: Темы (Topics, bullet points)
@Composable
fun TopicsTab(meeting: Meeting) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Анализируем контекст и выделяем ключевые темы...")
        return
    }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)

    val topics = remember(meeting.summaryTopics, meeting.summaryOverview) {
        // Fallback generator in case older/legacy DB record doesn't have summaryTopics filled
        meeting.summaryTopics ?: meeting.summaryOverview?.split(". ")
            ?.filter { it.isNotBlank() && it.length > 20 }
            ?.map { it.trim().removeSuffix(".") } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Topic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "О чём говорили (по темам)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (topics.isEmpty()) {
                        Text(
                            text = "Основные разделы темы беседы не зафиксированы.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        topics.forEachIndexed { index, topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp, end = 12.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 2: Решения (Decisions, what decisions agreed and by whom)
@Composable
fun DecisionsTab(meeting: Meeting) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Ищем принятые финальные решения в беседе...")
        return
    }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)
    val decisions = meeting.summaryDecisions ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Согласованные решения",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (decisions.isEmpty()) {
                        Text(
                            text = "Системных решений или соглашений по итогам встречи не обнаружено.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        decisions.forEachIndexed { _, decision ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .padding(top = 2.dp, end = 12.dp)
                                        .size(18.dp)
                                )
                                Text(
                                    text = decision,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 3: Задачи (Tasks, checkboxes, deadlines)
@Composable
fun TasksTab(
    meeting: Meeting,
    onTaskToggle: (Int) -> Unit
) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Распределяем задачи и фиксируем сроки исполнителей...")
        return
    }

    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)
    val tasks = meeting.summaryTasks ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Действия и ответственные",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (tasks.isEmpty()) {
                        Text(
                            text = "Задач или конкретных поручений по итогам беседы не зафиксировано.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        tasks.forEachIndexed { idx, task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskToggle(idx) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { onTaskToggle(idx) },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.taskText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Assignee Badge
                                        val isParticipantA = task.assignedTo.lowercase() == meeting.participantA.lowercase()
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isParticipantA) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Кому: ${task.assignedTo}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isParticipantA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        // Deadline visual hint inside text can be styled dynamically
                                        val hasDeadlineHint = remember(task.taskText) {
                                            val textLc = task.taskText.lowercase()
                                            textLc.contains("до ") || textLc.contains("к ") || textLc.contains("срок")
                                        }
                                        if (hasDeadlineHint) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "С пометкой срока",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 4: Транскрипция (Transcription list with search, highlights, click-to-seek, edit dialog)
@Composable
fun TranscriptTab(
    meeting: Meeting,
    playbackPositionMs: Long,
    isAudioPlaying: Boolean,
    onUtteranceClick: (Long) -> Unit,
    onEditUtterance: (Int, String, String) -> Unit
) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Расшифровываем аудиодорожку по спикерам...")
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val transcriptList = meeting.transcript ?: emptyList()

    val filteredList = remember(searchQuery, transcriptList) {
        if (searchQuery.isBlank()) {
            transcriptList.mapIndexed { index, utterance -> index to utterance }
        } else {
            transcriptList.mapIndexed { index, utterance -> index to utterance }
                .filter { (_, utterance) ->
                    utterance.text.contains(searchQuery, ignoreCase = true) ||
                    utterance.speaker.contains(searchQuery, ignoreCase = true)
                }
        }
    }

    // Dialogue playback highlight detection
    val currentActiveIndex = remember(playbackPositionMs, transcriptList, isAudioPlaying) {
        if (playbackPositionMs < 0) -1 else {
            transcriptList.indexOfLast { playbackPositionMs >= it.timestampMs }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Autoscroll to current highlighted sentence if playing
    LaunchedEffect(currentActiveIndex) {
        if (currentActiveIndex >= 0 && isAudioPlaying) {
            val visualIndex = filteredList.indexOfFirst { it.first == currentActiveIndex }
            if (visualIndex >= 0) {
                coroutineScope.launch {
                    listState.animateScrollToItem(visualIndex)
                }
            }
        }
    }

    // Edit states
    var editingUtteranceIndex by remember { mutableStateOf<Int?>(null) }
    var editingUtteranceSpeaker by remember { mutableStateOf("") }
    var editingUtteranceText by remember { mutableStateOf("") }

    if (editingUtteranceIndex != null) {
        AlertDialog(
            onDismissRequest = { editingUtteranceIndex = null },
            title = {
                Text("Редактировать реплику", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editingUtteranceSpeaker,
                        onValueChange = { editingUtteranceSpeaker = it },
                        label = { Text("Имя говорящего") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingUtteranceText,
                        onValueChange = { editingUtteranceText = it },
                        label = { Text("Текст реплики") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingUtteranceIndex?.let { idx ->
                            onEditUtterance(idx, editingUtteranceSpeaker, editingUtteranceText)
                        }
                        editingUtteranceIndex = null
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUtteranceIndex = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Filter TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск по расшифровке встречи...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ничего не найдено по вашему запросу.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(filteredList) { _, pair ->
                    val originalIndex = pair.first
                    val item = pair.second
                    val isParticipantA = item.speaker.lowercase() == meeting.participantA.lowercase()
                    val isActivePlayCell = originalIndex == currentActiveIndex

                    val formatTime = remember(item.timestampMs) {
                        val totalSecs = item.timestampMs / 1000
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        String.format("%02d:%02d", mins, secs)
                    }

                    // Animate colors of highlighted cell
                    val animBgColor by animateColorAsState(
                        targetValue = if (isActivePlayCell) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            if (isParticipantA) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
                        },
                        label = "cellBgColor"
                    )

                    val animBorderColor by animateColorAsState(
                        targetValue = if (isActivePlayCell) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            if (isParticipantA) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        },
                        label = "cellBorderColor"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUtteranceClick(item.timestampMs) },
                        horizontalArrangement = if (isParticipantA) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isParticipantA) {
                            AvatarCircle(name = item.speaker, isParticipantA = true)
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Dialogue Bubble Card
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isParticipantA) 4.dp else 16.dp,
                                        bottomEnd = if (!isParticipantA) 4.dp else 16.dp
                                    )
                                )
                                .background(animBgColor)
                                .border(
                                    width = if (isActivePlayCell) 2.dp else 1.dp,
                                    color = animBorderColor,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isParticipantA) 4.dp else 16.dp,
                                        bottomEnd = if (!isParticipantA) 4.dp else 16.dp
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Live play wave indicator
                                    if (isActivePlayCell) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Говорит",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = item.speaker,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isParticipantA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = formatTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    
                                    // Edit actions button (pencil)
                                    IconButton(
                                        onClick = {
                                            editingUtteranceIndex = originalIndex
                                            editingUtteranceSpeaker = item.speaker
                                            editingUtteranceText = item.text
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Редактировать реплику",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4e-1f.coerceAtLeast(0.4f)),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActivePlayCell) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25,
                                color = if (isActivePlayCell) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!isParticipantA) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AvatarCircle(name = item.speaker, isParticipantA = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarCircle(name: String, isParticipantA: Boolean) {
    val initial = remember(name) {
        if (name.isNotBlank()) name.first().uppercase() else "У"
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isParticipantA) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isParticipantA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun PendingStateIndicator(subtext: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
