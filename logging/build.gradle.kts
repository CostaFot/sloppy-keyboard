plugins {
    id("library.common")
}

android {
    namespace = "com.markedusduplicate.logging"
}

dependencies {
    api(libs.timber)
}
