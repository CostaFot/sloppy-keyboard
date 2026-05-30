package com.feelsokman.slopboard.keyboard.first

import com.feelsokman.logging.logDebug
import com.feelsokman.slopboard.keyboard.CustomViewModel
import com.feelsokman.slopboard.keyboard.KeyboardHandler
import com.feelsokman.slopboard.keyboard.KeyboardMessage
import kotlinx.coroutines.launch
import javax.inject.Inject


class FirstViewModel @Inject constructor(
    private val keyboardHandler: KeyboardHandler,
) : CustomViewModel() {

    init {
        logDebug { "FirstViewModel init ${hashCode()}" }
    }

    override fun onCleared() {
        super.onCleared()
        logDebug { "FirstViewModel cleared ${hashCode()}" }
    }

    fun onText(text: String) {
        viewModelScope.launch {
            keyboardHandler.queue.emit(KeyboardMessage.Text(text))
        }
    }
}
