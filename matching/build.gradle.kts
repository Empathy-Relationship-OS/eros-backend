plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // Module dependencies
    implementation(project(":common"))
    implementation(project(":users"))

    // Ktor dependencies
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Database dependencies (Exposed ORM)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}
