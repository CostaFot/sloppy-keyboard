plugins {
    id("library.common")
    id("hilt.common")
}

android {
    namespace = "com.feelsokman.common"
}

dependencies {
    implementation(project(":logging"))
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
}
