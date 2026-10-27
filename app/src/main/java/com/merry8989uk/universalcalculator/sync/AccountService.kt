package com.merry8989uk.universalcalculator.sync

import android.content.Context
import com.merry8989uk.universalcalculator.data.CustomCalcStore
import com.merry8989uk.universalcalculator.data.HistoryRepository
import com.merry8989uk.universalcalculator.settings.SettingsRepository

/**
 * Signing in, and moving the data once signed in.
 *
 * Nothing here runs on its own. There is no background job and no timer: the
 * user presses Back up, or presses Restore. Until they do, every calculation
 * stays on the phone in the archive, exactly as before.
 */
object AccountService {

    sealed interface Outcome {
        data class Ok(val message: String) : Outcome
        data class Failed(val message: String) : Outcome
    }

    /** Creates the account and signs this device in. */
    suspend fun signUp(context: Context, rawId: String, password: String): Outcome {
        val id = Account.normalise(rawId)
        Account.problemWithId(id)?.let { return Outcome.Failed(it) }
        Account.problemWithPassword(password)?.let { return Outcome.Failed(it) }

        val verifier = Account.verifier(id, password)
        SyncClient.count(context, "signup")
        return when (val result = SyncClient.signUp(id, verifier)) {
            is SyncClient.Result.Failed ->
                if (result.code == 409) Outcome.Failed("That id is already taken. Try another, or log in.")
                else Outcome.Failed(result.message)
            is SyncClient.Result.Ok -> {
                AccountStore.signIn(context, id, verifier)
                Outcome.Ok("Your account is ready. Nothing has been uploaded yet.")
            }
        }
    }

    /** Signs this device in to an account that already exists. */
    suspend fun logIn(context: Context, rawId: String, password: String): Outcome {
        val id = Account.normalise(rawId)
        Account.problemWithId(id)?.let { return Outcome.Failed(it) }
        if (password.isEmpty()) return Outcome.Failed("Enter your password.")

        val verifier = Account.verifier(id, password)
        SyncClient.count(context, "login")
        return when (val result = SyncClient.logIn(id, verifier)) {
            is SyncClient.Result.Failed -> when (result.code) {
                404 -> Outcome.Failed("There is no account with that id.")
                401 -> Outcome.Failed("That password is not right for that id.")
                else -> Outcome.Failed(result.message)
            }
            is SyncClient.Result.Ok -> {
                AccountStore.signIn(context, id, verifier)
                Outcome.Ok("Signed in.")
            }
        }
    }

    /** Forgets the account on this device. Nothing on the server is deleted. */
    fun signOut(context: Context) {
        AccountStore.signOut(context)
    }

    /**
     * Uploads everything, encrypted, under this account.
     *
     * The password is asked for again because it is never stored — the key that
     * encrypts the backup is derived from it, and a key the app kept lying
     * around would defeat the point of encrypting at all.
     */
    suspend fun backUp(
        context: Context,
        password: String,
        includeHistory: Boolean,
    ): Outcome {
        val id = AccountStore.id(context)
        val verifier = AccountStore.verifier(context)
        if (id.isBlank() || verifier.isBlank()) return Outcome.Failed("Sign in first.")
        if (Account.verifier(id, password) != verifier) {
            return Outcome.Failed("That password is not right for this account.")
        }

        val payload = SyncPayload(
            createdAt = System.currentTimeMillis(),
            settings = SettingsRepository(context).exportSyncable(),
            calculators = CustomCalcStore.load(context),
            history = if (includeHistory) HistoryRepository(context).readAll() else "",
        )
        val passphrase = Account.encryptionPassphrase(id, password)
        val blob = SyncCrypto.encrypt(SyncPayloadCodec.encode(payload), passphrase)

        SyncClient.count(context, "upload")
        // read first, so the version we replace is the one the server really has
        val current = SyncClient.download(id, verifier)
        val version = (current as? SyncClient.Result.Ok)?.version ?: 0

        return when (val result = SyncClient.upload(id, verifier, blob, version)) {
            is SyncClient.Result.Failed ->
                if (result.code == 409) {
                    Outcome.Failed(
                        "This account was backed up from another device more recently. " +
                            "Restore first, then back up.",
                    )
                } else Outcome.Failed(result.message)
            is SyncClient.Result.Ok -> {
                AccountStore.noteBackup(context)
                Outcome.Ok("Backed up ${payload.summary()}.")
            }
        }
    }

    /** Fetches what is stored and applies it to this device. */
    suspend fun restore(context: Context, password: String, includeHistory: Boolean): Outcome {
        val id = AccountStore.id(context)
        val verifier = AccountStore.verifier(context)
        if (id.isBlank() || verifier.isBlank()) return Outcome.Failed("Sign in first.")
        if (Account.verifier(id, password) != verifier) {
            return Outcome.Failed("That password is not right for this account.")
        }

        SyncClient.count(context, "restore")
        val fetched = when (val result = SyncClient.download(id, verifier)) {
            is SyncClient.Result.Failed -> return Outcome.Failed(result.message)
            is SyncClient.Result.Ok -> result.blob
                ?: return Outcome.Failed("There is no backup under this account yet.")
        }

        val passphrase = Account.encryptionPassphrase(id, password)
        val plain = SyncCrypto.decrypt(fetched, passphrase)
            ?: return Outcome.Failed("That password does not open this backup.")

        val payload = SyncPayloadCodec.decode(plain)
            ?: return Outcome.Failed("The backup is in a format this version does not understand.")

        SettingsRepository(context).importSyncable(payload.settings)
        CustomCalcStore.save(context, payload.calculators)
        if (includeHistory && payload.history.isNotBlank()) {
            HistoryRepository(context).replaceAll(payload.history)
        }
        AccountStore.noteRestore(context)
        return Outcome.Ok("Restored ${payload.summary()}.")
    }
}
