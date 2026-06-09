package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.ThemeColorsProvider
import com.example.ui.theme.CustomThemeColors
import com.example.data.model.VoiceProfile
import com.example.ui.components.UnifiedCard
import com.example.ui.viewmodel.MeetingViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceProfilesScreen(
    viewModel: MeetingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMeetingList: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToRecording: () -> Unit = {}
) {
    val activeThemeName by viewModel.activeThemeColor.collectAsState()
    val themeColors = remember(activeThemeName) { ThemeColorsProvider.getColors(activeThemeName) }

    val profiles by viewModel.allVoiceProfiles.collectAsState()
    val meetings by viewModel.allMeetings.collectAsState()
    val allUniqueParticipants = remember(meetings) {
        val list = mutableSetOf<String>()
        meetings.forEach { m ->
            if (m.participantA.isNotBlank()) list.add(m.participantA)
            if (m.participantB.isNotBlank()) list.add(m.participantB)
        }
        list
    }
    val isRecording by viewModel.isProfileRecording.collectAsState()
    val recordingSeconds by viewModel.profileRecordingSeconds.collectAsState()
    val amplitude by viewModel.profileAmplitude.collectAsState()
    val playingProfileId by viewModel.playingProfileId.collectAsState()
    val isSelectingForUpcomingRecord by viewModel.isSelectingForUpcomingRecord.collectAsState()
    val selectedUpcoming by viewModel.selectedUpcomingParticipants.collectAsState()

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { com.example.data.local.SessionManager(context) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showLimitExceededDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<VoiceProfile?>(null) }

    // Navigation Launcher for Voice permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val isPro = sessionManager.isProActive
            if (!isPro && profiles.size >= 5) {
                showLimitExceededDialog = true
            } else {
                showAddDialog = true
            }
        } else {
            Toast.makeText(context, "Разрешение на запись аудио необходимо для регистрации голоса", Toast.LENGTH_SHORT).show()
        }
    }

    // List of template read texts
    val textTemplates = remember {
        listOf(
            "Привет! Я записываю образец своего голоса, чтобы Deals Recorder мог автоматически распознавать меня на встречах, разделять реплики диалога и составлять точные текстовые протоколы.",
            "Уважаемые партнеры и коллеги, давайте зафиксируем наши договоренности по проекту. Все задачи будут автоматически распределены с указанием сроков и ответственных лиц.",
            "Мы договорились утвердить рекламный бюджет на маркетинг в полном объеме и начать разработку визуальных материалов уже на этой неделе, зафиксировав всё в ИИ-сводке."
        )
    }

    val currentEmail = sessionManager.loggedInEmail ?: "komilsay777@gmail.com"
    val currentName = sessionManager.loggedInName ?: ""

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

                // Top-left soft circular accent details (pastel light mint circle)
                drawCircle(
                    color = if (isDarkTheme) Color(0xFF132B2B).copy(alpha = 0.6f) else Color(0xFFABD8D4),
                    radius = 100.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(-15.dp.toPx(), -15.dp.toPx())
                )

                // Top-right soft circular accent details (pastel light mint circle)
                drawCircle(
                    color = if (isDarkTheme) Color(0xFF132B2B).copy(alpha = 0.5f) else Color(0xFFABD8D4),
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
        bottomBar = {},
        floatingActionButton = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            // Header height 165.dp (exactly matching main page)
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
                            text = "My Voices",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage speakers and recognition models",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E293B).copy(alpha = 0.75f)
                            )
                        )
                    }

                    // Header action buttons: back button only
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimalist back icon button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .clickable { onNavigateBack() }
                                .testTag("btn_back_voices")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Overlapping Card Container (identical to main page layout)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .offset(y = (-24).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(if (isDark) Color(0xFF0F172A) else Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Modern Header Row for Voice Profiles in the white area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (profiles.isEmpty()) "Ваши голоса" else "Сохраненные профили (${profiles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // High-fidelity icon-only Add Button in the white area!
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .clickable {
                                    val isPro = sessionManager.isProActive
                                    if (!isPro && profiles.size >= 5) {
                                        showLimitExceededDialog = true
                                    } else {
                                        val permissionCheck = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                            showAddDialog = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                                .testTag("add_voice_profile_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd, // Icon only, no "Add" word!
                                contentDescription = "Добавить голос",
                                tint = if (isDark) Color(0xFF38BDF8) else themeColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (profiles.isEmpty()) {
                        // Beautiful Minimalist Empty State (slightly adjusted padding to account for the header row)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Нет зарегистрированных голосов",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Запишите 20-30 секунд своей речи или речей ваших коллег. Наш ИИ научится распознавать каждого участника на записи встречи и разделять протокол по именам.",
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isSelectingForUpcomingRecord) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF0F9FF)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                            .testTag("selection_banner_upcoming")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Выбор голосов для записи",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF0369A1)
                                                )
                                                Text(
                                                    text = "Отметьте участников, которые будут говорить на записи встречи. Новые добавленные голоса выбираются автоматически.",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0E7490)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            items(profiles, key = { it.id }) { profile ->
                                VoiceProfileCard(
                                    profile = profile,
                                    isPlaying = playingProfileId == profile.id,
                                    onPlayToggle = {
                                        if (playingProfileId == profile.id) {
                                            viewModel.stopProfileSample()
                                        } else {
                                            viewModel.playProfileSample(profile)
                                        }
                                    },
                                    onDeleteRequested = {
                                        profileToDelete = profile
                                    },
                                    isSelectionMode = isSelectingForUpcomingRecord,
                                    isSelected = selectedUpcoming.contains(profile.name),
                                    onSelectToggle = {
                                        viewModel.toggleUpcomingParticipant(profile.name)
                                    },
                                    themeColors = themeColors
                                )
                            }
                        }
                    }
                }
            }
    } // closes Column

            // High-End Slide up voice registration drawer / dialog
            androidx.compose.animation.AnimatedVisibility(
                visible = showAddDialog,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                VoiceRegistrationPanel(
                    isRecording = isRecording,
                    seconds = recordingSeconds,
                    amplitude = amplitude,
                    templates = textTemplates,
                    onStartRecord = { viewModel.startProfileRecording() },
                    onStopRecord = {
                        val file = viewModel.stopProfileRecording()
                        file
                    },
                    onSaveProfile = { name, rel, path, duration, phrase ->
                        coroutineScope.launch {
                            viewModel.saveVoiceProfile(name, rel, path, duration, phrase)
                            Toast.makeText(context, "Голос профиля сохранен!", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    onCancel = {
                        if (isRecording) {
                            viewModel.stopProfileRecording()
                        }
                        showAddDialog = false
                    },
                    themeColors = themeColors
                )
            }

            // Standard Delete Confirmation Dialogue
            if (profileToDelete != null) {
                AlertDialog(
                    onDismissRequest = { profileToDelete = null },
                    title = { Text("Удалить профиль голоса?") },
                    text = { Text("Вы действительно хотите навсегда удалить голосовой профиль «${profileToDelete?.name}»? Это удалит звуковой образец с устройства.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                profileToDelete?.let { viewModel.deleteVoiceProfile(it) }
                                profileToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Удалить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { profileToDelete = null }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            if (showLimitExceededDialog) {
                AlertDialog(
                    onDismissRequest = { showLimitExceededDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Лимит превышен", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text("На бесплатном тарифе доступно до 5 голосовых профилей. Вы исчерпали этот лимит.\n\nПодключите тариф PRO для снятия лимитов и добавления неограниченного числа спикеров!")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLimitExceededDialog = false
                                onNavigateToSubscription()
                            }
                        ) {
                            Text("К тарифам PRO")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLimitExceededDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            // Elegant high-fidelity overlay floating bottom navigation bar
            if (!showAddDialog) {
                if (isSelectingForUpcomingRecord) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                onNavigateBack()
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .testTag("apply_upcoming_speakers_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Text(
                                    text = "Вернуться к записи (Спикеров: ${selectedUpcoming.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
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
                                        .clickable { onNavigateToMeetingList() }
                                        .padding(vertical = 12.dp)
                                        .testTag("nav_meetings_tab"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "Встречи",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
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
                                        .clickable { /* Already on voice profiles screen */ }
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
                                            tint = if (isDark) Color(0xFF38BDF8) else themeColors.primary, // Dynamic active highlight
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
                                    color = themeColors.primary.copy(alpha = homePulseAlpha),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = (-16).dp)
                                .size(54.dp)
                                .scale(homePulseScale2)
                                .background(
                                    color = themeColors.primary.copy(alpha = homePulseAlpha2),
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
                                .clickable { onNavigateToRecording() }
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
                }
            }
        }
    }
}

@Composable
fun VoiceProfileCard(
    profile: VoiceProfile,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDeleteRequested: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    themeColors: CustomThemeColors
) {
    val formattedDate = remember(profile.registeredAt) {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("ru"))
        formatter.format(Date(profile.registeredAt))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isSelectionMode && isSelected) 2.dp else 1.dp,
            color = if (isSelectionMode && isSelected) themeColors.primary else Color(0xFFE2E8F0)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelectionMode && isSelected) themeColors.primary.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelectionMode && isSelected) 3.dp else 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSelectionMode, onClick = onSelectToggle)
            .testTag("voice_profile_card_${profile.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant premium custom selection indicator on the left side (instead of basic Checkbox!)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) themeColors.primary else Color.Transparent)
                        .border(
                            width = if (isSelected) 0.dp else 2.dp,
                            color = if (isSelected) Color.Transparent else Color(0xFFCBD5E1),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Выбрано",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            // High-end Micro Play/Stop button
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) themeColors.primary else Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Остановить" else "Прослушать",
                    tint = if (isPlaying) Color.White else themeColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Body content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${profile.durationSeconds} сек",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "•",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Right actions face
            if (!isSelectionMode) {
                IconButton(
                    onClick = onDeleteRequested,
                    modifier = Modifier.testTag("delete_profile_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить профиль",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRegistrationPanel(
    isRecording: Boolean,
    seconds: Int,
    amplitude: Int,
    templates: List<String>,
    onStartRecord: () -> Boolean,
    onStopRecord: () -> File?,
    onSaveProfile: (name: String, rel: String, path: String, duration: Int, phrase: String) -> Unit,
    onCancel: () -> Unit,
    themeColors: CustomThemeColors
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Коллега") }
    var currentTemplateIndex by remember { mutableStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var finalRecordedSeconds by remember { mutableStateOf(0) }

    val relationships = listOf("Секретарь (Вы)", "Коллега", "Клиент", "Партнер", "Другое")
    var dropdownExpanded by remember { mutableStateOf(false) }

    val isDark = false

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onCancel() }
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false, onClick = {})
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Top cancel row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(themeColors.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = themeColors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Новый профиль голоса",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Profile Info Input - Name input takes full width, relationship selector removed cleanly
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя человека") },
                    placeholder = { Text("Александр") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        focusedLabelColor = themeColors.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presentation Teleprompter / Text Prompt Card - Pinned high up, very readable, max 110dp
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
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
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = themeColors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ПРОЧИТАЙТЕ ТЕКСТ ВСЛУХ РОТ СВОБОДНО",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }

                            TextButton(
                                onClick = {
                                    currentTemplateIndex = (currentTemplateIndex + 1) % templates.size
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = themeColors.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Другой", style = MaterialTheme.typography.labelSmall, color = themeColors.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 110.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "«${templates[currentTemplateIndex]}»",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 24.sp,
                                    color = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful voice console: live state and waveforms (Record button moved to bottom bar for superior ergonomic layout!)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1.0f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val activeSeconds = if (recordedFile != null) finalRecordedSeconds else seconds
                        val progressPercent = (activeSeconds / 20f).coerceIn(0f, 1f)
                        
                        // Status & Timer Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (isRecording) Color(0xFFEF4444) else if (recordedFile != null) Color(0xFF10B981) else Color(0xFF94A3B8),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = if (isRecording) "Идет запись..." else if (recordedFile != null) "Успешно записано" else "Готов к записи",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", activeSeconds / 60, activeSeconds % 60),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (isRecording) Color(0xFFEF4444) else themeColors.primary
                                )
                                Text(
                                    text = "/ 00:20 минимум",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large detailed Speech Wave Canvas - Beautiful and spacious!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val defaultWaveColor = themeColors.primary.copy(alpha = 0.2f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 3.5.dp.toPx()
                                val barGap = 3.dp.toPx()
                                val maxBars = (size.width / (barWidth + barGap)).toInt()
                                val ampFactor = (amplitude.coerceIn(0, 32767) / 32767f)
                                
                                for (i in 0 until maxBars) {
                                    val left = i * (barWidth + barGap)
                                    val baseHeight = 6.dp.toPx()
                                    val multiplier = if (isRecording) {
                                        val distFromCenter = 1f - Math.abs(i - maxBars/2f) / (maxBars/2f)
                                        (distFromCenter * ampFactor * 48.dp.toPx()).coerceAtLeast(0f)
                                    } else {
                                        0f
                                    }
                                    
                                    val barHeight = baseHeight + multiplier
                                    val top = (size.height - barHeight) / 2
                                    
                                    drawRoundRect(
                                        color = if (isRecording) Color(239, 68, 68, 180) else defaultWaveColor,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth/2, barWidth/2)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions Panel - Symmetrical bottom bar with glowing record button in the center
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeSeconds = if (recordedFile != null) finalRecordedSeconds else seconds
                    val progressPercent = (activeSeconds / 20f).coerceIn(0f, 1f)

                    // Left Side: Cancel Button
                    Box(
                        modifier = Modifier.weight(0.85f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF475569)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Отмена",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Center Side: Symmetrical Glowing Pulsing Circular Button with Progress Arc
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(76.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 4.dp.toPx()
                            val radius = (size.width - strokeWidth) / 2f
                            drawCircle(
                                color = Color(0xFFE2E8F0),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = if (progressPercent >= 1f) Color(0xFF10B981) else themeColors.primary,
                                startAngle = -90f,
                                sweepAngle = progressPercent * 360f,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                                size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }

                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .scale(pulseScale)
                                    .background(
                                        color = Color(0xFFEF4444).copy(alpha = pulseAlpha),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .scale(pulseScale2)
                                    .background(
                                        color = Color(0xFFEF4444).copy(alpha = pulseAlpha2),
                                        shape = CircleShape
                                    )
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    val file = onStopRecord()
                                    if (file != null) {
                                        recordedFile = file
                                        finalRecordedSeconds = seconds
                                    }
                                } else {
                                    recordedFile = null
                                    onStartRecord()
                                }
                            },
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color(0xFFEF4444) else themeColors.primary)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Остановить" else "Запись",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Right Side: Save Button
                    Box(
                        modifier = Modifier.weight(1.15f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        val canSave = name.isNotBlank() && recordedFile != null && finalRecordedSeconds >= 20
                        Button(
                            onClick = {
                                val file = recordedFile
                                if (file != null && canSave) {
                                    onSaveProfile(name, relationship, file.absolutePath, finalRecordedSeconds, templates[currentTemplateIndex])
                                }
                            },
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (finalRecordedSeconds >= 20) Color(0xFF10B981) else themeColors.primary,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_profile_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (canSave) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = "Сохранить",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (canSave) Color.White else Color(0xFF64748B),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
