plugins {
    id("library.common")
    id("hilt.common")
}

android {
    namespace = "com.markedusduplicate.common.test"
}

dependencies {
    implementation(project(":common"))
    implementation(project(":testing"))
}
