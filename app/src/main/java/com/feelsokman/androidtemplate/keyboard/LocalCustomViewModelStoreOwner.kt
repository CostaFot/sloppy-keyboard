package com.feelsokman.androidtemplate.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import kotlin.reflect.KClass

object LocalCustomViewModelStoreOwner {
    private val LocalLocalCustomViewModelStoreOwner =
        compositionLocalOf<CustomViewModelStoreOwner?> { null }

    val current: CustomViewModelStoreOwner?
        @Composable
        get() =
            LocalLocalCustomViewModelStoreOwner.current!!

    infix fun provides(
        customViewModelStoreOwner: CustomViewModelStoreOwner
    ): ProvidedValue<CustomViewModelStoreOwner?> {
        return LocalLocalCustomViewModelStoreOwner.provides(customViewModelStoreOwner)
    }
}


interface CustomViewModelStoreOwner {

    fun <T : CustomViewModel> get(key: Any, modelClass: KClass<T>): T

}


inline fun <reified T : CustomViewModel> CustomViewModelStoreOwner.get(key: Any): T =
    get(key, T::class)