plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // BCrypt for password hashing
    implementation(libs.jbcrypt)

    // Kotlin serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    // Testing
    testImplementation(libs.kotlin.test.junit)
}
