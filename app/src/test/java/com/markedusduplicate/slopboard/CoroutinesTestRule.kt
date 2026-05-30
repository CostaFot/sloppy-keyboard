package com.markedusduplicate.slopboard

import com.markedusduplicate.common.coroutine.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps the main dispatcher for a [TestDispatcher] for the duration of a test.
 *
 * Pass `eager = true` (default) for an [UnconfinedTestDispatcher] that runs coroutines immediately
 * (best for hot flows and fire-and-forget launches), or `eager = false` for a
 * [StandardTestDispatcher] whose virtual clock must be advanced manually.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutinesTestRule(
    eager: Boolean = true,
    val testDispatcher: TestDispatcher =
        if (eager) UnconfinedTestDispatcher() else StandardTestDispatcher(),
) : TestWatcher() {

    val testDispatcherProvider = object : DispatcherProvider {
        override val io: CoroutineDispatcher = testDispatcher
        override val ui: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
