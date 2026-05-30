package com.markedusduplicate.slopboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Each `record*` does insert-or-increment: try to insert a fresh row (count 1); if the row
 * already exists the insert is ignored and we bump its count by one instead. Wrapped in a
 * transaction so the two statements are atomic.
 */
@Dao
interface SuggestionDao {

    // --- ngrams ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNgramIgnore(entry: NgramEntry): Long

    @Query("UPDATE ngrams SET count = count + 1 WHERE context = :context AND nextWord = :nextWord")
    suspend fun bumpNgram(context: String, nextWord: String)

    @Transaction
    suspend fun recordNgram(context: String, nextWord: String) {
        if (insertNgramIgnore(NgramEntry(context, nextWord)) == -1L) {
            bumpNgram(context, nextWord)
        }
    }

    @Query(
        "SELECT nextWord FROM ngrams WHERE context = :context ORDER BY count DESC, nextWord ASC LIMIT :limit"
    )
    suspend fun topNextWords(context: String, limit: Int): List<String>

    // --- corrections ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCorrectionIgnore(entry: UserCorrection): Long

    @Query("UPDATE corrections SET count = count + 1 WHERE original = :original AND replacement = :replacement")
    suspend fun bumpCorrection(original: String, replacement: String)

    @Transaction
    suspend fun recordCorrection(original: String, replacement: String) {
        if (insertCorrectionIgnore(UserCorrection(original, replacement)) == -1L) {
            bumpCorrection(original, replacement)
        }
    }

    // --- accepted suggestions ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAcceptedIgnore(entry: AcceptedSuggestion): Long

    @Query("UPDATE accepted_suggestions SET count = count + 1 WHERE context = :context AND acceptedWord = :acceptedWord")
    suspend fun bumpAccepted(context: String, acceptedWord: String)

    @Transaction
    suspend fun recordAccepted(context: String, acceptedWord: String) {
        if (insertAcceptedIgnore(AcceptedSuggestion(context, acceptedWord)) == -1L) {
            bumpAccepted(context, acceptedWord)
        }
    }

    @Query(
        "SELECT acceptedWord FROM accepted_suggestions WHERE context = :context ORDER BY count DESC, acceptedWord ASC LIMIT :limit"
    )
    suspend fun topAccepted(context: String, limit: Int): List<String>
}
