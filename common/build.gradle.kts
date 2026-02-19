plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // BCrypt for password hashing
    implementation(libs.jbcrypt)

    // Kotlin serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.exposed.core)

    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.core)

    // Testing
    testImplementation(libs.kotlin.test.junit)
}
