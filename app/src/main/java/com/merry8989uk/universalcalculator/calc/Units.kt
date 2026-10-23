package com.merry8989uk.universalcalculator.calc

/**
 * A unit is defined by how it maps to and from its category's base unit.
 *
 * Every category holds one physical quantity only, so you can never "convert"
 * volts into ohms. Units that are not a simple ratio use [Unit.affine]
 * (temperature) or [Unit.reciprocal] (fuel economy).
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

        /**
         * For units that are inversely proportional to the base, like litres per
         * 100 km (= k / (km per litre)). Both directions use the same formula.
         */
        fun reciprocal(name: String, symbol: String, k: Double) =
            Unit(name, symbol, { k / it }, { k / it })
    }
}

data class UnitCategory(val name: String, val units: List<Unit>) {
    fun unit(symbol: String): Unit =
        units.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
            ?: units.first()
}

object Units {

    private fun L(name: String, s: String, f: Double) = Unit.linear(name, s, f)
    private fun A(name: String, s: String, scale: Double, offset: Double) = Unit.affine(name, s, scale, offset)
    private fun R(name: String, s: String, k: Double) = Unit.reciprocal(name, s, k)

    val categories: List<UnitCategory> = listOf(

        UnitCategory("Length", listOf(
            L("Kilometre", "km", 1000.0), L("Hectometre", "hm", 100.0), L("Decametre", "dam", 10.0),
            L("Metre", "m", 1.0), L("Decimetre", "dm", 0.1), L("Centimetre", "cm", 0.01),
            L("Millimetre", "mm", 0.001), L("Micrometre", "um", 1e-6), L("Nanometre", "nm", 1e-9),
            L("Picometre", "pm", 1e-12), L("Megametre", "Mm", 1e6), L("Gigametre", "Gm", 1e9),
            L("Mile", "mi", 1609.344), L("Yard", "yd", 0.9144), L("Foot", "ft", 0.3048),
            L("Inch", "in", 0.0254), L("Nautical mile", "nmi", 1852.0), L("Fathom", "ftm", 1.8288),
            L("Furlong", "fur", 201.168), L("Chain", "ch", 20.1168), L("Rod", "rd", 5.0292),
            L("League", "lea", 4828.032), L("Hand", "hand", 0.1016), L("Mil", "mil", 2.54e-5),
            L("Light year", "ly", 9.4607304725808e15), L("Astronomical unit", "au", 1.495978707e11),
            L("Parsec", "pc", 3.0856775814913673e16), L("Angstrom", "A", 1e-10),
            L("Caliber", "cal", 0.000254), L("Barleycorn", "bc", 0.0084667), L("Span", "span", 0.2286),
            L("Cubit", "cubit", 0.4572), L("Ell", "ell", 1.143), L("Link", "link", 0.201168),
            L("Verst", "verst", 1066.8), L("Arshin", "arshin", 0.7112), L("Sazhen", "sazhen", 2.1336),
            L("Vara", "vara", 0.8359), L("Cable", "cable", 185.2), L("Bolt", "bolt", 36.576),
            L("Skein", "skein", 109.728), L("Shackle", "shackle", 27.432),
            L("Football field", "fbfield", 91.44), L("Marathon", "marathon", 42195.0),
        )),

        UnitCategory("Area", listOf(
            L("Square kilometre", "km2", 1e6), L("Square hectometre", "hm2", 10000.0),
            L("Square decametre", "dam2", 100.0), L("Square metre", "m2", 1.0),
            L("Square decimetre", "dm2", 0.01), L("Square centimetre", "cm2", 1e-4),
            L("Square millimetre", "mm2", 1e-6), L("Square micrometre", "um2", 1e-12),
            L("Hectare", "ha", 10000.0), L("Are", "are", 100.0), L("Decare", "decare", 1000.0),
            L("Acre", "acre", 4046.8564224), L("Square mile", "mi2", 2589988.110336),
            L("Square yard", "yd2", 0.83612736), L("Square foot", "ft2", 0.09290304),
            L("Square inch", "in2", 0.00064516), L("Rood", "rood", 1011.7141056),
            L("Square rod", "rd2", 25.29285264), L("Bigha", "bigha", 2529.285264),
            L("Guntha", "guntha", 101.17141056), L("Cent", "cent", 40.468564224),
            L("Ground", "ground", 222.9672959), L("Dunam", "dunam", 1000.0),
            L("Barn", "barn", 1e-28), L("Section", "section", 2589988.110336),
            L("Township", "township", 93239571.9721), L("Sabin", "sabin", 0.09290304),
            L("Circular mil", "cmil", 5.067074790975e-10), L("Hide", "hide", 404685.64224),
        )),

        UnitCategory("Volume", listOf(
            L("Cubic metre", "m3", 1.0), L("Cubic decimetre", "dm3", 0.001),
            L("Cubic centimetre", "cm3", 1e-6), L("Cubic millimetre", "mm3", 1e-9),
            L("Cubic decametre", "dam3", 1000.0), L("Cubic hectometre", "hm3", 1e6),
            L("Cubic kilometre", "km3", 1e9),
            L("Litre", "L", 0.001), L("Decilitre", "dL", 1e-4), L("Centilitre", "cL", 1e-5),
            L("Millilitre", "mL", 1e-6), L("Microlitre", "uL", 1e-9), L("Nanolitre", "nL", 1e-12),
            L("Picolitre", "pL", 1e-15), L("Hectolitre", "hL", 0.1), L("Kilolitre", "kL", 1.0),
            L("Megalitre", "ML", 1000.0), L("Cubic centimetre (cc)", "cc", 1e-6),
            L("Gallon (US)", "galUS", 0.003785411784), L("Gallon (UK)", "galUK", 0.00454609),
            L("Quart (US)", "qt", 9.46352946e-4), L("Quart (UK)", "qtUK", 1.1365225e-3),
            L("Pint (US)", "pt", 4.73176473e-4), L("Pint (UK)", "ptUK", 5.6826125e-4),
            L("Cup (US)", "cup", 2.365882365e-4), L("Cup (metric)", "cupM", 2.5e-4),
            L("Fluid ounce (US)", "floz", 2.95735295625e-5), L("Fluid ounce (UK)", "flozUK", 2.84130625e-5),
            L("Tablespoon", "tbsp", 1.47867647813e-5), L("Teaspoon", "tsp", 4.92892159375e-6),
            L("Dessertspoon", "dsp", 1e-5), L("Gill", "gill", 1.18294118e-4),
            L("Minim", "minim", 6.161151992e-8), L("Fluid dram", "fldr", 3.696691195e-6),
            L("Oil barrel", "bbl", 0.158987294928), L("Cubic foot", "ft3", 0.028316846592),
            L("Cubic inch", "in3", 1.6387064e-5), L("Cubic yard", "yd3", 0.764554857984),
            L("Board foot", "fbm", 0.002359737216), L("Acre-foot", "acft", 1233.48183754752),
            L("Bushel", "bu", 0.03523907016688), L("Register ton", "rt", 2.8316846592),
        )),

        UnitCategory("Mass", listOf(
            L("Tonne", "t", 1000.0), L("Kilogram", "kg", 1.0), L("Hectogram", "hg", 0.1),
            L("Decagram", "dag", 0.01), L("Gram", "g", 0.001), L("Decigram", "dg", 1e-4),
            L("Centigram", "cg", 1e-5), L("Milligram", "mg", 1e-6), L("Microgram", "ug", 1e-9),
            L("Nanogram", "ng", 1e-12), L("Quintal", "q", 100.0), L("Centner", "centner", 100.0),
            L("Kilotonne", "kt", 1e6), L("Megatonne", "Mt", 1e9), L("Gigatonne", "Gt", 1e12),
            L("Pound", "lb", 0.45359237), L("Ounce", "oz", 0.028349523125),
            L("Stone", "st", 6.35029318), L("Carat", "ct", 2e-4), L("Tola", "tola", 0.0116638125),
            L("Grain", "gr", 6.479891e-5), L("Dram", "dr", 0.0017718451953125),
            L("Troy ounce", "ozt", 0.0311034768), L("Troy pound", "lbt", 0.3732417216),
            L("Pennyweight", "dwt", 0.00155517384), L("Apothecary dram", "apdr", 0.0038879346),
            L("Apothecary ounce", "apoz", 0.0311034768), L("Scruple", "scr", 0.0012959782),
            L("Mark", "mark", 0.2488278144), L("Quarter", "qtr", 12.70058636),
            L("Hundredweight (US)", "cwtUS", 45.359237), L("Hundredweight (UK)", "cwtUK", 50.80234544),
            L("Ton (US)", "tonUS", 907.18474), L("Ton (UK)", "tonUK", 1016.0469088),
            L("Slug", "slug", 14.59390294), L("Atomic mass unit", "u", 1.66053906660e-27),
            L("Solar mass", "Msol", 1.98847e30), L("Earth mass", "Mearth", 5.9722e24),
            L("Planck mass", "Mplanck", 2.176434e-8),
        )),

        UnitCategory("Temperature", listOf(
            A("Celsius", "C", 1.0, 273.15),
            A("Fahrenheit", "F", 5.0 / 9.0, 255.3722222222222),
            A("Kelvin", "K", 1.0, 0.0),
            A("Rankine", "R", 5.0 / 9.0, 0.0),
            A("Reaumur", "Re", 1.25, 273.15),
            A("Delisle", "De", -2.0 / 3.0, 373.15),
        )),

        UnitCategory("Speed", listOf(
            L("Metre/second", "m/s", 1.0), L("Kilometre/hour", "km/h", 0.2777777778),
            L("Mile/hour", "mph", 0.44704), L("Foot/second", "ft/s", 0.3048),
            L("Knot", "kn", 0.5144444444), L("Mach", "Mach", 340.29),
            L("Speed of light", "c", 299792458.0), L("Centimetre/second", "cm/s", 0.01),
            L("Millimetre/second", "mm/s", 0.001), L("Kilometre/minute", "km/min", 16.666666667),
            L("Kilometre/second", "km/s", 1000.0), L("Mile/minute", "mi/min", 26.8224),
            L("Mile/second", "mi/s", 1609.344), L("Yard/second", "yd/s", 0.9144),
            L("Foot/minute", "fpm", 0.00508), L("Foot/hour", "fph", 8.46666667e-5),
            L("Inch/second", "ips", 0.0254), L("Inch/minute", "in/min", 4.23333333e-4),
            L("Furlong/fortnight", "fur/fn", 1.663095238e-4),
        )),

        UnitCategory("Time", listOf(
            L("Nanosecond", "ns", 1e-9), L("Microsecond", "us", 1e-6), L("Millisecond", "ms", 0.001),
            L("Second", "s", 1.0), L("Minute", "min", 60.0), L("Hour", "h", 3600.0),
            L("Day", "day", 86400.0), L("Week", "wk", 604800.0), L("Fortnight", "fn", 1209600.0),
            L("Month (average)", "mo", 2629746.0), L("Year (average)", "yr", 31556952.0),
            L("Decade", "dec", 315569520.0), L("Century", "c", 3155695200.0),
            L("Millennium", "kyr", 31556952000.0), L("Shake", "shake", 1e-8),
            L("Sidereal day", "sday", 86164.0905), L("Julian year", "jyr", 31557600.0),
            L("Tropical year", "tyear", 31556925.2), L("Synodic month", "smonth", 2551442.8),
            L("Olympiad", "olympiad", 126227808.0), L("Lustrum", "lustrum", 157784760.0),
            L("Score", "score", 631139040.0),
        )),

        UnitCategory("Data", listOf(
            L("Bit", "bit", 1.0), L("Nibble", "nibble", 4.0), L("Byte", "B", 8.0),
            L("Kilobyte", "KB", 8000.0), L("Kibibyte", "KiB", 8192.0), L("Megabyte", "MB", 8e6),
            L("Mebibyte", "MiB", 8388608.0), L("Gigabyte", "GB", 8e9), L("Gibibyte", "GiB", 8589934592.0),
            L("Terabyte", "TB", 8e12), L("Tebibyte", "TiB", 8796093022208.0),
            L("Petabyte", "PB", 8e15), L("Pebibyte", "PiB", 9007199254740992.0),
            L("Exabyte", "EB", 8e18), L("Zettabyte", "ZB", 8e21), L("Yottabyte", "YB", 8e24),
            L("Kilobit", "Kb", 1000.0), L("Megabit", "Mb", 1e6), L("Gigabit", "Gb", 1e9),
            L("Terabit", "Tb", 1e12), L("Word", "word", 16.0), L("Block", "block", 32768.0),
            L("Sector", "sector", 32768.0),
        )),

        UnitCategory("Data rate", listOf(
            L("Bit/second", "bps", 1.0), L("Kilobit/second", "kbps", 1000.0),
            L("Megabit/second", "Mbps", 1e6), L("Gigabit/second", "Gbps", 1e9),
            L("Terabit/second", "Tbps", 1e12), L("Byte/second", "B/s", 8.0),
            L("Kilobyte/second", "KB/s", 8000.0), L("Kibibyte/second", "KiB/s", 8192.0),
            L("Megabyte/second", "MB/s", 8e6), L("Mebibyte/second", "MiB/s", 8388608.0),
            L("Gigabyte/second", "GB/s", 8e9), L("Gibibyte/second", "GiB/s", 8589934592.0),
            L("T1 line", "T1", 1544000.0), L("OC-3", "OC3", 155520000.0),
            L("OC-12", "OC12", 622080000.0), L("OC-48", "OC48", 2488320000.0),
        )),

        UnitCategory("Pressure", listOf(
            L("Pascal", "Pa", 1.0), L("Hectopascal", "hPa", 100.0), L("Kilopascal", "kPa", 1000.0),
            L("Megapascal", "MPa", 1e6), L("Gigapascal", "GPa", 1e9), L("Bar", "bar", 1e5),
            L("Millibar", "mbar", 100.0), L("Atmosphere", "atm", 101325.0),
            L("Pound/inch squared", "psi", 6894.757293168), L("Millimetre of mercury", "mmHg", 133.322387415),
            L("Torr", "torr", 133.322368421), L("Inch of mercury", "inHg", 3386.389),
            L("Centimetre of water", "cmH2O", 98.0665), L("Metre of water", "mH2O", 9806.65),
            L("Inch of water", "inH2O", 249.08891), L("Foot of water", "ftH2O", 2989.0669),
            L("Barye", "Ba", 0.1), L("Pieze", "pz", 1000.0), L("kgf/cm2", "kgf/cm2", 98066.5),
            L("Kilopound/inch squared", "ksi", 6894757.293168), L("Megapound/inch squared", "Msi", 6894757293.168),
            L("Ton/inch squared", "tsi", 15444256.3367),
        )),

        UnitCategory("Energy", listOf(
            L("Joule", "J", 1.0), L("Kilojoule", "kJ", 1000.0), L("Megajoule", "MJ", 1e6),
            L("Gigajoule", "GJ", 1e9), L("Calorie", "cal", 4.184), L("Kilocalorie", "kcal", 4184.0),
            L("Calorie (IT)", "calIT", 4.1868), L("Kilocalorie (IT)", "kcalIT", 4186.8),
            L("Watt-hour", "Wh", 3600.0), L("Kilowatt-hour", "kWh", 3.6e6),
            L("Megawatt-hour", "MWh", 3.6e9), L("Gigawatt-hour", "GWh", 3.6e12),
            L("Electronvolt", "eV", 1.602176634e-19), L("Kiloelectronvolt", "keV", 1.602176634e-16),
            L("Megaelectronvolt", "MeV", 1.602176634e-13), L("Gigaelectronvolt", "GeV", 1.602176634e-10),
            L("British thermal unit", "BTU", 1055.05585262), L("Therm", "therm", 1.05505585262e8),
            L("Erg", "erg", 1e-7), L("Foot-pound", "ftlb", 1.3558179483314004),
            L("Foot-poundal", "ftpdl", 0.0421401100938), L("Hartree", "Ha", 4.3597447222071e-18),
            L("Rydberg", "Ry", 2.1798723611035e-18), L("Ton of TNT", "tTNT", 4.184e9),
            L("Kilogram of TNT", "kgTNT", 4.184e6), L("Tonne of oil equivalent", "toe", 4.1868e10),
            L("Barrel of oil equivalent", "boe", 6.1178632e9), L("Quad", "quad", 1.05505585262e18),
            L("Thermie", "thermie", 4.1855e6),
        )),

        UnitCategory("Power", listOf(
            L("Watt", "W", 1.0), L("Kilowatt", "kW", 1000.0), L("Megawatt", "MW", 1e6),
            L("Gigawatt", "GW", 1e9), L("Terawatt", "TW", 1e12),
            L("Horsepower (metric)", "hpM", 735.49875), L("Horsepower (mechanical)", "hp", 745.6998715823),
            L("Horsepower (electric)", "hpE", 746.0), L("Horsepower (boiler)", "hpB", 9809.5),
            L("Horsepower (water)", "hpW", 746.043), L("BTU/hour", "BTU/h", 0.29307107),
            L("Foot-pound/second", "ftlb/s", 1.3558179483), L("Calorie/second", "cal/s", 4.184),
            L("Volt-ampere", "VA", 1.0), L("Kilovolt-ampere", "kVA", 1000.0),
            L("Megavolt-ampere", "MVA", 1e6), L("Volt-ampere reactive", "var", 1.0),
            L("Kilovolt-ampere reactive", "kvar", 1000.0), L("Ton of refrigeration", "TR", 3516.8528420667),
        )),

        UnitCategory("Force", listOf(
            L("Newton", "N", 1.0), L("Kilonewton", "kN", 1000.0), L("Meganewton", "MN", 1e6),
            L("Dyne", "dyn", 1e-5), L("Kilogram-force", "kgf", 9.80665),
            L("Gram-force", "gf", 0.00980665), L("Pound-force", "lbf", 4.4482216152605),
            L("Ounce-force", "ozf", 0.27801385095378), L("Poundal", "pdl", 0.138254954376),
            L("Kip", "kip", 4448.2216152605), L("Sthene", "sn", 1000.0),
            L("Ton-force (US)", "tonf", 9964.01641818352), L("Tonne-force", "tonnef", 9806.65),
        )),

        UnitCategory("Torque", listOf(
            L("Newton metre", "N.m", 1.0), L("Kilonewton metre", "kN.m", 1000.0),
            L("Millinewton metre", "mN.m", 0.001), L("Micronewton metre", "uN.m", 1e-6),
            L("Kilogram-force metre", "kgf.m", 9.80665), L("Kilogram-force centimetre", "kgf.cm", 0.0980665),
            L("Pound-force foot", "lbf.ft", 1.3558179483), L("Pound-force inch", "lbf.in", 0.11298482902),
            L("Ounce-force inch", "ozf.in", 0.0070615518), L("Dyne centimetre", "dyn.cm", 1e-7),
        )),

        UnitCategory("Frequency", listOf(
            L("Hertz", "Hz", 1.0), L("Kilohertz", "kHz", 1000.0), L("Megahertz", "MHz", 1e6),
            L("Gigahertz", "GHz", 1e9), L("Terahertz", "THz", 1e12),
            L("Revolution/minute", "rpm", 0.0166666667), L("Cycle/second", "cps", 1.0),
            L("Radian/second", "rad/s", 0.1591549431), L("Degree/second", "deg/s", 0.0027777778),
            L("Per hour", "1/h", 0.0002777778),
        )),

        UnitCategory("Fuel economy", listOf(
            L("Kilometre/litre", "km/L", 1.0),
            R("Litre/100 km", "L/100km", 100.0),
            L("Mile/gallon (US)", "mpgUS", 0.4251437075),
            L("Mile/gallon (UK)", "mpgUK", 0.354006),
            L("Mile/litre", "mi/L", 1.609344),
        )),

        UnitCategory("Illuminance", listOf(
            L("Lux", "lux", 1.0), L("Kilolux", "klx", 1000.0), L("Millilux", "mlx", 0.001),
            L("Foot-candle", "fc", 10.7639104), L("Phot", "phot", 10000.0), L("Nox", "nox", 0.001),
        )),

        UnitCategory("Luminance", listOf(
            L("Candela/metre squared", "cd/m2", 1.0), L("Nit", "nit", 1.0),
            L("Stilb", "stilb", 10000.0), L("Lambert", "Lb", 3183.0988618),
            L("Foot-lambert", "ftL", 3.4262591), L("Apostilb", "asb", 0.3183098862),
            L("Candela/inch squared", "cd/in2", 1550.0031),
        )),

        UnitCategory("Luminous flux", listOf(
            L("Lumen", "lm", 1.0), L("Kilolumen", "klm", 1000.0), L("Millilumen", "mlm", 0.001),
        )),

        UnitCategory("Radioactivity", listOf(
            L("Becquerel", "Bq", 1.0), L("Kilobecquerel", "kBq", 1000.0),
            L("Megabecquerel", "MBq", 1e6), L("Gigabecquerel", "GBq", 1e9),
            L("Curie", "Ci", 3.7e10), L("Millicurie", "mCi", 3.7e7), L("Microcurie", "uCi", 3.7e4),
        )),

        UnitCategory("Absorbed dose", listOf(
            L("Gray", "Gy", 1.0), L("Milligray", "mGy", 0.001), L("Microgray", "uGy", 1e-6),
            L("Rad", "rad", 0.01), L("Millirad", "mrad", 1e-5),
        )),

        UnitCategory("Equivalent dose", listOf(
            L("Sievert", "Sv", 1.0), L("Millisievert", "mSv", 0.001), L("Microsievert", "uSv", 1e-6),
            L("Rem", "rem", 0.01), L("Millirem", "mrem", 1e-5),
        )),

        UnitCategory("Current", listOf(
            L("Ampere", "A", 1.0), L("Milliampere", "mA", 0.001), L("Microampere", "uA", 1e-6),
            L("Nanoampere", "nA", 1e-9), L("Kiloampere", "kA", 1000.0),
        )),

        UnitCategory("Voltage", listOf(
            L("Volt", "V", 1.0), L("Millivolt", "mV", 0.001), L("Microvolt", "uV", 1e-6),
            L("Kilovolt", "kV", 1000.0), L("Megavolt", "MV", 1e6),
        )),

        UnitCategory("Resistance", listOf(
            L("Ohm", "ohm", 1.0), L("Milliohm", "mohm", 0.001), L("Kilo-ohm", "kohm", 1000.0),
            L("Mega-ohm", "Mohm", 1e6), L("Giga-ohm", "Gohm", 1e9), L("Abohm", "abohm", 1e-9),
        )),

        UnitCategory("Capacitance", listOf(
            L("Farad", "F", 1.0), L("Millifarad", "mF", 0.001), L("Microfarad", "uF", 1e-6),
            L("Nanofarad", "nF", 1e-9), L("Picofarad", "pF", 1e-12), L("Abfarad", "abF", 1e9),
            L("Statfarad", "statF", 1.112650056e-12),
        )),

        UnitCategory("Charge", listOf(
            L("Coulomb", "C", 1.0), L("Millicoulomb", "mC", 0.001), L("Microcoulomb", "uC", 1e-6),
            L("Nanocoulomb", "nC", 1e-9), L("Kilocoulomb", "kC", 1000.0),
            L("Ampere-hour", "Ah", 3600.0), L("Milliampere-hour", "mAh", 3.6),
        )),

        UnitCategory("Inductance", listOf(
            L("Henry", "H", 1.0), L("Millihenry", "mH", 0.001), L("Microhenry", "uH", 1e-6),
            L("Nanohenry", "nH", 1e-9), L("Abhenry", "abH", 1e-9),
        )),

        UnitCategory("Conductance", listOf(
            L("Siemens", "S", 1.0), L("Millisiemens", "mS", 0.001),
            L("Microsiemens", "uS", 1e-6), L("Kilosiemens", "kS", 1000.0),
        )),

        UnitCategory("Magnetic flux density", listOf(
            L("Tesla", "T", 1.0), L("Millitesla", "mT", 0.001), L("Microtesla", "uT", 1e-6),
            L("Nanotesla", "nT", 1e-9), L("Gauss", "G", 1e-4), L("Milligauss", "mG", 1e-7),
            L("Kilogauss", "kG", 0.1),
        )),

        UnitCategory("Magnetic flux", listOf(
            L("Weber", "Wb", 1.0), L("Milliweber", "mWb", 0.001), L("Microweber", "uWb", 1e-6),
            L("Maxwell", "Mx", 1e-8), L("Kiloweber", "kWb", 1000.0),
        )),

        UnitCategory("Density", listOf(
            L("Kilogram/cubic metre", "kg/m3", 1.0), L("Gram/cubic centimetre", "g/cm3", 1000.0),
            L("Gram/millilitre", "g/mL", 1000.0), L("Gram/litre", "g/L", 1.0),
            L("Kilogram/litre", "kg/L", 1000.0), L("Tonne/cubic metre", "t/m3", 1000.0),
            L("Kilogram/cubic decimetre", "kg/dm3", 1000.0), L("Gram/cubic decimetre", "g/dm3", 1.0),
            L("Pound/cubic foot", "lb/ft3", 16.01846337396), L("Pound/cubic inch", "lb/in3", 27679.904710191),
            L("Pound/gallon (US)", "lb/gal", 119.8264273167), L("Ounce/cubic inch", "oz/in3", 1729.994044387),
        )),

        UnitCategory("Dynamic viscosity", listOf(
            L("Pascal second", "Pa.s", 1.0), L("Millipascal second", "mPa.s", 0.001),
            L("Poise", "P", 0.1), L("Centipoise", "cP", 0.001),
            L("Pound/(foot second)", "lb/(ft.s)", 1.4881639436),
            L("Pound-force second/foot squared", "lbf.s/ft2", 47.8802589803),
        )),

        UnitCategory("Kinematic viscosity", listOf(
            L("Metre squared/second", "m2/s", 1.0), L("Stokes", "St", 1e-4),
            L("Centistokes", "cSt", 1e-6), L("Foot squared/second", "ft2/s", 0.09290304),
        )),

        UnitCategory("Cooking (volume)", listOf(
            L("Litre", "L", 1.0), L("Decilitre", "dL", 0.1), L("Centilitre", "cL", 0.01),
            L("Millilitre", "mL", 0.001), L("Cup (US)", "cup", 0.2365882365),
            L("Cup (metric)", "cupM", 0.25), L("Tablespoon", "tbsp", 0.01478676478),
            L("Teaspoon", "tsp", 0.00492892159), L("Dessertspoon", "dsp", 0.01),
            L("Fluid ounce (US)", "floz", 0.0295735295625), L("Fluid ounce (UK)", "flozUK", 0.0284130625),
            L("Pint (US)", "pt", 0.473176473), L("Quart (US)", "qt", 0.946352946),
            L("Gallon (US)", "gal", 3.785411784), L("Butter stick", "stick", 0.113),
            L("Pinch", "pinch", 3.0806e-4), L("Dash", "dash", 6.1612e-4),
        )),

        UnitCategory("Typography", listOf(
            L("Millimetre", "mm", 1.0), L("Centimetre", "cm", 10.0), L("Inch", "in", 25.4),
            L("Point", "pt", 0.3527777778), L("Pica", "pc", 4.233333333),
            L("Didot point", "didot", 0.375972222), L("Cicero", "cicero", 4.511666667),
            L("Twip", "twip", 0.0176388889), L("Agate", "agate", 1.411111111),
        )),

        UnitCategory("Sound level", listOf(
            L("Decibel", "dB", 1.0), L("Bel", "bel", 10.0), L("Neper", "Np", 8.685889638),
        )),

        UnitCategory("Acceleration", listOf(
            L("Metre/second squared", "m/s2", 1.0), L("Standard gravity", "g", 9.80665),
            L("Foot/second squared", "ft/s2", 0.3048), L("Gal", "Gal", 0.01),
            L("Inch/second squared", "in/s2", 0.0254),
        )),

        UnitCategory("Volumetric flow", listOf(
            L("Cubic metre/second", "m3/s", 1.0), L("Cubic metre/hour", "m3/h", 2.777777778e-4),
            L("Cubic metre/minute", "m3/min", 0.01666666667), L("Litre/second", "L/s", 0.001),
            L("Litre/minute", "L/min", 1.666666667e-5), L("Litre/hour", "L/h", 2.777777778e-7),
            L("Millilitre/minute", "mL/min", 1.666666667e-8),
            L("Cubic foot/minute", "cfm", 4.71947443e-4), L("Gallon/minute (US)", "gpm", 6.30901964e-5),
            L("Gallon/hour (US)", "gph", 1.05150327e-6),
        )),

        UnitCategory("Mass flow", listOf(
            L("Kilogram/second", "kg/s", 1.0), L("Gram/second", "g/s", 0.001),
            L("Kilogram/hour", "kg/h", 2.777777778e-4), L("Tonne/hour", "t/h", 0.2777777778),
            L("Pound/hour", "lb/h", 1.25997880e-4),
        )),

        UnitCategory("Concentration (mass)", listOf(
            L("Gram/litre", "g/L", 1.0), L("Milligram/litre", "mg/L", 0.001),
            L("Microgram/litre", "ug/L", 1e-6), L("Nanogram/litre", "ng/L", 1e-9),
            L("Gram/millilitre", "g/mL", 1000.0), L("Milligram/millilitre", "mg/mL", 1.0),
            L("Microgram/millilitre", "ug/mL", 0.001), L("Gram/decilitre", "g/dL", 10.0),
            L("Milligram/decilitre", "mg/dL", 0.01), L("Microgram/decilitre", "ug/dL", 1e-4),
        )),

        UnitCategory("Concentration (molar)", listOf(
            L("Mole/litre", "mol/L", 1.0), L("Millimole/litre", "mmol/L", 0.001),
            L("Micromole/litre", "umol/L", 1e-6), L("Nanomole/litre", "nmol/L", 1e-9),
            L("Picomole/litre", "pmol/L", 1e-12), L("Mole/cubic metre", "mol/m3", 0.001),
        )),

        UnitCategory("Angle", listOf(
            L("Degree", "deg", 1.0), L("Radian", "rad", 57.2957795130823),
            L("Gradian", "grad", 0.9), L("Arcminute", "arcmin", 0.01666666667),
            L("Arcsecond", "arcsec", 2.777777778e-4), L("Turn", "turn", 360.0),
            L("Sextant", "sextant", 60.0), L("Quadrant", "quadrant", 90.0),
            L("Sign", "sign", 30.0), L("Compass point", "point", 11.25),
            L("Mil (NATO)", "milN", 0.05625), L("Mil (Soviet)", "milS", 0.06),
            L("Binary degree", "binDeg", 1.40625),
        )),
    )

    /** Total number of units across every category. */
    val unitCount: Int get() = categories.sumOf { it.units.size }

    fun convert(value: Double, from: Unit, to: Unit): Double = to.fromBase(from.toBase(value))
}
