package com.feelsokman.androidtemplate.domain

import com.feelsokman.androidtemplate.domain.model.DomainTodo
import com.feelsokman.androidtemplate.net.JsonPlaceHolderService
import com.feelsokman.common.coroutine.DispatcherProvider
import com.feelsokman.common.result.Result
import com.feelsokman.common.result.attempt
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
