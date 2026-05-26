package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("deals_recorder_prefs", Context.MODE_PRIVATE)

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var loggedInEmail: String?
        get() = prefs.getString("logged_in_email", null)
        set(value) = prefs.edit().putString("logged_in_email", value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var isProActive: Boolean
        get() = prefs.getBoolean("is_pro_active", false)
        set(value) = prefs.edit().putBoolean("is_pro_active", value).apply()

    fun clearSession() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("logged_in_email", null)
            .putBoolean("biometric_enabled", false)
            .apply()
    }
}
