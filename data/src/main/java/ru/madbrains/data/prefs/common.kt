package ru.madbrains.data.prefs

import java.util.ArrayList

data class SentName(
    val name: String,
    val patronymic: String? = null
) {
    override fun toString(): String {
        return "$name $patronymic"
    }
}

data class AddressOptions(
    private val addresses: MutableMap<Int, AddressOption> = HashMap()
) {
    fun getOption(flatId: Int): AddressOption {
        return addresses.getOrPut(flatId, { AddressOption() })
    }
}
data class AddressOption(
    var notifySoundUri: String? = null
)

class NotificationData {
    val callNotifications: MutableList<Int> = ArrayList()
    val inboxNotifications: MutableList<Int> = ArrayList()

    var lastId: Int = 5
    val currentCallId: Int = 1
    val currentInboxId: Int get() = inboxNotifications.lastOrNull() ?: lastId
    fun addCallNotification(pref: PreferenceStorage) {
        callNotifications.add(++lastId)
        pref.notificationData = this
    }
    fun addInboxNotification(pref: PreferenceStorage) {
        inboxNotifications.add(++lastId)
        pref.notificationData = this
    }
}
