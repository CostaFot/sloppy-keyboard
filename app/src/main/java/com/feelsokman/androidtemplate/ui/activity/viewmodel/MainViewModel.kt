package com.feelsokman.androidtemplate.ui.activity.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.feelsokman.androidtemplate.domain.JsonPlaceHolderRepository
import com.feelsokman.auth.AndroidAccountManager
import com.feelsokman.logging.logDebug
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val jsonPlaceHolderRepository: JsonPlaceHolderRepository,
    private val workManager: WorkManager,
    private val androidAccountManager: AndroidAccountManager
) : ViewModel() {

    init {
        logDebug { this.hashCode().toString() }
    }

    private val _textData = MutableStateFlow(UUID.randomUUID().toString())
    val state: StateFlow<String>
        get() = _textData

    init {
        viewModelScope.launch {
            androidAccountManager.userState.collect {
                logDebug { "$it" }
            }
        }
    }

    fun getTodo() {
        viewModelScope.launch {
            val result = androidAccountManager.addAccount("costas")
            logDebug { "addAccount :$result" }
        }
    }

    fun cancelWork() {
        viewModelScope.launch {
            val result = androidAccountManager.removeAccount()
            logDebug { "Remove account :$result" }
        }
    }

    fun startTodoWork() {
        viewModelScope.launch {
            val result = androidAccountManager.updateAccount("hihi", "ffkrf")
            logDebug { "updateAccount :$result" }
        }
    }

    fun updateState() {
        _textData.update { UUID.randomUUID().toString() }
    }

    fun doSomethingElse() {
        _textData.update { UUID.randomUUID().toString() }
    }

}
