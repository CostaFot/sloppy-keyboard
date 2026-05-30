plugins {
    id("library.common")
    id("library.compose.common")
    id("hilt.common")
}

android {
    namespace = "com.feelsokman.testing"

    defaultConfig {
        testInstrumentationRunner = "com.feelsokman.testing.CustomTestRunner"
    }

}

dependencies {
    implementation(project(":common"))

    api(libs.androidx.compose.ui.test)
    api(libs.androidx.test.core)
    api(libs.androidx.test.espresso.core)
    api(libs.androidx.test.rules)
    api(libs.androidx.test.runner)
    api(libs.hilt.android.testing)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)

    debugApi(libs.androidx.compose.ui.testManifest)
}
