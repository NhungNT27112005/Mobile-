package com.example.mobile.core.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mobile.core.model.AuthSession
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SessionDataStore(context: Context) {

    private val gson = Gson()
    private val sessionKey = stringPreferencesKey("encrypted_session")
    private val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = {
            File(context.applicationContext.noBackupFilesDir, "newshub_session.preferences_pb")
        }
    )

    suspend fun save(session: AuthSession) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val encrypted = cipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        val value = listOf(
            "v1",
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        ).joinToString(":")

        dataStore.edit { preferences ->
            preferences[sessionKey] = value
        }
        Unit
    }

    suspend fun read(): AuthSession? = withContext(Dispatchers.IO) {
        val value = dataStore.data.first()[sessionKey] ?: return@withContext null
        val restored = try {
            val parts = value.split(':', limit = 3)
            require(parts.size == 3 && parts[0] == "v1")
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
            require(iv.size == 12 && encrypted.size >= 16)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, iv))
            val json = String(cipher.doFinal(encrypted), Charsets.UTF_8)
            gson.fromJson(json, AuthSession::class.java)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: JsonParseException) {
            null
        }

        val user = restored?.user
        if (
            restored == null ||
            restored.accessToken.isNullOrBlank() ||
            restored.expiresAt <= System.currentTimeMillis() / 1000 ||
            user == null ||
            user.id.isNullOrBlank() ||
            user.displayName.isNullOrBlank() ||
            user.email.isNullOrBlank()
        ) {
            clear()
            null
        } else {
            restored
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(sessionKey)
        }
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "newshub.session.aes"
    }
}
