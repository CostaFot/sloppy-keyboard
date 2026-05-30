package com.feelsokman.androidtemplate.ui.activity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PullToRefreshViewModel @Inject constructor() : ViewModel() {

    val isRefreshingState = MutableStateFlow(false)

    fun onRefresh() {
        viewModelScope.launch {
            isRefreshingState.update { true }
            delay(5) // simulate loading
            isRefreshingState.update { false }
        }
    }
}