plugins {
    id("application.common")
    id("application.compose.common")
    id("hilt.common")
    id("kotlinx-serialization")
}

android {
    defaultConfig {
        applicationId = "com.feelsokman.androidtemplate.compose"
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "com.feelsokman.testing.CustomTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        androidResources {
            localeFilters += setOf("en", "en-rAU", "it")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Compose Debug")
        }
        val release by getting {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "app_name", "Compose")
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    namespace = "com.feelsokman.androidtemplate"
}

dependencies {
    implementation(project(":auth"))
    implementation(project(":design"))
    implementation(project(":common"))
    implementation(project(":work"))
    implementation(project(":logging"))

    implementation(libs.material.design)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.window.manager)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.coil.kt.okhttp)
    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.svg)
    implementation(libs.coil.kt.gif)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.startup)

    // networking
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testManifest)

    // UI testing
    androidTestImplementation(project(":testing"))
    androidTestImplementation(project(":common-test"))
}
