package com.merry8989uk.universalcalculator.calc

/**
 * A unit is defined by how it maps to and from its category's base unit.
 * Linear units use [Unit.linear]; temperature uses [Unit.affine].
 */
class Unit(
    val name: String,
    val symbol: String,
    private val toBaseFn: (Double) -> Double,
    private val fromBaseFn: (Double) -> Double,
) {
    fun toBase(value: Double): Double = toBaseFn(value)
    fun fromBase(value: Double): Double = fromBaseFn(value)

    companion object {
        fun linear(name: String, symbol: String, factor: Double) =
            Unit(name, symbol, { it * factor }, { it / factor })

        /** base = value * scale + offset */
        fun affine(name: String, symbol: String, scale: Double, offset: Double) =
            Unit(name, symbol, { it * scale + offset }, { (it - offset) / scale })
    }
}

data class UnitCategory(val name: String, val units: List<Unit>) {
    fun unit(symbol: String): Unit = units.firstOrNull { it.symbol == symbol } ?: units.first()
}

object Units {

    private fun L(name: String, s: String, f: Double) = Unit.linear(name, s, f)

    val categories: List<UnitCategory> = listOf(
        UnitCategory("Length", listOf(
            L("Kilometre", "km", 1000.0), L("Metre", "m", 1.0), L("Centimetre", "cm", 0.01),
            L("Millimetre", "mm", 0.001), L("Micrometre", "um", 1e-6), L("Nanometre", "nm", 1e-9),
            L("Mile", "mi", 1609.344), L("Yard", "yd", 0.9144), L("Foot", "ft", 0.3048),
            L("Inch", "in", 0.0254), L("Nautical mile", "nmi", 1852.0), L("Light year", "ly", 9.4607e15),
            L("Parsec", "pc", 3.0857e16), L("Angstrom", "A", 1e-10),
        )),
        UnitCategory("Mass", listOf(
            L("Tonne", "t", 1000.0), L("Kilogram", "kg", 1.0), L("Gram", "g", 0.001),
            L("Milligram", "mg", 1e-6), L("Microgram", "ug", 1e-9), L("Pound", "lb", 0.45359237),
            L("Ounce", "oz", 0.028349523), L("Stone", "st", 6.35029318), L("Carat", "ct", 0.0002),
            L("Quintal", "q", 100.0), L("Tola", "tola", 0.0116638),
        )),
        UnitCategory("Temperature", listOf(
            Unit.affine("Celsius", "C", 1.0, 273.15),
            Unit.affine("Fahrenheit", "F", 5.0 / 9.0, 273.15 - 32.0 * 5.0 / 9.0),
            Unit.affine("Kelvin", "K", 1.0, 0.0),
            Unit.affine("Rankine", "R", 5.0 / 9.0, 0.0),
        )),
        UnitCategory("Area", listOf(
            L("Square kilometre", "km2", 1e6), L("Hectare", "ha", 10000.0), L("Acre", "ac", 4046.8564224),
            L("Square metre", "m2", 1.0), L("Square foot", "ft2", 0.09290304),
            L("Square yard", "yd2", 0.83612736), L("Square inch", "in2", 0.00064516),
            L("Bigha", "bigha", 2529.28), L("Guntha", "guntha", 101.17), L("Cent", "cent", 40.4686),
        )),
        UnitCategory("Volume", listOf(
            L("Cubic metre", "m3", 1000.0), L("Litre", "L", 1.0), L("Millilitre", "mL", 0.001),
            L("Cubic centimetre", "cm3", 0.001), L("Gallon (US)", "galUS", 3.785411784),
            L("Gallon (UK)", "galUK", 4.54609), L("Quart", "qt", 0.946352946),
            L("Pint", "pt", 0.473176473), L("Cup", "cup", 0.2365882365),
            L("Fluid ounce", "floz", 0.0295735296), L("Tablespoon", "tbsp", 0.0147867648),
            L("Teaspoon", "tsp", 0.00492892159), L("Barrel (oil)", "bbl", 158.987295),
        )),
        UnitCategory("Speed", listOf(
            L("Metre/second", "m/s", 1.0), L("Kilometre/hour", "km/h", 0.2777777778),
            L("Mile/hour", "mph", 0.44704), L("Foot/second", "ft/s", 0.3048),
            L("Knot", "kn", 0.514444444), L("Mach", "Mach", 340.29),
        )),
        UnitCategory("Time", listOf(
            L("Nanosecond", "ns", 1e-9), L("Microsecond", "us", 1e-6), L("Millisecond", "ms", 0.001),
            L("Second", "s", 1.0), L("Minute", "min", 60.0), L("Hour", "h", 3600.0),
            L("Day", "day", 86400.0), L("Week", "wk", 604800.0), L("Year", "yr", 31557600.0),
        )),
        UnitCategory("Data", listOf(
            L("Bit", "bit", 0.125), L("Byte", "B", 1.0), L("Kilobyte", "KB", 1000.0),
            L("Kibibyte", "KiB", 1024.0), L("Megabyte", "MB", 1e6), L("Mebibyte", "MiB", 1048576.0),
            L("Gigabyte", "GB", 1e9), L("Gibibyte", "GiB", 1073741824.0),
            L("Terabyte", "TB", 1e12), L("Tebibyte", "TiB", 1099511627776.0),
        )),
        UnitCategory("Pressure", listOf(
            L("Pascal", "Pa", 1.0), L("Kilopascal", "kPa", 1000.0), L("Bar", "bar", 100000.0),
            L("Atmosphere", "atm", 101325.0), L("PSI", "psi", 6894.757293), L("mmHg", "mmHg", 133.322368),
            L("Torr", "torr", 133.322368),
        )),
        UnitCategory("Energy", listOf(
            L("Joule", "J", 1.0), L("Kilojoule", "kJ", 1000.0), L("Calorie", "cal", 4.184),
            L("Kilocalorie", "kcal", 4184.0), L("Watt-hour", "Wh", 3600.0), L("Kilowatt-hour", "kWh", 3.6e6),
            L("Electronvolt", "eV", 1.602176634e-19), L("BTU", "BTU", 1055.05585262),
        )),
        UnitCategory("Power", listOf(
            L("Watt", "W", 1.0), L("Kilowatt", "kW", 1000.0), L("Megawatt", "MW", 1e6),
            L("Horsepower (metric)", "hpM", 735.49875), L("Horsepower (mech)", "hp", 745.699872),
        )),
        UnitCategory("Angle", listOf(
            L("Degree", "deg", 1.0), L("Radian", "rad", 57.295779513), L("Gradian", "grad", 0.9),
            L("Arcminute", "arcmin", 1.0 / 60.0), L("Arcsecond", "arcsec", 1.0 / 3600.0),
            L("Turn", "turn", 360.0),
        )),
    )

    fun convert(value: Double, from: Unit, to: Unit): Double = to.fromBase(from.toBase(value))
}
