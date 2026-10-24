package com.merry8989uk.universalcalculator.sync

import android.content.Context
import com.merry8989uk.universalcalculator.data.CustomCalcStore
import com.merry8989uk.universalcalculator.data.HistoryRepository
import com.merry8989uk.universalcalculator.settings.SettingsRepository

/**
 * Ties the pieces together: gather what should travel, encrypt it, hand it to
 * the server, and do the reverse on the way back.
 */
object SyncService {

    sealed interface Outcome {
        data class Ok(val message: String) : Outcome
        data class Failed(val message: String) : Outcome
    }

    /** Uploads the current state under the user's code. */
    suspend fun upload(context: Context, includeHistory: Boolean): Outcome {
        val code = SyncStore.code(context)
        if (code.length < SyncStore.MIN_CODE_LENGTH) {
            return Outcome.Failed("Choose a sync code of at least ${SyncStore.MIN_CODE_LENGTH} characters first.")
        }

        val payload = SyncPayload(
            createdAt = System.currentTimeMillis(),
            settings = SettingsRepository(context).exportSyncable(),
            calculators = CustomCalcStore.load(context),
            history = if (includeHistory) HistoryRepository(context).readAll() else "",
        )

        val blob = SyncCrypto.encrypt(SyncPayloadCodec.encode(payload), code)
        SyncClient.count(context, "upload")
        return when (val result = SyncClient.upload(SyncCrypto.slotId(code), blob)) {
            is SyncClient.Result.Failed -> Outcome.Failed(result.message)
            is SyncClient.Result.Ok -> {
                SyncStore.noteUpload(context, System.currentTimeMillis())
                Outcome.Ok("Uploaded ${payload.summary()}.")
            }
        }
    }

    /** Fetches the stored blob and applies it to this device. */
    suspend fun restore(context: Context, includeHistory: Boolean): Outcome {
        val code = SyncStore.code(context)
        if (code.length < SyncStore.MIN_CODE_LENGTH) {
            return Outcome.Failed("Choose a sync code of at least ${SyncStore.MIN_CODE_LENGTH} characters first.")
        }

        SyncClient.count(context, "restore")
        val fetched = when (val result = SyncClient.download(SyncCrypto.slotId(code))) {
            is SyncClient.Result.Failed -> return Outcome.Failed(result.message)
            is SyncClient.Result.Ok -> result.blob
                ?: return Outcome.Failed("Nothing is stored for that code yet.")
        }

        // A wrong code fails the authentication tag rather than decrypting to rubbish.
        val plain = SyncCrypto.decrypt(fetched, code)
            ?: return Outcome.Failed("That code does not open the stored data.")

        val payload = SyncPayloadCodec.decode(plain)
            ?: return Outcome.Failed("The stored data is in a format this version does not understand.")

        SettingsRepository(context).importSyncable(payload.settings)
        CustomCalcStore.save(context, payload.calculators)
        if (includeHistory && payload.history.isNotBlank()) {
            HistoryRepository(context).replaceAll(payload.history)
        }
        Outcome.Ok("Restored ${payload.summary()}.")
    }

    /** Removes the stored blob. The code and its data are gone for good. */
    suspend fun erase(context: Context): Outcome {
        val code = SyncStore.code(context)
        if (code.length < SyncStore.MIN_CODE_LENGTH) {
            return Outcome.Failed("There is no sync code set.")
        }
        SyncClient.count(context, "erase")
        return when (val result = SyncClient.delete(SyncCrypto.slotId(code))) {
            is SyncClient.Result.Failed -> Outcome.Failed(result.message)
            is SyncClient.Result.Ok -> Outcome.Ok("The stored copy has been deleted.")
        }
    }
}
