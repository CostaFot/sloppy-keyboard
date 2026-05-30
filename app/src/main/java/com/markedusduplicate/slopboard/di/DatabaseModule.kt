package com.markedusduplicate.slopboard.di

import android.content.Context
import androidx.room.Room
import com.markedusduplicate.slopboard.data.db.SlopboardDatabase
import com.markedusduplicate.slopboard.data.db.SuggestionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesSlopboardDatabase(@ApplicationContext context: Context): SlopboardDatabase =
        Room.databaseBuilder(context, SlopboardDatabase::class.java, "slopboard.db").build()

    @Provides
    fun providesSuggestionDao(database: SlopboardDatabase): SuggestionDao =
        database.suggestionDao()
}
