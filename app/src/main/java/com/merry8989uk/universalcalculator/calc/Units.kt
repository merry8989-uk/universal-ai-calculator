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
            L("Kilometre", "km", 1000.0), L("Hectometre", "hm", 100.0), L("Decametre", "dam", 10.0),
            L("Metre", "m", 1.0), L("Decimetre", "dm", 0.1), L("Centimetre", "cm", 0.01),
            L("Millimetre", "mm", 0.001), L("Micrometre", "um", 1e-6), L("Nanometre", "nm", 1e-9),
            L("Picometre", "pm", 1e-12), L("Megametre", "Mm", 1e6),
            L("Mile", "mi", 1609.344), L("Yard", "yd", 0.9144), L("Foot", "ft", 0.3048),
            L("Inch", "in", 0.0254), L("Nautical mile", "nmi", 1852.0), L("Fathom", "ftm", 1.8288),
            L("Furlong", "fur", 201.168), L("Chain", "ch", 20.1168), L("Rod", "rd", 5.0292),
            L("League", "lea", 4828.032), L("Hand", "hand", 0.1016), L("Barleycorn", "bc", 0.0084667),
            L("Mil", "mil", 2.54e-5), L("Light year", "ly", 9.4607e15), L("Parsec", "pc", 3.0857e16),
            L("Astronomical unit", "au", 1.495978707e11), L("Angstrom", "A", 1e-10),
        )),
        UnitCategory("Mass", listOf(
            L("Tonne", "t", 1000.0), L("Quintal", "q", 100.0), L("Kilogram", "kg", 1.0),
            L("Hectogram", "hg", 0.1), L("Decagram", "dag", 0.01), L("Gram", "g", 0.001),
            L("Decigram", "dg", 1e-4), L("Centigram", "cg", 1e-5), L("Milligram", "mg", 1e-6),
            L("Microgram", "ug", 1e-9), L("Nanogram", "ng", 1e-12),
            L("Pound", "lb", 0.45359237), L("Ounce", "oz", 0.028349523), L("Stone", "st", 6.35029318),
            L("Carat", "ct", 0.0002), L("Tola", "tola", 0.0116638),
            L("Grain", "gr", 6.479891e-5), L("Dram", "dr", 0.00177185),
            L("Troy ounce", "ozt", 0.0311034768), L("Troy pound", "lbt", 0.3732417216),
            L("Pennyweight", "dwt", 0.00155517), L("Hundredweight (US)", "cwtUS", 45.359237),
            L("Hundredweight (UK)", "cwtUK", 50.802345), L("Ton (US short)", "tonUS", 907.18474),
            L("Ton (UK long)", "tonUK", 1016.0469), L("Slug", "slug", 14.593903),
            L("Atomic mass unit", "u", 1.66053907e-27),
        )),
        UnitCategory("Temperature", listOf(
            Unit.affine("Celsius", "C", 1.0, 273.15),
            Unit.affine("Fahrenheit", "F", 5.0 / 9.0, 273.15 - 32.0 * 5.0 / 9.0),
            Unit.affine("Kelvin", "K", 1.0, 0.0),
            Unit.affine("Rankine", "R", 5.0 / 9.0, 0.0),
            Unit.affine("Reaumur", "Re", 1.25, 273.15),
        )),
        UnitCategory("Area", listOf(
            L("Square kilometre", "km2", 1e6), L("Hectare", "ha", 10000.0), L("Are", "are", 100.0),
            L("Acre", "ac", 4046.8564224), L("Square metre", "m2", 1.0), L("Square decimetre", "dm2", 0.01),
            L("Square centimetre", "cm2", 1e-4), L("Square millimetre", "mm2", 1e-6),
            L("Square mile", "mi2", 2589988.110336), L("Square foot", "ft2", 0.09290304),
            L("Square yard", "yd2", 0.83612736), L("Square inch", "in2", 0.00064516),
            L("Rood", "rood", 1011.7141056), L("Square rod", "rd2", 25.29295),
            L("Bigha", "bigha", 2529.28), L("Guntha", "guntha", 101.17), L("Cent", "cent", 40.4686),
            L("Ground", "ground", 222.967), L("Dunam", "dunam", 1000.0), L("Barn", "barn", 1e-28),
        )),
        UnitCategory("Volume", listOf(
            L("Cubic metre", "m3", 1000.0), L("Cubic decimetre", "dm3", 1.0), L("Litre", "L", 1.0),
            L("Decilitre", "dL", 0.1), L("Centilitre", "cL", 0.01), L("Millilitre", "mL", 0.001),
            L("Cubic centimetre", "cm3", 0.001), L("Cubic millimetre", "mm3", 1e-6),
            L("Gallon (US)", "galUS", 3.785411784), L("Gallon (UK)", "galUK", 4.54609),
            L("Quart (US)", "qt", 0.946352946), L("Quart (UK)", "qtUK", 1.1365225),
            L("Pint (US)", "pt", 0.473176473), L("Pint (UK)", "ptUK", 0.56826125),
            L("Cup (US)", "cup", 0.2365882365), L("Cup (metric)", "cupM", 0.25),
            L("Fluid ounce (US)", "floz", 0.0295735296), L("Fluid ounce (UK)", "flozUK", 0.0284130625),
            L("Tablespoon", "tbsp", 0.0147867648), L("Teaspoon", "tsp", 0.00492892159),
            L("Dessertspoon", "dsp", 0.01), L("Gill (US)", "gill", 0.118294118),
            L("Minim (US)", "minim", 6.161152e-5), L("Fluid dram (US)", "fldr", 0.003696691),
            L("Barrel (oil)", "bbl", 158.987295), L("Cubic foot", "ft3", 28.316846592),
            L("Cubic inch", "in3", 0.016387064), L("Cubic yard", "yd3", 764.554857984),
            L("Board foot", "fbm", 2.359737216), L("Acre-foot", "acft", 1233481.8375475),
            L("Bushel (US)", "bu", 35.23907016688), L("Register ton", "rt", 2831.6846592),
        )),
        UnitCategory("Speed", listOf(
            L("Metre/second", "m/s", 1.0), L("Kilometre/hour", "km/h", 0.2777777778),
            L("Mile/hour", "mph", 0.44704), L("Foot/second", "ft/s", 0.3048),
            L("Knot", "kn", 0.514444444), L("Mach", "Mach", 340.29),
            L("Speed of light", "c", 299792458.0),
        )),
        UnitCategory("Time", listOf(
            L("Nanosecond", "ns", 1e-9), L("Microsecond", "us", 1e-6), L("Millisecond", "ms", 0.001),
            L("Second", "s", 1.0), L("Minute", "min", 60.0), L("Hour", "h", 3600.0),
            L("Day", "day", 86400.0), L("Week", "wk", 604800.0), L("Fortnight", "fn", 1209600.0),
            L("Month (30 d)", "mo", 2592000.0), L("Year", "yr", 31557600.0),
            L("Decade", "dec", 315576000.0), L("Century", "c", 3.15576e9), L("Millennium", "kyr", 3.15576e10),
        )),
        UnitCategory("Data", listOf(
            L("Bit", "bit", 0.125), L("Nibble", "nibble", 0.5), L("Byte", "B", 1.0),
            L("Kilobyte", "KB", 1000.0), L("Kibibyte", "KiB", 1024.0),
            L("Megabyte", "MB", 1e6), L("Mebibyte", "MiB", 1048576.0),
            L("Gigabyte", "GB", 1e9), L("Gibibyte", "GiB", 1073741824.0),
            L("Terabyte", "TB", 1e12), L("Tebibyte", "TiB", 1099511627776.0),
            L("Petabyte", "PB", 1e15), L("Pebibyte", "PiB", 1125899906842624.0),
        )),
        UnitCategory("Data rate", listOf(
            L("bit/s", "bps", 1.0), L("kilobit/s", "kbps", 1000.0), L("megabit/s", "Mbps", 1e6),
            L("gigabit/s", "Gbps", 1e9), L("terabit/s", "Tbps", 1e12),
            L("byte/s", "B/s", 8.0), L("kilobyte/s", "KB/s", 8000.0), L("megabyte/s", "MB/s", 8e6),
        )),
        UnitCategory("Pressure", listOf(
            L("Pascal", "Pa", 1.0), L("Hectopascal", "hPa", 100.0), L("Kilopascal", "kPa", 1000.0),
            L("Megapascal", "MPa", 1e6), L("Bar", "bar", 100000.0), L("Millibar", "mbar", 100.0),
            L("Atmosphere", "atm", 101325.0), L("PSI", "psi", 6894.757293),
            L("mmHg", "mmHg", 133.322368), L("Torr", "torr", 133.322368),
            L("inHg", "inHg", 3386.389), L("cmH2O", "cmH2O", 98.0665), L("Barye", "Ba", 0.1),
        )),
        UnitCategory("Energy", listOf(
            L("Joule", "J", 1.0), L("Kilojoule", "kJ", 1000.0), L("Megajoule", "MJ", 1e6),
            L("Calorie", "cal", 4.184), L("Kilocalorie", "kcal", 4184.0),
            L("Watt-hour", "Wh", 3600.0), L("Kilowatt-hour", "kWh", 3.6e6), L("Megawatt-hour", "MWh", 3.6e9),
            L("Electronvolt", "eV", 1.602176634e-19), L("Kiloelectronvolt", "keV", 1.602176634e-16),
            L("Megaelectronvolt", "MeV", 1.602176634e-13),
            L("BTU", "BTU", 1055.05585262), L("Therm", "therm", 1.05505585262e8),
            L("Erg", "erg", 1e-7), L("Foot-pound", "ftlb", 1.3558179483),
        )),
        UnitCategory("Power", listOf(
            L("Watt", "W", 1.0), L("Kilowatt", "kW", 1000.0), L("Megawatt", "MW", 1e6),
            L("Gigawatt", "GW", 1e9), L("Horsepower (metric)", "hpM", 735.49875),
            L("Horsepower (mech)", "hp", 745.699872), L("BTU/hour", "BTUh", 0.29307107),
            L("Foot-pound/second", "ftlbs", 1.3558179483), L("Calorie/second", "cals", 4.184),
        )),
        UnitCategory("Force", listOf(
            L("Newton", "N", 1.0), L("Kilonewton", "kN", 1000.0), L("Meganewton", "MN", 1e6),
            L("Dyne", "dyn", 1e-5), L("Kilogram-force", "kgf", 9.80665),
            L("Pound-force", "lbf", 4.4482216153), L("Poundal", "pdl", 0.1382549544),
            L("Kip", "kip", 4448.2216153),
        )),
        UnitCategory("Torque", listOf(
            L("Newton metre", "Nm", 1.0), L("Kilonewton metre", "kNm", 1000.0),
            L("Kilogram-force metre", "kgfm", 9.80665), L("Pound-force foot", "lbfft", 1.3558179483),
            L("Pound-force inch", "lbfin", 0.1129848290), L("Dyne centimetre", "dyncm", 1e-7),
        )),
        UnitCategory("Frequency", listOf(
            L("Hertz", "Hz", 1.0), L("Kilohertz", "kHz", 1000.0), L("Megahertz", "MHz", 1e6),
            L("Gigahertz", "GHz", 1e9), L("Terahertz", "THz", 1e12), L("RPM", "rpm", 1.0 / 60.0),
        )),
        UnitCategory("Fuel economy", listOf(
            L("Kilometre/litre", "km/L", 1.0), L("Litre/100 km", "L/100km", 100.0),
            L("Mile/gallon (US)", "mpgUS", 0.4251437075), L("Mile/gallon (UK)", "mpgUK", 0.354006),
            L("Mile/litre", "mi/L", 1.609344),
        )),
        UnitCategory("Illuminance", listOf(
            L("Lux", "lux", 1.0), L("Foot-candle", "fc", 10.7639104),
            L("Phot", "phot", 10000.0), L("Nox", "nox", 0.001),
        )),
        UnitCategory("Radiation", listOf(
            L("Becquerel", "Bq", 1.0), L("Kilobecquerel", "kBq", 1000.0), L("Megabecquerel", "MBq", 1e6),
            L("Curie", "Ci", 3.7e10), L("Millicurie", "mCi", 3.7e7), L("Microcurie", "uCi", 3.7e4),
            L("Gray", "Gy", 1.0), L("Milligray", "mGy", 0.001),
            L("Sievert", "Sv", 1.0), L("Millisievert", "mSv", 0.001),
            L("Rem", "rem", 0.01), L("Rad (dose)", "rd", 0.01),
        )),
        UnitCategory("Electric", listOf(
            L("Ampere", "A", 1.0), L("Milliampere", "mA", 0.001), L("Microampere", "uA", 1e-6),
            L("Kiloampere", "kA", 1000.0), L("Volt", "V", 1.0), L("Millivolt", "mV", 0.001),
            L("Kilovolt", "kV", 1000.0), L("Ohm", "ohm", 1.0), L("Kilo-ohm", "kohm", 1000.0),
            L("Mega-ohm", "Mohm", 1e6), L("Farad", "F", 1.0), L("Millifarad", "mF", 0.001),
            L("Microfarad", "uF", 1e-6), L("Nanofarad", "nF", 1e-9), L("Picofarad", "pF", 1e-12),
            L("Coulomb", "C", 1.0), L("Millicoulomb", "mC", 0.001), L("Microcoulomb", "uC", 1e-6),
            L("Henry", "H", 1.0), L("Millihenry", "mH", 0.001), L("Microhenry", "uH", 1e-6),
            L("Siemens", "S", 1.0), L("Millisiemens", "mS", 0.001),
        )),
        UnitCategory("Density", listOf(
            L("Kilogram/cubic metre", "kg/m3", 1.0), L("Gram/cubic centimetre", "g/cm3", 1000.0),
            L("Gram/millilitre", "g/mL", 1000.0), L("Gram/litre", "g/L", 1.0),
            L("Kilogram/litre", "kg/L", 1000.0), L("Pound/cubic foot", "lb/ft3", 16.018463),
            L("Pound/gallon (US)", "lb/gal", 119.826427),
        )),
        UnitCategory("Viscosity", listOf(
            L("Pascal second", "Pas", 1.0), L("Millipascal second", "mPas", 0.001),
            L("Poise", "P", 0.1), L("Centipoise", "cP", 0.001),
            L("Stokes", "St", 1e-4), L("Centistokes", "cSt", 1e-6),
        )),
        UnitCategory("Cooking", listOf(
            L("Cup", "cupC", 236.5882365), L("Tablespoon", "tbspC", 14.7867648),
            L("Teaspoon", "tspC", 4.92892159), L("Millilitre", "mLC", 1.0), L("Litre", "LC", 1000.0),
            L("Gram", "gC", 1.0), L("Kilogram", "kgC", 1000.0), L("Ounce", "ozC", 28.349523),
            L("Pound", "lbC", 453.59237), L("Pinch", "pinch", 0.3080576), L("Dash", "dash", 0.6161152),
            L("Stick (butter)", "stick", 113.0),
        )),
        UnitCategory("Typography", listOf(
            L("Point", "pt", 0.3527777778), L("Pica", "pc", 4.233333333), L("Pixel (96 dpi)", "px", 0.2645833333),
            L("Millimetre", "mmT", 1.0), L("Centimetre", "cmT", 10.0), L("Inch", "inT", 25.4),
            L("Twip", "twip", 0.0176388889), L("Em (16 px)", "em", 4.233333333),
        )),
        UnitCategory("Sound", listOf(
            L("Decibel", "dB", 1.0), L("Decibel-milliwatt", "dBm", 1.0),
            L("Decibel-watt", "dBW", 1.0), L("Bel", "bel", 10.0), L("Neper", "Np", 8.685889638),
        )),
    )

    /** Total number of units across every category. */
    val unitCount: Int get() = categories.sumOf { it.units.size }

    fun convert(value: Double, from: Unit, to: Unit): Double = to.fromBase(from.toBase(value))
}
