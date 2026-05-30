package com.feelsokman.androidtemplate.keyboard

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavEntryDecorator
import com.feelsokman.androidtemplate.keyboard.first.FirstViewModel
import com.feelsokman.androidtemplate.keyboard.second.SecondViewModel
import com.feelsokman.logging.logDebug
import javax.inject.Inject
import kotlin.reflect.KClass

class KeyboardStateHolder @Inject constructor(
    private val application: Application,
) : CustomViewModelStoreOwner {
    val backStack = mutableStateListOf<Any>(First)
    private val customViewModelStore = mutableMapOf<Any, CustomViewModel>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : CustomViewModel> get(
        key: Any,
        modelClass: KClass<T>
    ): T = customViewModelStore.getOrPut(key) {
        val entryPoint = application.keyboardEntryPoint()
        when (modelClass) {
            FirstViewModel::class -> entryPoint.firstScreenViewModel()
            SecondViewModel::class -> entryPoint.secondScreenViewModel()
            else -> throw IllegalStateException("Unknown class $modelClass")
        }
    } as T


    private val onPop: (Any) -> Unit = { key ->
        logDebug { "Popping $key" }
        logDebug { "${customViewModelStore.keys}" }
        customViewModelStore[key]?.let { vm ->
            vm.onCleared()
            customViewModelStore.remove(key)
            logDebug { "Cleared VM for $key" }
        }
    }

    val keyboardNavEntryDecorator = KeyboardNavEntryDecorator<Any>(onPop)



    init {
        logDebug { "Init KeyboardStateHolder, id: ${hashCode()}" }
    }
}


class KeyboardNavEntryDecorator<T : Any>(
    onPop: (contentKey: Any) -> Unit
) : NavEntryDecorator<T>(
    decorate = { entry ->
        CompositionLocalProvider(
            LocalKeyProvider provides entry.contentKey
        ) {
            entry.Content()
        }
    },
    onPop = { contentKey ->
        onPop(contentKey)
    }
)