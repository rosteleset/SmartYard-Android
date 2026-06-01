package com.sesameware.smartyard_oem.ui.custom_web_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NfcViewModel : ViewModel() {

    sealed class State {
        data object Idle : State()
        data object Scanning : State()
        data object Timeout : State()
        data class Success(val uid: String) : State()
        data class Error(val message: String) : State()
        data object NotSupported : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    private var timeoutJob: Job? = null

    fun startScan(timeoutMs: Long) {
        timeoutJob?.cancel()

        _state.value = State.Scanning

        timeoutJob = viewModelScope.launch {
            delay(timeoutMs)

            if (_state.value is State.Scanning) {
                _state.value = State.Timeout
            }
        }
    }

    fun onTagScanned(uid: String) {
        timeoutJob?.cancel()
        _state.value = State.Success(uid)
    }

    fun stopScan() {
        timeoutJob?.cancel()
        _state.value = State.Idle
    }

    fun notSupported() {
        timeoutJob?.cancel()
        _state.value = State.NotSupported
    }
}
