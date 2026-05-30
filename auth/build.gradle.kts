plugins {
    id("library.common")
    id("hilt.common")
}

android {
    namespace = "com.feelsokman.auth"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":design"))
    implementation(project(":logging"))
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
}
