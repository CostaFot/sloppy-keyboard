package com.markedusduplicate.slopboard.data.db

import androidx.room.Entity

/**
 * "what the" -> "hell", seen [count] times. Composite PK on (context, nextWord) so repeated
 * sequences increment a single row rather than inserting duplicates.
 */
@Entity(tableName = "ngrams", primaryKeys = ["context", "nextWord"])
data class NgramEntry(
    val context: String,
    val nextWord: String,
    val count: Int = 1,
)

/** User typed [original], deleted it, typed [replacement] instead. */
@Entity(tableName = "corrections", primaryKeys = ["original", "replacement"])
data class UserCorrection(
    val original: String,
    val replacement: String,
    val count: Int = 1,
)

/** User tapped [acceptedWord] from the suggestion bar while the context was [context]. */
@Entity(tableName = "accepted_suggestions", primaryKeys = ["context", "acceptedWord"])
data class AcceptedSuggestion(
    val context: String,
    val acceptedWord: String,
    val count: Int = 1,
)
