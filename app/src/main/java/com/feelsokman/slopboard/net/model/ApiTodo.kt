package com.feelsokman.slopboard.net.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiTodo(
    val id: Int?,
    val title: String?,
    val completed: Boolean?
)
