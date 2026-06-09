package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.viewmodel.HighlightsExtractionState
import com.example.ui.viewmodel.MeetingHighlightsResult
import com.example.ui.viewmodel.HighlightActionItem
import com.example.ui.theme.CorporatePrimaryDark
import com.example.ui.theme.CorporatePrimaryLight
import com.example.ui.theme.CorporateSecondaryDark
import com.example.ui.theme.CorporateSecondaryLight
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
    val highlightsMap by viewModel.highlightsState.collectAsState()

    val activeThemeName by viewModel.activeThemeColor.collectAsState()
    val themeColors = remember(activeThemeName) { com.example.ui.theme.ThemeColorsProvider.getColors(activeThemeName) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.data.local.SessionManager(context) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showHighlightsPanel by remember { mutableStateOf(false) }
    
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

    val isDark = false

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val primaryColor = themeColors.primary
                val secondaryColor = themeColors.secondary
                val radius = size.minDimension * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.06f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.05f),
                        radius = radius
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryColor.copy(alpha = 0.06f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.95f),
                        radius = radius
                    )
                )
            },
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                ),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                showHighlightsPanel = true
                                if (highlightsMap[meeting.id] == null || highlightsMap[meeting.id] is HighlightsExtractionState.Idle) {
                                    viewModel.extractHighlightsAndActions(meeting)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "ИИ-Хайлайты",
                                tint = themeColors.primary
                            )
                        }
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
                    }
                }
            )
        },
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
                themeColors = themeColors,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Dynamic Tabs Header (5 main parts of a meeting)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = themeColors.primary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = themeColors.primary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Summarize, contentDescription = null, tint = if (selectedTab == 0) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    text = { Text("Резюме", color = if (selectedTab == 0) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Topic, contentDescription = null, tint = if (selectedTab == 1) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    text = { Text("Темы", color = if (selectedTab == 1) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = if (selectedTab == 2) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    text = { Text("Решения", color = if (selectedTab == 2) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null, tint = if (selectedTab == 3) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    text = { Text("Задачи", color = if (selectedTab == 3) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Forum, contentDescription = null, tint = if (selectedTab == 4) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    text = { Text("Транскрипция", color = if (selectedTab == 4) themeColors.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Tab contents
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> SummaryTab(
                        meeting = meeting,
                        highlightsMap = highlightsMap,
                        themeColors = themeColors,
                        onTriggerAnalysis = {
                            showHighlightsPanel = true
                            if (highlightsMap[meeting.id] == null || highlightsMap[meeting.id] is HighlightsExtractionState.Idle) {
                                viewModel.extractHighlightsAndActions(meeting)
                            }
                        }
                    )
                    1 -> TopicsTab(meeting = meeting, themeColors = themeColors)
                    2 -> DecisionsTab(meeting = meeting, themeColors = themeColors)
                    3 -> TasksTab(meeting = meeting, themeColors = themeColors, onTaskToggle = { idx ->
                        viewModel.toggleTaskCompletion(meeting.id, idx)
                    })
                    4 -> TranscriptTab(
                        meeting = meeting,
                        playbackPositionMs = if (playingId == meeting.id) playbackPositionMs else -1L,
                        isAudioPlaying = isPlaying,
                        themeColors = themeColors,
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

    if (showHighlightsPanel) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showHighlightsPanel = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            themeColors.primary,
                                            themeColors.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ИИ-Ассистент Gemini",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(onClick = { showHighlightsPanel = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
                
                Text(
                    text = "Выделение ключевых договоренностей и конкретных задач",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                val highlightStateForMeeting = highlightsMap[meeting.id] ?: HighlightsExtractionState.Idle

                when (highlightStateForMeeting) {
                    is HighlightsExtractionState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = themeColors.primary.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = "ИИ готов проанализировать транскрипт и составить списки хайлайтов и экшн-айлемов ваших переговоров.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { viewModel.extractHighlightsAndActions(meeting) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColors.primary
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Запустить ИИ-Анализ")
                                }
                            }
                        }
                    }
                    is HighlightsExtractionState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = themeColors.primary,
                                    strokeWidth = 4.dp
                                )
                                val pulseTransition = rememberInfiniteTransition(label = "pulse_text")
                                val pulseAlpha by pulseTransition.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                Text(
                                    text = "ИИ анализирует транскрипт встречи...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = pulseAlpha)
                                )
                                Text(
                                    text = "Выделяем задачи, ответственных и приоритеты",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is HighlightsExtractionState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    text = "Не удалось выполнить анализ",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = highlightStateForMeeting.error,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { viewModel.extractHighlightsAndActions(meeting) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Повторить")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Попробовать снова")
                                }
                            }
                        }
                    }
                    is HighlightsExtractionState.Success -> {
                        val result = highlightStateForMeeting.result
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                Text(
                                    text = "✨ Ключевые хайлайты и договоренности",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        result.highlights.forEach { highlight ->
                                            Row(
                                                verticalAlignment = Alignment.Top,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = 6.dp)
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(themeColors.primary)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = highlight,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "📋 Выделенные задачи и экшины",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (result.actionItems.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Задачи в разговоре не обнаружены",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(result.actionItems) { index, item ->
                                    val priorityColor = when (item.priority.lowercase()) {
                                        "высокий" -> MaterialTheme.colorScheme.error
                                        "средний" -> Color(0xFFF97316)
                                        "низкий" -> themeColors.secondary
                                        else -> MaterialTheme.colorScheme.outline
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val nameInitial = item.owner.firstOrNull()?.uppercase() ?: "?"
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(themeColors.secondary.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = nameInitial,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = themeColors.secondary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = item.owner,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = themeColors.secondary
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(priorityColor.copy(alpha = 0.12f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = item.priority,
                                                        color = priorityColor,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = item.task,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        val textToShare = StringBuilder().apply {
                                            append("✨ Ключевые хайлайты встречи \"${meeting.title}\":\n")
                                            result.highlights.forEach { append("- $it\n") }
                                            append("\n📋 Список экшн-задач:\n")
                                            result.actionItems.forEach { 
                                                append("- [${it.owner}] (${it.priority}): ${it.task}\n")
                                            }
                                        }.toString()

                                        val shareIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Поделиться анализом Gemini"))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = themeColors.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Поделиться сводкой и задачами")
                                }
                            }
                        }
                    }
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
    themeColors: com.example.ui.theme.CustomThemeColors,
    modifier: Modifier = Modifier
) {
    val isDark = false
    var sliderPosition by remember(currentTimeMs) { mutableFloatStateOf(currentTimeMs.toFloat()) }
    val totalFloat = maxOf(totalDurationMs.toFloat(), 1f)

    // Breathing pulse scale animation for active items
    val infiniteTransition = rememberInfiniteTransition(label = "detail_playback_pulse")
    val pulseScale by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1250, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPlaying) {
                themeColors.primary.copy(alpha = 0.5f)
            } else if (isDark) {
                Color(0xFF2D323E)
            } else {
                Color(0xFFDDE2EA)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 6.dp else 2.dp
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = themeColors.primary.copy(alpha = 0.08f),
                        contentColor = themeColors.primary
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("detail_play_toggle")
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
                    thumbColor = themeColors.primary,
                    activeTrackColor = themeColors.primary,
                    inactiveTrackColor = themeColors.primary.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_audio_slider")
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
fun SummaryTab(
    meeting: Meeting,
    highlightsMap: Map<Long, HighlightsExtractionState>,
    themeColors: com.example.ui.theme.CustomThemeColors,
    onTriggerAnalysis: () -> Unit
) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Умный ИИ проводит глубокий анализ встречи...")
        return
    }

    val isDark = false
    val borderColor = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI HIGHLIGHT ASSISTANT BANNER
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, themeColors.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.primary.copy(alpha = 0.04f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = onTriggerAnalysis,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        themeColors.primary,
                                        themeColors.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Договоренности и Задачи Gemini AI",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = themeColors.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Интеллектуальное извлечение хайлайтов и экшн-айлемов из расшифровки вашего разговора",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = themeColors.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }

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
                            tint = themeColors.primary,
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
fun TopicsTab(meeting: Meeting, themeColors: com.example.ui.theme.CustomThemeColors) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Анализируем контекст и выделяем ключевые темы...")
        return
    }

    val isDark = false
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
                            tint = themeColors.secondary,
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
                                        .background(themeColors.secondary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColors.secondary
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
fun DecisionsTab(meeting: Meeting, themeColors: com.example.ui.theme.CustomThemeColors) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Ищем принятые финальные решения в беседе...")
        return
    }

    val isDark = false
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
                            tint = themeColors.primary,
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
                                    tint = themeColors.primary,
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
    themeColors: com.example.ui.theme.CustomThemeColors,
    onTaskToggle: (Int) -> Unit
) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Распределяем задачи и фиксируем сроки исполнителей...")
        return
    }

    val isDark = false
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
                            tint = themeColors.primary,
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
                                    colors = CheckboxDefaults.colors(checkedColor = themeColors.primary)
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
                                        val (badgeBg, badgeText) = getSpeakerColors(task.assignedTo, meeting.participantA, themeColors)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(badgeBg)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Кому: ${task.assignedTo}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeText
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
                                                    .background(themeColors.secondary.copy(alpha = 0.08f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = themeColors.secondary,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "С пометкой срока",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = themeColors.secondary
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
    themeColors: com.example.ui.theme.CustomThemeColors,
    onUtteranceClick: (Long) -> Unit,
    onEditUtterance: (Int, String, String) -> Unit
) {
    if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
        PendingStateIndicator("Расписываем аудиодорожку по спикерам...")
        return
    }

    val isDark = false
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
            // Helper data structures for group-by-speaker presentation
            val groupedUtterances = remember(filteredList) {
                val result = mutableListOf<GroupedUtterances>()
                if (filteredList.isEmpty()) return@remember result
                
                var currentSpeaker = filteredList.first().second.speaker
                var currentGroup = mutableListOf<IndexedUtterance>()
                
                for (pair in filteredList) {
                    val originalIndex = pair.first
                    val item = pair.second
                    
                    if (item.speaker.lowercase() == currentSpeaker.lowercase()) {
                        currentGroup.add(IndexedUtterance(originalIndex, item))
                    } else {
                        result.add(GroupedUtterances(currentSpeaker, currentGroup))
                        currentSpeaker = item.speaker
                        currentGroup = mutableListOf(IndexedUtterance(originalIndex, item))
                    }
                }
                if (currentGroup.isNotEmpty()) {
                    result.add(GroupedUtterances(currentSpeaker, currentGroup))
                }
                result
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(groupedUtterances) { index, group ->
                    val isParticipantA = group.speaker.lowercase() == meeting.participantA.lowercase()
                    val groupContainsActive = remember(group, currentActiveIndex) {
                        group.items.any { it.originalIndex == currentActiveIndex }
                    }
                    val (speakerBg, speakerColor) = getSpeakerColors(group.speaker, meeting.participantA, themeColors)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isParticipantA) Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isParticipantA) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .pulseHalo(enabled = groupContainsActive && isAudioPlaying, color = speakerColor)
                            ) {
                                AvatarCircle(name = group.speaker, speakerBgColor = speakerBg, speakerTextColor = speakerColor)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        // Widescreen Speaker Box representing consecutive discourse
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isParticipantA) 4.dp else 16.dp,
                                bottomEnd = if (!isParticipantA) 4.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (groupContainsActive && isAudioPlaying) {
                                    themeColors.primary.copy(alpha = 0.08f)
                                } else {
                                    if (isDark) Color(0xFF1E2430) else Color(0xFFF3F5F9)
                                }
                            ),
                            border = BorderStroke(
                                width = if (groupContainsActive && isAudioPlaying) 2.dp else 1.dp,
                                color = if (groupContainsActive && isAudioPlaying) {
                                    themeColors.primary.copy(alpha = 0.5f)
                                } else {
                                    if (isDark) Color(0xFF2D3545) else Color(0xFFE2E8F0)
                                }
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                // Speaker Header with glowing dynamic voice wave badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = group.speaker,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = speakerColor
                                        )
                                        
                                        if (groupContainsActive && isAudioPlaying) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444))
                                            )
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Говорит",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sentences list inside the Speaker Box
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    group.items.forEach { indexedItem ->
                                        val utterance = indexedItem.utterance
                                        val originalIdx = indexedItem.originalIndex
                                        val isThisUtteranceActive = originalIdx == currentActiveIndex
                                        
                                        val formatTime = remember(utterance.timestampMs) {
                                            val totalSecs = utterance.timestampMs / 1000
                                            val mins = totalSecs / 60
                                            val secs = totalSecs % 60
                                            String.format("%02d:%02d", mins, secs)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isThisUtteranceActive) {
                                                        themeColors.primary.copy(alpha = 0.12f)
                                                    } else {
                                                        Color.Transparent
                                                    }
                                                )
                                                .clickable { onUtteranceClick(utterance.timestampMs) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isThisUtteranceActive) {
                                                    Icon(
                                						imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = themeColors.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                
                                                Text(
                                                    text = utterance.text,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isThisUtteranceActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isThisUtteranceActive) themeColors.primary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = formatTime,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                                
                                                IconButton(
                                                    onClick = {
                                                        editingUtteranceIndex = originalIdx
                                                        editingUtteranceSpeaker = utterance.speaker
                                                        editingUtteranceText = utterance.text
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Редактировать",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (!isParticipantA) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .pulseHalo(enabled = groupContainsActive && isAudioPlaying, color = speakerColor)
                            ) {
                                AvatarCircle(name = group.speaker, speakerBgColor = speakerBg, speakerTextColor = speakerColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Group structures and helpers
data class GroupedUtterances(
    val speaker: String,
    val items: List<IndexedUtterance>
)

data class IndexedUtterance(
    val originalIndex: Int,
    val utterance: Utterance
)

@Composable
fun Modifier.pulseHalo(enabled: Boolean, color: Color): Modifier {
    if (!enabled) return this
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return this.drawBehind {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = (size.minDimension / 2f) * scale
        )
    }
}

@Composable
fun getSpeakerColors(speakerName: String, primarySpeakerName: String, themeColors: com.example.ui.theme.CustomThemeColors): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    if (speakerName.lowercase() == primarySpeakerName.lowercase()) {
        return themeColors.primary.copy(alpha = 0.04f) to themeColors.primary
    }
    val hash = speakerName.hashCode().let { if (it < 0) -it else it }
    val colors = listOf(
        themeColors.secondary,
        androidx.compose.ui.graphics.Color(0xFF3F51B5), // Indigo
        androidx.compose.ui.graphics.Color(0xFF009688), // Teal
        androidx.compose.ui.graphics.Color(0xFFE91E63), // Pink
        androidx.compose.ui.graphics.Color(0xFF7B1FA2), // Purple
        androidx.compose.ui.graphics.Color(0xFFD84315), // Deep Orange
        androidx.compose.ui.graphics.Color(0xFF2E7D32)  // Dark Green
    )
    val baseColor = colors[hash % colors.size]
    return baseColor.copy(alpha = 0.04f) to baseColor
}

@Composable
fun AvatarCircle(name: String, speakerBgColor: androidx.compose.ui.graphics.Color, speakerTextColor: androidx.compose.ui.graphics.Color) {
    val initial = remember(name) {
        if (name.isNotBlank()) name.first().uppercase() else "У"
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(speakerBgColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = speakerTextColor
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
