package com.feelsokman.androidtemplate.domain

import com.feelsokman.androidtemplate.domain.model.DomainTodo
import com.feelsokman.androidtemplate.net.model.ApiTodo

class TodoMapper {

    fun map(apiTodo: ApiTodo?): DomainTodo {
        return DomainTodo(
            id = apiTodo!!.id!!,
            title = apiTodo.title!!,
            completed = apiTodo.completed!!
        )
    }
}
