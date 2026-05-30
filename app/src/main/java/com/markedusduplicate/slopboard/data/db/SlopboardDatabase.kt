package com.markedusduplicate.slopboard.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NgramEntry::class, UserCorrection::class, AcceptedSuggestion::class],
    version = 1,
    exportSchema = false,
)
abstract class SlopboardDatabase : RoomDatabase() {
    abstract fun suggestionDao(): SuggestionDao
}
