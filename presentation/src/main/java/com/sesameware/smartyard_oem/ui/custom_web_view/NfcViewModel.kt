package com.sesameware.smartyard_oem.ui.custom_web_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class NfcViewModel : ViewModel() {

    sealed class State {
        data object Idle : State()
        data object Scanning : State()
        data object Timeout : State()
        data object NotSupported : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    private var timeoutJob: Job? = null

    fun startScan(timeoutMs: Long) {
        Timber.d("debug_nfc start scan")
        timeoutJob?.cancel()

        _state.value = State.Scanning

        if (timeoutMs <= 0) {
            Timber.d("debug_nfc persistent scan without timeout")
            return
        }

        timeoutJob = viewModelScope.launch {
            delay(timeoutMs.milliseconds)

            if (_state.value is State.Scanning) {
                _state.value = State.Timeout
            }
        }
    }

    fun onTagScanned(uid: String) {
        Timber.d("debug_nfc success uid=$uid")
        timeoutJob?.cancel()
    }

    fun stopScan() {
        Timber.d("debug_nfc stop scan")
        timeoutJob?.cancel()
        _state.value = State.Idle
    }

    fun notSupported() {
        timeoutJob?.cancel()
        _state.value = State.NotSupported
    }
}
