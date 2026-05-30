package com.feelsokman.androidtemplate.ui

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent

@Module
// always needs ActivityComponent/SingletonComponent to work with retain
@InstallIn(ActivityComponent::class, ViewModelComponent::class)
object BallModule {

    @Provides
    fun providesHello(
    ): Hello = Hello()
}

class Hello