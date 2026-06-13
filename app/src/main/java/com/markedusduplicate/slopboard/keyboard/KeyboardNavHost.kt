package com.markedusduplicate.slopboard.keyboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.markedusduplicate.slopboard.keyboard.main.KeyboardScreen
import com.markedusduplicate.slopboard.keyboard.reply.SmartReplyScreen
import com.markedusduplicate.slopboard.retain.rememberRetainDecorator

/**
 * Wraps the keyboard's [NavDisplay] with a persistent debug nav bar so routes can be reached
 * directly while developing. Navigation lives on [KeyboardStateHolder]; this just wires its
 * functions to the bar's chips and each screen's `onDone`.
 */
@Composable
fun KeyboardNavHost(stateHolder: KeyboardStateHolder) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DebugNavBar(current = stateHolder.currentRoute, onSelect = stateHolder::navigateTo)
        NavDisplay(
            entryDecorators = listOf(rememberRetainDecorator<KeyboardRoute>()),
            backStack = stateHolder.backStack,
            onBack = { stateHolder.back() },
            entryProvider = entryProvider {
                entry<KeyboardRoute.Main> { KeyboardScreen() }
                entry<KeyboardRoute.SmartReply> {
                    SmartReplyScreen(onDone = { stateHolder.navigateTo(KeyboardRoute.Main) })
                }
            },
        )
    }
}

@Composable
private fun DebugNavBar(current: KeyboardRoute?, onSelect: (KeyboardRoute) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Listed inline (at composition time, where the route objects are fully constructed) rather
        // than via a companion registry, which would risk the sealed-object init-order trap.
        for (route in listOf(KeyboardRoute.Main, KeyboardRoute.SmartReply)) {
            FilterChip(
                selected = current == route,
                onClick = { onSelect(route) },
                label = { Text(route.label) },
            )
        }
    }
}
