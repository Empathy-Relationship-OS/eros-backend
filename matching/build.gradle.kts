plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // Module dependencies
    implementation(project(":common"))
    implementation(project(":users"))
    implementation(project(":auth"))
    implementation(project(":database"))

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
    testImplementation(libs.kotlin.test.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.server.auth)
    testImplementation(libs.firebase.admin)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.postgresql)
}

tasks.test {
    useJUnitPlatform()
}
