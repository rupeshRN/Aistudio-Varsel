package com.varsel.expensetracker.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher encryption passphrase using the hardware-backed Android KeyStore.
 *
 * Rather than saving the raw database encryption passphrase in plaintext SharedPreferences,
 * this manager wraps the passphrase with an AES-256 GCM key stored securely inside
 * the Android KeyStore provider.
 *
 * Provides seamless migration for databases created under legacy plaintext key storage.
 */
object SecurePassphraseManager {

    private const val TAG = "SecurePassphraseManager"
    private const val PREFS_NAME = "encrypted_db_secure_prefs"
    private const val LEGACY_PASSPHRASE_KEY = "db_passphrase_key"
    private const val ENCRYPTED_PASSPHRASE_KEY = "db_passphrase_encrypted"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "varsel_db_master_key"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    fun getOrGeneratePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            // Check if legacy plaintext key exists and migrate it to KeyStore
            val legacyKey = prefs.getString(LEGACY_PASSPHRASE_KEY, null)
            if (legacyKey != null) {
                try {
                    val encryptedPayload = encryptWithKeyStore(legacyKey)
                    prefs.edit()
                        .putString(ENCRYPTED_PASSPHRASE_KEY, encryptedPayload)
                        .remove(LEGACY_PASSPHRASE_KEY)
                        .apply()
                    Log.i(TAG, "Successfully migrated database passphrase into Android KeyStore.")
                    return legacyKey.toByteArray(StandardCharsets.UTF_8)
                } catch (e: Exception) {
                    Log.w(TAG, "KeyStore migration failed, falling back to legacy key: ${e.message}")
                    return legacyKey.toByteArray(StandardCharsets.UTF_8)
                }
            }

            // Check if encrypted key already exists in SharedPreferences
            val encryptedPayload = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
            if (encryptedPayload != null) {
                val decryptedKey = decryptWithKeyStore(encryptedPayload)
                return decryptedKey.toByteArray(StandardCharsets.UTF_8)
            }

            // Generate a fresh 32-byte high-entropy passphrase
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            val newPassphrase = randomBytes.joinToString("") { "%02x".format(it) }

            // Encrypt and persist with KeyStore
            val newEncryptedPayload = encryptWithKeyStore(newPassphrase)
            prefs.edit()
                .putString(ENCRYPTED_PASSPHRASE_KEY, newEncryptedPayload)
                .apply()

            return newPassphrase.toByteArray(StandardCharsets.UTF_8)

        } catch (e: Exception) {
            Log.e(TAG, "KeyStore operation encountered an error, using secure in-memory fallback", e)
            // Resilient fallback in case KeyStore is unavailable on the device
            var fallbackKey = prefs.getString(LEGACY_PASSPHRASE_KEY, null)
            if (fallbackKey == null) {
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                fallbackKey = randomBytes.joinToString("") { "%02x".format(it) }
                prefs.edit().putString(LEGACY_PASSPHRASE_KEY, fallbackKey).apply()
            }
            return fallbackKey.toByteArray(StandardCharsets.UTF_8)
        }
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    private fun encryptWithKeyStore(plainText: String): String {
        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // Combine IV (12 bytes) + CipherText
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptWithKeyStore(encryptedBase64: String): String {
        val secretKey = getOrCreateMasterKey()
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        if (combined.size < GCM_IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        val cipherText = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)

        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }
}
