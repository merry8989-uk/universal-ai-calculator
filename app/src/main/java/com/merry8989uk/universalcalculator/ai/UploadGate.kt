package com.merry8989uk.universalcalculator.ai

import com.merry8989uk.universalcalculator.data.DataPolicy

/**
 * What the user is told when a picture is held back.
 *
 * It says what happened, whose decision it was and where to undo it, because a
 * refusal is the app obeying a setting rather than something going wrong — and
 * an upload path that answered a refusal with "check your connection" would
 * send the user off to look at their Wi-Fi for a switch in their own settings.
 */
const val CONTENT_REFUSED =
    "Not sent: data saving is off, so the picture stays on this device. " +
        "Turn Settings \u2192 Data saving back on to send it to the assistant."

/**
 * What an upload path may do with the picture it is holding.
 *
 * Before this, an upload path had only one kind of thing to report — the model's
 * reply or its error prose — so a refusal had nowhere to go and would have
 * arrived at the user dressed as a failed request: the same card, the same
 * "try again", the same advice about the network. The two events are not alike.
 * One is the app doing as it was told; the other is a request that was made and
 * did not come back. A sealed type keeps a caller from passing one off as the
 * other, and makes "was anything actually sent?" a question with an answer
 * rather than a matter of reading the code above the call.
 *
 * The rule itself lives in [DataPolicy.maySendContent]; this type only says what
 * its answer means at the point where a request would otherwise be made.
 */
sealed interface UploadDecision {

    /** The switch allows it, so the request may be made. */
    data object Allowed : UploadDecision

    /**
     * Nothing left the device and no request was made. [message] is shown in
     * place of an answer, and there is nothing to retry: retrying the same
     * upload would refuse again.
     */
    data class Refused(val message: String) : UploadDecision
}

/**
 * The one decision that stands between a picture and the wire.
 *
 * Every path that would put a photo, a sketch, an audio clip or a picked file on
 * the network asks this first, so there is a single answer to "may this leave
 * the device" instead of one per screen — which is how the sketch pad and the
 * camera came to post their pictures while the switch sat in a different file,
 * consulted by nobody.
 */
fun uploadDecision(savingEnabled: Boolean): UploadDecision =
    if (DataPolicy.maySendContent(savingEnabled)) {
        UploadDecision.Allowed
    } else {
        UploadDecision.Refused(CONTENT_REFUSED)
    }
