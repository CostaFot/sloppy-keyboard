package com.markedusduplicate.slopboard.net

import com.markedusduplicate.slopboard.net.model.GiphySearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyService {

    @GET("v1/gifs/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 15,
        @Query("rating") rating: String = "pg-13",
    ): GiphySearchResponse
}
