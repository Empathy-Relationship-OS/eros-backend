plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // Common module
    implementation(project(":common"))

    // Kotlin serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    // Database dependencies
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari)

    // Flyway migrations
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Testing
    testImplementation(libs.kotlin.test.junit)
}