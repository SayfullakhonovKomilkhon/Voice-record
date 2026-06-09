package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SessionManager
import com.example.data.model.DictionaryTerm
import com.example.ui.components.UnifiedCard
import com.example.ui.viewmodel.MeetingViewModel
import com.example.ui.theme.ThemeColorsProvider
import com.example.ui.theme.CustomThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MeetingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMeetingList: () -> Unit,
    onNavigateToVoiceProfiles: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToCreateMeeting: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    
    val activeThemeName by viewModel.activeThemeColor.collectAsState()
    val themeColors = remember(activeThemeName) { ThemeColorsProvider.getColors(activeThemeName) }
    
    // Collect ViewModel states
    val dictionaryTerms by viewModel.allDictionaryTerms.collectAsState()
    val storageSizeValue by viewModel.storageSizeByte.collectAsState()
    val meetings by viewModel.allMeetings.collectAsState()
    
    val allUniqueParticipants = remember(meetings) {
        val list = mutableSetOf<String>()
        meetings.forEach { m ->
            if (m.participantA.isNotBlank()) list.add(m.participantA)
            if (m.participantB.isNotBlank()) list.add(m.participantB)
        }
        list
    }
    
    // Infinite pulse transitions for center FAB synchronization
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
    
    // Local Inputs & UI States
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    var isEditingProfile by remember { mutableStateOf(false) }
    var editName by remember(sessionManager.loggedInName) {
        mutableStateOf(sessionManager.loggedInName ?: "")
    }
    var editEmail by remember(sessionManager.loggedInEmail) {
        mutableStateOf(sessionManager.loggedInEmail ?: "komilsay777@gmail.com")
    }

    val currentEmail = sessionManager.loggedInEmail ?: "komilsay777@gmail.com"
    val currentName = sessionManager.loggedInName ?: ""
    val displayName = if (currentName.isNotBlank()) currentName else currentEmail.substringBefore("@")
    val userInitials = remember(currentName, currentEmail) {
        if (currentName.isNotBlank()) {
            val parts = currentName.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else {
                currentName.take(2).uppercase()
            }
        } else {
            if (currentEmail.length >= 2) currentEmail.take(2).uppercase() else "U"
        }
    }
    
    var termWordInput by remember { mutableStateOf("") }
    var termExplInput by remember { mutableStateOf("") }

    // Button Interaction Sources & Scales
    val subInteractionSource = remember { MutableInteractionSource() }
    val isSubHovered by subInteractionSource.collectIsHoveredAsState()
    val isSubPressed by subInteractionSource.collectIsPressedAsState()
    val subScale by animateFloatAsState(
        targetValue = if (isSubPressed) 0.96f else if (isSubHovered) 1.04f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "subScale"
    )

    val addTermInteractionSource = remember { MutableInteractionSource() }
    val isAddTermHovered by addTermInteractionSource.collectIsHoveredAsState()
    val isAddTermPressed by addTermInteractionSource.collectIsPressedAsState()
    val addTermScale by animateFloatAsState(
        targetValue = if (isAddTermPressed) 0.95f else if (isAddTermHovered) 1.08f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "addTermScale"
    )

    val clearInteractionSource = remember { MutableInteractionSource() }
    val isClearHovered by clearInteractionSource.collectIsHoveredAsState()
    val isClearPressed by clearInteractionSource.collectIsPressedAsState()
    val clearScale by animateFloatAsState(
        targetValue = if (isClearPressed) 0.96f else if (isClearHovered) 1.04f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "clearScale"
    )

    val delAccInteractionSource = remember { MutableInteractionSource() }
    val isDelAccHovered by delAccInteractionSource.collectIsHoveredAsState()
    val isDelAccPressed by delAccInteractionSource.collectIsPressedAsState()
    val delAccScale by animateFloatAsState(
        targetValue = if (isDelAccPressed) 0.96f else if (isDelAccHovered) 1.04f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "delAccScale"
    )
    
    // Load storage size on launch
    LaunchedEffect(Unit) {
        viewModel.updateStorageSize()
    }

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
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                            text = "My Settings",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Preferences and configurations",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1E293B).copy(alpha = 0.75f)
                            )
                        )
                    }

                    // Header action buttons: back button only (removed redundant profile badge)
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
                                .testTag("btn_back_settings")
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            
            // --- SECTION 1: ACCOUNT & SHORTCUTS GROUP (АККАУНТ И УПРАВЛЕНИЕ) ---
            item {
                Text(
                    text = "Аккаунт и возможности",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                UnifiedCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().testTag("settings_account_group_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isPro = sessionManager.isProActive
                        if (isEditingProfile) {
                            // --- EDIT PROFILE MODE ---
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = userInitials,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.BottomEnd)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Редактирование профиля",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Имя") },
                                placeholder = { Text("Например, Комил Саидов") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_profile_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Email") },
                                placeholder = { Text("komilsay777@gmail.com") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_profile_email_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isEditingProfile = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("edit_profile_cancel_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Отмена")
                                }
                                
                                Button(
                                    onClick = {
                                        if (editEmail.isBlank()) {
                                            viewModel.showToast("Email не может быть пустым!")
                                        } else {
                                            sessionManager.loggedInEmail = editEmail.trim()
                                            sessionManager.loggedInName = if (editName.isBlank()) null else editName.trim()
                                            isEditingProfile = false
                                            viewModel.showToast("Профиль успешно обновлен!")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("edit_profile_save_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Сохранить")
                                }
                            }
                        } else {
                            // --- DISPLAY VIEW PROFILE MODE ---
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        editName = sessionManager.loggedInName ?: ""
                                        editEmail = sessionManager.loggedInEmail ?: "komilsay777@gmail.com"
                                        isEditingProfile = true
                                    }
                                    .testTag("settings_avatar_editor_trigger")
                            ) {
                                Text(
                                    text = userInitials,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary)
                                        .align(Alignment.BottomEnd)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать",
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (currentName.isNotBlank()) {
                                Text(
                                    text = currentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            
                            Text(
                                text = currentEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (currentName.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (currentName.isNotBlank()) FontWeight.Normal else FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Subscription Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isPro) Color(0xFFFFD700).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPro) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (isPro) Color(0xFFE5A900) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPro) "PRO ТАРИФ" else "БАЗОВЫЙ ТАРИФ",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPro) Color(0xFFC49A00) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        
                        if (!isEditingProfile) {
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Subscription Control Action
                            val subBtnBrush = if (isPro) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFB800))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(themeColors.buttonGradientStart, themeColors.buttonGradientEnd)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(subScale)
                                    .shadow(2.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(subBtnBrush)
                                    .clickable(
                                        interactionSource = subInteractionSource,
                                        indication = LocalIndication.current,
                                        onClick = onNavigateToSubscription
                                    )
                                    .padding(vertical = 14.dp, horizontal = 24.dp)
                                    .testTag("manage_subscription_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = if (isPro) Color(0xFF3E2F00) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPro) "Изменить / Отключить подписку" else "Активировать PRO подписку",
                                        color = if (isPro) Color(0xFF3E2F00) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Left-aligned section subheader inside the card for actions
                        Text(
                            text = "Быстрый запуск и управление",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                        )

                        // Action 1: Начать запись встречи (Master Record Action), nested!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToCreateMeeting() }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("start_recording_settings_card"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Начать запись встречи",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Старт аудиозаписи и анализа новой встречи",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Action 2: Голосовые профили (Voice Profiles Management), nested!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToVoiceProfiles() }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("voice_profiles_nav_card"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Голосовые профили",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Настройка слепков голоса участников",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // --- SECTION: COLOR THEME CUSTOMIZATION GROUP ---
            item {
                Text(
                    text = "Цветовая тема",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                val currentTheme by viewModel.activeThemeColor.collectAsState()
                
                UnifiedCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().testTag("settings_theme_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Выберите основной цвет приложения",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Настройка цветового оформления верхней панели, кнопок записи и акцентов интерфейса в соответствии с вашим стилем. Дизайн останется лаконичным.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Theme Color Option 1: Teal (current/ocean)
                            ThemeColorCell(
                                name = "Бирюзовый",
                                themeKey = "teal",
                                themeColor = Color(0xFF00AAA6),
                                isSelected = currentTheme.lowercase() == "teal",
                                onSelect = {
                                    viewModel.updateThemeColor("teal")
                                    viewModel.showToast("Цветовая тема: Бирюзовый!")
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Theme Color Option 2: Lilac (сиреневый)
                            ThemeColorCell(
                                name = "Сиреневый",
                                themeKey = "lilac",
                                themeColor = Color(0xFF8B5CF6),
                                isSelected = currentTheme.lowercase() == "lilac",
                                onSelect = {
                                    viewModel.updateThemeColor("lilac")
                                    viewModel.showToast("Цветовая тема: Сиреневый!")
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Theme Color Option 3: Pink (розовый)
                            ThemeColorCell(
                                name = "Розовый",
                                themeKey = "pink",
                                themeColor = Color(0xFFEC4899),
                                isSelected = currentTheme.lowercase() == "pink",
                                onSelect = {
                                    viewModel.updateThemeColor("pink")
                                    viewModel.showToast("Цветовая тема: Розовый!")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- SECTION 2: AI PREFERENCES GROUP (ИИ-ПРЕДПОЧТЕНИЯ) ---
            item {
                Text(
                    text = "ИИ-Предпочтения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                var selectedLevel by remember { mutableStateOf(sessionManager.summaryDetailLevel) }

                UnifiedCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().testTag("settings_ai_group_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Part A: Summary settings
                        Text(
                            text = "Детализация саммари встреч",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Уровень подробности хайлайтов и списка задач, которые ИИ извлекает из транскрипта вашей беседы.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Option 1: Executive Brief
                        val isExecutiveSelected = selectedLevel == "executive"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isExecutiveSelected) themeColors.primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
                                .border(
                                    width = 1.dp,
                                    color = if (isExecutiveSelected) themeColors.primary else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedLevel = "executive"
                                    sessionManager.summaryDetailLevel = "executive"
                                    viewModel.showToast("Настройки детализации успешно сохранены!")
                                }
                                .padding(12.dp)
                                .testTag("detail_level_executive"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isExecutiveSelected) themeColors.primary else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    imageVector = if (isExecutiveSelected) Icons.Default.Check else Icons.Default.ShortText,
                                    contentDescription = null,
                                    tint = if (isExecutiveSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Краткий обзор (Executive Brief)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExecutiveSelected) Color(0xFF044E4C) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Ключевые выводы в 3 предложениях и критические задачи.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isExecutiveSelected) Color(0xFF044E4C).copy(alpha = 0.8f) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: Detailed Minutes
                        val isDetailedSelected = selectedLevel == "detailed"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDetailedSelected) themeColors.primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
                                .border(
                                    width = 1.dp,
                                    color = if (isDetailedSelected) themeColors.primary else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedLevel = "detailed"
                                    sessionManager.summaryDetailLevel = "detailed"
                                    viewModel.showToast("Настройки детализации успешно сохранены!")
                                }
                                .padding(12.dp)
                                .testTag("detail_level_detailed"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDetailedSelected) themeColors.primary else Color(0xFFE2E8F0))
                            ) {
                                Icon(
                                    imageVector = if (isDetailedSelected) Icons.Default.Check else Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    tint = if (isDetailedSelected) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Детальный протокол (Detailed Minutes)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDetailedSelected) Color(0xFF044E4C) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Максимально полный детализированный список задач.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDetailedSelected) Color(0xFF044E4C).copy(alpha = 0.8f) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Part B: Terminology glossary
                        Text(
                            text = "Словарь терминов",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Уникальные термины, аббревиатуры и имена для улучшенного качества распознавания аудиозаписей.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // New term form nested beautifully
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = termWordInput,
                                onValueChange = { termWordInput = it },
                                label = { Text("Термин/Имя", fontSize = 11.sp) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f).testTag("term_input"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    focusedContainerColor = if (isDark) Color(0xFF1E242F) else Color(0xFFF1F5F9),
                                    unfocusedContainerColor = if (isDark) Color(0xFF1E242F) else Color(0xFFF1F5F9),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                            TextField(
                                value = termExplInput,
                                onValueChange = { termExplInput = it },
                                label = { Text("Объяснение", fontSize = 11.sp) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1.2f).testTag("definition_input"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    focusedContainerColor = if (isDark) Color(0xFF1E242F) else Color(0xFFF1F5F9),
                                    unfocusedContainerColor = if (isDark) Color(0xFF1E242F) else Color(0xFFF1F5F9),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                            IconButton(
                                onClick = {
                                        if (termWordInput.isNotBlank()) {
                                            viewModel.addDictionaryTerm(termWordInput, termExplInput)
                                            termWordInput = ""
                                            termExplInput = ""
                                        } else {
                                            viewModel.showToast("Введите слово или аббревиатуру", false)
                                        }
                                    },
                                interactionSource = addTermInteractionSource,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(addTermScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isAddTermHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                                        else MaterialTheme.colorScheme.primary
                                    )
                                    .testTag("add_term_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить термин",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Terminology list inside the card
                        if (dictionaryTerms.isEmpty()) {
                            Text(
                                text = "Словарь пуст. Добавьте свои первые термины выше.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                dictionaryTerms.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(themeColors.primary)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = item.term,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E293B)
                                                )
                                            }
                                            if (item.explanation.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.explanation,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF64748B),
                                                    modifier = Modifier.padding(start = 14.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteDictionaryTerm(item) },
                                            modifier = Modifier.size(32.dp).testTag("delete_term_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Удалить термин",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: STORAGE AND CLEANUP GROUP (ПАМЯТЬ И УДАЛЕНИЕ ДАННЫХ) ---
            item {
                Text(
                    text = "Память и данные",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                UnifiedCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().testTag("settings_storage_group_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    "Используемая память приложением",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val sizeKB = storageSizeValue / 1024L
                                val sizeText = if (sizeKB >= 1024L) {
                                    String.format("%.2f МБ", sizeKB.toFloat() / 1024f)
                                } else {
                                    "$sizeKB КБ"
                                }
                                Text(
                                    text = sizeText,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(clearScale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isClearHovered) Color(0xFFF1F5F9) else Color(0xFFF8FAFC))
                                .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = clearInteractionSource,
                                    indication = LocalIndication.current,
                                    onClick = { showClearConfirmDialog = true }
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Очистить локальные аудиозаписи",
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "* Очистка освободит место на диске. Будут удалены только локальные звуковые файлы; вся текстовая аналитика и саммари встреч останутся доступными в истории.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Security and Dangerous Zone Option
                        Text(
                            text = "Удаление учетной записи",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Полное безвозвратное стирание профиля, всех встреч, ИИ-данных, словаря и голосовых профилей с устройства без возможности восстановления.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(delAccScale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDelAccPressed) Color(0xFFFEF2F2) else Color.White)
                                .border(
                                    width = 1.5.dp,
                                    color = Color(0xFFFCA5A5),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(
                                    interactionSource = delAccInteractionSource,
                                    indication = LocalIndication.current,
                                    onClick = { showDeleteConfirmDialog = true }
                                )
                                .padding(vertical = 12.dp)
                                .testTag("delete_account_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Стереть аккаунт и все данные",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
        }

            // Beautiful completely transparent floating navigation bar overlay (meeting the request of free/unobstructed background)
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
                        .clickable { onNavigateToCreateMeeting() }
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

    // --- DIALOGS ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Очистить аудиофайлы?") },
            text = { Text("Вы действительно хотите удалить все аудиозаписи с устройства? Вся текстовая аналитика и саммари встреч останутся доступными.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearOldRecordings()
                        showClearConfirmDialog = false
                        viewModel.showToast("Аудиозаписи успешно удалены для экономии места")
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить аудио", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Стереть все данные?") },
            text = { Text("Это абсолютно необратимое действие. Все встречи, голосовые профили и подписка будут удалены навсегда. Вы действительно хотите продолжить?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteAccountAndAllData {
                            Toast.makeText(context, "Ваш аккаунт и все данные успешно удалены", Toast.LENGTH_LONG).show()
                            onAccountDeleted()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить всё окончательно", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ThemeColorCell(
    name: String,
    themeKey: String,
    themeColor: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cellScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) themeColor.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) themeColor else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("theme_color_option_$themeKey")
    ) {
        // Colored dot with white border and inner checkmark if selected
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(themeColor)
                .border(2.dp, Color.White, CircleShape)
                .shadow(1.dp, CircleShape)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) themeColor else Color(0xFF475569),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

