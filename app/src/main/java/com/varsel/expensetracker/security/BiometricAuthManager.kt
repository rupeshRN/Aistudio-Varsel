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

    fun checkLockOnResume(timeout: BiometricTimeout) {
        if (timeout == BiometricTimeout.OFF) {
            _isLocked.value = false
            return
        }

        if (backgroundTimestamp == 0L) {
            // First launch of app with biometric lock enabled
            _isLocked.value = true
            return
        }

        val elapsed = System.currentTimeMillis() - backgroundTimestamp
        if (elapsed >= timeout.timeoutMillis) {
            _isLocked.value = true
        }
    }

    fun onAppBackgrounded(timeout: BiometricTimeout) {
        if (timeout != BiometricTimeout.OFF) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    fun unlockManually() {
        _isLocked.value = false
        backgroundTimestamp = System.currentTimeMillis()
    }

    fun lockImmediately() {
        _isLocked.value = true
    }

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Varsel",
        subtitle: String = "Authenticate to access your finances",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _isLocked.value = false
                backgroundTimestamp = System.currentTimeMillis()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed. Please try again.")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
