package com.markedusduplicate.slopboard.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.slopboard.BuildConfig
import com.markedusduplicate.slopboard.domain.model.GifItem
import com.markedusduplicate.slopboard.net.GiphyService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

/**
 * Searches Giphy and downloads a chosen gif into app cache, exposed as a `content://` [Uri] via the
 * FileProvider so the IME can grant the target app temporary read access on `commitContent`.
 */
class GifRepository @Inject constructor(
    private val giphyService: GiphyService,
    private val okHttpClient: OkHttpClient,
    private val dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
) {
    suspend fun search(query: String): List<GifItem> = withContext(dispatcherProvider.io) {
        giphyService.search(apiKey = BuildConfig.GIPHY_API_KEY, query = query)
            .data
            .mapNotNull { gif ->
                val images = gif.images ?: return@mapNotNull null
                val gifUrl = images.downsized?.url ?: images.original?.url ?: return@mapNotNull null
                GifItem(
                    previewUrl = images.fixedWidthSmall?.url ?: gifUrl,
                    gifUrl = gifUrl,
                    description = gif.title?.takeIf { it.isNotBlank() } ?: "gif",
                )
            }
    }

    suspend fun downloadToCache(gifUrl: String): Uri = withContext(dispatcherProvider.io) {
        val dir = File(context.cacheDir, GIF_DIR).apply { mkdirs() }
        val file = File(dir, "${gifUrl.hashCode().toUInt()}.gif")
        if (!file.exists() || file.length() == 0L) {
            okHttpClient.newCall(Request.Builder().url(gifUrl).build()).execute().use { response ->
                check(response.isSuccessful) { "Gif download failed: ${response.code}" }
                file.outputStream().use { out -> response.body.byteStream().copyTo(out) }
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private companion object {
        const val GIF_DIR = "gifs"
    }
}
