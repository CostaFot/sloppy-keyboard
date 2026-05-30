package com.markedusduplicate.common


interface FlagProvider {
    val isDebugEnabled: Boolean
    val isRunningUiTest: Boolean
}


