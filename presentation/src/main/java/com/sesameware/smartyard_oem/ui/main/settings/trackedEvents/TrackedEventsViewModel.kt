package com.sesameware.smartyard_oem.ui.main.settings.trackedEvents

import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.AddressInteractor
import com.sesameware.domain.model.response.TrackedEvent
import com.sesameware.smartyard_oem.GenericViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackedEventsState(
    val isLoading: Boolean = false,
    val events: List<TrackedEvent> = emptyList(),
    val error: String? = null
)

class TrackedEventsViewModel(
    private val addressInteractor: AddressInteractor
) : GenericViewModel() {

    private val _state = MutableStateFlow(TrackedEventsState())
    val state: StateFlow<TrackedEventsState> = _state.asStateFlow()

    private var currentFlatId: Int = -1

    fun loadEvents(flatId: Int) {
        currentFlatId = flatId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = addressInteractor.getTrackedEvents(flatId)
                val events = response?.data ?: emptyList()
                _state.update { it.copy(isLoading = false, events = events) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun untrackEvent(eventId: Int) {
        viewModelScope.launch {
            try {
                addressInteractor.untrackEvent(eventId)
                loadEvents(currentFlatId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}
