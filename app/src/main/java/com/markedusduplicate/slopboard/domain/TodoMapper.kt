package com.markedusduplicate.slopboard.domain

import com.markedusduplicate.slopboard.domain.model.DomainTodo
import com.markedusduplicate.slopboard.net.model.ApiTodo

class TodoMapper {

    fun map(apiTodo: ApiTodo?): DomainTodo {
        return DomainTodo(
            id = apiTodo!!.id!!,
            title = apiTodo.title!!,
            completed = apiTodo.completed!!
        )
    }
}
