package com.feelsokman.androidtemplate.keyboard.second

import com.feelsokman.androidtemplate.keyboard.CustomViewModel
import com.feelsokman.logging.logDebug
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