package com.feelsokman.slopboard.domain

import com.feelsokman.slopboard.domain.model.DomainTodo
import com.feelsokman.slopboard.net.model.ApiTodo

class TodoMapper {

    fun map(apiTodo: ApiTodo?): DomainTodo {
        return DomainTodo(
            id = apiTodo!!.id!!,
            title = apiTodo.title!!,
            completed = apiTodo.completed!!
        )
    }
}
