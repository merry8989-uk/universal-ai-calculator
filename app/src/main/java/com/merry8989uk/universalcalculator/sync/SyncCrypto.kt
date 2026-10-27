package com.merry8989uk.universalcalculator.sync

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption for sync.
 *
 * The user picks a sync code and never types it into the server. Two things are
 * derived from it:
 *
 *  - a **slot id**, a plain SHA-256 of the code, which is the only thing the
 *    server ever sees — it cannot be turned back into the code;
 *  - an **AES key**, through PBKDF2 with a random salt, which encrypts the
 *    payload before it leaves the device.
 *
 * The salt travels inside the encrypted blob, so another device with the same
 * code can derive the same key. Without the code the stored blob is unreadable,
 * and a wrong code fails the authentication tag rather than returning rubbish.
 */
object SyncCrypto {

    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    /** A fresh random salt. */
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    /** base64( salt | iv | ciphertext-with-tag ). */
    fun encrypt(plain: String, code: String, salt: ByteArray = newSalt()): String {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(code, salt), GCMParameterSpec(TAG_BITS, iv))
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        val packed = ByteArray(salt.size + iv.size + body.size)
        System.arraycopy(salt, 0, packed, 0, salt.size)
        System.arraycopy(iv, 0, packed, salt.size, iv.size)
        System.arraycopy(body, 0, packed, salt.size + iv.size, body.size)
        return Base64.getEncoder().encodeToString(packed)
    }

    /** Null when the code is wrong or the blob is damaged. */
    fun decrypt(packed: String, code: String): String? = try {
        val raw = Base64.getDecoder().decode(packed)
        if (raw.size <= SALT_BYTES + IV_BYTES + 1) {
            null
        } else {
            val salt = raw.copyOfRange(0, SALT_BYTES)
            val iv = raw.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
            val body = raw.copyOfRange(SALT_BYTES + IV_BYTES, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(code, salt), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }

    private fun deriveKey(code: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
        return SecretKeySpec(bytes, "AES")
    }
}
