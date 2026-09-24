package com.sesameware.smartyard_oem.ui.main.settings.faceSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.DataModule
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.FRSInteractor
import com.sesameware.domain.interfaces.AddressRepository
import com.sesameware.domain.model.response.FaceData
import com.sesameware.domain.model.response.GroupData
import com.sesameware.domain.model.response.Plog
import com.sesameware.smartyard_oem.GenericViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FaceSettingsState(
    val faces: List<FaceData> = emptyList(),
    val groups: List<GroupData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedGroupId: Int? = null // null - all, -1 - ungrouped, else - groupId
)

class FaceSettingsViewModel(
    private val frsInteractor: FRSInteractor,
    private val addressRepository: AddressRepository,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    private val _state = MutableStateFlow(FaceSettingsState())
    val state: StateFlow<FaceSettingsState> = _state.asStateFlow()

    private val _faces = MutableLiveData<List<FaceData>?>()
    //val faces: LiveData<List<FaceData>?> = _faces

    fun listFaces(flatId: Int, noCache: Boolean = false) {
        viewModelScope.withProgress {
            if (noCache) {
                mPreferenceStorage.xDmApiRefresh = true
            }
            _state.update { it.copy(isLoading = true) }
            val res = frsInteractor.listFaces(flatId, null)
            val facesList = res?.data ?: emptyList()
            _faces.postValue(facesList)
            _state.update { it.copy(faces = facesList, isLoading = false, error = null) }
        }
    }

    fun listGroups(flatId: Int) {
        if (!DataModule.providerConfig.hasFaceGroups) return
        viewModelScope.withProgress(progress = null) {
            val res = addressRepository.listGroups(flatId)
            _state.update { it.copy(groups = res?.data ?: emptyList()) }
        }
    }

    fun selectGroup(flatId: Int, groupId: Int?) {
        _state.update { it.copy(selectedGroupId = groupId) }
    }

    fun addGroup(flatId: Int, groupName: String, doTracking: Boolean?) {
        if (groupName.isBlank()) return
        viewModelScope.withProgress {
            val result =  addressRepository.addGroup(flatId, groupName)
            result?.data?.groupId?.let { groupId ->
                if (doTracking == true) {
                    addressRepository.trackEvent(flatId, Plog.EVENT_OPEN_BY_FACE, groupId.toString(), "")
                }
            }
            listGroups(flatId)
        }
    }

    fun updateGroup(flatId: Int, groupId: Int, groupName: String, watcherId: Int?, doTracking: Boolean) {
        if (groupName.isBlank()) return
        viewModelScope.withProgress {
            addressRepository.updateGroup(groupId.toString(), flatId, groupName)
            if (watcherId == null) {
                if (doTracking) {
                    addressRepository.trackEvent(flatId, Plog.EVENT_OPEN_BY_FACE, groupId.toString(), "")
                }
            } else {
                if (!doTracking) {
                    addressRepository.untrackEvent(watcherId)
                }
            }
            listGroups(flatId)
        }
    }

    fun deleteGroup(flatId: Int, groupId: Int) {
        viewModelScope.withProgress {
            addressRepository.deleteGroup(groupId.toString(), flatId)
            _state.update { it.copy(selectedGroupId = null) }
            listFaces(flatId, true)
            listGroups(flatId)
        }
    }

    fun attachFaceToGroup(flatId: Int, faceId: Int, groupId: Int) {
        viewModelScope.withProgress {
            frsInteractor.attachFaceToGroup(faceId, groupId.toString())
            listFaces(flatId, true)
        }
    }

    fun detachFaceFromGroup(flatId: Int, faceId: Int, groupId: Int) {
        viewModelScope.withProgress {
            frsInteractor.detachFaceFromGroup(faceId, groupId.toString())
            listFaces(flatId, true)
        }
    }

    fun clusterFaces(flatId: Int, prefixName: String) {
        if (prefixName.isBlank()) return
        viewModelScope.withProgress {
            frsInteractor.clusterFaces(flatId, prefixName)
            listGroups(flatId)
            listFaces(flatId, true)
        }
    }

    fun removeFace(flatId: Int, faceId: Int) {
        viewModelScope.withProgress(progress = null) {
            frsInteractor.disLike(null, flatId, faceId)
            listFaces(flatId, true)
        }
    }
}
