package com.feelsokman.androidtemplate.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.feelsokman.androidtemplate.BuildConfig
import timber.log.Timber


class Ref(var value: Int)

// Note the inline function below which ensures that this function is essentially
// copied at the call site to ensure that its logging only recompositions from the
// original call site.
@Composable
inline fun LogCompositions(tag: String, msg: String) {
    if (BuildConfig.DEBUG) {
        val ref = remember { Ref(0) }
        SideEffect { ref.value++ }
        Timber.tag("LogCompositions").d("Compositions: $msg ${ref.value}")
    }
}
