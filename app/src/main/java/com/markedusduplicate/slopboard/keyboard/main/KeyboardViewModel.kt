package com.markedusduplicate.slopboard.keyboard.main

import com.markedusduplicate.logging.logDebug
import com.markedusduplicate.slopboard.keyboard.KeyboardHandler
import com.markedusduplicate.slopboard.keyboard.KeyboardMessage
import com.markedusduplicate.slopboard.retain.RetainedViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface KeyboardViewModelEntryPoint {
    fun keyboardViewModel(): KeyboardViewModel
}

class KeyboardViewModel @Inject constructor(
    private val keyboardHandler: KeyboardHandler,
) : RetainedViewModel() {

    init {
        logDebug { "KeyboardViewModel init ${hashCode()}" }
    }

    override fun onCleared() {
        super.onCleared()
        logDebug { "KeyboardViewModel cleared ${hashCode()}" }
    }

    fun onText(text: String) {
        viewModelScope.launch {
            keyboardHandler.queue.emit(KeyboardMessage.Text(text))
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            keyboardHandler.queue.emit(KeyboardMessage.Delete())
        }
    }
}
