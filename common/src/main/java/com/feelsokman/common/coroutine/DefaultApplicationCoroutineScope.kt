package com.feelsokman.common.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object DefaultApplicationCoroutineScope : CoroutineScope by CoroutineScope(SupervisorJob())
