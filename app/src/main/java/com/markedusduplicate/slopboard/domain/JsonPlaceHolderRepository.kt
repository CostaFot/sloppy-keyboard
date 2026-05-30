package com.markedusduplicate.slopboard.domain

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.result.Result
import com.markedusduplicate.common.result.attempt
import com.markedusduplicate.slopboard.domain.model.DomainTodo
import com.markedusduplicate.slopboard.net.JsonPlaceHolderService
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
