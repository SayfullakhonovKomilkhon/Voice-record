package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceProfile
import com.example.ui.viewmodel.MeetingViewModel
import com.example.ui.theme.CorporatePrimaryDark
import com.example.ui.theme.CorporatePrimaryLight
import com.example.ui.theme.CorporateSecondaryDark
import com.example.ui.theme.CorporateSecondaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    viewModel: MeetingViewModel,
    onNavigateBack: () -> Unit,
    onStartRecording: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val profiles by viewModel.allVoiceProfiles.collectAsState()
    val isDark = false
    val context = LocalContext.current

    var titleInput by remember { mutableStateOf("") }
    var participantAInput by remember { mutableStateOf("") }
    var participantBInput by remember { mutableStateOf("") }
    var additionalParticipantsInput by remember { mutableStateOf(listOf<String>()) }

    var expandedA by remember { mutableStateOf(false) }
    var expandedB by remember { mutableStateOf(false) }

    // Dynamically auto-fill with first and second available voice profiles if any
    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty()) {
            if (participantAInput.isBlank()) {
                participantAInput = profiles.getOrNull(0)?.name ?: ""
            }
            if (participantBInput.isBlank()) {
                participantBInput = profiles.getOrNull(1)?.name ?: ""
            }
        }
    }

    var showWarningConsentDialog by remember { mutableStateOf(false) }
    var showLimitExceededDialog by remember { mutableStateOf(false) }

    // Request permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showWarningConsentDialog = true
        } else {
            Toast.makeText(context, "Необходимо разрешение на использование микрофона", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val primaryColor = if (isDark) CorporatePrimaryDark else CorporatePrimaryLight
                val secondaryColor = if (isDark) CorporateSecondaryDark else CorporateSecondaryLight
                val radius = size.minDimension * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent)
                        } else {
                            listOf(primaryColor.copy(alpha = 0.07f), Color.Transparent)
                        },
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.05f),
                        radius = radius
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(secondaryColor.copy(alpha = 0.12f), Color.Transparent)
                        } else {
                            listOf(secondaryColor.copy(alpha = 0.07f), Color.Transparent)
                        },
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.95f),
                        radius = radius
                    )
                )
            },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Новые переговоры",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("create_meeting_back")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Параметры встречи",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Задайте тему обсуждения и укажите участников переговоров для повышения точности ИИ-распознавания спикеров.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )
            }

            item {
                // Meeting Title Input
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Тема переговоров") },
                    placeholder = { Text("Например: Маркетинговая стратегия Q3") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meeting_title_input")
                )
            }

            // Participant A Row / Dropdown
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Первый собеседник (Участник А)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedA,
                        onExpandedChange = { expandedA = !expandedA }
                    ) {
                        OutlinedTextField(
                            value = participantAInput,
                            onValueChange = { participantAInput = it },
                            label = { Text("Введите имя или выберите профиль") },
                            placeholder = { Text("Например: Иван Козлов") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { expandedA = !expandedA }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать профиль"
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("participant_a_input")
                        )

                        ExposedDropdownMenu(
                            expanded = expandedA,
                            onDismissRequest = { expandedA = false }
                        ) {
                            if (profiles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Нет профилей голоса") },
                                    enabled = false,
                                    onClick = {}
                                )
                            } else {
                                profiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.RecordVoiceOver,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text("${profile.name} (${profile.relationship})")
                                            }
                                        },
                                        onClick = {
                                            participantAInput = profile.name
                                            expandedA = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Participant B Row / Dropdown
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Второй собеседник (Участник Б)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedB,
                        onExpandedChange = { expandedB = !expandedB }
                    ) {
                        OutlinedTextField(
                            value = participantBInput,
                            onValueChange = { participantBInput = it },
                            label = { Text("Введите имя или выберите профиль") },
                            placeholder = { Text("Например: Сергей Петров") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { expandedB = !expandedB }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать профиль"
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("participant_b_input")
                        )

                        ExposedDropdownMenu(
                            expanded = expandedB,
                            onDismissRequest = { expandedB = false }
                        ) {
                            if (profiles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Нет профилей голоса") },
                                    enabled = false,
                                    onClick = {}
                                )
                            } else {
                                profiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.RecordVoiceOver,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text("${profile.name} (${profile.relationship})")
                                            }
                                        },
                                        onClick = {
                                            participantBInput = profile.name
                                            expandedB = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Other dynamic participants
            additionalParticipantsInput.forEachIndexed { index, value ->
                item(key = "additional_part_$index") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Дополнительный участник ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    additionalParticipantsInput = additionalParticipantsInput.toMutableList().apply {
                                        removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить участника",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        var isExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = isExpanded,
                            onExpandedChange = { isExpanded = !isExpanded }
                        ) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { newValue ->
                                    additionalParticipantsInput = additionalParticipantsInput.toMutableList().apply {
                                        set(index, newValue)
                                    }
                                },
                                label = { Text("Введите имя или выберите профиль") },
                                placeholder = { Text("Например: Алексей Смирнов") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(onClick = { isExpanded = !isExpanded }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Выбрать профиль"
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = isExpanded,
                                onDismissRequest = { isExpanded = false }
                            ) {
                                if (profiles.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Нет профилей голоса") },
                                        enabled = false,
                                        onClick = {}
                                    )
                                } else {
                                    profiles.forEach { profile ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.RecordVoiceOver,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text("${profile.name} (${profile.relationship})")
                                                }
                                            },
                                            onClick = {
                                                additionalParticipantsInput = additionalParticipantsInput.toMutableList().apply {
                                                    set(index, profile.name)
                                                }
                                                isExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Button to add another participant
            item {
                OutlinedButton(
                    onClick = {
                        additionalParticipantsInput = additionalParticipantsInput + "Собеседник ${additionalParticipantsInput.size + 3}"
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_participant_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Добавить участника")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить еще участника")
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Proceed Trigger Button
                Button(
                    onClick = {
                        val recordPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (recordPermission == PackageManager.PERMISSION_GRANTED) {
                            showWarningConsentDialog = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("proceed_to_record_button")
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Перейти к записи",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // MANDATORY Notification Warning & Consent Overly dialog
    if (showWarningConsentDialog) {
        AlertDialog(
            onDismissRequest = { showWarningConsentDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Предупреждение",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Запись начинается",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Пожалуйста, предупредите всех участников встречи о ведении аудиозаписи согласно законодательным требованиям и регламенту конфиденциальности.\n\nПродолжая, вы подтверждаете получение согласия всех сторон.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Left
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWarningConsentDialog = false
                        // Set info in ViewModel first
                        viewModel.setMeetingTitle(titleInput)
                        viewModel.setParticipantA(participantAInput)
                        viewModel.setParticipantB(participantBInput)
                        viewModel.setAdditionalParticipants(additionalParticipantsInput.filter { it.isNotBlank() })

                        // Start recording inside foreground service and check limit
                        if (viewModel.startRecording()) {
                            onStartRecording()
                        } else {
                            showLimitExceededDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("consent_confirm_button")
                ) {
                    Text("Я согласен, начать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWarningConsentDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showLimitExceededDialog) {
        AlertDialog(
            onDismissRequest = {
                showLimitExceededDialog = false
                viewModel.resetPremiumLimitReached()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Превышен лимит", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("На бесплатном тарифе доступно до 3 часов записи в месяц. Вы исчерпали этот лимит.\n\nПодключите тариф PRO для безлимитной записи переговоров и экспорта решений!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLimitExceededDialog = false
                        viewModel.resetPremiumLimitReached()
                        onNavigateToSubscription()
                    }
                ) {
                    Text("Тарифы PRO")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLimitExceededDialog = false
                        viewModel.resetPremiumLimitReached()
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
