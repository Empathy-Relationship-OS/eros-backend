import org.gradle.kotlin.dsl.project

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // Internal modules
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":users"))
    implementation(project(":dates"))

    // Ktor dependencies
    implementation(libs.ktor.serialization.kotlinx.json)


    // DB Dependencies
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)

    // Testing
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(libs.mockk)

    implementation(libs.logstash.logback.encoder)
}

tasks.test {
    useJUnitPlatform()
}
