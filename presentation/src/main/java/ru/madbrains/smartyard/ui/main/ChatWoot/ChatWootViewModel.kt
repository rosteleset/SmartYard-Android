package ru.madbrains.smartyard.ui.main.ChatWoot

import android.provider.MediaStore.Images
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.getScopeName
import ru.madbrains.domain.interactors.ChatInteractor
import ru.madbrains.domain.model.request.chatMessageRequest.ChatMessageRequest
import ru.madbrains.domain.model.request.chatMessageRequest.SendMessageRequest
import ru.madbrains.domain.model.response.chatResponse.*
import ru.madbrains.smartyard.GenericViewModel


class ChatWootViewModel(
    private val chatInteractor: ChatInteractor
) : GenericViewModel() {

    private val _dataMessage = MutableLiveData<ArrayList<ChatMessageResponseItem>>()
    val dataMessage: LiveData<ArrayList<ChatMessageResponseItem>>
        get() = _dataMessage

    init {
        _dataMessage.value = arrayListOf()
    }


    fun getMessage(before: Int? = null) {
        viewModelScope.withProgress(progress = null) {
            val result = chatInteractor.getMessages(ChatMessageRequest("p", before))
            val arr = messageBuilder(result)
            withContext(Dispatchers.Main) {
                _dataMessage.postValue(arr)
            }
        }
    }


    fun sendMessage(
        message: String? = null,
        messageType: String? = null,
        images: List<String>? = null
    ) {
        viewModelScope.withProgress(progress = null) {
            chatInteractor.sendMessages(
                SendMessageRequest(
                    "p",
                    message = message,
                    messageType = messageType,
                    images = images
                )
            )
        }
    }


    fun refresh(before: Int? = null) {
        _dataMessage.value?.clear()
        dataMessage.value?.clear()
        getMessage(before)
    }


    private fun messageBuilder(result: ChatWootResponse): ArrayList<ChatMessageResponseItem> {
        var messageItem: ChatMessageResponseItem
        val arr = ArrayList<ChatMessageResponseItem>()
        result?.data?.forEach { d ->
            messageItem = chatResponseItemBuilder(d)
            arr.add(messageItem)
        }
        return arr
    }


    private fun chatResponseItemBuilder(d: ChatMessageResponseItem): ChatMessageResponseItem {
        val attach: ArrayList<AttachmentsItem> = arrayListOf()
        if (!d.attachments.isNullOrEmpty()) {
            for (i in 0 until d.attachments!!.size) {
                attach.add(
                    AttachmentsItem(
                        id = d.attachments?.get(i)?.id,
                        account_id = d.attachments?.get(i)?.account_id,
                        message_id = d.attachments?.get(i)?.message_id,
                        data_url = d.attachments?.get(i)?.data_url,
                        thumb_url = d.attachments?.get(i)?.thumb_url,
                        file_type = d.attachments?.get(i)?.file_type,
                        file_size = d.attachments?.get(i)?.file_size,
                        extension = d.attachments?.get(i)?.extension
                    )
                )
            }
        }
        return ChatMessageResponseItem(
            content = d.content ?: "",
            contentAttributes = d.contentAttributes.toString(),
            contentType = d.contentType,
            conversationId = d.conversationId,
            createdAt = d.createdAt,
            id = d.id,
            messageType = d.messageType,
            attachments = attach,
            sender = Sender(
                additionalAttributes = listOf(d.sender.additionalAttributes.toString()),
                availabilityStatus = d.sender.availabilityStatus ?: "",
                availableName = d.sender.availableName ?: "",
                avatarUrl = d.sender.avatarUrl ?: "",
                customAttributes = listOf(d.sender.customAttributes.toString()),
                email = d.sender.email ?: "",
                id = d.sender.id,
                identifier = d.sender.identifier ?: "",
                name = d.sender.name ?: "",
                phoneNumber = d.sender.phoneNumber ?: "",
                thumbnail = d.sender.thumbnail ?: "",
                type = d.sender.type ?: ""
            ),
        )
    }


}
