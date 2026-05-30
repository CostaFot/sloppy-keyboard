package com.feelsokman.slopboard.net

import com.feelsokman.slopboard.net.model.ApiTodo
import retrofit2.http.GET
import retrofit2.http.Path

interface JsonPlaceHolderService {

    @GET("/todos/{id}")
    suspend fun getTodo(@Path(value = "id") todoId: Int): ApiTodo
}
