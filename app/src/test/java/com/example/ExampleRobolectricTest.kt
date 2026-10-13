package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Samsung Notes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Samsung Notes", appName)
    }

    @Test
    fun `security manager pin verification and aes encryption`() {
        val pin = "1234"
        val hash = SecurityManager.hashPin(pin)
        assertTrue(SecurityManager.verifyPin("1234", hash))
        assertFalse(SecurityManager.verifyPin("9999", hash))

        val plainText = "Gizli Samsung Notu"
        val cipher = SecurityManager.encrypt(plainText, pin)
        val decrypted = SecurityManager.decrypt(cipher, pin)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `encrypted backup payload auto and password protection`() {
        val jsonPayload = "{\"version\": 2, \"notes\": [{\"title\": \"Test Note\"}]}"

        // Auto encryption (no custom password)
        val autoEncrypted = SecurityManager.encryptBackupPayload(jsonPayload, null)
        assertTrue(SecurityManager.isEncryptedBackup(autoEncrypted))
        val (autoSuccess, autoDecrypted) = SecurityManager.decryptBackupPayload(autoEncrypted, null)
        assertTrue(autoSuccess)
        assertEquals(jsonPayload, autoDecrypted)

        // Custom password encryption
        val customPass = "MySuperSecretKey99"
        val passEncrypted = SecurityManager.encryptBackupPayload(jsonPayload, customPass)
        assertTrue(SecurityManager.isEncryptedBackup(passEncrypted))

        // Wrong password fails
        val (failSuccess, _) = SecurityManager.decryptBackupPayload(passEncrypted, "WrongPassword")
        assertFalse(failSuccess)

        // Correct password succeeds
        val (correctSuccess, passDecrypted) = SecurityManager.decryptBackupPayload(passEncrypted, customPass)
        assertTrue(correctSuccess)
        assertEquals(jsonPayload, passDecrypted)
    }

    @Test
    fun `device sync code format verification`() {
        val code = SecurityManager.generateDeviceSyncCode()
        assertTrue(code.startsWith("SNOTE-"))
        assertEquals(3, code.split("-").size)
    }

    @Test
    fun `drawing stroke serialization round trip`() {
        val points = listOf(DrawingPoint(10f, 20f), DrawingPoint(30f, 40f))
        val stroke = DrawingStroke(points = points, color = 0xFFE65100, strokeWidth = 8f, isEraser = false)
        val json = DrawingStroke.listToJson(listOf(stroke))
        val restored = DrawingStroke.listFromJson(json)

        assertEquals(1, restored.size)
        assertEquals(2, restored[0].points.size)
        assertEquals(0xFFE65100, restored[0].color)
    }
}
