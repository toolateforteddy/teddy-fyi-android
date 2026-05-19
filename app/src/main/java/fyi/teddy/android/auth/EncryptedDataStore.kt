package fyi.teddy.android.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.KeyTemplates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_session_datastore")

class EncryptedDataStore(private val context: Context) {
    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset", "master_key")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://tink_master_key")
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    suspend fun saveEncrypted(keyName: String, value: String?) {
        if (value == null) {
            context.dataStore.edit { preferences ->
                preferences.remove(stringPreferencesKey(keyName))
            }
            return
        }
        val encryptedValue = aead.encrypt(value.toByteArray(), null)
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(keyName)] = android.util.Base64.encodeToString(encryptedValue, android.util.Base64.DEFAULT)
        }
    }

    suspend fun getDecrypted(keyName: String): String? {
        val encryptedValue = context.dataStore.data.map { it[stringPreferencesKey(keyName)] }.first()
        return encryptedValue?.let {
            try {
                val decoded = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                String(aead.decrypt(decoded, null))
            } catch (_: Exception) {
                null
            }
        }
    }
}
