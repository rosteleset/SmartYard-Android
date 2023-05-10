package ru.madbrains.smartyard.ui.main.chat

import ru.madbrains.data.prefs.PreferenceStorage
import ru.madbrains.smartyard.GenericViewModel
import ru.madbrains.smartyard.md5
import ru.madbrains.smartyard.p8

class ChatViewModel(
    private val mPreferenceStorage: PreferenceStorage
) : GenericViewModel() {

    init {
        globalData.progressVisibility.postValue(true)
    }

    fun getClientIdHash(): String? {
        return mPreferenceStorage.phone?.p8?.md5()
    }

    fun getJsClientInfo(): String {
        return "{name: \"${mPreferenceStorage.sentName?.toString()}\", phone: \"${mPreferenceStorage.phone?.p8}\"}"
    }

    fun finishedLoading() {
        globalData.progressVisibility.postValue(false)
    }
}
