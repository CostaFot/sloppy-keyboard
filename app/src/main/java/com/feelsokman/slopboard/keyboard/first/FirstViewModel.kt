package com.feelsokman.slopboard.keyboard.first

import com.feelsokman.logging.logDebug
import com.feelsokman.slopboard.keyboard.KeyboardHandler
import com.feelsokman.slopboard.keyboard.KeyboardMessage
import com.feelsokman.slopboard.retain.RetainedViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FirstViewModelEntryPoint {
    fun firstViewModel(): FirstViewModel
}

class FirstViewModel @Inject constructor(
    private val keyboardHandler: KeyboardHandler,
) : RetainedViewModel() {

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
