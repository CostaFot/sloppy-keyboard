package com.markedusduplicate.slopboard.suggestion.dictionary

/** Seam over the loaded dictionary so suggestion sources can be unit-tested without the asset. */
fun interface Dictionary {
    /** The loaded [WordIndex], or `null` while the dictionary is still loading. */
    fun indexOrNull(): WordIndex?
}
