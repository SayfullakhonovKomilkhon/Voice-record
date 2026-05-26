package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
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
import com.example.data.model.VoiceProfile
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
    onNavigateToSubscription: () -> Unit
) {
    val profiles by viewModel.allVoiceProfiles.collectAsState()
    val isRecording by viewModel.isProfileRecording.collectAsState()
    val recordingSeconds by viewModel.profileRecordingSeconds.collectAsState()
    val amplitude by viewModel.profileAmplitude.collectAsState()
    val playingProfileId by viewModel.playingProfileId.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Голосовые профили",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!showAddDialog) {
                ExtendedFloatingActionButton(
                    onClick = {
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
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Зарегистрировать голос") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).testTag("add_voice_profile_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (profiles.isEmpty()) {
                // Beautiful Minimalist Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
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
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Сохраненные профили (${profiles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
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
                            }
                        )
                    }
                }
            }

            // High-End Slide up voice registration drawer / dialog
            if (showAddDialog) {
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
                    }
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
        }
    }
}

@Composable
fun VoiceProfileCard(
    profile: VoiceProfile,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDeleteRequested: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val formattedDate = remember(profile.registeredAt) {
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("ru"))
        formatter.format(Date(profile.registeredAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF0F4FC)
        ),
        modifier = Modifier.fillMaxWidth().testTag("voice_profile_card_${profile.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Micro Play capsule button
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Остановить" else "Прослушать"
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Body
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Chip for relationship indicator
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = profile.relationship,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Запись: ${profile.durationSeconds} сек",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Trash action
            IconButton(
                onClick = onDeleteRequested,
                modifier = Modifier.testTag("delete_profile_${profile.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить профиль",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
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
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Коллега") }
    var currentTemplateIndex by remember { mutableStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var finalRecordedSeconds by remember { mutableStateOf(0) }

    val relationships = listOf("Секретарь (Вы)", "Коллега", "Клиент", "Партнер", "Другое")
    var dropdownExpanded by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(top = 24.dp)
            .clickable(enabled = false, onClick = {}) // Block clicks underneath
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Top cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Новый голосовой профиль",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // Profile Info Inputs
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Имя человека") },
                            placeholder = { Text("Например: Александр Петров") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )

                        // Custom Dropdown Menu for relationship selector
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = relationship,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Кто это?") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                relationships.forEach { rel ->
                                    DropdownMenuItem(
                                        text = { Text(rel) },
                                        onClick = {
                                            relationship = rel
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Presentation and Read instructions
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFF0F4FC)
                        ),
                        shape = RoundedCornerShape(16.dp),
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
                                Text(
                                    text = "Прочитайте вслух следующий текст:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Change template button
                                TextButton(
                                    onClick = {
                                        currentTemplateIndex = (currentTemplateIndex + 1) % templates.size
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Другой текст", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "«${templates[currentTemplateIndex]}»",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    // Recording area with wave visualizer and timer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val activeSeconds = if (recordedFile != null) finalRecordedSeconds else seconds
                        val progressPercent = (activeSeconds / 20f).coerceIn(0f, 1f)
                        
                        Text(
                            text = if (isRecording) "Идет запись голоса..." else if (recordedFile != null) "Голос успешно записан" else "Готов к записи",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Mic with visualizer and timer stack
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = String.format("%02d:%02d", activeSeconds / 60, activeSeconds % 60),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "/ 00:20 сек минимум",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Live Speech Wave Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val defaultWaveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 4.dp.toPx()
                                val barGap = 4.dp.toPx()
                                val maxBars = (size.width / (barWidth + barGap)).toInt()
                                
                                val ampFactor = (amplitude.coerceIn(0, 32767) / 32767f)
                                
                                // Draw stylized random/live waves
                                for (i in 0 until maxBars) {
                                    val left = i * (barWidth + barGap)
                                    val baseHeight = 4.dp.toPx()
                                    
                                    // Make center waves correspond to the voice amplitude
                                    val multiplier = if (isRecording) {
                                        val distFromCenter = 1f - Math.abs(i - maxBars/2f) / (maxBars/2f)
                                        (distFromCenter * ampFactor * 40.dp.toPx()).coerceAtLeast(0f)
                                    } else {
                                        0f
                                    }
                                    
                                    val barHeight = baseHeight + multiplier
                                    val top = (size.height - barHeight) / 2
                                    
                                    drawRoundRect(
                                        color = if (isRecording) Color(244, 67, 54, 180) else defaultWaveColor,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth/2, barWidth/2)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Circular Record Trigger button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            // Border progress tracking target 20s minimum
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                                drawArc(
                                    color = if (progressPercent >= 1f) Color(0xFF4CAF50) else Color(0xFF2196F3),
                                    startAngle = -90f,
                                    sweepAngle = progressPercent * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }

                            // Inner Trigger button
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
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Остановить" else "Запись",
                                    tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer proceed buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Отмена")
                }

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
                        containerColor = if (finalRecordedSeconds >= 20) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f).testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}
