package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.core.content.ContextCompat
import com.example.data.model.Meeting
import com.example.data.model.MeetingStatus
import com.example.ui.viewmodel.MeetingViewModel
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MeetingListScreen(
    viewModel: MeetingViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToProcessing: (Long) -> Unit,
    onNavigateToVoiceProfiles: () -> Unit,
    onNavigateToCreateMeeting: () -> Unit,
    onNavigateToRecording: () -> Unit,
    onLogoutRequested: () -> Unit = {},
    onNavigateToSubscription: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val meetings by viewModel.allMeetings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val participantA by viewModel.participantA.collectAsState()
    val participantB by viewModel.participantB.collectAsState()
    val meetingTitle by viewModel.meetingTitle.collectAsState()
    val currentPlayingId by viewModel.currentPlayingMeetingId.collectAsState()
    val processingId by viewModel.processingMeetingId.collectAsState()
    val apiError by viewModel.apiError.collectAsState()

    val context = LocalContext.current
    var showRecordPanel by remember { mutableStateOf(false) }
    var meetingToDelete by remember { mutableStateOf<Meeting?>(null) }

    // Toast API error notifications
    LaunchedEffect(apiError) {
        apiError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showRecordPanel = true
        } else {
            Toast.makeText(context, "Разрешение на микрофон необходимо для записи встреч", Toast.LENGTH_SHORT).show()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("All") } // "All", "Today", "Yesterday", "Week", "Older"
    var selectedParticipants by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFiltersPanel by remember { mutableStateOf(false) }

    var meetingToManageTags by remember { mutableStateOf<Meeting?>(null) }
    var tagInputText by remember { mutableStateOf("") }

    // Helper functions for dates
    fun isToday(timeMs: Long): Boolean {
        val calendar = Calendar.getInstance()
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)
        calendar.timeInMillis = timeMs
        return calendar.get(Calendar.DAY_OF_YEAR) == todayDay && calendar.get(Calendar.YEAR) == todayYear
    }

    fun isYesterday(timeMs: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayDay = calendar.get(Calendar.DAY_OF_YEAR)
        val yesterdayYear = calendar.get(Calendar.YEAR)
        calendar.timeInMillis = timeMs
        return calendar.get(Calendar.DAY_OF_YEAR) == yesterdayDay && calendar.get(Calendar.YEAR) == yesterdayYear
    }

    fun isWithinLast7Days(timeMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        return diff in 0..(7L * 24 * 60 * 60 * 1000L)
    }

    // List of all unique participants found across all meetings
    val allUniqueParticipants = remember(meetings) {
        val list = mutableSetOf<String>()
        meetings.forEach { m ->
            if (m.participantA.isNotBlank()) list.add(m.participantA)
            if (m.participantB.isNotBlank()) list.add(m.participantB)
        }
        list.sorted()
    }

    // List of all unique tags found across all meetings
    val allUniqueTags = remember(meetings) {
        val tagsSet = mutableSetOf<String>()
        meetings.forEach { m ->
            tagsSet.addAll(m.tags)
        }
        tagsSet.sorted()
    }

    // Helper to check token match
    fun isMatch(targetStr: String?, tokens: List<String>): Boolean {
        if (targetStr.isNullOrBlank()) return false
        val lowerTarget = targetStr.lowercase()
        return tokens.any { token -> lowerTarget.contains(token) }
    }

    val filteredMeetings = remember(
        meetings,
        searchQuery,
        showFavoritesOnly,
        selectedPeriod,
        selectedParticipants,
        selectedTags
    ) {
        val searchTokens = if (searchQuery.isNotBlank()) {
            val words = searchQuery.lowercase(Locale("ru")).split("\\s+".toRegex()).filter { it.isNotBlank() }
            val expanded = mutableListOf<String>()
            val dict = mapOf(
                "цен" to listOf("стоимост", "бюджет", "деньг", "оплат", "рубл", "руб", "сумм", "финанс", "договор"),
                "стоимост" to listOf("цен", "бюджет", "деньг", "оплат", "рубл", "руб", "сумм", "договор"),
                "бюджет" to listOf("цен", "стоимост", "деньг", "оплат", "рубл", "расход", "финанс", "сумм"),
                "деньг" to listOf("цен", "стоимост", "бюджет", "оплат", "рубл", "сумм", "валют", "финанс"),
                "оплат" to listOf("цен", "стоимост", "бюджет", "деньг", "плат", "выплат"),
                "задач" to listOf("поручен", "сделат", "план", "цел", "действ", "срок", "таск", "сделано"),
                "план" to listOf("задач", "поручен", "цел", "действ", "проект"),
                "решен" to listOf("договор", "соглас", "утверд", "принят", "решил"),
                "договор" to listOf("соглашен", "контракт", "подпис", "решен", "бумаг"),
                "участн" to listOf("собеседн", "говор", "спикер")
            )
            for (word in words) {
                expanded.add(word)
                for ((stem, synonyms) in dict) {
                    if (word.startsWith(stem) || stem.startsWith(word.take(4))) {
                        expanded.addAll(synonyms)
                    }
                }
            }
            expanded.distinct()
        } else {
            emptyList()
        }

        meetings.filter { meeting ->
            if (showFavoritesOnly && !meeting.isFavorite) return@filter false

            when (selectedPeriod) {
                "Today" -> if (!isToday(meeting.date)) return@filter false
                "Yesterday" -> if (!isYesterday(meeting.date)) return@filter false
                "Week" -> if (!isWithinLast7Days(meeting.date)) return@filter false
                "Older" -> if (isWithinLast7Days(meeting.date)) return@filter false
            }

            if (selectedParticipants.isNotEmpty()) {
                val hasSelected = selectedParticipants.any { part ->
                    meeting.participantA.equals(part, ignoreCase = true) ||
                    meeting.participantB.equals(part, ignoreCase = true)
                }
                if (!hasSelected) return@filter false
            }

            if (selectedTags.isNotEmpty()) {
                val hasAllTags = selectedTags.all { tag ->
                    meeting.tags.any { mt -> mt.equals(tag, ignoreCase = true) }
                }
                if (!hasAllTags) return@filter false
            }

            if (searchTokens.isNotEmpty()) {
                val matchesTitle = isMatch(meeting.title, searchTokens)
                val matchesPartA = isMatch(meeting.participantA, searchTokens)
                val matchesPartB = isMatch(meeting.participantB, searchTokens)
                val matchesOverview = isMatch(meeting.summaryOverview, searchTokens)
                val matchesTopics = meeting.summaryTopics?.any { isMatch(it, searchTokens) } == true
                val matchesDecisions = meeting.summaryDecisions?.any { isMatch(it, searchTokens) } == true
                val matchesTasks = meeting.summaryTasks?.any { isMatch(it.taskText, searchTokens) } == true
                val matchesTags = meeting.tags.any { isMatch(it, searchTokens) }
                val matchesTranscript = meeting.transcript?.any { isMatch(it.text, searchTokens) } == true

                if (!matchesTitle && !matchesPartA && !matchesPartB && !matchesOverview &&
                    !matchesTopics && !matchesDecisions && !matchesTasks && !matchesTags && !matchesTranscript
                ) {
                    return@filter false
                }
            }

            true
        }
    }

    val filtersActive = showFavoritesOnly || selectedPeriod != "All" || selectedParticipants.isNotEmpty() || selectedTags.isNotEmpty()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
            ) {
                // Top minimal profile row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left profile initials style matching JD + Logout Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onNavigateToSettings() }
                                .testTag("profile_avatar_settings")
                        ) {
                            Text(
                                text = "JD",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                              )
                        }

                        IconButton(
                            onClick = onLogoutRequested,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Выйти из системы",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToVoiceProfiles,
                            modifier = Modifier.size(32.dp).testTag("voice_profiles_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Голосовые профили",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val contextForPrefs = androidx.compose.ui.platform.LocalContext.current
                        val sessionPrefs = remember { com.example.data.local.SessionManager(contextForPrefs) }
                        val isProActiveUser = sessionPrefs.isProActive

                        IconButton(
                            onClick = onNavigateToSubscription,
                            modifier = Modifier.size(32.dp).testTag("subscription_shortcut")
                        ) {
                            Icon(
                                imageVector = if (isProActiveUser) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Тариф",
                                tint = if (isProActiveUser) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(32.dp).testTag("settings_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Dynamic Live status or app badge on the right
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (processingId != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.error)
                                )
                                Text(
                                    text = "LIVE NOW",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // App Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Deals Recorder",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page Large Title
                Text(
                    text = "Meetings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Professional Polish search capsule bar paired with a Filter Toggle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text(
                                text = "Search meetings and transcripts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Search, 
                                contentDescription = "Search", 
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ) 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear, 
                                        contentDescription = "Clear", 
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF1F242F) else Color(0xFFEFF1F8),
                            unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF1F242F) else Color(0xFFEFF1F8),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    FilledIconButton(
                        onClick = { showFiltersPanel = !showFiltersPanel },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (filtersActive) MaterialTheme.colorScheme.primaryContainer else (if (isSystemInDarkTheme()) Color(0xFF1F242F) else Color(0xFFEFF1F8)),
                            contentColor = if (filtersActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (showFiltersPanel) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Фильтры"
                        )
                    }
                }

                // Collapsible detailed filters panel
                AnimatedVisibility(
                    visible = showFiltersPanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSystemInDarkTheme()) Color(0xFF1E242F) else Color(0xFFF3F4F9))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Favorites and Period Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showFavoritesOnly,
                                onClick = { showFavoritesOnly = !showFavoritesOnly },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Избранные") }
                            )

                            Text(
                                text = "Период:", 
                                style = MaterialTheme.typography.labelMedium, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val periods = listOf("All" to "Все", "Today" to "Сегодня", "Yesterday" to "Вчера", "Week" to "7 дней", "Older" to "Ранее")
                                items(periods) { (key, display) ->
                                    FilterChip(
                                        selected = selectedPeriod == key,
                                        onClick = { selectedPeriod = key },
                                        label = { Text(display) }
                                    )
                                }
                            }
                        }

                        // 2. Filter by Participant
                        if (allUniqueParticipants.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Участники:", 
                                    style = MaterialTheme.typography.labelMedium, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(allUniqueParticipants) { part ->
                                        val isSelected = selectedParticipants.contains(part)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedParticipants = if (isSelected) {
                                                    selectedParticipants - part
                                                } else {
                                                    selectedParticipants + part
                                                }
                                            },
                                            label = { Text(part) }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Filter by Tag
                        if (allUniqueTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Теги:", 
                                    style = MaterialTheme.typography.labelMedium, 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(allUniqueTags) { tag ->
                                        val isSelected = selectedTags.contains(tag)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedTags = if (isSelected) {
                                                    selectedTags - tag
                                                } else {
                                                    selectedTags + tag
                                                }
                                            },
                                            label = { Text("#$tag") }
                                        )
                                    }
                                }
                            }
                        }

                        // Reset filters link
                        if (filtersActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        showFavoritesOnly = false
                                        selectedPeriod = "All"
                                        selectedParticipants = emptySet()
                                        selectedTags = emptySet()
                                    }
                                ) {
                                    Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Сбросить фильтры", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        onNavigateToRecording()
                    } else {
                        onNavigateToCreateMeeting()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer else (if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer else Color(0xFFD3E4FF)),
                contentColor = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else (if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF001C38)),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("record_meeting_fab")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else (if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f) else Color(0xFF001C38).copy(alpha = 0.15f)))
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.RadioButtonChecked else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Активная запись" else "Запись",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredMeetings.isEmpty()) {
                // Beautiful Minimalist empty state / Search placeholder empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Ничего не найдено" else "Протоколы переговоров пусты",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Попробуйте изменить поисковый запрос." else "Положите телефон между собой и собеседником, включите запись. ИИ автоматически расшифрует диалог по ролям и создаст умную сводку решений.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMeetings, key = { it.id }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            isPlaying = currentPlayingId == meeting.id,
                            isCurrentlyAnalyzing = processingId == meeting.id,
                            onPlayToggle = {
                                if (currentPlayingId == meeting.id) {
                                    viewModel.stopPlaying()
                                } else {
                                    viewModel.playMeetingAudio(meeting)
                                }
                            },
                            onDeleteClick = { meetingToDelete = meeting },
                            onCardClick = {
                                if (meeting.status == MeetingStatus.FAILED) {
                                    viewModel.retryTranscription(meeting)
                                } else if (meeting.status == MeetingStatus.TRANSCRIPTION_PENDING) {
                                    onNavigateToProcessing(meeting.id)
                                } else {
                                    onNavigateToDetail(meeting.id)
                                }
                            },
                            onFavoriteToggle = {
                                viewModel.toggleFavorite(meeting.id)
                            },
                            onManageTagsClick = {
                                meetingToManageTags = meeting
                            }
                        )
                    }
                }
            }

            // Beautiful Floating bottom indicator for ongoing background recording session
            AnimatedVisibility(
                visible = isRecording,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToRecording() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Идет запись переговоров...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                text = meetingTitle.ifBlank { "Параметры не заданы" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Открыть запись",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Material 3 Danger Zone deletion confirmation dialog
    if (meetingToDelete != null) {
        AlertDialog(
            onDismissRequest = { meetingToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить протокол?") },
            text = { Text("Это действие удалит аудиофайл и всю ИИ-сводку. Восстановление невозможно.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        meetingToDelete?.let { viewModel.deleteMeeting(it) }
                        meetingToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { meetingToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Interactive custom label/tag editor Dialog 
    if (meetingToManageTags != null) {
        val activeMeeting = meetingToManageTags!!
        AlertDialog(
            onDismissRequest = { meetingToManageTags = null },
            title = {
                Text("Управление тегами", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Протокол: ${activeMeeting.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Text("Текущие теги (нажмите для удаления):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    if (activeMeeting.tags.isEmpty()) {
                        Text(
                            text = "У этой встречи еще нет тегов.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeMeeting.tags) { tag ->
                                SuggestionChip(
                                    onClick = {
                                        val newTags = activeMeeting.tags.filter { it != tag }
                                        viewModel.updateMeetingTags(activeMeeting.id, newTags)
                                        meetingToManageTags = activeMeeting.copy(tags = newTags)
                                    },
                                    label = { Text("#$tag") },
                                    icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                )
                            }
                        }
                    }

                    // Text Field for adding custom tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tagInputText,
                            onValueChange = { tagInputText = it },
                            placeholder = { Text("Новый тег...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = tagInputText.trim().removePrefix("#")
                                if (trimmed.isNotBlank() && !activeMeeting.tags.contains(trimmed)) {
                                    val newTags = activeMeeting.tags + trimmed
                                    viewModel.updateMeetingTags(activeMeeting.id, newTags)
                                    meetingToManageTags = activeMeeting.copy(tags = newTags)
                                }
                                tagInputText = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }

                    // Suggestion quick tags
                    Text("Рекомендованные:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    val recommendations = listOf("Маркетинг", "Продажи", "Сделка", "Договор", "Бюджет", "Сроки", "Важно", "ИИ")
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recommendations) { rec ->
                            if (!activeMeeting.tags.contains(rec)) {
                                SuggestionChip(
                                    onClick = {
                                        val newTags = activeMeeting.tags + rec
                                        viewModel.updateMeetingTags(activeMeeting.id, newTags)
                                        meetingToManageTags = activeMeeting.copy(tags = newTags)
                                    },
                                    label = { Text(rec) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { meetingToManageTags = null }) {
                    Text("Готово")
                }
            }
        )
    }
}

@Composable
fun MeetingCard(
    meeting: Meeting,
    isPlaying: Boolean,
    isCurrentlyAnalyzing: Boolean,
    onPlayToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onManageTagsClick: () -> Unit
) {
    val durationText = remember(meeting.durationMs) {
        val sec = (meeting.durationMs / 1000) % 60
        val min = (meeting.durationMs / (1000 * 60)) % 60
        String.format("%02d:%02d", min, sec)
    }
    
    val dateText = remember(meeting.date) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("ru")).format(Date(meeting.date))
    }

    val isDark = isSystemInDarkTheme()

    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyAnalyzing) {
                if (isDark) Color(0xFF1E2530) else Color(0xFFF3F4F9)
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio control button
                IconButton(
                    onClick = onPlayToggle,
                    enabled = meeting.status == MeetingStatus.COMPLETED,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isPlaying) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        },
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести"
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Metadata details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meeting.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Favorite button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (meeting.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (meeting.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Manage tags button
                IconButton(
                    onClick = onManageTagsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = "Теги",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lower Section holding Speaker Badges and AI chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Participants info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${meeting.participantA} • ${meeting.participantB}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Pill
                when (meeting.status) {
                    MeetingStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF43474E) else Color(0xFFE1E2EC))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SUMMARY READY",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isDark) Color(0xFFE2E2E6) else Color(0xFF191C1E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    MeetingStatus.TRANSCRIPTION_PENDING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "ANALYZING...",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    MeetingStatus.FAILED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(10.dp), 
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "RETRY REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            // High-end elegant Transcript quote preview bubble (matches design concept precisely!)
            val firstUtteranceText = remember(meeting.transcript) {
                meeting.transcript?.firstOrNull()?.text
            }
            if (!firstUtteranceText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF232A37) else Color(0xFFF3F4F9))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "\"${firstUtteranceText.trim()}\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Display beautiful tags list
            if (meeting.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(meeting.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingPanel(
    isRecording: Boolean,
    amplitude: Int,
    recordingSeconds: Int,
    participantA: String,
    participantB: String,
    meetingTitle: String,
    onParticipantAChange: (String) -> Unit,
    onParticipantBChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag indicator/close handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRecording) "Идет запись переговоров" else "Протокол встречи",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isRecording) {
                    IconButton(onClick = onCloseClick) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRecording) {
                // Setup panel
                OutlinedTextField(
                    value = meetingTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Название встречи (необязательно)") },
                    placeholder = { Text("Обсуждение рекламы / Партнерство") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = participantA,
                        onValueChange = onParticipantAChange,
                        label = { Text("Собеседник А") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = participantB,
                        onValueChange = onParticipantBChange,
                        label = { Text("Собеседник Б") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Включить запись", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // Active recording panel with waveform!
                Spacer(modifier = Modifier.height(16.dp))

                // Waveform drawing
                VoiceWaveform(
                    amplitude = amplitude,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Format (MM:SS)
                val formatMinSec = remember(recordingSeconds) {
                    val m = recordingSeconds / 60
                    val s = recordingSeconds % 60
                    String.format("%02d:%02d", m, s)
                }

                Text(
                    text = formatMinSec,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Положите микрофон между спикерами",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Button(
                    onClick = onStopClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Завершить и расшифровать", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun VoiceWaveform(amplitude: Int, modifier: Modifier = Modifier) {
    // Elegant animating bar scale mimicking voice frequency
    val randomValues = remember(amplitude) {
        val count = 21
        val normalizer = 18000f // normal max MediaRecorder amplitude is around 32767
        val ratio = (amplitude.toFloat() / normalizer).coerceIn(0.05f, 1.0f)
        List(count) { 
            val randomFactor = 0.25f + (Math.random().toFloat() * 0.75f)
            (ratio * randomFactor).coerceIn(0.1f, 1.0f)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        randomValues.forEach { scale ->
            val height = 64.dp * scale
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.5.dp)
                    .width(4.5.dp)
                    .height(height.coerceAtLeast(6.dp))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (scale > 0.6f) 1.0f else 0.7f),
                        shape = RoundedCornerShape(2.2.dp)
                    )
            )
        }
    }
}
