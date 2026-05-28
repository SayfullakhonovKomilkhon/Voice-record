package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }
    val isDark = isSystemInDarkTheme()
    
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // Top row with short indicator or skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deals Recorder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            
            TextButton(onClick = onFinish) {
                Text(
                    text = "Пропустить",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Center visual presentation area (Slide animation canvas stack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            contentAlignment = Alignment.Center
        ) {
            when (currentSlide) {
                0 -> SlideOneAnimation(accentColor = accentColor)
                1 -> SlideTwoAnimation(accentColor = accentColor)
                2 -> SlideThreeAnimation(accentColor = accentColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Onboarding Content details (text information)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                },
                label = "onboarding_text"
            ) { slide ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (slide) {
                            0 -> "Умная запись и расшифровка"
                            1 -> "Мгновенный ИИ-анализ"
                            else -> "Интерактивные чек-листы"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = when (slide) {
                            0 -> "Включите запись во время встречи. ИИ расшифрует диалог, определит участников и создаст точный текстовый протокол вашей сделки."
                            1 -> "Наш интеллектуальный движок выделит главные тезисы, ключевые договоренности и готовое краткое резюме за считанные секунды."
                            else -> "Все договоренности автоматически преобразятся в пошаговый список задач с указанием ответственных лиц для идеального контроля."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }

        // Navigation control block: slide indicators + next button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slide count dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isSelected = currentSlide == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.15f
                                )
                            )
                    )
                }
            }

            // High-End Proceed FAB
            Button(
                onClick = {
                    if (currentSlide < 2) {
                        currentSlide++
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentSlide == 2) accentColor else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (currentSlide == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentSlide == 2) "Начать" else "Далее",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (currentSlide == 2) Icons.Default.ArrowForward else Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun SlideOneAnimation(accentColor: Color) {
    var waveTick by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(40)
            waveTick = (waveTick + 0.05f) % 1f
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Ripple wave circles surrounding the center element
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = 45.dp.toPx()
            val maxRadius = 110.dp.toPx()
            
            val t1 = waveTick
            val t2 = (waveTick + 0.5f) % 1f
            
            val r1 = baseRadius + (maxRadius - baseRadius) * t1
            val alpha1 = 1f - t1
            
            val r2 = baseRadius + (maxRadius - baseRadius) * t2
            val alpha2 = 1f - t2

            drawCircle(
                color = accentColor.copy(alpha = alpha1 * 0.25f),
                radius = r1,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = accentColor.copy(alpha = alpha2 * 0.25f),
                radius = r2,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Center mic design representation
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(accentColor)
        ) {
            // Internal mic structure inside circle
            Canvas(modifier = Modifier.size(36.dp)) {
                val micWidth = 14.dp.toPx()
                val micHeight = 24.dp.toPx()
                val standY = 28.dp.toPx()
                
                // Capsule body
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset((size.width - micWidth)/2, 0f),
                    size = androidx.compose.ui.geometry.Size(micWidth, micHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(micWidth/2, micWidth/2)
                )

                // Loop stand
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset((size.width - 24.dp.toPx())/2, 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Vertical connector stand line
                drawLine(
                    color = Color.White,
                    start = Offset(size.width / 2, 26.dp.toPx()),
                    end = Offset(size.width / 2, standY),
                    strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun SlideTwoAnimation(accentColor: Color) {
    var pulseState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            pulseState = !pulseState
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (pulseState) 1.08f else 0.95f,
        animationSpec = androidx.compose.animation.core.tween(1000),
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // High-end smart neural ring visualization
        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            
            // Outer dashed orbit
            drawCircle(
                color = accentColor.copy(alpha = 0.15f),
                radius = 75.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )

            // Dynamic neural branches matching Professional Polish
            listOf(-45f, 45f, 135f, 225f).forEach { angle ->
                val r = 68.dp.toPx()
                val rad = Math.toRadians(angle.toDouble())
                val x = center.x + r * Math.cos(rad).toFloat()
                val y = center.y + r * Math.sin(rad).toFloat()
                
                // Connection node wires
                drawLine(
                    color = accentColor.copy(alpha = 0.4f),
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 2.dp.toPx()
                )

                // Small terminal lights/beads
                drawCircle(
                    color = accentColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Center Cognitive Spark container structure
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(accentColor.copy(alpha = 0.12f))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accentColor)
            ) {
                // Custom drawn spark/star outline style
                Canvas(modifier = Modifier.size(24.dp)) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerX, 0f)
                        quadraticTo(centerX, centerY, size.width, centerY)
                        quadraticTo(centerX, centerY, centerX, size.height)
                        quadraticTo(centerX, centerY, 0f, centerY)
                        quadraticTo(centerX, centerY, centerX, 0f)
                    }
                    drawPath(path = path, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SlideThreeAnimation(accentColor: Color) {
    var checkOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            checkOn = !checkOn
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Slide checklist layout
        Column(
            modifier = Modifier
                .width(180.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header simulated line
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
            )

            // Animated Task 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Checkbox replica
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (checkOn) accentColor else accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor, RoundedCornerShape(4.dp))
                ) {
                    if (checkOn) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, size.height * 0.45f)
                                lineTo(size.width * 0.35f, size.height * 0.8f)
                                lineTo(size.width, size.height * 0.15f)
                            }
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }
                }

                // Text line placeholder matching completion state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(if (checkOn) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )
            }

            // Task 2 Placeholder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )
            }

            // Task 3 Placeholder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )
            }
        }
    }
}
