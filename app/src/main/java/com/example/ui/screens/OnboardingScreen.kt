package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
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
    
    // Exact colors matching the user-received mock mockup
    val royalBlue = Color(0xFF2E52FF)
    val inactiveGrey = Color(0xFFDCDFEA)
    val darkSlate = Color(0xFF0F172A)
    val bodySlate = Color(0xFF64748B)

    // White base backdrop matching the screen cells
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Top Layout with Title and Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deals Recorder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = royalBlue,
                letterSpacing = (-0.5).sp
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFinish() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Пропустить",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = bodySlate
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.05f))

        // Large high-fidelity illustration canvas area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.48f),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300)))
                },
                label = "onboarding_illustration"
            ) { slide ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (slide) {
                        0 -> SlideOneArt()
                        1 -> SlideTwoArt()
                        2 -> SlideThreeArt()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // EXACT MOCK ALIGNMENT: 3 pills page indicator sits ABOVE the headings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val isSelected = currentSlide == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 22.dp else 12.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) royalBlue else inactiveGrey)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Typography text content block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.32f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { w -> w / 2 }) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally(targetOffsetX = { w -> -w / 2 }) + fadeOut(animationSpec = tween(300)))
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
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 25.sp,
                            lineHeight = 31.sp
                        ),
                        fontWeight = FontWeight.W800,
                        textAlign = TextAlign.Center,
                        color = darkSlate,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = when (slide) {
                            0 -> "Включите запись во время встречи. ИИ расшифрует диалог, определит участников и создаст точный текстовый протокол вашей сделки."
                            1 -> "Наш интеллектуальный движок выделит главные тезисы, ключевые договоренности и готовое краткое резюме за считанные секунды."
                            else -> "Все договоренности автоматически преобразятся в пошаговый список задач с указанием ответственных лиц для идеального контроля."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = bodySlate,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Spacious royal blue action button centered at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Button(
                onClick = {
                    if (currentSlide < 2) {
                        currentSlide++
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = royalBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (currentSlide == 2) "Начать" else "Далее",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// Drawing helper for vector illustration sparkles/stars matching the mock doddles
private fun drawSparkleStar(drawScope: DrawScope, center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        quadraticTo(center.x, center.y, center.x + size, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + size)
        quadraticTo(center.x, center.y, center.x - size, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - size)
        close()
    }
    drawScope.drawPath(path = path, color = color)
}

@Composable
fun SlideOneArt() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide_1_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .offset(y = floatOffset.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val thinStroke = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
        val blackColor = Color(0xFF1E293B)
        val royalBlue = Color(0xFF2E52FF)
        val yellowGold = Color(0xFFFBBF24)

        // 1. Draw elegant, clean shield background wireframe outline
        val shieldPath = Path().apply {
            moveTo(centerX, centerY - 80.dp.toPx())
            quadraticTo(centerX + 60.dp.toPx(), centerY - 80.dp.toPx(), centerX + 70.dp.toPx(), centerY - 30.dp.toPx())
            quadraticTo(centerX + 70.dp.toPx(), centerY + 30.dp.toPx(), centerX, centerY + 80.dp.toPx())
            quadraticTo(centerX - 70.dp.toPx(), centerY + 30.dp.toPx(), centerX - 70.dp.toPx(), centerY - 30.dp.toPx())
            quadraticTo(centerX - 60.dp.toPx(), centerY - 80.dp.toPx(), centerX, centerY - 80.dp.toPx())
            close()
        }
        drawPath(path = shieldPath, color = blackColor.copy(alpha = 0.08f))
        drawPath(path = shieldPath, color = blackColor.copy(alpha = 0.4f), style = thinStroke)

        // 2. Artistic dynamic blue swoosh accent hugging the shield's lower right
        val accentSwoosh = Path().apply {
            moveTo(centerX + 30.dp.toPx(), centerY + 62.dp.toPx())
            quadraticTo(centerX + 75.dp.toPx(), centerY + 20.dp.toPx(), centerX + 72.dp.toPx(), centerY - 35.dp.toPx())
        }
        drawPath(path = accentSwoosh, color = royalBlue, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

        // 3. Verified checkmark circle badge in the top right (✓)
        val badgeX = centerX + 60.dp.toPx()
        val badgeY = centerY - 65.dp.toPx()
        drawCircle(
            color = Color.White,
            radius = 14.dp.toPx(),
            center = Offset(badgeX, badgeY)
        )
        drawCircle(
            color = royalBlue,
            radius = 12.dp.toPx(),
            center = Offset(badgeX, badgeY),
            style = Stroke(width = 2.dp.toPx())
        )
        // Checkmark itself inside the badge
        val badgeCheck = Path().apply {
            moveTo(badgeX - 5.dp.toPx(), badgeY)
            lineTo(badgeX - 1.dp.toPx(), badgeY + 4.dp.toPx())
            lineTo(badgeX + 5.dp.toPx(), badgeY - 4.dp.toPx())
        }
        drawPath(path = badgeCheck, color = royalBlue, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // 4. Handshake thin sketch lines inside the shield
        // Left hand arm
        drawLine(
            color = blackColor,
            start = Offset(centerX - 50.dp.toPx(), centerY - 5.dp.toPx()),
            end = Offset(centerX - 10.dp.toPx(), centerY + 5.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = blackColor,
            start = Offset(centerX - 46.dp.toPx(), centerY + 12.dp.toPx()),
            end = Offset(centerX - 12.dp.toPx(), centerY + 16.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Right hand arm
        drawLine(
            color = blackColor,
            start = Offset(centerX + 50.dp.toPx(), centerY - 8.dp.toPx()),
            end = Offset(centerX + 12.dp.toPx(), centerY + 3.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = blackColor,
            start = Offset(centerX + 44.dp.toPx(), centerY + 10.dp.toPx()),
            end = Offset(centerX + 14.dp.toPx(), centerY + 14.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Clasping hands sketch representation
        drawRoundRect(
            color = Color.LightGray.copy(alpha = 0.3f),
            topLeft = Offset(centerX - 14.dp.toPx(), centerY - 12.dp.toPx()),
            size = Size(28.dp.toPx(), 24.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = thinStroke
        )
        // Cute detailed handshake line inside
        val handCrossLines = Path().apply {
            moveTo(centerX - 8.dp.toPx(), centerY - 6.dp.toPx())
            lineTo(centerX + 8.dp.toPx(), centerY + 6.dp.toPx())
            moveTo(centerX - 6.dp.toPx(), centerY + 4.dp.toPx())
            lineTo(centerX + 4.dp.toPx(), centerY - 8.dp.toPx())
        }
        drawPath(path = handCrossLines, color = blackColor, style = thinStroke)

        // 5. Team silhouette icon outline above the hands inside
        drawCircle(
            color = blackColor,
            radius = 3.5.dp.toPx(),
            center = Offset(centerX, centerY - 45.dp.toPx()),
            style = thinStroke
        )
        drawArc(
            color = blackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - 7.dp.toPx(), centerY - 40.dp.toPx()),
            size = Size(14.dp.toPx(), 8.dp.toPx()),
            style = thinStroke
        )
        // Left side team silhouettes
        drawCircle(
            color = blackColor.copy(alpha = 0.5f),
            radius = 3.dp.toPx(),
            center = Offset(centerX - 12.dp.toPx(), centerY - 41.dp.toPx()),
            style = thinStroke
        )
        drawArc(
            color = blackColor.copy(alpha = 0.5f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - 18.dp.toPx(), centerY - 37.dp.toPx()),
            size = Size(12.dp.toPx(), 7.dp.toPx()),
            style = thinStroke
        )
        // Right side team silhouettes
        drawCircle(
            color = blackColor.copy(alpha = 0.5f),
            radius = 3.dp.toPx(),
            center = Offset(centerX + 12.dp.toPx(), centerY - 41.dp.toPx()),
            style = thinStroke
        )
        drawArc(
            color = blackColor.copy(alpha = 0.5f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX + 6.dp.toPx(), centerY - 37.dp.toPx()),
            size = Size(12.dp.toPx(), 7.dp.toPx()),
            style = thinStroke
        )

        // 6. Detailed thin coin lines at bottom-left ($ logo)
        drawCircle(
            color = blackColor,
            radius = 10.dp.toPx(),
            center = Offset(centerX - 54.dp.toPx(), centerY + 58.dp.toPx()),
            style = thinStroke
        )
        // Coin details
        drawCircle(
            color = royalBlue.copy(alpha = 0.3f),
            radius = 7.dp.toPx(),
            center = Offset(centerX - 54.dp.toPx(), centerY + 58.dp.toPx())
        )
        // Little dollar line
        drawLine(
            color = blackColor,
            start = Offset(centerX - 54.dp.toPx(), centerY + 53.dp.toPx()),
            end = Offset(centerX - 54.dp.toPx(), centerY + 63.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )

        // Secondary small coin overlapping
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = Offset(centerX - 41.dp.toPx(), centerY + 65.dp.toPx())
        )
        drawCircle(
            color = blackColor,
            radius = 7.dp.toPx(),
            center = Offset(centerX - 41.dp.toPx(), centerY + 65.dp.toPx()),
            style = thinStroke
        )

        // 7. Add sparkles matching the mockup
        drawSparkleStar(this, Offset(centerX - 65.dp.toPx(), centerY - 50.dp.toPx()), 6.dp.toPx(), yellowGold)
        drawSparkleStar(this, Offset(centerX + 60.dp.toPx(), centerY + 45.dp.toPx()), 5.dp.toPx(), yellowGold)
        drawSparkleStar(this, Offset(centerX - 35.dp.toPx(), centerY + 78.dp.toPx()), 4.dp.toPx(), yellowGold)
    }
}

@Composable
fun SlideTwoArt() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide_2_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .offset(y = floatOffset.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val thinStroke = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
        val blackColor = Color(0xFF1E293B)
        val royalBlue = Color(0xFF2E52FF)
        val yellowGold = Color(0xFFFBBF24)

        // 1. Draw a credit card sketch tilted to the left
        val cardPath = Path().apply {
            moveTo(centerX - 90.dp.toPx(), centerY - 25.dp.toPx())
            lineTo(centerX - 25.dp.toPx(), centerY - 45.dp.toPx())
            lineTo(centerX - 10.dp.toPx(), centerY + 5.dp.toPx())
            lineTo(centerX - 75.dp.toPx(), centerY + 25.dp.toPx())
            close()
        }
        drawPath(path = cardPath, color = Color.White)
        drawPath(path = cardPath, color = blackColor, style = thinStroke)

        // Credit card details (magnetic strip or simple chips in blue)
        val cardBlueStrip = Path().apply {
            moveTo(centerX - 85.dp.toPx(), centerY - 10.dp.toPx())
            lineTo(centerX - 20.dp.toPx(), centerY - 30.dp.toPx())
        }
        drawPath(path = cardBlueStrip, color = royalBlue, style = Stroke(width = 4.dp.toPx()))

        // Visa bullet circle details
        drawCircle(
            color = blackColor.copy(alpha = 0.5f),
            radius = 3.dp.toPx(),
            center = Offset(centerX - 32.dp.toPx(), centerY + 5.dp.toPx()),
            style = thinStroke
        )
        drawCircle(
            color = blackColor.copy(alpha = 0.5f),
            radius = 3.dp.toPx(),
            center = Offset(centerX - 24.dp.toPx(), centerY + 3.dp.toPx()),
            style = thinStroke
        )

        // 2. Draw a clean smartphone frame outline centered, slightly tilted to the right
        val phonePath = Path().apply {
            moveTo(centerX + 10.dp.toPx(), centerY - 65.dp.toPx())
            lineTo(centerX + 55.dp.toPx(), centerY - 55.dp.toPx())
            lineTo(centerX + 30.dp.toPx(), centerY + 55.dp.toPx())
            lineTo(centerX - 15.dp.toPx(), centerY + 45.dp.toPx())
            close()
        }
        drawPath(path = phonePath, color = Color.White)
        drawPath(path = phonePath, color = blackColor.copy(alpha = 0.1f))
        drawPath(path = phonePath, color = blackColor, style = thinStroke)

        // Blue phone status screen top indicator bar
        drawLine(
            color = royalBlue,
            start = Offset(centerX + 23.dp.toPx(), centerY - 58.dp.toPx()),
            end = Offset(centerX + 40.dp.toPx(), centerY - 54.dp.toPx()),
            strokeWidth = 3.dp.toPx()
        )

        // Smart document lines (sheets representing transcript notes protruding from smartphone)
        val paperPath1 = Path().apply {
            moveTo(centerX + 25.dp.toPx(), centerY - 40.dp.toPx())
            lineTo(centerX + 48.dp.toPx(), centerY - 35.dp.toPx())
            lineTo(centerX + 42.dp.toPx(), centerY - 10.dp.toPx())
            lineTo(centerX + 19.dp.toPx(), centerY - 15.dp.toPx())
            close()
        }
        drawPath(path = paperPath1, color = Color.White)
        drawPath(path = paperPath1, color = blackColor, style = thinStroke)
        // Mini checklist lines on sheet
        drawLine(color = royalBlue, start = Offset(centerX + 24.dp.toPx(), centerY - 33.dp.toPx()), end = Offset(centerX + 42.dp.toPx(), centerY - 29.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        drawLine(color = blackColor.copy(alpha = 0.6f), start = Offset(centerX + 22.dp.toPx(), centerY - 25.dp.toPx()), end = Offset(centerX + 38.dp.toPx(), centerY - 21.dp.toPx()), strokeWidth = 1.5.dp.toPx())

        // Overlapping paper sheet 2 lower down
        val paperPath2 = Path().apply {
            moveTo(centerX + 15.dp.toPx(), centerY - 5.dp.toPx())
            lineTo(centerX + 38.dp.toPx(), centerY - 10.dp.toPx())
            lineTo(centerX + 32.dp.toPx(), centerY + 25.dp.toPx())
            lineTo(centerX + 9.dp.toPx(), centerY + 30.dp.toPx())
            close()
        }
        drawPath(path = paperPath2, color = Color.White)
        drawPath(path = paperPath2, color = blackColor, style = thinStroke)
        drawLine(color = royalBlue, start = Offset(centerX + 14.dp.toPx(), centerY), end = Offset(centerX + 32.dp.toPx(), centerY - 4.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        drawLine(color = blackColor.copy(alpha = 0.6f), start = Offset(centerX + 12.dp.toPx(), centerY + 10.dp.toPx()), end = Offset(centerX + 28.dp.toPx(), centerY + 6.dp.toPx()), strokeWidth = 1.5.dp.toPx())

        // 3. Draw magnifying glass on top of credit card and phone, centered-low
        val magGlassX = centerX - 12.dp.toPx()
        val magGlassY = centerY + 20.dp.toPx()
        val magRadius = 16.dp.toPx()

        // Handle outline in black, filled with yellow gold
        val handlePath = Path().apply {
            moveTo(magGlassX - 10.dp.toPx(), magGlassY + 10.dp.toPx())
            lineTo(magGlassX - 25.dp.toPx(), magGlassY + 28.dp.toPx())
            lineTo(magGlassX - 31.dp.toPx(), magGlassY + 23.dp.toPx())
            lineTo(magGlassX - 16.dp.toPx(), magGlassY + 5.dp.toPx())
            close()
        }
        drawPath(path = handlePath, color = yellowGold)
        drawPath(path = handlePath, color = blackColor, style = thinStroke)

        // Glass circle
        drawCircle(
            color = Color.White,
            radius = magRadius,
            center = Offset(magGlassX, magGlassY)
        )
        // Soft blue lens reflection splash
        drawCircle(
            color = royalBlue.copy(alpha = 0.12f),
            radius = magRadius,
            center = Offset(magGlassX, magGlassY)
        )
        // Perfect stroke matching line-art mockup
        drawCircle(
            color = royalBlue,
            radius = magRadius,
            center = Offset(magGlassX, magGlassY),
            style = Stroke(width = 3.dp.toPx())
        )
        // Magnifying glass lens arc gloss
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(magGlassX - 11.dp.toPx(), magGlassY - 11.dp.toPx()),
            size = Size(22.dp.toPx(), 22.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // 4. Beautiful floating skyline structures in light grey wireframe in background
        drawLine(
            color = blackColor.copy(alpha = 0.2f),
            start = Offset(centerX - 42.dp.toPx(), centerY - 32.dp.toPx()),
            end = Offset(centerX - 42.dp.toPx(), centerY - 80.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = blackColor.copy(alpha = 0.2f),
            start = Offset(centerX - 42.dp.toPx(), centerY - 80.dp.toPx()),
            end = Offset(centerX - 12.dp.toPx(), centerY - 80.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = blackColor.copy(alpha = 0.2f),
            start = Offset(centerX - 12.dp.toPx(), centerY - 80.dp.toPx()),
            end = Offset(centerX - 12.dp.toPx(), centerY - 45.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
        // Overlapping cloud or dome draft
        drawArc(
            color = blackColor.copy(alpha = 0.15f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - 24.dp.toPx(), centerY - 65.dp.toPx()),
            size = Size(36.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 5. High-fidelity sparkles
        drawSparkleStar(this, Offset(centerX + 62.dp.toPx(), centerY - 35.dp.toPx()), 5.dp.toPx(), yellowGold)
        drawSparkleStar(this, Offset(centerX - 55.dp.toPx(), centerY - 72.dp.toPx()), 6.dp.toPx(), yellowGold)
    }
}

@Composable
fun SlideThreeArt() {
    val infiniteTransition = rememberInfiniteTransition(label = "slide_3_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .offset(y = floatOffset.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val thinStroke = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round)
        val blackColor = Color(0xFF1E293B)
        val royalBlue = Color(0xFF2E52FF)
        val yellowGold = Color(0xFFFBBF24)

        // 1. Draw Globe latitude/longitude concentric grid lines in background
        drawCircle(
            color = blackColor.copy(alpha = 0.08f),
            radius = 65.dp.toPx(),
            center = Offset(centerX, centerY - 15.dp.toPx())
        )
        drawCircle(
            color = blackColor.copy(alpha = 0.25f),
            radius = 65.dp.toPx(),
            center = Offset(centerX, centerY - 15.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
        )
        // Globe lines
        drawArc(
            color = blackColor.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(centerX - 35.dp.toPx(), centerY - 80.dp.toPx()),
            size = Size(70.dp.toPx(), 130.dp.toPx()),
            style = thinStroke
        )

        // 2. Draw Greek classical Bank pillar building in the upper left
        val bankX = centerX - 65.dp.toPx()
        val bankY = centerY - 55.dp.toPx()
        // Triangle pediment roof (blue filled accent)
        val roofPath = Path().apply {
            moveTo(bankX, bankY)
            lineTo(bankX + 22.dp.toPx(), bankY - 14.dp.toPx())
            lineTo(bankX + 44.dp.toPx(), bankY)
            close()
        }
        drawPath(path = roofPath, color = royalBlue)
        drawPath(path = roofPath, color = blackColor, style = thinStroke)
        // Foundation steps
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bankX - 4.dp.toPx(), bankY + 28.dp.toPx()),
            size = Size(52.dp.toPx(), 6.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = thinStroke
        )
        // Pillars (4 columns)
        repeat(4) { i ->
            val colX = bankX + 4.dp.toPx() + (i * 11.dp.toPx())
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(colX, bankY + 4.dp.toPx()),
                size = Size(4.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx()),
                style = thinStroke
            )
            drawLine(
                color = royalBlue,
                start = Offset(colX + 2.dp.toPx(), bankY + 6.dp.toPx()),
                end = Offset(colX + 2.dp.toPx(), bankY + 22.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // 3. Draw modern upright Smartphone layout on the right
        val phoneX = centerX + 15.dp.toPx()
        val phoneY = centerY - 35.dp.toPx()
        val phoneW = 50.dp.toPx()
        val phoneH = 92.dp.toPx()

        val phoneRect = Path().apply {
            moveTo(phoneX, phoneY)
            lineTo(phoneX + phoneW, phoneY - 8.dp.toPx())
            lineTo(phoneX + phoneW - 12.dp.toPx(), phoneY + phoneH)
            lineTo(phoneX - 12.dp.toPx(), phoneY + phoneH + 8.dp.toPx())
            close()
        }
        drawPath(path = phoneRect, color = Color.White)
        drawPath(path = phoneRect, color = blackColor, style = thinStroke)

        // Royal blue rounded checklist card mockup inside phone screen
        val checkCardX = phoneX + 8.dp.toPx()
        val checkCardY = phoneY + 22.dp.toPx()
        drawRoundRect(
            color = royalBlue.copy(alpha = 0.1f),
            topLeft = Offset(checkCardX, checkCardY),
            size = Size(28.dp.toPx(), 36.dp.toPx()),
            cornerRadius = CornerRadius(5.dp.toPx())
        )
        // Bold yellow checking mark mockup box inside
        drawRoundRect(
            color = yellowGold,
            topLeft = Offset(checkCardX + 8.dp.toPx(), checkCardY + 12.dp.toPx()),
            size = Size(12.dp.toPx(), 12.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx())
        )
        // Checkmark inside yellow gold box
        val innerCheck = Path().apply {
            moveTo(checkCardX + 11.dp.toPx(), checkCardY + 18.dp.toPx())
            lineTo(checkCardX + 14.dp.toPx(), checkCardY + 21.dp.toPx())
            lineTo(checkCardX + 17.dp.toPx(), checkCardY + 15.dp.toPx())
        }
        drawPath(path = innerCheck, color = Color.White, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // 4. Blue Security Lock Shield floating beside the Phone on Left
        val lockX = centerX - 18.dp.toPx()
        val lockY = centerY + 10.dp.toPx()
        val lockShield = Path().apply {
            moveTo(lockX, lockY - 14.dp.toPx())
            lineTo(lockX + 10.dp.toPx(), lockY - 18.dp.toPx())
            lineTo(lockX + 20.dp.toPx(), lockY - 14.dp.toPx())
            lineTo(lockX + 20.dp.toPx(), lockY + 2.dp.toPx())
            quadraticTo(lockX + 20.dp.toPx(), lockY + 12.dp.toPx(), lockX + 10.dp.toPx(), lockY + 18.dp.toPx())
            quadraticTo(lockX, lockY + 12.dp.toPx(), lockX, lockY + 2.dp.toPx())
            close()
        }
        drawPath(path = lockShield, color = Color.White)
        drawPath(path = lockShield, color = royalBlue.copy(alpha = 0.15f))
        drawPath(path = lockShield, color = royalBlue, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        // Mini keyhole inside lock
        drawCircle(
            color = royalBlue,
            radius = 2.5.dp.toPx(),
            center = Offset(lockX + 10.dp.toPx(), lockY - 1.dp.toPx())
        )
        drawLine(
            color = royalBlue,
            start = Offset(lockX + 10.dp.toPx(), lockY),
            end = Offset(lockX + 10.dp.toPx(), lockY + 6.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // 5. Add sparkly mockup stars
        drawSparkleStar(this, Offset(centerX + 64.dp.toPx(), centerY - 65.dp.toPx()), 6.dp.toPx(), yellowGold)
        drawSparkleStar(this, Offset(centerX - 68.dp.toPx(), centerY + 28.dp.toPx()), 5.dp.toPx(), yellowGold)
    }
}
