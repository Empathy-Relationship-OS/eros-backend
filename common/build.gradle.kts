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
    api(libs.aws.s3)

    // Testing
    testImplementation(libs.kotlin.test.junit)
}
