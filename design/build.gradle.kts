plugins {
    id("library.common")
    id("library.compose.common")
}

android {
    lint {
        checkDependencies = true
    }

    namespace = "com.feelsokman.design"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material.design)
    api(libs.androidx.core.splashscreen)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.material.iconsExtended)
    api(libs.androidx.compose.material3)
    debugApi(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.ui.util)
    api(libs.androidx.compose.runtime)
    lintPublish(project(":lint"))
}
