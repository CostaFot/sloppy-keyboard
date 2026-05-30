package com.feelsokman.androidtemplate.retain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.navigation3.runtime.NavEntryDecorator
import com.feelsokman.logging.logDebug

class RetainDecorator<T : Any>(
    retainedValuesStoreRegistry: RetainedValuesStoreRegistry,
) : NavEntryDecorator<T>(
    decorate = { entry ->
        logDebug { "Retaining ${entry.contentKey}" }
        retainedValuesStoreRegistry.LocalRetainedValuesStoreProvider(entry.contentKey) {
            entry.Content()
        }

    },
    onPop = { contentKey ->
        logDebug { "Popping $contentKey" }
        retainedValuesStoreRegistry.clearChild(contentKey)
    }
)


@Composable
fun rememberRetainDecorator(): RetainDecorator<Any> {
    val retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()

    return remember {
        RetainDecorator(
            retainedValuesStoreRegistry = retainedValuesStoreRegistry,
        )
    }
}