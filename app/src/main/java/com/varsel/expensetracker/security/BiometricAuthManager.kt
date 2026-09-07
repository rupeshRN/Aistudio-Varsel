package com.varsel.expensetracker.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.varsel.expensetracker.data.preference.BiometricTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L
    private var isAppInBackground: Boolean = false
    private var isPromptActive: Boolean = false

    // Precision biometric security: strong biometrics with device credentials (PIN/pattern/password) fallback
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun lockOnAppLaunch(timeout: BiometricTimeout) {
        if (timeout != BiometricTimeout.OFF) {
            _isLocked.value = true
        } else {
            _isLocked.value = false
        }
    }

    fun checkLockOnResume(timeout: BiometricTimeout) {
        if (timeout == BiometricTimeout.OFF) {
            _isLocked.value = false
            isAppInBackground = false
            return
        }

        // Do not re-evaluate lock if resume was caused by the biometric dialog dismissal itself
        if (isPromptActive) {
            return
        }

        if (backgroundTimestamp == 0L) {
            // Cold launch with biometric lock enabled
            _isLocked.value = true
            return
        }

        if (isAppInBackground) {
            isAppInBackground = false
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (timeout == BiometricTimeout.IMMEDIATELY || elapsed >= timeout.timeoutMillis) {
                _isLocked.value = true
            }
        }
    }

    fun onAppBackgrounded(timeout: BiometricTimeout) {
        if (timeout == BiometricTimeout.OFF) return
        if (isPromptActive) return // Prompt dialog triggered onPause; app is not being backgrounded by user

        isAppInBackground = true
        backgroundTimestamp = System.currentTimeMillis()
        if (timeout == BiometricTimeout.IMMEDIATELY) {
            _isLocked.value = true
        }
    }

    fun onAppClosed(timeout: BiometricTimeout) {
        if (timeout != BiometricTimeout.OFF) {
            _isLocked.value = true
            backgroundTimestamp = 0L
            isAppInBackground = false
        }
    }

    fun unlockManually() {
        _isLocked.value = false
        isAppInBackground = false
        backgroundTimestamp = 0L
    }

    fun lockImmediately() {
        _isLocked.value = true
    }

    fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(authenticators)
    }

    fun isAuthenticationAvailable(context: Context): Boolean {
        return canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Varsel",
        subtitle: String = "Confirm biometric or device PIN/pattern/password",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isLocked.value) {
            onSuccess()
            return
        }

        val canAuth = canAuthenticate(activity)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            when (canAuth) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    onError("No biometric or device screen lock enrolled. Set a PIN, pattern, or fingerprint in settings.")
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    onError("Biometric or lock credentials hardware not available on this device.")
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    onError("Security hardware is currently unavailable. Please try again.")
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    onError("Security update required to use biometric authentication.")
                    return
                }
                else -> {
                    onError("Biometric authentication error (code $canAuth)")
                    return
                }
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        isPromptActive = true

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isPromptActive = false
                _isLocked.value = false
                isAppInBackground = false
                backgroundTimestamp = 0L
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isPromptActive = false
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onError("Authentication cancelled. Tap below to unlock.")
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Please try again or use device credentials.")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            isPromptActive = false
            onError(e.message ?: "Failed to launch authentication prompt")
        }
    }
}
