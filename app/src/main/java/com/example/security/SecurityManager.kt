package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {

    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    const val DEFAULT_ENCRYPTION_KEY = "SamsungNotes_AES256_SecureStorage_DefaultKey_v2"
    const val ENCRYPTED_HEADER_PREFIX = "ENC:SNOTE:AES256:"

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, expectedHash: String): Boolean {
        if (expectedHash.isEmpty()) return true
        val computed = hashPin(pin)
        return computed == expectedHash
    }

    fun encrypt(plainText: String, secretKey: String): String {
        try {
            val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

            val spec = PBEKeySpec(secretKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = factory.generateSecret(spec).encoded
            val key = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)

            return "$saltB64:$ivB64:$cipherB64"
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText
        }
    }

    fun decrypt(encryptedPayload: String, secretKey: String): String {
        try {
            val parts = encryptedPayload.split(":")
            if (parts.size != 3) return encryptedPayload

            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[2], Base64.NO_WRAP)

            val spec = PBEKeySpec(secretKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keyBytes = factory.generateSecret(spec).encoded
            val key = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            val plainBytes = cipher.doFinal(cipherText)

            return String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return "[Şifre Çözülemedi - Hatalı PIN/Şifre]"
        }
    }

    fun encryptBackupPayload(plainJson: String, customPassword: String?): String {
        val key = if (customPassword.isNullOrBlank()) DEFAULT_ENCRYPTION_KEY else customPassword
        val encryptedBody = encrypt(plainJson, key)
        val hasCustomPassword = !customPassword.isNullOrBlank()
        return "$ENCRYPTED_HEADER_PREFIX${if (hasCustomPassword) "CUSTOM" else "AUTO"}:$encryptedBody"
    }

    fun isEncryptedBackup(content: String): Boolean {
        return content.startsWith(ENCRYPTED_HEADER_PREFIX)
    }

    fun decryptBackupPayload(content: String, customPassword: String?): Pair<Boolean, String> {
        if (!isEncryptedBackup(content)) {
            // Already raw JSON
            return Pair(true, content)
        }

        val afterPrefix = content.removePrefix(ENCRYPTED_HEADER_PREFIX)
        val colonIdx = afterPrefix.indexOf(":")
        if (colonIdx == -1) return Pair(false, "Geçersiz yedek başlığı")

        val mode = afterPrefix.substring(0, colonIdx)
        val cipherPayload = afterPrefix.substring(colonIdx + 1)

        val keyToUse = if (mode == "CUSTOM") {
            if (customPassword.isNullOrBlank()) return Pair(false, "Bu yedek parola korumalıdır. Lütfen parolanızı girin.")
            customPassword
        } else {
            DEFAULT_ENCRYPTION_KEY
        }

        val decrypted = decrypt(cipherPayload, keyToUse)
        if (decrypted.startsWith("{") && decrypted.contains("\"notes\"")) {
            return Pair(true, decrypted)
        }
        return Pair(false, "Hatalı parola veya bozuk yedek verisi!")
    }

    fun generateDeviceSyncCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = SecureRandom()
        val part1 = (1000..9999).random()
        val part2 = (0..3).map { chars[r.nextInt(chars.length)] }.joinToString("")
        return "SNOTE-$part1-$part2"
    }
}
