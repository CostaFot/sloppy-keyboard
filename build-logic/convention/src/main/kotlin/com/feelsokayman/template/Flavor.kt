package com.feelsokayman.template

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

enum class FlavorDimension {
    style
}

enum class Flavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null) {
    free(dimension = FlavorDimension.style, applicationIdSuffix = ".free"),
    premium(dimension = FlavorDimension.style, applicationIdSuffix = ".premium")
}

fun Project.configureFlavors(
    commonExtension: CommonExtension
) {
    commonExtension.apply {
        flavorDimensions += FlavorDimension.style.name
        productFlavors.apply {
            Flavor.values().forEach {
                create(it.name) {
                    dimension = it.dimension.name
                    if (commonExtension is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (it.applicationIdSuffix != null) {
                            this.applicationIdSuffix = it.applicationIdSuffix
                        }
                    }
                }
            }
        }
    }
}
