package com.feelsokman.slopboard.domain

import com.feelsokman.common.coroutine.DispatcherProvider
import com.feelsokman.common.result.Result
import com.feelsokman.common.result.attempt
import com.feelsokman.slopboard.domain.model.DomainTodo
import com.feelsokman.slopboard.net.JsonPlaceHolderService
import kotlinx.coroutines.withContext
import javax.inject.Inject

class JsonPlaceHolderRepository @Inject constructor(
    private val jsonPlaceHolderService: JsonPlaceHolderService,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun getTodo(todoId: Int): Result<Throwable, DomainTodo> =
        withContext(dispatcherProvider.io) {
            attempt {
                val apiTodo = jsonPlaceHolderService.getTodo(todoId)
                DomainTodo(apiTodo.id!!, apiTodo.title!!, apiTodo.completed!!)
            }
        }
}
