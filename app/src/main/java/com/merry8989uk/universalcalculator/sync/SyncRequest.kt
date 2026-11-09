package com.merry8989uk.universalcalculator.sync

import org.json.JSONObject

/**
 * The bodies this app posts to its own Worker, built as plain values.
 *
 * They are kept apart from [SyncClient] for one reason: the request that
 * destroys something is worth reading back before it goes out, and a body that
 * can be built without a socket is a body a test can open and inspect. Nothing
 * here touches Android, the disk or the network.
 */
object SyncRequest {

    /**
     * The delete, in the shape the Worker insists on.
     *
     * `confirm` is the entire guard on the server side — a delete that does not
     * carry it is refused — so it is set here, in the one place the delete can be
     * built, rather than at the call site where a caller in a hurry could leave
     * it out. The account is named by the id and the verifier that proves it; the
     * slot the blob lives in is derived from the id on the server, so no slot is
     * ever sent, and knowing an id alone still destroys nothing.
     */
    fun delete(id: String, verifier: String): JSONObject =
        JSONObject()
            .put("sync", "delete")
            .put("id", id)
            .put("verifier", verifier)
            .put("confirm", true)
}
