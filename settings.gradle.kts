pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "android-template-compose"
include(":app")
include(":lint")
include(":design")
include(":common")
include(":testing")
include(":work")
include(":logging")
include(":common-test")
include(":auth")
