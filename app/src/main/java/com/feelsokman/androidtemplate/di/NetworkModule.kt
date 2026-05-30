package com.feelsokman.androidtemplate.di

import android.content.Context
import com.feelsokman.androidtemplate.net.JsonPlaceHolderService
import com.feelsokman.common.FlagProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesCache(@ApplicationContext context: Context): Cache {
        return Cache(context.cacheDir, 10 * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun providesHttpLoggingInterceptor(
        flagProvider: FlagProvider
    ): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = when {
                flagProvider.isDebugEnabled -> HttpLoggingInterceptor.Level.BODY
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        cache: Cache,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient().newBuilder()
            .apply {
                addInterceptor(httpLoggingInterceptor)
                cache(cache)
            }.build()
    }

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    fun providesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .apply {
                baseUrl("https://jsonplaceholder.typicode.com/")
                client(okHttpClient)
                addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType())
                )
            }.build()
    }

    @Provides
    fun providesJsonPlaceHolderService(
        retrofit: Retrofit
    ): JsonPlaceHolderService = retrofit.create(JsonPlaceHolderService::class.java)
}
