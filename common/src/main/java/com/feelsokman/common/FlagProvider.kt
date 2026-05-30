package com.feelsokman.common


interface FlagProvider {
    val isDebugEnabled: Boolean
    val isRunningUiTest: Boolean
}


