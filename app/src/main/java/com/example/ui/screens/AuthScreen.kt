package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.data.local.SessionManager
import com.example.util.BiometricHelper
import kotlinx.coroutines.launch

enum class AuthState {
    WELCOME,
    CREATE_ACCOUNT,
    CONFIRM_PHONE,
    CREATE_PASSCODE,
    LOGIN_FORM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    sessionManager: SessionManager,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = false

    // Royal Blue palette matching the mockup exactly
    val royalBlue = Color(0xFF2E52FF)
    val inactiveGrey = Color(0xFFDCDFEA)
    val borderGrey = Color(0xFFE2E8F0)
    val darkSlate = Color(0xFF0F172A)
    val bodySlate = Color(0xFF64748B)

    // Current screen state in our redesign flow
    var currentScreen by remember { mutableStateOf(AuthState.WELCOME) }

    // User inputs
    var phoneNumber by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var inputCode by remember { mutableStateOf("") } // 6 digits for code
    var inputPasscode by remember { mutableStateOf("") } // 4 digits for passcode
    
    // Traditional login inputs
    var loginEmailOrPhone by remember { mutableStateOf("") }
    var loginPasswordText by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Popup visibility
    var showVerifyPopup by remember { mutableStateOf(false) }

    // UI helpers
    var isLoading by remember { mutableStateOf(false) }
    var showGoogleBottomSheet by remember { mutableStateOf(false) }
    var showBiometricEnrollDialog by remember { mutableStateOf(false) }
    var pendingEmailForBiometrics by remember { mutableStateOf("") }

    val googleAccounts = listOf(
        "komilsay777@gmail.com",
        "work.say777@gmail.com"
    )
    val sheetState = rememberModalBottomSheetState()

    // Complete Auth action helper
    val completeAuthenticationFlow = { authenticatedEmail: String ->
        sessionManager.isLoggedIn = true
        sessionManager.loggedInEmail = authenticatedEmail

        val canBiometric = BiometricHelper.isBiometricAvailable(context)
        if (canBiometric && !sessionManager.isBiometricEnabled) {
            pendingEmailForBiometrics = authenticatedEmail
            showBiometricEnrollDialog = true
        } else {
            onAuthSuccess()
        }
    }

    // Google bottom sheet
    if (showGoogleBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGoogleBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF1F8))
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFF44474E)
                        )
                    }
                    Column {
                        Text(
                            text = "Вход через Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkSlate
                        )
                        Text(
                            text = "Выберите аккаунт для продолжения в Deals Recorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = bodySlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                googleAccounts.forEach { googleEmail ->
                    Card(
                        onClick = {
                            coroutineScope.launch {
                                showGoogleBottomSheet = false
                                isLoading = true
                                kotlinx.coroutines.delay(1000)
                                isLoading = false
                                completeAuthenticationFlow(googleEmail)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderGrey),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(royalBlue.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = royalBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = googleEmail,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = darkSlate
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = bodySlate.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Biometric dialog enrollment
    if (showBiometricEnrollDialog) {
        AlertDialog(
            onDismissRequest = {
                showBiometricEnrollDialog = false
                onAuthSuccess()
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = royalBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Вход по биометрии",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = darkSlate
                    )
                }
            },
            text = {
                Text(
                    text = "Хотите использовать биометрию для быстрого входа при следующем запуске Deals Recorder?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodySlate
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activity = BiometricHelper.findFragmentActivity(context)
                        if (activity != null) {
                            BiometricHelper.showBiometricPrompt(
                                activity = activity,
                                title = "Подтверждение биометрии",
                                subtitle = "Отсканируйте биометрию для привязки профиля",
                                onSuccess = {
                                    sessionManager.isBiometricEnabled = true
                                    showBiometricEnrollDialog = false
                                    Toast.makeText(context, "Успешно настроено!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                },
                                onError = { error ->
                                    if (error != "Canceled") {
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }
                                    showBiometricEnrollDialog = false
                                    onAuthSuccess()
                                }
                            )
                        } else {
                            showBiometricEnrollDialog = false
                            onAuthSuccess()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = royalBlue),
                    shape = RoundedCornerShape(100)
                ) {
                    Text("Включить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBiometricEnrollDialog = false
                        onAuthSuccess()
                    }
                ) {
                    Text("Позже", color = bodySlate)
                }
            }
        )
    }

    // Verification popup overlay (Design 4)
    if (showVerifyPopup) {
        Dialog(onDismissRequest = { showVerifyPopup = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button upper right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { showVerifyPopup = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = bodySlate
                            )
                        }
                    }

                    // Vector sketch inside dialog (Blue circle + letter envelope + check)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Soft blue circular backdrop
                            drawCircle(color = royalBlue.copy(alpha = 0.1f), radius = w / 2.2f)
                            
                            // Envelope and checkmark outlines
                            val path = Path().apply {
                                moveTo(w * 0.25f, h * 0.35f)
                                lineTo(w * 0.75f, h * 0.35f)
                                lineTo(w * 0.75f, h * 0.7f)
                                lineTo(w * 0.25f, h * 0.7f)
                                close()
                            }
                            drawPath(path = path, color = darkSlate, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                            
                            // Envelope fold line
                            val foldPath = Path().apply {
                                moveTo(w * 0.25f, h * 0.35f)
                                lineTo(w * 0.5f, h * 0.53f)
                                lineTo(w * 0.75f, h * 0.35f)
                            }
                            drawPath(path = foldPath, color = darkSlate, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                            // Check icon overlay
                            drawCircle(color = Color.White, radius = 12.dp.toPx(), center = Offset(w * 0.72f, h * 0.3f))
                            drawCircle(color = royalBlue, radius = 10.dp.toPx(), center = Offset(w * 0.72f, h * 0.3f), style = Stroke(width = 2.dp.toPx()))
                            val checkPath = Path().apply {
                                moveTo(w * 0.68f, h * 0.3f)
                                lineTo(w * 0.71f, h * 0.33f)
                                lineTo(w * 0.76f, h * 0.26f)
                            }
                            drawPath(path = checkPath, color = royalBlue, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                            // Sparkle dots around
                            drawCircle(color = royalBlue, radius = 3.dp.toPx(), center = Offset(w * 0.15f, h * 0.25f))
                            drawCircle(color = Color(0xFFFBBF24), radius = 2.dp.toPx(), center = Offset(w * 0.85f, h * 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Verify your phone number before we send code",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = darkSlate
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Is this correct? $phoneNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodySlate,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showVerifyPopup = false
                            currentScreen = AuthState.CONFIRM_PHONE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = royalBlue),
                        shape = RoundedCornerShape(100),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Yes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showVerifyPopup = false },
                        border = androidx.compose.foundation.BorderStroke(1.dp, royalBlue),
                        shape = RoundedCornerShape(100),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "No",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = royalBlue
                        )
                    }
                }
            }
        }
    }

    // Main layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = royalBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Подождите, выполняется вход...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodySlate
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top control bar with back navigation arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentScreen != AuthState.WELCOME) {
                        IconButton(
                            onClick = {
                                currentScreen = when (currentScreen) {
                                    AuthState.CREATE_ACCOUNT -> AuthState.WELCOME
                                    AuthState.CONFIRM_PHONE -> AuthState.CREATE_ACCOUNT
                                    AuthState.CREATE_PASSCODE -> AuthState.CONFIRM_PHONE
                                    AuthState.LOGIN_FORM -> AuthState.WELCOME
                                    else -> AuthState.WELCOME
                                }
                            },
                            modifier = Modifier.testTag("auth_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = darkSlate
                            )
                        }
                    } else {
                        // Decorative invisible spacer to keep alignment stable
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // Smooth animated content transitions on registration state machine
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (currentScreen) {
                        AuthState.WELCOME -> {
                            WelcomeScreenLayout(
                                onSignUpClick = { currentScreen = AuthState.CREATE_ACCOUNT },
                                onLoginClick = { currentScreen = AuthState.LOGIN_FORM },
                                royalBlue = royalBlue,
                                darkSlate = darkSlate,
                                bodySlate = bodySlate
                            )
                        }
                        AuthState.CREATE_ACCOUNT -> {
                            CreateAccountLayout(
                                phoneNumber = phoneNumber,
                                onPhoneChange = { phoneNumber = it },
                                passwordText = passwordText,
                                onPasswordChange = { passwordText = it },
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                onSignUpSubmit = { showVerifyPopup = true },
                                royalBlue = royalBlue,
                                darkSlate = darkSlate,
                                bodySlate = bodySlate,
                                borderGrey = borderGrey
                            )
                        }
                        AuthState.CONFIRM_PHONE -> {
                            ConfirmPhoneLayout(
                                destinationPhone = phoneNumber,
                                inputCode = inputCode,
                                onCodeChange = { inputCode = it },
                                onVerifySubmit = { currentScreen = AuthState.CREATE_PASSCODE },
                                royalBlue = royalBlue,
                                darkSlate = darkSlate,
                                bodySlate = bodySlate,
                                inactiveGrey = inactiveGrey
                            )
                        }
                        AuthState.CREATE_PASSCODE -> {
                            CreatePasscodeLayout(
                                typedPasscode = inputPasscode,
                                onPasscodeChange = { inputPasscode = it },
                                onPasscodeFinish = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        kotlinx.coroutines.delay(1000)
                                        isLoading = false
                                        completeAuthenticationFlow(phoneNumber.ifBlank { "komilsay777@gmail.com" })
                                    }
                                },
                                darkSlate = darkSlate,
                                bodySlate = bodySlate,
                                royalBlue = royalBlue,
                                inactiveGrey = inactiveGrey
                            )
                        }
                        AuthState.LOGIN_FORM -> {
                            LoginFormLayout(
                                emailOrPhone = loginEmailOrPhone,
                                onEmailOrPhoneChange = { loginEmailOrPhone = it },
                                passwordText = loginPasswordText,
                                onPasswordChange = { loginPasswordText = it },
                                passwordVisible = loginPasswordVisible,
                                onTogglePassword = { loginPasswordVisible = !loginPasswordVisible },
                                onSubmit = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        kotlinx.coroutines.delay(1000)
                                        isLoading = false
                                        completeAuthenticationFlow(loginEmailOrPhone.ifBlank { "komilsay777@gmail.com" })
                                    }
                                },
                                onGoogleClick = { showGoogleBottomSheet = true },
                                royalBlue = royalBlue,
                                darkSlate = darkSlate,
                                bodySlate = bodySlate,
                                borderGrey = borderGrey
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreenLayout(
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
    royalBlue: Color,
    darkSlate: Color,
    bodySlate: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            WelcomeArtIllustration()
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Create your\nDeals Recorder account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    lineHeight = 35.sp
                ),
                fontWeight = FontWeight.W800,
                textAlign = TextAlign.Center,
                color = darkSlate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Deals Recorder is a powerful tool that allows you to easily record, transcribe, and keep secure logs of all your transactions and meetings.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                ),
                textAlign = TextAlign.Center,
                color = bodySlate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            // Sign up fully rounded heavy blue button
            Button(
                onClick = onSignUpClick,
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(containerColor = royalBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_signup_button")
            ) {
                Text(
                    text = "Sign up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Log in fully rounded outlined button
            OutlinedButton(
                onClick = onLoginClick,
                shape = RoundedCornerShape(100),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, royalBlue),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_login_button")
            ) {
                Text(
                    text = "Log in",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = royalBlue
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Attentive policy terms at direct bottom
            Text(
                text = "By continuing you accept our\nTerms of Service and Privacy Policy",
                style = MaterialTheme.typography.labelSmall.copy(
                    lineHeight = 16.sp
                ),
                textAlign = TextAlign.Center,
                color = bodySlate.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
fun WelcomeArtIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_art_anim")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "art_bounce"
    )

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .offset(y = bounceOffset.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val thinStroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        val blackColor = Color(0xFF1E293B)
        val royalBlue = Color(0xFF2E52FF)
        val goldColor = Color(0xFFFBBF24)

        // 1. Draw nested cascading credit cards
        // Card leftmost (farthest behind)
        val cardLeft = Path().apply {
            moveTo(cx - 75.dp.toPx(), cy - 40.dp.toPx())
            lineTo(cx - 15.dp.toPx(), cy - 55.dp.toPx())
            lineTo(cx, cy - 10.dp.toPx())
            lineTo(cx - 60.dp.toPx(), cy + 5.dp.toPx())
            close()
        }
        drawPath(path = cardLeft, color = Color.White)
        drawPath(path = cardLeft, color = blackColor.copy(alpha = 0.3f), style = thinStroke)

        // Card centered-back
        val cardMid = Path().apply {
            moveTo(cx - 50.dp.toPx(), cy - 20.dp.toPx())
            lineTo(cx + 15.dp.toPx(), cy - 35.dp.toPx())
            lineTo(cx + 30.dp.toPx(), cy + 10.dp.toPx())
            lineTo(cx - 35.dp.toPx(), cy + 25.dp.toPx())
            close()
        }
        drawPath(path = cardMid, color = Color.White)
        drawPath(path = cardMid, color = blackColor.copy(alpha = 0.2f), style = Stroke(width = 3.dp.toPx()))
        drawPath(path = cardMid, color = blackColor, style = thinStroke)

        // Card front (prominent, with blue strip details)
        val cardFront = Path().apply {
            moveTo(cx - 20.dp.toPx(), cy - 5.dp.toPx())
            lineTo(cx + 50.dp.toPx(), cy - 22.dp.toPx())
            lineTo(cx + 65.dp.toPx(), cy + 20.dp.toPx())
            lineTo(cx - 5.dp.toPx(), cy + 37.dp.toPx())
            close()
        }
        drawPath(path = cardFront, color = Color.White)
        drawPath(path = cardFront, color = blackColor, style = thinStroke)
        // Draw blue chip or stripe on cardFront
        val cardStripe = Path().apply {
            moveTo(cx + 10.dp.toPx(), cy + 4.dp.toPx())
            lineTo(cx + 45.dp.toPx(), cy - 4.dp.toPx())
        }
        drawPath(path = cardStripe, color = royalBlue, style = Stroke(width = 4.dp.toPx()))

        // 2. Dashboard visual badge overlap (Lower Right of illustration)
        val dashW = 48.dp.toPx()
        val dashH = 34.dp.toPx()
        val dashX = cx + 22.dp.toPx()
        val dashY = cy + 12.dp.toPx()
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(dashX, dashY),
            size = Size(dashW, dashH),
            cornerRadius = CornerRadius(5.dp.toPx()),
            style = thinStroke
        )
        // Blue user silhouette inside badge
        drawCircle(
            color = royalBlue.copy(alpha = 0.15f),
            radius = 6.dp.toPx(),
            center = Offset(dashX + dashW / 2f, dashY + 12.dp.toPx())
        )
        drawCircle(
            color = royalBlue,
            radius = 5.dp.toPx(),
            center = Offset(dashX + dashW / 2f, dashY + 12.dp.toPx()),
            style = thinStroke
        )
        drawArc(
            color = royalBlue,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(dashX + dashW / 2f - 7.dp.toPx(), dashY + 16.dp.toPx()),
            size = Size(14.dp.toPx(), 8.dp.toPx()),
            style = thinStroke
        )
        // Little secure indicator dots under user silhouette
        repeat(5) { i ->
            drawCircle(
                color = blackColor.copy(alpha = 0.6f),
                radius = 1.25.dp.toPx(),
                center = Offset(dashX + 11.dp.toPx() + (i * 6.dp.toPx()), dashY + 28.dp.toPx())
            )
        }

        // 3. Round transaction coins stacking (Lower Left)
        val coin1X = cx - 55.dp.toPx()
        val coin1Y = cy + 25.dp.toPx()
        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(coin1X, coin1Y))
        drawCircle(color = blackColor, radius = 10.dp.toPx(), center = Offset(coin1X, coin1Y), style = thinStroke)
        drawCircle(color = royalBlue, radius = 7.dp.toPx(), center = Offset(coin1X, coin1Y), style = thinStroke)

        val coin2X = cx - 68.dp.toPx()
        val coin2Y = cy + 12.dp.toPx()
        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(coin2X, coin2Y))
        drawCircle(color = blackColor, radius = 8.dp.toPx(), center = Offset(coin2X, coin2Y), style = thinStroke)

        val coin3X = cx - 74.dp.toPx()
        val coin3Y = cy - 2.dp.toPx()
        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(coin3X, coin3Y))
        drawCircle(color = blackColor, radius = 6.dp.toPx(), center = Offset(coin3X, coin3Y), style = thinStroke)

        // 4. Gold Sparkles floating beautifully around
        drawAuthSparkleStar(this, Offset(cx - 52.dp.toPx(), cy - 48.dp.toPx()), 5.dp.toPx(), goldColor)
        drawAuthSparkleStar(this, Offset(cx + 62.dp.toPx(), cy - 35.dp.toPx()), 6.dp.toPx(), goldColor)
        drawAuthSparkleStar(this, Offset(cx - 30.dp.toPx(), cy + 42.dp.toPx()), 4.dp.toPx(), goldColor)
    }
}

// Drawing helper helper for sparkles
fun drawAuthSparkleStar(drawScope: DrawScope, center: Offset, size: Float, color: Color) {
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
fun CreateAccountLayout(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    passwordText: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    onSignUpSubmit: () -> Unit,
    royalBlue: Color,
    darkSlate: Color,
    bodySlate: Color,
    borderGrey: Color
) {
    var countryCodeSelected by remember { mutableStateOf("+880") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Text(
                text = "Create an Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W800,
                color = darkSlate
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter your mobile number to verify your account",
                style = MaterialTheme.typography.bodyMedium,
                color = bodySlate
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            // PHONE Input Label
            Text(
                text = "Phone",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = darkSlate,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            // High Fidelity Custom Row container representing styled textfield with flag & dropdown + input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.2.dp, borderGrey, RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bangladesh flag / Select drop mock
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* mock choose code */ }
                ) {
                    // Small drawn Bangladesh flag icon matching mockup style
                    Canvas(modifier = Modifier.size(20.dp, 14.dp)) {
                        // Green block
                        drawRect(color = Color(0xFF006A4E))
                        // Red circle
                        drawCircle(color = Color(0xFFF42A41), radius = 4.5.dp.toPx(), center = Offset(size.width * 0.45f, size.height / 2f))
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = "$countryCodeSelected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = darkSlate
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбор страны",
                        tint = bodySlate,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Vertical separator column line
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .fillMaxHeight(0.5f)
                        .background(borderGrey)
                )

                // Phone number textfield
                TextField(
                    value = phoneNumber,
                    onValueChange = onPhoneChange,
                    placeholder = { Text("Mobile number", color = bodySlate.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = darkSlate,
                        unfocusedTextColor = darkSlate
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("signup_phone_input")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            // PASSWORD Label
            Text(
                text = "Password",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = darkSlate,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            // High Fidelity Password Input
            OutlinedTextField(
                value = passwordText,
                onValueChange = onPasswordChange,
                placeholder = { Text("•••••••••", color = bodySlate.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = bodySlate
                    )
                },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(imageVector = icon, contentDescription = null, tint = bodySlate)
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = royalBlue,
                    unfocusedBorderColor = borderGrey,
                    focusedTextColor = darkSlate,
                    unfocusedTextColor = darkSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_input")
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        item {
            // Button is enabled if all fields filled
            val isButtonEnabled = phoneNumber.isNotBlank() && passwordText.isNotBlank()
            
            Button(
                onClick = { if (isButtonEnabled) onSignUpSubmit() },
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isButtonEnabled) royalBlue else Color(0xFFC5C9D6),
                    disabledContainerColor = Color(0xFFC5C9D6)
                ),
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_signup_button")
            ) {
                Text(
                    text = "Sign up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ConfirmPhoneLayout(
    destinationPhone: String,
    inputCode: String,
    onCodeChange: (String) -> Unit,
    onVerifySubmit: () -> Unit,
    royalBlue: Color,
    darkSlate: Color,
    bodySlate: Color,
    inactiveGrey: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Text(
                text = "Confirm your phone",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W800,
                color = darkSlate
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "We send 6 digits code to ${destinationPhone.ifBlank { "+880 1995 86 74 06" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = bodySlate
            )
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            // Custom drawn 6 horizontal slots representing underlined inputs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val digitChar = if (index < inputCode.length) inputCode[index].toString() else ""
                    val isCurrentSlot = index == inputCode.length
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(44.dp)
                            .clickable {
                                // Request keyboard simply
                            }
                    ) {
                        Text(
                            text = digitChar,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = royalBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (isCurrentSlot) royalBlue else if (digitChar.isNotEmpty()) blackOrGreyColor(true, royalBlue) else inactiveGrey)
                        )
                    }
                }
            }

            // Hidden textfield for inputting standard keys to make it incredibly robust
            TextField(
                value = inputCode,
                onValueChange = { newVal ->
                    if (newVal.length <= 6 && newVal.all { it.isDigit() }) {
                        onCodeChange(newVal)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Transparent,
                    unfocusedTextColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .testTag("phone_code_hidden_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Resend link mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Didn't get a code? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = bodySlate
                )
                Text(
                    text = "Resend",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = royalBlue,
                    modifier = Modifier.clickable { /* resend flow triggered */ }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        item {
            val isVerifyEnabled = inputCode.length == 6
            
            Button(
                onClick = { if (isVerifyEnabled) onVerifySubmit() },
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVerifyEnabled) royalBlue else Color(0xFFC5C9D6),
                    disabledContainerColor = Color(0xFFC5C9D6)
                ),
                enabled = isVerifyEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_verify_button")
            ) {
                Text(
                    text = "Verify Your Number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

fun blackOrGreyColor(isFilled: Boolean, accent: Color): Color {
    return if (isFilled) Color(0xFF0F172A) else Color(0xFFDCDFEA)
}

@Composable
fun CreatePasscodeLayout(
    typedPasscode: String,
    onPasscodeChange: (String) -> Unit,
    onPasscodeFinish: () -> Unit,
    darkSlate: Color,
    bodySlate: Color,
    royalBlue: Color,
    inactiveGrey: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Create passcode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W800,
                color = darkSlate
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "This info needs to be accurate with your ID document.",
                style = MaterialTheme.typography.bodyMedium,
                color = bodySlate
            )
        }

        // Concentric bullets passcode verification indicator row (Design 6)
        Row(
            modifier = Modifier
                .width(160.dp)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val hasDigit = index < typedPasscode.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (hasDigit) darkSlate else inactiveGrey)
                )
            }
        }

        // Premium In-Screen Numeric Custom Keyboard Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: 1, 2, 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NumKeyButton(label = "1", sub = "", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("1", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                NumKeyButton(label = "2", sub = "ABC", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("2", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                NumKeyButton(label = "3", sub = "DEF", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("3", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
            }

            // Row 2: 4, 5, 6
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NumKeyButton(label = "4", sub = "GHI", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("4", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                NumKeyButton(label = "5", sub = "JKL", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("5", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                NumKeyButton(label = "6", sub = "MNO", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("6", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
            }

            // Row 3: 7, 8, 9
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NumKeyButton(label = "7", sub = "PQRS", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("7", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                NumKeyButton(label = "8", sub = "TUV", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("8", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                NumKeyButton(label = "9", sub = "WXYZ", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("9", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
            }

            // Row 4: Empty, 0, Back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) // Empty
                
                NumKeyButton(label = "0", sub = "", modifier = Modifier.weight(1f)) {
                    appendPasscodeDigit("0", typedPasscode, onPasscodeChange, onPasscodeFinish)
                }
                
                // Back key
                Surface(
                    onClick = {
                        if (typedPasscode.isNotEmpty()) {
                            onPasscodeChange(typedPasscode.dropLast(1))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Удалить",
                            tint = darkSlate,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

fun appendPasscodeDigit(
    digit: String,
    currentPass: String,
    onPasscodeChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    if (currentPass.length < 4) {
        val nextPass = currentPass + digit
        onPasscodeChange(nextPass)
        if (nextPass.length == 4) {
            onFinish()
        }
    }
}

@Composable
fun NumKeyButton(
    label: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        modifier = modifier.height(56.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun LoginFormLayout(
    emailOrPhone: String,
    onEmailOrPhoneChange: (String) -> Unit,
    passwordText: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    royalBlue: Color,
    darkSlate: Color,
    bodySlate: Color,
    borderGrey: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Text(
                text = "Log In",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.W800,
                color = darkSlate
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter your credentials to access your Deals Recorder logs",
                style = MaterialTheme.typography.bodyMedium,
                color = bodySlate
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            // PHONE/EMAIL Label
            Text(
                text = "Email / Phone",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = darkSlate,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = onEmailOrPhoneChange,
                placeholder = { Text("E.g. komilsay777@gmail.com", color = bodySlate.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = bodySlate
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = royalBlue,
                    unfocusedBorderColor = borderGrey,
                    focusedTextColor = darkSlate,
                    unfocusedTextColor = darkSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_username_input")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            // PASSWORD Label
            Text(
                text = "Password",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = darkSlate,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            OutlinedTextField(
                value = passwordText,
                onValueChange = onPasswordChange,
                placeholder = { Text("•••••••••", color = bodySlate.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = bodySlate
                    )
                },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = onTogglePassword) {
                        Icon(imageVector = icon, contentDescription = null, tint = bodySlate)
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = royalBlue,
                    unfocusedBorderColor = borderGrey,
                    focusedTextColor = darkSlate,
                    unfocusedTextColor = darkSlate
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        item {
            val isButtonEnabled = emailOrPhone.isNotBlank() && passwordText.isNotBlank()
            
            Button(
                onClick = { if (isButtonEnabled) onSubmit() },
                shape = RoundedCornerShape(100),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isButtonEnabled) royalBlue else Color(0xFFC5C9D6),
                    disabledContainerColor = Color(0xFFC5C9D6)
                ),
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_login_button")
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google auth button
            OutlinedButton(
                onClick = onGoogleClick,
                shape = RoundedCornerShape(100),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, borderGrey),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = royalBlue
                    )
                    Text(
                        text = "Sign In with Google",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = darkSlate
                    )
                }
            }
        }
    }
}
