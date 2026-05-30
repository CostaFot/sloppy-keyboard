package com.feelsokman.androidtemplate.keyboard.first

import com.feelsokman.androidtemplate.keyboard.CustomViewModel
import com.feelsokman.androidtemplate.keyboard.KeyboardHandler
import com.feelsokman.androidtemplate.keyboard.KeyboardMessage
import com.feelsokman.logging.logDebug
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
