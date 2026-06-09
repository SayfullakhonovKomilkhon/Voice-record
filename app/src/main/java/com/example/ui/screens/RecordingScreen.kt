package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.UnifiedCard
import com.example.ui.viewmodel.MeetingViewModel
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.max

import com.example.ui.theme.CustomThemeColors
import com.example.ui.theme.ThemeColorsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: MeetingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVoiceProfiles: () -> Unit = {},
    onStopAndStartProcessing: (Long) -> Unit
) {
    val activeThemeColor by viewModel.activeThemeColor.collectAsState()
    val themeColors = ThemeColorsProvider.getColors(activeThemeColor)

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val rawAmplitude by viewModel.amplitude.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val isDark = false // Exclusively light/white theme for this page as requested

    val context = LocalContext.current
    var showSetupSheet by remember { mutableStateOf(false) }
    var setupTitleText by remember { mutableStateOf("") }
    var isNavigatingAway by remember { mutableStateOf(false) }
    val selectedUpcoming by viewModel.selectedUpcomingParticipants.collectAsState()

    // Reset navigating away flag when back stack returns to this screen
    LaunchedEffect(Unit) {
        isNavigatingAway = false
    }

    // Handle setup sheet auto opening and initialization
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            val currentTitle = viewModel.meetingTitle.value
            if (currentTitle.isNotBlank()) {
                setupTitleText = currentTitle
            } else {
                val currentDateTime = java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.SHORT, 
                    java.text.DateFormat.SHORT, 
                    java.util.Locale("ru")
                ).format(java.util.Date())
                setupTitleText = "Встреча от $currentDateTime"
                viewModel.setMeetingTitle(setupTitleText)
            }
            showSetupSheet = true
        } else {
            showSetupSheet = false
        }
    }

    val meetings by viewModel.allMeetings.collectAsState()
    val allUniqueParticipants = remember(meetings) {
        val list = mutableSetOf<String>()
        meetings.forEach { m ->
            if (m.participantA.isNotBlank()) list.add(m.participantA)
            if (m.participantB.isNotBlank()) list.add(m.participantB)
        }
        list
    }

    val title by viewModel.meetingTitle.collectAsState()
    val partA by viewModel.participantA.collectAsState()
    val partB by viewModel.participantB.collectAsState()
    val additionalParticipants by viewModel.additionalParticipants.collectAsState()

    val voiceProfiles by viewModel.allVoiceProfiles.collectAsState()
    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var newParticipantName by remember { mutableStateOf("") }

    val activeParticipants = remember(partA, partB, additionalParticipants) {
        (setOf(partA, partB) + additionalParticipants).filter { it.isNotBlank() }.toSet()
    }
    
    val availableContacts = remember(voiceProfiles, allUniqueParticipants, activeParticipants) {
        val contactNames = voiceProfiles.map { it.name }.toSet() + allUniqueParticipants
        contactNames.filter { it.isNotBlank() && !activeParticipants.contains(it) }
    }

    // Smooth amplitude transition for waves behavior
    val animatedAmplitude = animateFloatAsState(
        targetValue = max(0f, rawAmplitude.toFloat() / 32768f).coerceIn(0.01f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "amplitudeAnimator"
    )

    // Format seconds to HH:MM:SS or MM:SS
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

    // Phase animations for the continuous wave oscillation
    val waveTransition = rememberInfiniteTransition(label = "wave_oscillation")
    val wavePhase1 by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase1"
    )
    val wavePhase2 by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase2"
    )
    val wavePhase3 by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase3"
    )

    // Pulse action scale
    val pulseScale by waveTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Interaction states
    val pauseInteractionSource = remember { MutableInteractionSource() }
    val isPauseHovered by pauseInteractionSource.collectIsHoveredAsState()
    val isPausePressed by pauseInteractionSource.collectIsPressedAsState()
    val pauseScaleAnimate by animateFloatAsState(
        targetValue = if (isPausePressed) 0.92f else if (isPauseHovered) 1.08f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pauseScale"
    )



    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Запись встречи",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onNavigateBack() },
                        modifier = Modifier.testTag("recording_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            // Elegant, high-fidelity floating capsule bottom navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 64.dp)
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                        // Tab 1: Встречи (Meetings List)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .clickable { onNavigateBack() }
                                .padding(vertical = 12.dp)
                                .testTag("nav_meetings_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Встречи",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Symmetrical visual gap for center overlapping Button
                        Spacer(modifier = Modifier.weight(1.2f))

                        // Tab 2: Голоса (Voice Profiles)
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
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Overlapping Floating central red Stop Button
                Box(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .size(54.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            )
                        )
                        .clickable {
                            viewModel.stopRecording { newId ->
                                onStopAndStartProcessing(newId)
                            }
                        }
                        .testTag("stop_recording_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Остановить",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                    )
                )
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful Top-aligned live recording status and timer pill (moved to the very top!)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A),
                    shadowElevation = 6.dp,
                    modifier = Modifier.scale(if (!isPaused && isRecording) pulseScale else 1.0f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    if (isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                                    CircleShape
                                )
                        )
                        Text(
                            text = timerText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // Beautiful interactive central sound volume visualizer: A stunning, ultra-premium Light Mode Wavefield with fluid organic waves stretching edge-to-edge
            val voiceAmp = if (!isPaused && isRecording) animatedAmplitude.value else 0f

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Main audio wave field Canvas spanning edge-to-edge
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerY = size.height / 2f

                    // Layer A: Glowing Aqua Sound Wave (Primary)
                    val pathA = Path()
                    val pointsCount = 100
                    for (i in 0..pointsCount) {
                        val x = (size.width / pointsCount) * i
                        val progress = i.toFloat() / pointsCount
                        
                        // Fluid organic wave formula
                        val sineVal = Math.sin((progress * 3.0 * Math.PI + wavePhase1 * 1.5).toDouble()).toFloat()
                        val edgeFade = Math.sin(progress * Math.PI).toFloat() // Fade smoothly to flat line at edges
                        
                        val dynamicAmp = (voiceAmp * 110.dp.toPx()) * edgeFade
                        val y = centerY + sineVal * dynamicAmp

                        if (i == 0) pathA.moveTo(x, y) else pathA.lineTo(x, y)
                    }
                    drawPath(
                        path = pathA,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0D9488), Color(0xFF0EA5E9), Color(0xFF2563EB))
                        ),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Layer B: Energetic Purple/Indigo Ribbon (Secondary)
                    val pathB = Path()
                    for (i in 0..pointsCount) {
                        val x = (size.width / pointsCount) * i
                        val progress = i.toFloat() / pointsCount
                        
                        val cosVal = Math.cos((progress * 3.8 * Math.PI - wavePhase2 * 1.8).toDouble()).toFloat()
                        val edgeFade = Math.sin(progress * Math.PI).toFloat()
                        
                        val dynamicAmp = (voiceAmp * 80.dp.toPx()) * edgeFade
                        val y = centerY + cosVal * dynamicAmp

                        if (i == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
                    }
                    drawPath(
                        path = pathB,
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF8B5CF6), Color(0xFFEC4899))
                        ),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Layer C: High-frequency Cyan Ray (Tertiary)
                    val pathC = Path()
                    for (i in 0..pointsCount) {
                        val x = (size.width / pointsCount) * i
                        val progress = i.toFloat() / pointsCount
                        
                        val sineVal = Math.sin((progress * 5.0 * Math.PI + wavePhase3 * 2.2).toDouble()).toFloat()
                        val edgeFade = Math.sin(progress * Math.PI).toFloat()
                        
                        val dynamicAmp = (voiceAmp * 50.dp.toPx()) * edgeFade
                        val y = centerY + sineVal * dynamicAmp

                        if (i == 0) pathC.moveTo(x, y) else pathC.lineTo(x, y)
                    }
                    drawPath(
                        path = pathC,
                        color = Color(0xFF06B6D4).copy(alpha = 0.6f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 3. Glowing Active Sound Particles gliding along Wave path A & B when sound is present
                    if (voiceAmp > 0.02f) {
                        val numSparks = 5
                        for (p in 0 until numSparks) {
                            val progress = ((p.toFloat() / numSparks) + (wavePhase1 / 6.28f)) % 1.0f
                            val x = progress * size.width
                            val sineVal = Math.sin((progress * 3.0 * Math.PI + wavePhase1 * 1.5).toDouble()).toFloat()
                            val edgeFade = Math.sin(progress * Math.PI).toFloat()
                            val dynamicAmp = (voiceAmp * 110.dp.toPx()) * edgeFade
                            val y = centerY + sineVal * dynamicAmp

                            // Spark outer halo
                            drawCircle(
                                color = Color(0xFF22D3EE).copy(alpha = 0.35f * edgeFade * voiceAmp),
                                radius = (6.dp.toPx() + animatedAmplitude.value * 10.dp.toPx()) * voiceAmp,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            // Spark inner bright core
                            drawCircle(
                                color = Color.White,
                                radius = (2.5.dp.toPx() + animatedAmplitude.value * 1.5.dp.toPx()) * voiceAmp,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }

                        // Sparks gliding on Wave path B
                        val numSparksB = 3
                        for (p in 0 until numSparksB) {
                            val progress = ((p.toFloat() / numSparksB) - (wavePhase2 / 6.28f)) % 1.0f
                            val positiveProgress = if (progress < 0f) progress + 1f else progress
                            val x = positiveProgress * size.width
                            val cosVal = Math.cos((positiveProgress * 3.8 * Math.PI - wavePhase2 * 1.8).toDouble()).toFloat()
                            val edgeFade = Math.sin(positiveProgress * Math.PI).toFloat()
                            val dynamicAmp = (voiceAmp * 80.dp.toPx()) * edgeFade
                            val y = centerY + cosVal * dynamicAmp

                            drawCircle(
                                color = Color(0xFF818CF8).copy(alpha = 0.3f * edgeFade * voiceAmp),
                                radius = (5.dp.toPx() + animatedAmplitude.value * 8.dp.toPx()) * voiceAmp,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = (2.dp.toPx()) * voiceAmp,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                    }
                }

                // Minimalist Light Mode Telemetry overlay
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    // Top-Left corner: Recording State Status
                    Row(
                        modifier = Modifier.align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(
                                    if (isPaused) Color(0xFFF59E0B) else Color(0xFF0D9488),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isPaused) "STANDBY" else "DYNAMIC FLOW",
                            color = if (isPaused) Color(0xFF94A3B8) else Color(0xFF0D9488),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Top-Right corner: Live Amplitude Output
                    Text(
                        text = "AMP: ${(voiceAmp * 100).toInt()}%",
                        color = Color(0xFF0D9488),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    // Bottom-Left corner: Calculated Wave Frequency Offset
                    val frequencyHz = remember(voiceAmp) {
                        if (voiceAmp > 0f) (110 + (voiceAmp * 280)).toInt() else 0
                    }
                    Text(
                        text = "FREQ: ${frequencyHz}Hz",
                        color = Color(0xFF4F46E5),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    // Bottom-Right corner: Device model identity
                    Text(
                        text = "ACOUSTIC_WAVE_v3",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // Beautiful White/Teal Curved Semicircle Spheroid bottom layout with smooth wave contours
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Interactive Waveform Semicircle Canvas (Cleaned up the horizontal bars marked in red)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    val width = size.width
                    val height = size.height

                    // Semicircle boundary parameters
                    val centerX = width / 2f
                    val centerY = height * 1.05f
                    val baseRadius = width * 0.62f

                    // Subtle teal glow behind the dome for depth
                    drawCircle(
                        color = themeColors.primary.copy(alpha = 0.08f),
                        radius = baseRadius + 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )

                    // Draw the smooth, pristine light back dome
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                        ),
                        radius = baseRadius,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )

                    // Draw active fluctuating wave bands only when recording and not paused
                    val amplitudeFactor = if (!isPaused && isRecording) animatedAmplitude.value else 0.05f

                    // Let's draw 3 beautiful layered sine waves along the circular arc for high-fidelity finish
                    val steps = 120
                    val path1 = Path()
                    val path2 = Path()
                    val path3 = Path()

                    for (i in 0..steps) {
                        val angleDeg = 180f + (180f * i / steps)
                        val angleRad = Math.toRadians(angleDeg.toDouble())

                        // Base coordinate along circle
                        val cosVal = Math.cos(angleRad).toFloat()
                        val sinVal = Math.sin(angleRad).toFloat()

                        // Multi-octave wave oscillation
                        val xOffset = i.toFloat() / steps
                        // Sine wave math calculations
                        val s1 = java.lang.Math.sin((xOffset * 11.5f + wavePhase1).toDouble()).toFloat()
                        val s2 = java.lang.Math.sin((xOffset * 17.0f + wavePhase2).toDouble()).toFloat()
                        val s3 = java.lang.Math.sin((xOffset * 22.0f + wavePhase3).toDouble()).toFloat()
                        
                        val offset1 = s1 * 20f * amplitudeFactor
                        val offset2 = s2 * 14f * amplitudeFactor
                        val offset3 = s3 * 10f * amplitudeFactor

                        val r1 = baseRadius + offset1
                        val r2 = baseRadius + offset2
                        val r3 = baseRadius + offset3

                        val x1 = centerX + r1 * cosVal
                        val y1 = centerY + r1 * sinVal

                        val x2 = centerX + r2 * cosVal
                        val y2 = centerY + r2 * sinVal

                        val x3 = centerX + r3 * cosVal
                        val y3 = centerY + r3 * sinVal

                        if (i == 0) {
                            path1.moveTo(x1, y1)
                            path2.moveTo(x2, y2)
                            path3.moveTo(x3, y3)
                        } else {
                            path1.lineTo(x1, y1)
                            path2.lineTo(x2, y2)
                            path3.lineTo(x3, y3)
                        }
                    }

                    // Apply glowing modern strokes (White highlights on top of Teal/Blue core waves)
                    drawPath(
                        path = path1,
                        color = Color.White.copy(alpha = 0.85f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path2,
                        color = themeColors.primary.copy(alpha = 0.55f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path3,
                        color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Overlay beautiful digital controls right inside the clean dome (without the duplicate top timer pill)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .offset(y = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Centered high-fidelity sleek Play/Pause Button mirroring third picture
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                color = if (isPauseHovered) themeColors.primary else Color(0xFF0F172A)
                            )
                            .clickable(
                                interactionSource = pauseInteractionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    if (isPaused) {
                                        viewModel.resumeRecording()
                                    } else {
                                        viewModel.pauseRecording()
                                    }
                                }
                            )
                            .testTag("pause_resume_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Возобновить" else "Пауза",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = if (isPaused) "ЗАПИСЬ ПРИОСТАНОВЛЕНА" else "ИДЕТ ЗАПИСЬ...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (isPaused) Color(0xFFB45309) else themeColors.primary
                    )
                }
            }
        }
    }

    if (showAddParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = {
                Text(
                    text = "Добавить участника",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Введите имя вручную или выберите существующий контакт из вашей книги голосов:",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = newParticipantName,
                        onValueChange = { newParticipantName = it },
                        label = { Text("Имя участника") },
                        placeholder = { Text("Например: Анна") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primary,
                            focusedLabelColor = themeColors.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_participant_name_input")
                    )

                    if (availableContacts.isNotEmpty()) {
                        Text(
                            text = "Доступные профили из книги голосов:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(availableContacts) { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable {
                                                viewModel.addAdditionalParticipant(contact)
                                                showAddParticipantDialog = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = themeColors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = contact,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E293B),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Все контакты из адресной книги уже добавлены.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newParticipantName.isNotBlank()) {
                            viewModel.addAdditionalParticipant(newParticipantName.trim())
                            newParticipantName = ""
                            showAddParticipantDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primary
                    ),
                    modifier = Modifier.testTag("confirm_add_participant_btn")
                ) {
                    Text("Добавить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newParticipantName = ""
                        showAddParticipantDialog = false
                    }
                ) {
                    Text("Отмена", color = Color(0xFF64748B))
                }
            }
        )
    }

    if (showSetupSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val profiles by viewModel.allVoiceProfiles.collectAsState()

        ModalBottomSheet(
            onDismissRequest = {
                if (!isNavigatingAway) {
                    showSetupSheet = false
                    viewModel.setIsSelectingForUpcomingRecord(false)
                    viewModel.resetUpcomingParticipants()
                    onNavigateBack()
                }
            },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.70f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(themeColors.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Настройка записи",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                Text(
                    text = "Укажите тему и выберите участников с готовыми моделями голоса.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = setupTitleText,
                    onValueChange = { setupTitleText = it },
                    label = { Text("Тема встречи") },
                    placeholder = { Text("Например: Летучка") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primary,
                        focusedLabelColor = themeColors.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_record_title_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выберите спикеров (ИИ):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        letterSpacing = 0.2.sp
                    )

                    // Sleek, compact and lightweight Record Voice button (instead of the giant bottom button)
                    androidx.compose.material3.TextButton(
                        onClick = {
                            isNavigatingAway = true
                            viewModel.setMeetingTitle(setupTitleText)
                            viewModel.setIsSelectingForUpcomingRecord(true)
                            showSetupSheet = false
                            onNavigateToVoiceProfiles()
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("add_voice_from_setup_modal_button"),
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            containerColor = themeColors.primary.copy(alpha = 0.08f),
                            contentColor = themeColors.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = themeColors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Новый голос",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.primary
                            )
                        }
                    }
                }

                if (profiles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "В базе еще нет голосов для автоматического разделения.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    // Optimized scrollable area for quick flowing chips
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                profiles.forEach { profile ->
                                    val isSelected = selectedUpcoming.contains(profile.name)
                                    val displayName = remember(profile.name) {
                                        profile.name.trim().split(" ").joinToString(" ") { word ->
                                            if (word.isNotEmpty()) {
                                                word.substring(0, 1).uppercase(java.util.Locale("ru")) + 
                                                word.substring(1).lowercase(java.util.Locale("ru"))
                                            } else ""
                                        }
                                    }
                                    val initial = remember(displayName) {
                                        if (displayName.isNotBlank()) {
                                            displayName.substring(0, 1).uppercase(java.util.Locale("ru"))
                                        } else {
                                            "?"
                                        }
                                    }

                                    // Distinct, beautiful pastel theme colors for initial-badge chips
                                    val avatarBgColors = listOf(
                                        Color(0xFFFFECEF) to Color(0xFFE11D48), // Rose
                                        Color(0xFFF0FDF4) to Color(0xFF16A34A), // Green
                                        Color(0xFFEFF6FF) to Color(0xFF2563EB), // Blue
                                        Color(0xFFF9F5FF) to Color(0xFF7C3AED), // Purple
                                        Color(0xFFF0FDFB) to Color(0xFF00AAA6), // Teal
                                        Color(0xFFFEF7EA) to Color(0xFFD97706)  // Amber
                                    )

                                    val colorPair = remember(profile.name) {
                                        val index = Math.abs(profile.name.hashCode()) % avatarBgColors.size
                                        avatarBgColors[index]
                                    }

                                    // Interactive speaker chip/button
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) themeColors.primary.copy(alpha = 0.08f) else Color.White)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) themeColors.primary else Color(0xFFCBD5E1).copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.toggleUpcomingParticipant(profile.name) }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        // Indicator: mini check or colored text avatar
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(themeColors.primary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(colorPair.first, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initial,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = colorPair.second
                                                )
                                            }
                                        }

                                        Text(
                                            text = displayName,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) themeColors.primary else Color(0xFF334155),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                        text = "Внимание: Нажимая кнопку запуска записи, вы подтверждаете согласие всех участников встречи на ведение аудиозаписи.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        lineHeight = 15.sp
                    )
                }

                // Improved Button Proportions for Bottom Navigation Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button is now a proper secondary filled button, balanced and neat
                    Button(
                        onClick = {
                            showSetupSheet = false
                            viewModel.setIsSelectingForUpcomingRecord(false)
                            viewModel.resetUpcomingParticipants()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Отмена",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // Primary active action takes higher proportional weight
                    Button(
                        onClick = {
                            // Setup ViewModel fields
                            viewModel.setMeetingTitle(setupTitleText)
                            val list = selectedUpcoming.toList()
                            if (list.size >= 1) viewModel.setParticipantA(list[0]) else viewModel.setParticipantA("")
                            if (list.size >= 2) viewModel.setParticipantB(list[1]) else viewModel.setParticipantB("")
                            if (list.size > 2) {
                                viewModel.setAdditionalParticipants(list.drop(2))
                            } else {
                                viewModel.setAdditionalParticipants(emptyList())
                            }

                            // Close dialog stage and start recording
                            viewModel.setIsSelectingForUpcomingRecord(false)
                            viewModel.resetPremiumLimitReached()
                            if (viewModel.startRecording()) {
                                showSetupSheet = false
                            } else {
                                Toast.makeText(context, "Достигнут лимит на запись на бесплатной версии!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Начать запись",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
