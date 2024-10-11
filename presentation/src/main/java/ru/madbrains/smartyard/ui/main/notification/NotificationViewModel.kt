package ru.madbrains.smartyard.ui.main.notification

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ru.madbrains.domain.interactors.InboxInteractor
import ru.madbrains.domain.model.response.Inbox
import ru.madbrains.smartyard.Event
import ru.madbrains.smartyard.GenericViewModel

/**
 * @author Nail Shakurov
 * Created on 27/03/2020.
 */
class NotificationViewModel(
    private val inboxInteractor: InboxInteractor
) : GenericViewModel() {

    val TAG = NotificationViewModel::class.simpleName

    val loaded = MutableLiveData<Event<Inbox>>()

    val progress = MutableLiveData<Boolean>()

    fun finishedLoading() {
        globalData.progressVisibility.postValue(false)
    }

    fun loadInbox() {
        viewModelScope.withProgress(progress = null) {
            val res = inboxInteractor.inbox()
            loaded.postValue(Event(res.data))
            progress.postValue(false)
        }
    }

    fun delivered(messageId: String) {
        viewModelScope.withProgress {
            inboxInteractor.delivered(messageId)
        }
    }

    fun onStart() {
        globalData.progressVisibility.postValue(true)
    }
}
