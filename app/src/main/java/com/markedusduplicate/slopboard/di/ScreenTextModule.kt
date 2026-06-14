package com.markedusduplicate.slopboard.di

import com.markedusduplicate.slopboard.slop.AccessibilityScreenTextReader
import com.markedusduplicate.slopboard.slop.OcrScreenTextReader
import com.markedusduplicate.slopboard.slop.ScreenTextReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ScreenTextModule {

    @Binds
    @OcrScreenText
    fun bindsOcrScreenTextReader(impl: OcrScreenTextReader): ScreenTextReader

    @Binds
    @AccessibilityScreenText
    fun bindsAccessibilityScreenTextReader(impl: AccessibilityScreenTextReader): ScreenTextReader
}
