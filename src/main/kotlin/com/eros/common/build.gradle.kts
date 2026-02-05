plugins {
    id("application")
}


application {
    mainClass = "com.eros.common.Main"
}

dependencies {
    // Required for hashing passwords using Bcrypt.
    implementation(libs.jbcrypt)
}
