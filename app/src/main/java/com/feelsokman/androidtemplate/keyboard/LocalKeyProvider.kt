package com.feelsokman.androidtemplate.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf

object LocalKeyProvider {
    private val LocalKeyProvider = compositionLocalOf<Any?> { null }

    val current: Any?
        @Composable
        get() = LocalKeyProvider.current

    infix fun provides(value: Any): ProvidedValue<Any?> {
        return LocalKeyProvider.provides(value)
    }
}