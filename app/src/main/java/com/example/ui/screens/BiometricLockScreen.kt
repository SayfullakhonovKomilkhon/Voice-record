package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.SessionManager
import com.example.util.BiometricHelper

@Composable
fun BiometricLockScreen(
    sessionManager: SessionManager,
    onAuthPassed: () -> Unit,
    onResetAuth: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    val accentColor = if (isDark) Color(0xFFADC6FF) else Color(0xFF005FAC)
    
    val triggerBiometricVerification = {
        val activity = BiometricHelper.findFragmentActivity(context)
        if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Авторизация в Deals Recorder",
                subtitle = "Подтвердите личность, чтобы разблокировать записи",
                onSuccess = {
                    onAuthPassed()
                },
                onError = { error ->
                    if (error != "Canceled") {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            // Fallback in environments without host fragment or biometric support (e.g. Emulator)
            onAuthPassed()
        }
    }

    // Trigger auto prompt on screen entrance
    LaunchedEffect(Unit) {
        triggerBiometricVerification()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Deals Recorder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Mid security shield presentation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f))
                    .clickable { triggerBiometricVerification() }
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Разблокировать",
                    tint = accentColor,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Приложение заблокировано",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Используйте отпечаток пальца или PIN-код устройства для доступа к вашим записям встреч и умной сводке решений.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (sessionManager.loggedInEmail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Вошедший аккаунт: ${sessionManager.loggedInEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }
        }

        // Bottom alternative access mechanisms
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { triggerBiometricVerification() },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text("Разблокировать", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = {
                    sessionManager.clearSession()
                    onResetAuth()
                }
            ) {
                Text(
                    text = "Сменить аккаунт / Выйти",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
