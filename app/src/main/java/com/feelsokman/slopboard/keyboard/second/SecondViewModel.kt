package com.feelsokman.slopboard.keyboard.second

import com.feelsokman.logging.logDebug
import com.feelsokman.slopboard.retain.RetainedViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SecondViewModelEntryPoint {
    fun secondViewModel(): SecondViewModel
}

class SecondViewModel @Inject constructor() : RetainedViewModel() {

    init {
        logDebug { "SecondViewModel init ${hashCode()}" }
    }

    override fun onCleared() {
        super.onCleared()
        logDebug { "SecondViewModel cleared ${hashCode()}" }
    }
}
