package com.markedusduplicate.common.test

import com.markedusduplicate.common.Toaster
import com.markedusduplicate.common.di.ToastModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ToastModule::class],
)
object OverrideToastModule {
    @Singleton
    @Provides
    fun providesToaster(): Toaster = FakeToaster
}

object FakeToaster : Toaster {
    val toasts = mutableListOf<String>()
    override fun showToast(text: String) {
        toasts.add(text)
    }
}
