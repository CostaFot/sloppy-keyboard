@file:Suppress("NOTHING_TO_INLINE")

package com.feelsokman.logging

import timber.log.Timber

/** Log a verbose exception and a message that will be evaluated lazily when the message is printed */
inline fun logVerbose(t: Throwable? = null, message: () -> String?) = log { Timber.v(t, message()) }

inline fun logVerbose(t: Throwable?) = Timber.v(t)

/** Log a debug exception and a message that will be evaluated lazily when the message is printed */
inline fun logDebug(t: Throwable? = null, message: () -> String?) = log { Timber.d(t, message()) }

inline fun logDebug(t: Throwable?) = Timber.d(t)

/** Log an info exception and a message that will be evaluated lazily when the message is printed */
inline fun logInfo(t: Throwable? = null, message: () -> String?) = log { Timber.i(t, message()) }

inline fun logInfo(t: Throwable?) = Timber.i(t)

/** Log a warning exception and a message that will be evaluated lazily when the message is printed */
inline fun logWarning(t: Throwable? = null, message: () -> String?) = log { Timber.w(t, message()) }

inline fun logWarning(t: Throwable?) = Timber.w(t)

/** Log an error exception and a message that will be evaluated lazily when the message is printed */
inline fun logError(t: Throwable? = null, message: () -> String?) = log { Timber.e(t, message()) }

inline fun logError(t: Throwable?) = Timber.e(t)

/** Log an assert exception and a message that will be evaluated lazily when the message is printed */
inline fun logWhatTheFuck(t: Throwable? = null, message: () -> String?) =
    log { Timber.wtf(t, message()) }

inline fun logWhatTheFuck(t: Throwable?) = Timber.wtf(t)

/** @suppress */
@PublishedApi
internal inline fun log(block: () -> Unit) {
    if (Timber.treeCount > 0) block()
}
