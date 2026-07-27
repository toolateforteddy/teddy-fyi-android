package fyi.teddy.android.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.KeyTemplates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore

val Context.dataStore by preferencesDataStore(name = "user_session_datastore")

@Suppress("TooGenericExceptionCaught")
class EncryptedDataStore(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {
    private val aead: Aead?

    init {
        aead = try {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, "tink_keyset", "master_key")
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://tink_master_key")
                .build()
                .keysetHandle
            keysetHandle.getPrimitive(com.google.crypto.tink.RegistryConfiguration.get(), Aead::class.java)
        } catch (e: Exception) {
            android.util.Log.e("EncryptedDataStore", "Failed to initialize Aead. Resetting all.", e)
            
            // Hard reset: Delete keyset and the datastore entries
            context.getSharedPreferences("tink_keyset", Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences("master_key", Context.MODE_PRIVATE).edit().clear().apply()
            
            null
        }
    }

    @Suppress("unused")
    suspend fun saveEncrypted(keyName: String, value: String?) {
        saveAllEncrypted(mapOf(keyName to value))
    }

    suspend fun saveAllEncrypted(pairs: Map<String, String?>) {
        val aead = aead ?: return
        dataStore.edit { preferences ->
            pairs.forEach { (keyName, value) ->
                if (value == null) {
                    preferences.remove(stringPreferencesKey(keyName))
                } else {
                    val encryptedValue = aead.encrypt(value.toByteArray(), null)
                    val encoded = android.util.Base64.encodeToString(
                        encryptedValue, 
                        android.util.Base64.DEFAULT
                    )
                    preferences[stringPreferencesKey(keyName)] = encoded
                }
            }
        }
    }

    suspend fun getDecrypted(keyName: String): String? {
        val aead = aead ?: return null
        val encryptedValue = dataStore.data.map { it[stringPreferencesKey(keyName)] }.first()
        return encryptedValue?.let {
            try {
                val decoded = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                String(aead.decrypt(decoded, null))
            } catch (e: Exception) {
                android.util.Log.e("EncryptedDataStore", "Failed to decrypt key: $keyName", e)
                null
            }
        }
    }
}
