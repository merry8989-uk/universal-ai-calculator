package com.merry8989uk.universalcalculator.calc

/**
 * The mark-up key's arithmetic.
 *
 * A margin is a share of the selling price, not of the cost, which is the thing
 * people get wrong: 25% on a cost of 100 is not a price of 125, it is 133.33.
 * Keeping it here, in one place with its own test, means the difference is
 * written down rather than rediscovered.
 */
/**
 * The grand total: every answer added up, the way a shop calculator's GT works.
 *
 * Nothing here parses a printed number. The old version tried to read the answer
 * back out of the grouped text on screen, which fails the moment a result is
 * written as `1 234` or `2.8x10^14` — so the total quietly stayed at zero.
 */
object GrandTotal {

    /** [total] with [answer] added, or [total] unchanged when there is no number. */
    fun add(total: Double, answer: Double?): Double {
        if (answer == null || !answer.isFinite()) return total
        val sum = total + answer
        return if (sum.isFinite()) sum else total
    }

    /** What pressing GT shows. Pressed again, the total goes back to zero. */
    fun afterPress(total: Double, justPressed: Boolean): Double =
        if (justPressed) 0.0 else total
}

object Markup {

    /**
     * The price that leaves [marginPercent] of it as profit.
     *
     * Null when the margin is 100% or more, because no price can be all profit.
     */
    fun sellingPrice(cost: Double, marginPercent: Double): Double? {
        if (!cost.isFinite() || !marginPercent.isFinite()) return null
        if (marginPercent >= 100.0) return null
        return cost / (1.0 - marginPercent / 100.0)
    }

    /** The margin a price leaves, as a percentage of the price. */
    fun margin(cost: Double, price: Double): Double? {
        if (!cost.isFinite() || !price.isFinite() || price == 0.0) return null
        return (price - cost) / price * 100.0
    }

    /** The mark-up on cost, which is a different number from the margin. */
    fun markupPercent(cost: Double, price: Double): Double? {
        if (!cost.isFinite() || !price.isFinite() || cost == 0.0) return null
        return (price - cost) / cost * 100.0
    }
}
