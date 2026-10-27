package com.merry8989uk.universalcalculator.sync

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The rules an account has to follow, as plain functions.
 *
 * Nothing here touches the network or the disk, so every rule — what an id may
 * look like, what makes a password acceptable, how the two become a verifier and
 * an encryption key — can be tested on its own.
 */
object Account {

    /** The shortest password worth accepting. */
    const val MIN_PASSWORD = 8

    const val MIN_ID = 3
    const val MAX_ID = 24

    private const val VERIFIER_ITERATIONS = 120_000
    private const val VERIFIER_BITS = 256
    private const val VERIFIER_PREFIX = "nexuscalc-verifier-v1|"

    /** An id folded to the form the server stores, so `Amit` and `amit` are one account. */
    fun normalise(raw: String): String = raw.trim().lowercase()

    /**
     * Why this id cannot be used, or null when it can.
     *
     * The rules are shown to the user as they type, so this returns the sentence
     * to show rather than a boolean.
     */
    fun problemWithId(raw: String): String? {
        val id = normalise(raw)
        return when {
            id.isEmpty() -> "Choose an id."
            id.length < MIN_ID -> "An id needs at least $MIN_ID characters."
            id.length > MAX_ID -> "An id can be at most $MAX_ID characters."
            !id.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' } ->
                "Use letters, numbers, dots, dashes or underscores — nothing else."
            !id[0].isLetterOrDigit() -> "An id has to start with a letter or a number."
            else -> null
        }
    }

    /** Why this password cannot be used, or null when it can. */
    fun problemWithPassword(password: String): String? = when {
        password.isEmpty() -> "Choose a password."
        password.length < MIN_PASSWORD -> "A password needs at least $MIN_PASSWORD characters."
        else -> null
    }

    /** Why the two passwords do not match, or null when they do. */
    fun problemWithConfirmation(password: String, confirmation: String): String? =
        if (password == confirmation) null else "The two passwords do not match."

    /**
     * What the server stores instead of the password.
     *
     * PBKDF2 over the password, salted with the id. The server can compare it to
     * what it holds, and cannot turn it back into the password. It is *not* the
     * key the data is encrypted with — that one never leaves the device.
     */
    fun verifier(id: String, password: String): String {
        val salt = MessageDigest.getInstance("SHA-256")
            .digest((VERIFIER_PREFIX + normalise(id)).toByteArray(Charsets.UTF_8))
        val spec = PBEKeySpec(password.toCharArray(), salt, VERIFIER_ITERATIONS, VERIFIER_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * The passphrase the data is encrypted with.
     *
     * The id is part of it, so the same password under two ids produces two
     * unrelated keys.
     */
    fun encryptionPassphrase(id: String, password: String): String =
        normalise(id) + ":" + password
}
