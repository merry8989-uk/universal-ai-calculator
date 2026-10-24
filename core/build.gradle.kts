// The calculation engine on its own, with no Android in it.
//
// Both the phone app and the watch app depend on this, so the two can never
// disagree about what 2+2 is. It is a plain Kotlin library, which also means
// its tests run in a fraction of a second.
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
