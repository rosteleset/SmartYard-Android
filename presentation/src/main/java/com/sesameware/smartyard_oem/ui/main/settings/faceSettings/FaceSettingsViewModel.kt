package com.sesameware.smartyard_oem.ui.main.settings.faceSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.FRSInteractor
import com.sesameware.domain.model.response.FaceData
import com.sesameware.smartyard_oem.GenericViewModel

class FaceSettingsViewModel(
    private val frsInteractor: FRSInteractor,
    override val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    var faces = MutableLiveData<List<FaceData>?>()

    fun listFaces(flatId: Int, noCache: Boolean = false) {
        viewModelScope.withProgress {
            if (noCache) {
                mPreferenceStorage.xDmApiRefresh = true
            }
            val res = frsInteractor.listFaces(flatId)
            faces.postValue(res?.data)
        }
    }

    fun removeFace(flatId: Int, faceId: Int) {
        viewModelScope.withProgress(progress = null) {
            frsInteractor.disLike(null, flatId, faceId)
            listFaces(flatId, true)
        }
    }
}
