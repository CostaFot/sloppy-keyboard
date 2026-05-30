plugins {
    id("library.common")
    id("hilt.common")
}

android {
    namespace = "com.feelsokman.work"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":design"))
    implementation(project(":logging"))

    api(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.testing)
    ksp(libs.hilt.ext.compiler)

    androidTestImplementation(libs.androidx.work.testing)
}
