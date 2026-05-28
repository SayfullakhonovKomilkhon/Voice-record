package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.SessionManager
import com.example.util.BiometricHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    sessionManager: SessionManager,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    
    val accentColor = if (isDark) Color(0xFFADC6FF) else Color(0xFF005FAC)
    val cardBg = if (isDark) Color(0xFF1F242F) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF2D323E) else Color(0xFFDDE2EA)

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showGoogleBottomSheet by remember { mutableStateOf(false) }
    
    // Biometric modal setup state
    var showBiometricEnrollDialog by remember { mutableStateOf(false) }
    var pendingEmailForBiometrics by remember { mutableStateOf("") }

    // Google accounts template
    val googleAccounts = listOf(
        "komilsay777@gmail.com",
        "work.say777@gmail.com"
    )

    val sheetState = rememberModalBottomSheetState()

    // Process success path & guide biometric prompt if supported
    val completeAuthenticationFlow = { authenticatedEmail: String ->
        sessionManager.isLoggedIn = true
        sessionManager.loggedInEmail = authenticatedEmail
        
        val canBiometric = BiometricHelper.isBiometricAvailable(context)
        if (canBiometric && !sessionManager.isBiometricEnabled) {
            // Offer user to set up prompt
            pendingEmailForBiometrics = authenticatedEmail
            showBiometricEnrollDialog = true
        } else {
            onAuthSuccess()
        }
    }

    if (showBiometricEnrollDialog) {
        AlertDialog(
            onDismissRequest = {
                showBiometricEnrollDialog = false
                onAuthSuccess()
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = cardBg,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Вход по отпечатку",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Text(
                    text = "Хотите использовать биометрию (отпечаток или Face ID) для быстрого входа при следующем запуске Deals Recorder?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                                subtitle = "Отсканируйте датчик биометрии, чтобы привязать профиль",
                                onSuccess = {
                                    sessionManager.isBiometricEnabled = true
                                    showBiometricEnrollDialog = false
                                    Toast.makeText(context, "Биометрия успешно настроена!", Toast.LENGTH_SHORT).show()
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
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
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
                    Text("Позже", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    if (showGoogleBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGoogleBottomSheet = false },
            sheetState = sheetState,
            containerColor = cardBg,
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
                    // Google logo placeholder
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF232A37) else Color(0xFFEFF1F8))
                    ) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else Color(0xFF44474E)
                        )
                    }
                    Column {
                        Text(
                            text = "Вход через Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Выберите аккаунт для продолжения в Deals Recorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // List Google Accounts
                googleAccounts.forEach { googleEmail ->
                    Card(
                        onClick = {
                            coroutineScope.launch {
                                showGoogleBottomSheet = false
                                isLoading = true
                                kotlinx.coroutines.delay(1000) // Realistic loading
                                isLoading = false
                                completeAuthenticationFlow(googleEmail)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        .background(accentColor.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = googleEmail,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Or create custom
                Card(
                    onClick = {
                        coroutineScope.launch {
                            showGoogleBottomSheet = false
                            // Pre-fill user inputs with a realistic template and allow quick signup
                            email = "komilsay777@gmail.com"
                            password = "password123"
                            isLoginMode = true
                            Toast.makeText(context, "Данные Google подставлены для быстрого входа!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Использовать другой аккаунт",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = accentColor)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Подождите, выполняется вход...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    // Title section
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = "Deals Recorder",
                        tint = accentColor,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isLoginMode) "Добро пожаловать" else "Создать аккаунт",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLoginMode) "Войдите, чтобы сохранять протоколы сделок в безопасности" else "Зарегистрируйтесь, чтобы получить доступ к умной расшифровке",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Input Form Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!isLoginMode) {
                                // Full name input for sign up
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Ваше имя") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Email input
                            TextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input")
                            )

                            // Password input
                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Пароль") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = icon, contentDescription = null)
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input")
                            )

                            if (!isLoginMode) {
                                // Confirm password input for sign up
                                TextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Повторите пароль") },
                                    leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Main Action Submit Button
                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "Имя пользователя и пароль не могут быть пустыми!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!isLoginMode && password != confirmPassword) {
                                        Toast.makeText(context, "Пароли не совпадают!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    coroutineScope.launch {
                                        isLoading = true
                                        kotlinx.coroutines.delay(1200) // realistic wait
                                        isLoading = false
                                        completeAuthenticationFlow(email)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("auth_submit_button")
                            ) {
                                Text(
                                    text = if (isLoginMode) "Войти" else "Зарегистрироваться",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Switch Toggle
                    Text(
                        text = if (isLoginMode) "Еще нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        modifier = Modifier
                            .clickable { isLoginMode = !isLoginMode }
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // OR divider
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                        Text(
                            text = " ИЛИ ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean Google authentication button
                    Button(
                        onClick = { showGoogleBottomSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF1E242F) else Color(0xFFEFF1F8),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = accentColor
                            )
                            Text(
                                text = "Войти через Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
