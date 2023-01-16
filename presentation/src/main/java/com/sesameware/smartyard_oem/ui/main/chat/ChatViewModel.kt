package com.sesameware.smartyard_oem.ui.main.chat

import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.smartyard_oem.GenericViewModel
import com.sesameware.smartyard_oem.md5

class ChatViewModel(
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    init {
        globalData.progressVisibility.postValue(true)
    }

    fun getClientIdHash(): String? {
        return mPreferenceStorage.phone?.md5()
    }

    fun getJsClientInfo(): String {
        return "{name: \"${mPreferenceStorage.sentName?.toString()}\", phone: \"${mPreferenceStorage.phone}\"}"
    }

    fun finishedLoading() {
        globalData.progressVisibility.postValue(false)
    }
}
