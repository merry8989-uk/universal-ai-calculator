package com.merry8989uk.universalcalculator.sync

import android.content.Context
import com.merry8989uk.universalcalculator.data.ArchiveStore
import com.merry8989uk.universalcalculator.data.CustomCalcStore
import com.merry8989uk.universalcalculator.data.DataPolicy
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
     * Uploads the backup, encrypted, under this account.
     *
     * The password is asked for again because it is never stored — the key that
     * encrypts the backup is derived from it, and a key the app kept lying
     * around would defeat the point of encrypting at all.
     *
     * What is in the backup is decided by [BackupContents] and nowhere else, so
     * the switch on the screen and the bytes that leave the phone cannot drift
     * apart again. [includeCalculations] is that switch: it governs the archive
     * and the history together, because both of them are the user's own working
     * and a label about one of them must not leave the other going out in
     * silence.
     */
    suspend fun backUp(
        context: Context,
        password: String,
        includeCalculations: Boolean,
    ): Outcome {
        val id = AccountStore.id(context)
        val verifier = AccountStore.verifier(context)
        if (id.isBlank() || verifier.isBlank()) return Outcome.Failed("Sign in first.")
        if (Account.verifier(id, password) != verifier) {
            return Outcome.Failed("That password is not right for this account.")
        }

        // The master saving switch decides whether this phone keeps an archive
        // at all; the switch on the screen decides whether that archive travels.
        val archiveKept = DataPolicy.mayArchive(SettingsRepository(context).currentSavingEnabled())
        val payload = SyncPayload(
            createdAt = System.currentTimeMillis(),
            settings = SettingsRepository(context).exportSyncable(),
            calculators = CustomCalcStore.load(context),
            history = if (BackupContents.historyIncluded(includeCalculations)) {
                HistoryRepository(context).readAll()
            } else {
                ""
            },
            archive = if (BackupContents.archiveIncluded(includeCalculations, archiveKept)) {
                ArchiveStore.backupText(context)
            } else {
                ""
            },
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

    /**
     * Deletes the encrypted blob from the server.
     *
     * This is the one action here with no undo, so it is never reached from a tap
     * alone: the screen asks first, in words that name every part that goes. The
     * password is not asked for again, and that is deliberate — the password is
     * the key to the data, not to the account, and the verifier this device
     * already holds is what the server checks. Asking for it here would suggest,
     * wrongly, that the password is what stands between a stranger and the copy.
     *
     * Idempotent on purpose. A second delete, or a first one whose reply was lost
     * on the way back, is reported as done rather than as a failure: the user
     * asked for their data to be gone, and it is gone. What is *not* deleted is
     * the account itself — the id and its verifier stay, so the same id can be
     * backed up to again — and the screen says so.
     */
    suspend fun deleteBackup(context: Context): Outcome {
        val id = AccountStore.id(context)
        val verifier = AccountStore.verifier(context)
        if (id.isBlank() || verifier.isBlank()) return Outcome.Failed("Sign in first.")

        SyncClient.count(context, "delete")
        return when (val result = SyncClient.delete(id, verifier)) {
            is SyncClient.Result.Failed -> when (result.code) {
                // The account is not on the server any more, so nothing of the
                // user's can be left there either. Staying signed in to an
                // account the server has never heard of is a promise the phone
                // cannot keep, so this signs out as well.
                404 -> {
                    signOut(context)
                    Outcome.Ok(
                        "There is no account with that id on the server any more, so there was " +
                            "nothing left to delete. Signed out on this phone.",
                    )
                }
                // A Worker that predates deleting answers this way. Said plainly,
                // because the user has just been told their data would go and it
                // has not.
                400, 501 -> Outcome.Failed(
                    "This server cannot delete a backup yet (" + result.message + "). " +
                        "Nothing was deleted, the backup is still on the server, and you are " +
                        "still signed in on this phone.",
                )
                else -> Outcome.Failed(
                    "Nothing was deleted: " + result.message + " The backup is still on the " +
                        "server and you are still signed in on this phone.",
                )
            }
            is SyncClient.Result.Ok -> {
                AccountStore.forgetBackup(context)
                Outcome.Ok(
                    if (result.removed) {
                        "Deleted. Your encrypted backup is gone from the server; nothing of " +
                            "yours is stored there now. You are still signed in on this phone."
                    } else {
                        "There was no backup on the server to delete, so nothing is left " +
                            "behind. You are still signed in on this phone."
                    },
                )
            }
        }
    }

    /** Fetches what is stored and applies it to this device. */
    suspend fun restore(context: Context, password: String, includeCalculations: Boolean): Outcome {
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
        if (BackupContents.historyIncluded(includeCalculations) && payload.history.isNotBlank()) {
            HistoryRepository(context).replaceAll(payload.history)
        }
        // The archive is put back into its day folders, and an entry already
        // there is not written twice, so restoring twice changes nothing.
        var restored = 0
        if (payload.archive.isNotBlank() &&
            DataPolicy.mayArchive(SettingsRepository(context).currentSavingEnabled())
        ) {
            restored = ArchiveStore.restoreBackup(context, payload.archive)
        }
        AccountStore.noteRestore(context)
        return Outcome.Ok(
            "Restored ${payload.summary()}." +
                if (restored > 0) " $restored archived calculations came back." else "",
        )
    }
}
