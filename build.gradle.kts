// Top-level build file. Plugins are declared here (apply false) and applied in
// :app, :core and :wear. Declaring them all here is what lets a module ask for
// one without repeating the version. Versions come from gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
