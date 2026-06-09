package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.SessionManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.util.BiometricHelper
import com.example.ui.screens.MeetingDetailScreen
import com.example.ui.screens.MeetingListScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.VoiceProfilesScreen
import com.example.ui.screens.CreateMeetingScreen
import com.example.ui.screens.RecordingScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MeetingViewModel
import com.example.data.firebase.FirebaseManager
import android.util.Log

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Establish an uncaught exception handler to safely capture and log any app crashes on launch
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("FATAL_LAUNCH_CRASH", "Uncaught exception on thread: ${thread.name}", throwable)
            oldHandler?.uncaughtException(thread, throwable)
        }

        // 2. Initialize safe Firebase managers/contexts prior to first composition state flow accesses
        try {
            FirebaseManager.initialize(applicationContext)
        } catch (e: Exception) {
            Log.e("MainActivity", "Caught startup exception during FirebaseManager initialize: ${e.localizedMessage}", e)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MeetingViewModel = viewModel()
                    val sessionManager = remember { SessionManager(applicationContext) }

                    val toastState by viewModel.toast.collectAsState()
                    val activeThemeName by viewModel.activeThemeColor.collectAsState()
                    val themeColors = remember(activeThemeName) { com.example.ui.theme.ThemeColorsProvider.getColors(activeThemeName) }

                    LaunchedEffect(toastState) {
                        if (toastState != null) {
                            delay(3000L)
                            viewModel.clearToast()
                        }
                    }

                    // Dynamically calculate our starting destination based on authentication/onboarding state
                    val startDest = remember {
                        if (!sessionManager.isOnboardingCompleted) {
                            "onboarding"
                        } else if (!sessionManager.isLoggedIn) {
                            "auth"
                        } else if (sessionManager.isBiometricEnabled && BiometricHelper.isBiometricAvailable(applicationContext)) {
                            "biometric_lock"
                        } else {
                            "list"
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDest
                        ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinish = {
                                    sessionManager.isOnboardingCompleted = true
                                    navController.navigate("auth") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("auth") {
                            AuthScreen(
                                sessionManager = sessionManager,
                                onAuthSuccess = {
                                    navController.navigate("list") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("biometric_lock") {
                            BiometricLockScreen(
                                sessionManager = sessionManager,
                                onAuthPassed = {
                                    navController.navigate("list") {
                                        popUpTo("biometric_lock") { inclusive = true }
                                    }
                                },
                                onResetAuth = {
                                    navController.navigate("auth") {
                                        popUpTo("biometric_lock") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("list") {
                            MeetingListScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                },
                                onNavigateToProcessing = { id ->
                                    navController.navigate("processing/$id")
                                },
                                onNavigateToVoiceProfiles = {
                                    navController.navigate("voice_profiles") {
                                        popUpTo("list") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToCreateMeeting = {
                                    navController.navigate("create_meeting")
                                },
                                onNavigateToRecording = {
                                    navController.navigate("recording")
                                },
                                onLogoutRequested = {
                                    sessionManager.clearSession()
                                    navController.navigate("auth") {
                                        popUpTo("list") { inclusive = true }
                                    }
                                },
                                onNavigateToSubscription = {
                                    navController.navigate("subscription")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings") {
                                        popUpTo("list") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable("create_meeting") {
                            CreateMeetingScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onStartRecording = {
                                    navController.navigate("recording") {
                                        popUpTo("create_meeting") { inclusive = true }
                                    }
                                },
                                onNavigateToSubscription = {
                                    navController.navigate("subscription")
                                }
                            )
                        }

                        composable("recording") {
                            RecordingScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToVoiceProfiles = {
                                    navController.navigate("voice_profiles")
                                },
                                onStopAndStartProcessing = { meetingId ->
                                    navController.navigate("processing/$meetingId") {
                                        popUpTo("recording") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "processing/{meetingId}",
                            arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: 0L
                            ProcessingScreen(
                                meetingId = meetingId,
                                viewModel = viewModel,
                                onNavigateToList = {
                                    navController.navigate("list") {
                                        popUpTo("list") { inclusive = true }
                                    }
                                },
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id") {
                                        popUpTo("processing/$meetingId") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("voice_profiles") {
                            VoiceProfilesScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToRecording = {
                                    navController.navigate("recording")
                                },
                                onNavigateToMeetingList = {
                                    navController.navigate("list") {
                                        popUpTo("list") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings") {
                                        popUpTo("list") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToSubscription = {
                                    navController.navigate("subscription")
                                }
                            )
                        }

                        composable(
                            route = "detail/{meetingId}",
                            arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: 0L
                            MeetingDetailScreen(
                                meetingId = meetingId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSubscription = {
                                    navController.navigate("subscription")
                                }
                            )
                        }

                        composable("subscription") {
                            SubscriptionScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToMeetingList = {
                                    navController.navigate("list") {
                                        popUpTo("list") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToVoiceProfiles = {
                                    navController.navigate("voice_profiles") {
                                        popUpTo("list") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToSubscription = {
                                    navController.navigate("subscription")
                                },
                                onNavigateToCreateMeeting = {
                                    navController.navigate("create_meeting")
                                },
                                onAccountDeleted = {
                                    navController.navigate("onboarding") {
                                        popUpTo("list") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    // Beautiful Unified Custom Toast overlay that floats on top of all screens
                    AnimatedVisibility(
                        visible = toastState != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 16.dp)
                            .testTag("custom_toast_container")
                    ) {
                        toastState?.let { activeToast ->
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(
                                    1.dp, 
                                    if (activeToast.isSuccess) themeColors.primary.copy(alpha = 0.3f) 
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .widthIn(max = 500.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (activeToast.isSuccess) themeColors.primary.copy(alpha = 0.12f) 
                                                else MaterialTheme.colorScheme.errorContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (activeToast.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (activeToast.isSuccess) themeColors.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = activeToast.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
