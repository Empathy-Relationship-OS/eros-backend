plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // Internal modules
    implementation(project(":common"))
    implementation(project(":database"))

    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.config.yaml)

    // Firebase
    implementation(libs.firebase.admin)

    // DB Dependencies
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)

    // Testing
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
}

tasks.test {
    useJUnitPlatform()
}
