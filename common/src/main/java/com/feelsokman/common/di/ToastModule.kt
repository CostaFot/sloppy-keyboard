package com.feelsokman.common.di

import android.content.Context
import android.widget.Toast
import com.feelsokman.common.Toaster
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ToastModule {
    @Singleton
    @Provides
    fun providesToaster(
        @ApplicationContext context: Context,
    ): Toaster = object : Toaster {
        override fun showToast(text: String) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

