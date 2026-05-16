plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    // BCrypt for password hashing
    implementation(libs.jbcrypt)

    // Ktor config (ApplicationConfig used in S3Config.fromApplicationConfig)
    implementation(libs.ktor.server.config.yaml)

    implementation(libs.exposed.core)

    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.core)

    // Kotlin serialization
    implementation(libs.ktor.serialization.kotlinx.json)

    // AWS S3 (S3Config uses AWS SDK types)
    implementation(libs.aws.s3)

    // AWS CloudFront for signed URLs
    implementation(libs.aws.cloudfront)
    implementation(libs.aws.cloudfront.url.signer)

    // Cache - Lettuce (Redis/Valkey client with TLS support)
    implementation(libs.lettuce.core)

    // Kotlin logging
    implementation(libs.kotlin.logging)

    // Testing
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Expose test classes for other modules to use test fixtures
configurations {
    create("testClasses") {
        extendsFrom(configurations["testImplementation"])
    }
}

tasks.register<Jar>("testJar") {
    archiveClassifier.set("test")
    from(sourceSets.test.get().output)
}

artifacts {
    add("testClasses", tasks.named<Jar>("testJar"))
}
