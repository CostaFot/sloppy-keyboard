plugins {
    id("library.common")
}

android {
    namespace = "com.feelsokman.logging"
}

dependencies {
    api(libs.timber)
}
