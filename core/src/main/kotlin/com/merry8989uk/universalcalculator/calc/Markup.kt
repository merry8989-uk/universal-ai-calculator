package com.merry8989uk.universalcalculator.calc

/**
 * The mark-up key's arithmetic.
 *
 * A margin is a share of the selling price, not of the cost, which is the thing
 * people get wrong: 25% on a cost of 100 is not a price of 125, it is 133.33.
 * Keeping it here, in one place with its own test, means the difference is
 * written down rather than rediscovered.
 */
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
