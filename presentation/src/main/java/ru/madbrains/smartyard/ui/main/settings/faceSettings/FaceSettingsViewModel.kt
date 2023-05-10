package ru.madbrains.smartyard.ui.main.settings.faceSettings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.domain.interactors.FRSInteractor
import ru.madbrains.domain.model.response.FaceData
import ru.madbrains.smartyard.GenericViewModel

class FaceSettingsViewModel(
    private val frsInteractor: FRSInteractor,
    private val preferenceStorage: PreferenceStorage
) : GenericViewModel() {

    var faces = MutableLiveData<List<FaceData>?>()

    fun listFaces(flatId: Int, noCache: Boolean = false) {
        viewModelScope.withProgress {
            if (noCache) {
                preferenceStorage.xDmApiRefresh = true
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
