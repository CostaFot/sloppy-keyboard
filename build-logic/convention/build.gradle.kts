import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.feelsokayman.template.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("applicationComposeCommonPlugin") {
            id = "application.compose.common"
            implementationClass = "ApplicationComposeCommonPlugin"
        }
        register("applicationCommon") {
            id = "application.common"
            implementationClass = "ApplicationCommonPlugin"
        }
        register("libraryComposeCommon") {
            id = "library.compose.common"
            implementationClass = "LibraryComposeCommonPlugin"
        }
        register("libraryCommon") {
            id = "library.common"
            implementationClass = "LibraryCommonPlugin"
        }
        register("hiltCommon") {
            id = "hilt.common"
            implementationClass = "HiltCommonPlugin"
        }
    }
}
