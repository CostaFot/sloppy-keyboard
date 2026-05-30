package com.feelsokman.slopboard.keyboard.second

import com.feelsokman.logging.logDebug
import com.feelsokman.slopboard.keyboard.CustomViewModel
import javax.inject.Inject

class SecondViewModel @Inject constructor() : CustomViewModel() {

    init {
        logDebug { "SecondViewModel init ${hashCode()}" }
    }

    override fun onCleared() {
        super.onCleared()
        logDebug { "SecondViewModel cleared ${hashCode()}" }
    }
}
