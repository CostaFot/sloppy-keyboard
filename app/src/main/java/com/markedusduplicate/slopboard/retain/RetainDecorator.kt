package com.markedusduplicate.slopboard.retain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainedValuesStoreRegistry
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.navigation3.runtime.NavEntryDecorator
import com.markedusduplicate.logging.logDebug

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
fun <T : Any> rememberRetainDecorator(
    retainedValuesStoreRegistry: RetainedValuesStoreRegistry = retainRetainedValuesStoreRegistry(),
): RetainDecorator<T> = remember(retainedValuesStoreRegistry) {
    RetainDecorator(
        retainedValuesStoreRegistry = retainedValuesStoreRegistry,
    )
}