package com.sesameware.smartyard_oem.ui.main.notification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.domain.model.response.Inbox
import com.sesameware.smartyard_oem.Event
import com.sesameware.smartyard_oem.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
class NotificationViewModel(
    private val inboxInteractor: InboxInteractor
) : GenericViewModel() {
    val loaded = MutableLiveData<Event<Inbox>>()

    val progress = MutableLiveData<Boolean>()

    fun finishedLoading() {
        globalData.progressVisibility.postValue(false)
    }

    fun loadInbox() {
        viewModelScope.withProgress(progress = progress) {
            val res = inboxInteractor.inbox()
            loaded.postValue(Event(res.data))
            progress.postValue(false)
        }
    }

    fun onStart() {
        globalData.progressVisibility.postValue(true)
    }
}
