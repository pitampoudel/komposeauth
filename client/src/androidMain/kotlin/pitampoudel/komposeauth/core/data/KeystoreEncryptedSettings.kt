package pitampoudel.komposeauth.core.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.russhwolf.settings.Settings
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps its strings sealed with an AES-GCM key that lives in the Android Keystore and never leaves
 * it, so a copy of the preferences file (a backup, a rooted device) holds no usable token.
 *
 * A value written before sealing is sealed the first time it is read, which writes over it. One that
 * won't open reads as absent and is removed: after a restore onto another device the key is not
 * there, and the user signs in again. If the Keystore can't seal at all, nothing is stored rather
 * than plaintext.
 */
internal class KeystoreEncryptedSettings(
    private val delegate: Settings,
    private val keyAlias: String = "komposeauth_auth_prefs"
) : Settings by delegate {

    private val key: SecretKey by lazy {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(keyAlias, null) as? SecretKey ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
            }
            .generateKey()
    }

    override fun putString(key: String, value: String) {
        val sealed = try {
            seal(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Could not seal '$key'; it is not stored", e)
            null
        }
        if (sealed == null) delegate.remove(key) else delegate.putString(key, sealed)
    }

    override fun getStringOrNull(key: String): String? {
        val stored = delegate.getStringOrNull(key) ?: return null
        if (!stored.startsWith(SEALED_PREFIX)) {
            putString(key, stored)
            return stored.takeIf { delegate.hasKey(key) }
        }
        return try {
            open(key, stored)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open '$key'; treating it as absent", e)
            delegate.remove(key)
            null
        }
    }

    override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue

    /** The setting's name is bound in as associated data, so a value can't be moved to another name. */
    private fun seal(name: String, value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // The Keystore picks the nonce; it refuses one chosen by the caller.
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val sealed = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return SEALED_PREFIX + Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun open(name: String, stored: String): String {
        val sealed = Base64.decode(stored.removePrefix(SEALED_PREFIX), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES), Charsets.UTF_8)
    }

    private companion object {
        const val TAG = "KomposeAuth"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEALED_PREFIX = "ks1:"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
    }
}
