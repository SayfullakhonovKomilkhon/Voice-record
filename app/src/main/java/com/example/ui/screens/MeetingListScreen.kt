package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.core.content.ContextCompat
import com.example.data.model.Meeting
import com.example.data.model.MeetingStatus
import com.example.ui.components.UnifiedCard
import com.example.ui.viewmodel.MeetingViewModel
import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.ThemeColorsProvider
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
    val activeThemeName by viewModel.activeThemeColor.collectAsState()
    val themeColors = remember(activeThemeName) { com.example.ui.theme.ThemeColorsProvider.getColors(activeThemeName) }

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

    val isSemanticSearching by viewModel.isSemanticSearching.collectAsState()
    val semanticSearchResultIds by viewModel.semanticSearchResultIds.collectAsState()
    val semanticSearchExplanations by viewModel.semanticSearchExplanations.collectAsState()
    val semanticSearchEnabled by viewModel.semanticSearchEnabled.collectAsState()

    val context = LocalContext.current
    val sessionPrefs = remember { com.example.data.local.SessionManager(context) }
    val isProActiveUser = sessionPrefs.isProActive
    var showAccountMenu by remember { mutableStateOf(false) }

    val homePulseTransition = rememberInfiniteTransition(label = "home_record_pulse")
    val homePulseScale by homePulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homePulseScale"
    )
    val homePulseAlpha by homePulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homePulseAlpha"
    )
    val homePulseScale2 by homePulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homePulseScale2"
    )
    val homePulseAlpha2 by homePulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homePulseAlpha2"
    )

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
            onNavigateToRecording()
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
    var selectedCalendarDay by remember { mutableStateOf<Calendar?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    var meetingToManageTags by remember { mutableStateOf<Meeting?>(null) }
    var tagInputText by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery, semanticSearchEnabled) {
        if (semanticSearchEnabled) {
            kotlinx.coroutines.delay(800)
            viewModel.executeSemanticSearch(searchQuery)
        }
    }

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

    fun isWithinLast30Days(timeMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        return diff in 0..(30L * 24 * 60 * 60 * 1000L)
    }

    fun isSameDay(timeMs: Long, cal: Calendar): Boolean {
        val mCal = Calendar.getInstance()
        mCal.timeInMillis = timeMs
        return mCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
               mCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
    }

    fun isSameCalendarDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
               cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    fun countRecordingsForDay(dayCal: Calendar): Int {
        return meetings.count { isSameDay(it.date, dayCal) }
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
        selectedTags,
        semanticSearchEnabled,
        semanticSearchResultIds,
        selectedCalendarDay
    ) {
        val searchTokens = if (searchQuery.isNotBlank() && !semanticSearchEnabled) {
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

            // Bypass period filters when searching globally as requested by the user,
            // but always respect explicitly selected calendar day if user chose one.
            val isSearchingActive = searchQuery.isNotBlank()
            if (selectedCalendarDay != null) {
                if (!isSameDay(meeting.date, selectedCalendarDay!!)) return@filter false
            } else if (!isSearchingActive) {
                when (selectedPeriod) {
                    "Today" -> if (!isToday(meeting.date)) return@filter false
                    "Yesterday" -> if (!isYesterday(meeting.date)) return@filter false
                    "Week" -> if (!isWithinLast7Days(meeting.date)) return@filter false
                    "Month" -> if (!isWithinLast30Days(meeting.date)) return@filter false
                    "Older" -> if (isWithinLast30Days(meeting.date)) return@filter false
                }
            }

            if (selectedParticipants.isNotEmpty()) {
                val hasSelected = selectedParticipants.any { part ->
                    meeting.participantA.equals(part, ignoreCase = true) ||
                    meeting.participantB.equals(part, ignoreCase = true) ||
                    meeting.additionalParticipants.any { it.equals(part, ignoreCase = true) }
                }
                if (!hasSelected) return@filter false
            }

            if (selectedTags.isNotEmpty()) {
                val hasAllTags = selectedTags.all { tag ->
                    meeting.tags.any { mt -> mt.equals(tag, ignoreCase = true) }
                }
                if (!hasAllTags) return@filter false
            }

            if (semanticSearchEnabled && searchQuery.isNotBlank()) {
                val allowedIds = semanticSearchResultIds
                if (allowedIds != null && !allowedIds.contains(meeting.id)) {
                    return@filter false
                }
            } else if (searchTokens.isNotEmpty()) {
                val matchesTitle = isMatch(meeting.title, searchTokens)
                val matchesPartA = isMatch(meeting.participantA, searchTokens)
                val matchesPartB = isMatch(meeting.participantB, searchTokens)
                val matchesPartC = meeting.additionalParticipants.any { isMatch(it, searchTokens) }
                val matchesOverview = isMatch(meeting.summaryOverview, searchTokens)
                val matchesTopics = meeting.summaryTopics?.any { isMatch(it, searchTokens) } == true
                val matchesDecisions = meeting.summaryDecisions?.any { isMatch(it, searchTokens) } == true
                val matchesTasks = meeting.summaryTasks?.any { isMatch(it.taskText, searchTokens) } == true
                val matchesTags = meeting.tags.any { isMatch(it, searchTokens) }
                val matchesTranscript = meeting.transcript?.any { isMatch(it.text, searchTokens) } == true

                // Dynamic Date format matching (supports e.g. "08.06", "июня", "June", "Monday", etc.)
                val meetingDate = Date(meeting.date)
                val formatMediumRu = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("ru")).format(meetingDate)
                val formatLongRu = DateFormat.getDateInstance(DateFormat.LONG, Locale("ru")).format(meetingDate)
                val formatShortNumeric = java.text.SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(meetingDate)
                val formatShortNumericSlash = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("ru")).format(meetingDate)
                val formatDayAndMonth = java.text.SimpleDateFormat("dd.MM", Locale("ru")).format(meetingDate)
                val dayOfWeekRu = java.text.SimpleDateFormat("EEEE", java.util.Locale("ru")).format(meetingDate)
                val dayOfWeekEn = java.text.SimpleDateFormat("EEEE", java.util.Locale("en")).format(meetingDate)
                val monthNameEn = java.text.SimpleDateFormat("MMMM", java.util.Locale("en")).format(meetingDate)
                val searchableDateText = "$formatMediumRu $formatLongRu $formatShortNumeric $formatShortNumericSlash $formatDayAndMonth $dayOfWeekRu $dayOfWeekEn $monthNameEn"
                val matchesDate = isMatch(searchableDateText, searchTokens)

                if (!matchesTitle && !matchesPartA && !matchesPartB && !matchesPartC && !matchesOverview &&
                    !matchesTopics && !matchesDecisions && !matchesTasks && !matchesTags && !matchesTranscript && !matchesDate
                ) {
                    return@filter false
                }
            }

            true
        }
    }

    val filtersActive = showFavoritesOnly || selectedPeriod != "All" || selectedParticipants.isNotEmpty() || selectedTags.isNotEmpty() || selectedCalendarDay != null

    val totalAirtimeMs = remember(meetings) { meetings.sumOf { it.durationMs } }
    val totalAirtimeMin = remember(totalAirtimeMs) { totalAirtimeMs / (1000 * 60) }
    val favoritesCount = remember(meetings) { meetings.count { it.isFavorite } }

    val isDark = false

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val isDarkTheme = isDark
                
                // Draw a beautiful base screen background (eliminates any black or grey bottom space)
                drawRect(
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color.White
                )

                val bannerHeight = 220.dp.toPx()
                
                // Draw vibrant brand teal banner block at the top
                val bannerBrush = if (isDarkTheme) {
                    Brush.verticalGradient(listOf(Color(0xFF082221), Color(0xFF031414)))
                } else {
                    Brush.verticalGradient(listOf(themeColors.bannerStart, themeColors.bannerEnd))
                }
                drawRect(
                    brush = bannerBrush,
                    size = androidx.compose.ui.geometry.Size(size.width, bannerHeight)
                )

                // Top-left soft circular accent details
                drawCircle(
                    color = if (isDarkTheme) Color(0xFF132B2B).copy(alpha = 0.6f) else themeColors.accentCircle,
                    radius = 100.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(-15.dp.toPx(), -15.dp.toPx())
                )

                // Top-right soft circular accent details
                drawCircle(
                    color = if (isDarkTheme) Color(0xFF132B2B).copy(alpha = 0.5f) else themeColors.accentCircle,
                    radius = 120.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width + 15.dp.toPx(), 45.dp.toPx())
                )

                // Beautiful sweeping crescent arch curve
                val orbitPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(-40.dp.toPx(), bannerHeight * 0.95f)
                    cubicTo(
                        size.width * 0.25f, bannerHeight * 0.15f,
                        size.width * 0.75f, bannerHeight * 0.15f,
                        size.width + 40.dp.toPx(), bannerHeight * 0.95f
                    )
                }
                drawPath(
                    path = orbitPath,
                    color = Color.White.copy(alpha = 0.35f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )

                // Draw a five-pointed star on the orbit arch, near the top right
                val starCenterX = size.width * 0.72f
                val starCenterY = bannerHeight * 0.35f
                val starSize = 8.dp.toPx()
                val starPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(starCenterX, starCenterY - starSize)
                    lineTo(starCenterX + starSize * 0.3f, starCenterY - starSize * 0.3f)
                    lineTo(starCenterX + starSize, starCenterY)
                    lineTo(starCenterX + starSize * 0.3f, starCenterY + starSize * 0.3f)
                    lineTo(starCenterX, starCenterY + starSize)
                    lineTo(starCenterX - starSize * 0.3f, starCenterY + starSize * 0.3f)
                    lineTo(starCenterX - starSize, starCenterY)
                    lineTo(starCenterX - starSize * 0.3f, starCenterY - starSize * 0.3f)
                    close()
                }
                drawPath(path = starPath, color = Color.White.copy(alpha = 0.9f))
            },
        bottomBar = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // --- SECTION 1: MASTER TEAL HEADER BLOCK ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .background(Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = "My Records",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Curate Every Record Ever Recorded",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E293B).copy(alpha = 0.75f)
                                )
                            )
                        }

                        // Top Header Actions (Settings & Profile side-by-side)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Settings Button (moved from bottom bar to header)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .clickable { onNavigateToSettings() }
                                    .testTag("action_settings_top")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Настройки",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Minimalist circular user action card
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .clickable { showAccountMenu = true }
                                    .testTag("action_profile_top")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Профиль",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

            // --- SECTION 2: OVERLAPPING CARD CONTAINER ---
            val allExtendedDays = remember {
                val list = mutableListOf<Calendar>()
                val startCal = Calendar.getInstance(Locale("ru"))
                startCal.add(Calendar.DAY_OF_YEAR, -180) // 180 days in the past
                for (i in 0..210) { // 210 days total: 180 in past, 30 in the future
                    val c = startCal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, i)
                    list.add(c)
                }
                list
            }

            val currentCalendarInstance = remember { Calendar.getInstance(Locale("ru")) }
            val todayDayOfYear = remember { currentCalendarInstance.get(Calendar.DAY_OF_YEAR) }
            val todayYear = remember { currentCalendarInstance.get(Calendar.YEAR) }

            val todayIndex = remember(allExtendedDays) {
                val today = Calendar.getInstance(Locale("ru"))
                allExtendedDays.indexOfFirst { isSameCalendarDay(it, today) }.coerceAtLeast(0)
            }

            val calendarRowState = androidx.compose.foundation.lazy.rememberLazyListState(
                initialFirstVisibleItemIndex = (todayIndex - 3).coerceAtLeast(0)
            )

            // Auto-scroll/center to selected position smoothly or to today initially
            LaunchedEffect(selectedCalendarDay) {
                selectedCalendarDay?.let { selected ->
                    val index = allExtendedDays.indexOfFirst { isSameCalendarDay(it, selected) }
                    if (index >= 0) {
                        val targetIndex = (index - 3).coerceAtLeast(0)
                        calendarRowState.animateScrollToItem(targetIndex)
                    }
                }
            }

            val monthYearRow = remember {
                val cal = Calendar.getInstance(Locale("en"))
                val months = listOf(
                    "January", "February", "March", "April", "May", "June", 
                    "July", "August", "September", "October", "November", "December"
                )
                val monthName = months[cal.get(Calendar.MONTH)]
                val yearVal = cal.get(Calendar.YEAR).toString()
                Pair(monthName, yearVal)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset(y = (-24).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Calendar Week Day Strip Row (Horizontal Scrollable via LazyRow)
                    androidx.compose.foundation.lazy.LazyRow(
                        state = calendarRowState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allExtendedDays.size) { index ->
                            val dayCal = allExtendedDays[index]
                            val dayIsToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear && dayCal.get(Calendar.YEAR) == todayYear
                            val isSelected = selectedCalendarDay != null && isSameCalendarDay(dayCal, selectedCalendarDay!!)
                            
                            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
                            val dayAbbreviation = when (dayOfWeek) {
                                Calendar.MONDAY -> "Mon"
                                Calendar.TUESDAY -> "Tue"
                                Calendar.WEDNESDAY -> "Wed"
                                Calendar.THURSDAY -> "Thu"
                                Calendar.FRIDAY -> "Fri"
                                Calendar.SATURDAY -> "Sat"
                                Calendar.SUNDAY -> "Sun"
                                else -> "Mon"
                            }
                            
                            val dayNo = dayCal.get(Calendar.DAY_OF_MONTH)
                            val count = countRecordingsForDay(dayCal)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) themeColors.primary.copy(alpha = 0.08f)
                                        else if (dayIsToday) Color(0xFFF1F5F9)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedCalendarDay = if (isSelected) null else dayCal
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = dayAbbreviation,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (dayIsToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) themeColors.primary 
                                                else if (dayIsToday) Color(0xFF0F172A) 
                                                else Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) themeColors.primary
                                                else if (dayIsToday) themeColors.primary.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .then(
                                                if (count > 0 && !isSelected) {
                                                    Modifier.border(1.5.dp, themeColors.primary.copy(alpha = 0.6f), CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        Text(
                                            text = dayNo.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White 
                                                        else if (dayIsToday) themeColors.primary 
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                    
                                    if (count > 0) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 1.dp, y = (-1).dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        ) {
                                            Text(
                                                text = count.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp,
                                                     fontWeight = FontWeight.Black,
                                                     color = Color.White
                                                 )
                                             )
                                         }
                                     }
                                 }
                             }
                         }
                     }

                     Spacer(modifier = Modifier.height(16.dp))

                     // Month & Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Записи",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )

                        // Beautiful toggle filters chip and badge with active state indicators
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredMeetings.isNotEmpty()) {
                                Text(
                                    text = "${filteredMeetings.size} сессий",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                )
                            }

                            // Small toggle filters pill
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (showFiltersPanel) themeColors.primary.copy(alpha = 0.12f) else Color(0xFFF1F5F9))
                                    .clickable { showFiltersPanel = !showFiltersPanel }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Фильтры",
                                        tint = if (showFiltersPanel) themeColors.primary else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                       text = "Фильтры",
                                       style = MaterialTheme.typography.labelSmall.copy(
                                           fontWeight = FontWeight.Bold,
                                           color = if (showFiltersPanel) themeColors.primary else Color(0xFF64748B)
                                       )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Combined Search input and Calendar button row (Header Area)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search field designed for filtering title, participants, tags, and meeting date format
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Поиск по названию или дате...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.6f) else Color(0xFF64748B).copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Поиск",
                                    tint = themeColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Очистить",
                                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                unfocusedTextColor = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF334155),
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("action_search_input")
                        )

                        // Calendar Button stays perfectly placed near/next to the search bar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(themeColors.primary)
                                .clickable {
                                    showCalendarDialog = true
                                }
                                .testTag("action_calendar_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Календарь",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Expandable filter options panel
                    AnimatedVisibility(
                        visible = showFiltersPanel,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            ),
                            border = BorderStroke(1.dp, themeColors.primary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                // Note: we keep other paddings unchanged
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Фильтры поиска",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF1E293B)
                                        )
                                    )
                                    
                                    TextButton(
                                        onClick = {
                                            selectedPeriod = "All"
                                            showFavoritesOnly = false
                                            selectedParticipants = emptySet()
                                            selectedTags = emptySet()
                                            selectedCalendarDay = null
                                        }
                                    ) {
                                        Text(
                                            text = "Сбросить всё",
                                            color = themeColors.primary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Период записи",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                                
                                val periods = listOf(
                                    "All" to "Всё время",
                                    "Today" to "Сегодня",
                                    "Yesterday" to "Вчера",
                                    "Week" to "За неделю",
                                    "Month" to "За месяц",
                                    "Older" to "Старше"
                                )
                                
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(periods) { (key, label) ->
                                        val isSel = selectedPeriod == key
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedPeriod = key },
                                            label = { Text(label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = themeColors.primary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Только избранные записи",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Switch(
                                        checked = showFavoritesOnly,
                                        onCheckedChange = { showFavoritesOnly = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = themeColors.primary
                                        )
                                    )
                                }
                                
                                if (allUniqueParticipants.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                    Text(
                                        text = "По участнику",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(allUniqueParticipants.toList()) { participant ->
                                            val isSel = selectedParticipants.contains(participant)
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    selectedParticipants = if (isSel) {
                                                        selectedParticipants - participant
                                                    } else {
                                                        selectedParticipants + participant
                                                    }
                                                },
                                                label = { Text(participant) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = themeColors.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                                
                                if (allUniqueTags.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                    Text(
                                        text = "Теги",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(allUniqueTags.toList()) { tag ->
                                            val isSel = selectedTags.contains(tag)
                                            FilterChip(
                                                selected = isSel,
                                                onClick = {
                                                    selectedTags = if (isSel) {
                                                        selectedTags - tag
                                                    } else {
                                                        selectedTags + tag
                                                    }
                                                },
                                                label = { Text("#$tag") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = themeColors.primary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))               /*
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$favoritesCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            Text("Избранные", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    */ // Open container Box for content
                    Box(modifier = Modifier.weight(1f)) {

            if (filteredMeetings.isEmpty()) {
                // High-fidelity premium stacked papers empty state from the design mockup wrapped in cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    UnifiedCard(
                        isDark = isDark,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("empty_state_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(140.dp)
                                    .padding(bottom = 16.dp)
                            ) {
                                // Soft glow backdrop shadow
                                Box(
                                    modifier = Modifier
                                        .size(60.dp, 75.dp)
                                        .offset(x = (-8).dp, y = (-2).dp)
                                        .graphicsLayer(rotationZ = -12f, alpha = 0.15f)
                                        .background(themeColors.primary, RoundedCornerShape(14.dp))
                                )

                                // Back Paper Page with soft rotation slant
                                Box(
                                    modifier = Modifier
                                        .size(58.dp, 72.dp)
                                        .offset(x = (-10).dp, y = (-4).dp)
                                        .graphicsLayer(rotationZ = -8f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                                        .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                )

                                // Front Paper Page overlapping perfectly with text line placeholders
                                Box(
                                    modifier = Modifier
                                        .size(58.dp, 72.dp)
                                        .offset(x = 6.dp, y = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF334155) else Color(0xFFF8FAFC))
                                        .border(1.5.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Miniature bullet list representations
                                        Box(modifier = Modifier.size(32.dp, 4.dp).clip(CircleShape).background(if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)))
                                        Box(modifier = Modifier.size(18.dp, 4.dp).clip(CircleShape).background(if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)))
                                        Box(modifier = Modifier.size(36.dp, 4.dp).clip(CircleShape).background(themeColors.primary.copy(alpha = 0.6f)))
                                        Box(modifier = Modifier.size(24.dp, 4.dp).clip(CircleShape).background(if (isDark) Color(0xFF475569) else Color(0xFFE2E8F0)))
                                    }
                                }
                            }

                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "Ничего не найдено",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Попробуйте изменить поисковый запрос.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color(0xFF64748B)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Нет записей встреч",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Список записей пока пуст. Чтобы сделать первую запись и сгенерировать протокол, нажмите на центральную кнопку «Запись» ниже.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color(0xFF64748B),
                                        lineHeight = 18.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMeetings, key = { it.id }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            isPlaying = currentPlayingId == meeting.id,
                            isCurrentlyAnalyzing = processingId == meeting.id,
                            aiExplanation = if (semanticSearchEnabled) semanticSearchExplanations[meeting.id] else null,
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
                            },
                            themeColors = themeColors
                        )
                    }
                }
            }
            }
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

            // Elegant, high-fidelity floating white capsule bottom navigation bar (matching Image 2 & 3)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 64.dp)
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item 1: Встречи (Meetings List). Icon-only representation.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { /* Already on meetings screen */ }
                                .padding(vertical = 12.dp)
                                .testTag("nav_meetings_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Встречи",
                                tint = if (isDark) Color(0xFF38BDF8) else themeColors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Symmetrical visual gap for the centered overlapping Record FAB!
                        Spacer(modifier = Modifier.weight(1.2f))

                        // Item 2: Голоса (Voice Profiles). Icon-only representation.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { onNavigateToVoiceProfiles() }
                                .padding(vertical = 12.dp)
                                .testTag("nav_voices_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (allUniqueParticipants.isNotEmpty()) {
                                        Badge(containerColor = Color(0xFFEF4444)) {
                                            Text("${allUniqueParticipants.size}", color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Голоса",
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Glowing pulsing background wave rings behind the overlapping Record Button
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(54.dp)
                        .scale(homePulseScale)
                        .background(
                            color = themeColors.pulseAlphaColor.copy(alpha = homePulseAlpha),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(54.dp)
                        .scale(homePulseScale2)
                        .background(
                            color = themeColors.pulseAlphaColor.copy(alpha = homePulseAlpha2),
                            shape = CircleShape
                        )
                )

                // The primary overlapping Floating central Record Button (FAB)
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(54.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(themeColors.buttonGradientStart, themeColors.buttonGradientEnd)
                            )
                        )
                        .clickable {
                            if (isRecording) {
                                onNavigateToRecording()
                            } else {
                                val recordPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                if (recordPermission == PackageManager.PERMISSION_GRANTED) {
                                    onNavigateToRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("home_center_record_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Быстрая Запись",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
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

    if (showCalendarDialog) {
        val isDarkTheme = isDark
        var viewedMonthCalendar by remember { mutableStateOf(Calendar.getInstance().apply { 
            selectedCalendarDay?.let { timeInMillis = it.timeInMillis }
        }) }
        
        val monthsRu = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        val monthsEn = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val yearVal = viewedMonthCalendar.get(Calendar.YEAR)
        val monthVal = viewedMonthCalendar.get(Calendar.MONTH)
        val monthTitle = if (Locale.getDefault().language == "ru") monthsRu[monthVal] else monthsEn[monthVal]
        
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newCal = viewedMonthCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, -1)
                                viewedMonthCalendar = newCal
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Предыдущий месяц",
                                tint = themeColors.primary
                            )
                        }
                        
                        Text(
                            text = "$monthTitle $yearVal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                            )
                        )
                        
                        IconButton(
                            onClick = {
                                val newCal = viewedMonthCalendar.clone() as Calendar
                                newCal.add(Calendar.MONTH, 1)
                                viewedMonthCalendar = newCal
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Следующий месяц",
                                tint = themeColors.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Days of week Headers Row
                    val daysOfWeekShort = if (Locale.getDefault().language == "ru") {
                        listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    } else {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysOfWeekShort.forEach { dayHead ->
                            Text(
                                text = dayHead,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Days Grid
                    val gridDays = remember(viewedMonthCalendar) {
                        val list = mutableListOf<Calendar?>()
                        val cal = viewedMonthCalendar.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        val offset = when (firstDayOfWeek) {
                            Calendar.MONDAY -> 0
                            Calendar.TUESDAY -> 1
                            Calendar.WEDNESDAY -> 2
                            Calendar.THURSDAY -> 3
                            Calendar.FRIDAY -> 4
                            Calendar.SATURDAY -> 5
                            Calendar.SUNDAY -> 6
                            else -> 0
                        }
                        for (i in 0 until offset) {
                            list.add(null)
                        }
                        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        for (i in 1..maxDays) {
                            val dCal = cal.clone() as Calendar
                            dCal.set(Calendar.DAY_OF_MONTH, i)
                            list.add(dCal)
                        }
                        list
                    }
                    
                    val currentTodayIdx = Calendar.getInstance()
                    val todayDay = currentTodayIdx.get(Calendar.DAY_OF_YEAR)
                    val todayYearVal = currentTodayIdx.get(Calendar.YEAR)
                    
                    var currentWeekRow = mutableListOf<Calendar?>()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        gridDays.forEachIndexed { idx, dayCal ->
                            currentWeekRow.add(dayCal)
                            if (currentWeekRow.size == 7 || idx == gridDays.size - 1) {
                                while (currentWeekRow.size < 7) {
                                    currentWeekRow.add(null)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    currentWeekRow.forEach { gridDayCal ->
                                        if (gridDayCal == null) {
                                            Box(modifier = Modifier.weight(1f))
                                        } else {
                                            val gridDayIsToday = gridDayCal.get(Calendar.DAY_OF_YEAR) == todayDay && gridDayCal.get(Calendar.YEAR) == todayYearVal
                                            val isSelected = selectedCalendarDay != null && isSameCalendarDay(gridDayCal, selectedCalendarDay!!)
                                            val dayNo = gridDayCal.get(Calendar.DAY_OF_MONTH)
                                            val recCount = countRecordingsForDay(gridDayCal)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(44.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) themeColors.primary
                                                                else if (gridDayIsToday) themeColors.primary.copy(alpha = 0.15f)
                                                                else Color.Transparent
                                                            )
                                                            .then(
                                                                if (recCount > 0 && !isSelected) {
                                                                    Modifier.border(1.5.dp, themeColors.primary.copy(alpha = 0.8f), CircleShape)
                                                                } else {
                                                                    Modifier
                                                                }
                                                            )
                                                            .clickable {
                                                                selectedCalendarDay = if (isSelected) null else gridDayCal
                                                                showCalendarDialog = false
                                                            }
                                                    ) {
                                                        Text(
                                                            text = dayNo.toString(),
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) Color.White 
                                                                        else if (gridDayIsToday) themeColors.primary 
                                                                        else if (recCount > 0) (if (isDarkTheme) Color.White else Color(0xFF1E293B))
                                                                        else (if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF1E293B).copy(alpha = 0.6f))
                                                            )
                                                        )
                                                    }
                                                    
                                                    if (recCount > 0) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = 2.dp, y = (-2).dp)
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFFEF4444))
                                                        ) {
                                                            Text(
                                                                text = recCount.toString(),
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = Color.White
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                currentWeekRow = mutableListOf()
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedCalendarDay = null
                                showCalendarDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = themeColors.primary
                            ),
                            border = BorderStroke(1.dp, themeColors.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Сбросить дату", fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { showCalendarDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Закрыть", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAccountMenu) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        // Dynamic user details retrieved from SessionManager
        val currentLoggedInEmail = sessionPrefs.loggedInEmail ?: "komilsay777@gmail.com"
        val currentLoggedInName = remember(sessionPrefs.loggedInName, currentLoggedInEmail) {
            val name = sessionPrefs.loggedInName
            if (!name.isNullOrBlank()) {
                name
            } else {
                currentLoggedInEmail.substringBefore("@")
                    .split(".", "_", "-")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    }
            }
        }
        val currentLoggedInInitials = remember(currentLoggedInName) {
            val parts = currentLoggedInName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else if (parts.isNotEmpty()) {
                parts[0].take(2).uppercase()
            } else {
                "JD"
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAccountMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                ) {
                    Text(
                        text = currentLoggedInInitials,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = currentLoggedInName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = currentLoggedInEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Subscription Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E242F) else Color(0xFFEFF1F8)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Текущий тариф",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isProActiveUser) "PRO Максимальный" else "Базовый тариф",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isProActiveUser) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        if (!isProActiveUser) {
                            Button(
                                onClick = {
                                    showAccountMenu = false
                                    onNavigateToSubscription()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEAB308),
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("PRO за 0 ₽", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEAB308).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "АКТИВЕН",
                                    color = Color(0xFFEAB308),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Logout Button
                Button(
                    onClick = {
                        showAccountMenu = false
                        onLogoutRequested()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти из аккаунта", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { showAccountMenu = false }
                ) {
                    Text("Закрыть", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
    }
}

@Composable
fun MeetingCard(
    meeting: Meeting,
    isPlaying: Boolean,
    isCurrentlyAnalyzing: Boolean,
    aiExplanation: String? = null,
    onPlayToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onManageTagsClick: () -> Unit,
    themeColors: CustomThemeColors
) {
    val durationText = remember(meeting.durationMs) {
        val sec = (meeting.durationMs / 1000) % 60
        val min = (meeting.durationMs / (1000 * 60)) % 60
        String.format("%02d:%02d", min, sec)
    }
    
    val dateText = remember(meeting.date) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("ru")).format(Date(meeting.date))
    }

    val isDark = false

    // Breathing pulse scale animation for playing audio control
    val infiniteTransition = rememberInfiniteTransition(label = "playback_pulse")
    val pulseScale by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    // High-fidelity modern container with soft dynamic border outline
    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = if (isPlaying) 2.dp else 1.dp,
            color = if (isPlaying) {
                themeColors.primary
            } else if (isCurrentlyAnalyzing) {
                themeColors.secondary.copy(alpha = 0.4f)
            } else {
                Color(0xFFE2E8F0)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyAnalyzing) {
                Color(0xFFF8FAFC)
            } else if (isPlaying) {
                themeColors.primary.copy(alpha = 0.02f)
            } else {
                Color.White
            },
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 4.dp else 1.5.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meeting_card_${meeting.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Segment 1: Header (Title, Date/Duration metadata & Status pill on the far right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meeting.title.ifBlank { "Запись без названия" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        ),
                        color = if (isPlaying) themeColors.primary else Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF64748B)
                        )
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF64748B)
                        )
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Beautiful Status Badges
                when (meeting.status) {
                    MeetingStatus.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFECFDF5))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "ГОТОВО",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.6.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    MeetingStatus.TRANSCRIPTION_PENDING -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeColors.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.8.dp,
                                    color = themeColors.primary
                                )
                                Text(
                                    text = "АНАЛИЗ...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.6.sp,
                                    color = themeColors.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    MeetingStatus.FAILED -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(10.dp), 
                                    tint = Color(0xFFEF4444)
                                )
                                Text(
                                    text = "ПОВТОРИТЬ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.6.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Segment 2: Participant Badges with initials
            val participantList = remember(meeting.participantA, meeting.participantB, meeting.additionalParticipants) {
                (listOf(meeting.participantA, meeting.participantB) + meeting.additionalParticipants)
                    .filter { it.isNotBlank() }
                    .distinct()
            }
            
            if (participantList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = themeColors.primary.copy(alpha = 0.7f)
                    )
                    
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        participantList.take(3).forEachIndexed { index, name ->
                            val pColor = when (index) {
                                0 -> themeColors.primary
                                1 -> themeColors.secondary
                                else -> Color(0xFF64748B)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val initial = name.trim().take(1).uppercase()
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                ) {
                                    Text(
                                        text = initial,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = pColor,
                                    maxLines = 1,
                                    fontWeight = FontWeight.SemiBold,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (participantList.size > 3) {
                            Text(
                                text = "+${participantList.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Segment 3: Premium AI Explanation Banner (for AI search matching context)
            if (!aiExplanation.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    themeColors.primary.copy(alpha = 0.08f),
                                    themeColors.secondary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.dp, themeColors.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI-совпадение",
                        tint = themeColors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = aiExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = themeColors.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Segment 4: Unique Speech Quotation preview bubble (beautiful cozy design)
            val firstUtteranceText = remember(meeting.transcript) {
                meeting.transcript?.firstOrNull()?.text
            }
            if (!firstUtteranceText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(BorderStroke(0.8.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "“",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.primary.copy(alpha = 0.35f),
                        lineHeight = 16.sp,
                        modifier = Modifier.offset(y = (-2).dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = firstUtteranceText.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF475569),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Segment 5: Beautiful hashtags grid row
            if (meeting.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(meeting.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(themeColors.primary.copy(alpha = 0.05f))
                                .border(1.dp, themeColors.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = themeColors.primary.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = themeColors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Segment 6: Beautiful bottom action bar (highly accessible, comfortable buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Play/Pause Action Pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPlaying) themeColors.primary 
                            else themeColors.primary.copy(alpha = 0.08f)
                        )
                        .clickable(enabled = meeting.status == MeetingStatus.COMPLETED) { onPlayToggle() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("playback_toggle_${meeting.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Приостановить" else "Воспроизвести",
                            tint = if (isPlaying) Color.White else themeColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPlaying) "Пауза" else "Слушать запись",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) Color.White else themeColors.primary
                            )
                        )
                    }
                }

                // Companion action buttons group on the right (Favorite, LabelTags, TrashDelete)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Favorite Button
                    val favBgColor = if (meeting.isFavorite) Color(0xFFFFF1F2) else Color(0xFFF8FAFC)
                    val favIconColor = if (meeting.isFavorite) Color(0xFFF43F5E) else Color(0xFF64748B)
                    val favBorderColor = if (meeting.isFavorite) Color(0xFFFECDD3) else Color(0xFFE2E8F0)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(favBgColor)
                            .border(1.dp, favBorderColor, RoundedCornerShape(10.dp))
                            .clickable { onFavoriteToggle() }
                            .testTag("favorite_button_${meeting.id}")
                    ) {
                        Icon(
                            imageVector = if (meeting.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = favIconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Modern Manage Tags Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .clickable { onManageTagsClick() }
                            .testTag("manage_tags_button_${meeting.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = "Теги",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Modern Delete Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                            .clickable { onDeleteClick() }
                            .testTag("delete_button_${meeting.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    badgeText: String? = null
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .height(48.dp)
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
