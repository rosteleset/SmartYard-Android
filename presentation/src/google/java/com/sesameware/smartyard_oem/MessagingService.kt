package com.sesameware.smartyard_oem

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sesameware.data.prefs.PreferenceStorage
import com.sesameware.domain.interactors.AuthInteractor
import com.sesameware.domain.interactors.InboxInteractor
import com.sesameware.domain.model.PushCallData
import com.sesameware.domain.utils.listenerGeneric
import com.sesameware.smartyard_oem.ui.SoundChooser
import com.sesameware.smartyard_oem.ui.call.IncomingCallActivity.Companion.NOTIFICATION_ID
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.notification.NotificationFragment.Companion.BROADCAST_ACTION_NOTIF
import com.sesameware.smartyard_oem.ui.main.pay.PayAddressFragment.Companion.BROADCAST_PAY_UPDATE
import com.squareup.moshi.Moshi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class MessagingService : FirebaseMessagingService(), KoinComponent {
    private var mHandler = Handler(Looper.getMainLooper())
    private val preferenceStorage: PreferenceStorage by inject()
    private val mInteractor: AuthInteractor by inject()
    private val moshi: Moshi by inject()
    private val inboxInteractor: InboxInteractor by inject()
    private val context: Context get() = this

    private val TAG = "notification"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.tag(TAG).d("debug_dmm remoteMessage: from ${remoteMessage.from}; ttl = ${remoteMessage.ttl}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Timber.tag(TAG).d( "debug_dmm Message Notification Body: ${it.body}")
        }

        if (remoteMessage.data.isNotEmpty()) {
            handleNow(remoteMessage)
        }
    }

    private fun handleNow(remoteMessage: RemoteMessage) {
        remoteMessage.data.let { data: MutableMap<String, String> ->
            Timber.tag(TAG).d("debug_dmm push: $data")
            with(data) {
                val dataTitle = get("title")
                val dataBody = get("body")
                when {
                    get("action") == "newAddress" -> {
                        val intentBroadcast = Intent(MainActivity.BROADCAST_LIST_UPDATE)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)

                        val messageId = get("messageId")
                        val messageType = "inbox"
                        val badge = get("badge")?.toInt()
                        val title = dataTitle ?: remoteMessage.notification?.title
                        val message = dataBody ?: remoteMessage.notification?.body

                        sendNotificationInbox(
                            messageId ?: "",
                            title ?: "",
                            message ?: "",
                            messageType,
                            badge ?: 0
                        )
                    }

                    containsKey("server") -> {
                        val json = JSONObject(data as Map<*, *>).toString()
                        Timber.tag(TAG).d("debug_dmm incoming call json: $json")
                        moshi.adapter(PushCallData::class.java).fromJson(json)?.let { msg ->
                            //если пришло значение в поле hash, то параметры pass, live, image игнорируются
                            //и вычисляются из hash
                            msg.hash?.let { hash ->
                                msg.pass = hash
                                msg.live = "${preferenceStorage.providerBaseUrl}call/live/${hash}"
                                msg.image = "${preferenceStorage.providerBaseUrl}call/camshot/${hash}"
                            }

                            //for test
                            /*if (msg.callerId == "Support") {
                                msg.videoToken = ""
                                msg.videoStream = "https://fl4.lanta.me:8443/95594"
                                msg.videoServer = "flussonic"
                                msg.videoType = "webrtc"
                                /*msg.live = ""
                                msg.image = ""*/
                            }*/

                            waitForLinServiceAndRun(msg) {
                                Timber.d("debug_dmm linphone service is running")
                                it.listenAndGetNotifications(msg)
                            }
                        }
                    }

                    get("action") == "inbox" || get("action") == "videoReady" -> {
                        val messageId = get("messageId")
                        val messageType = "inbox"
                        val badge = get("badge")?.toInt()
                        val title = dataTitle ?: remoteMessage.notification?.title
                        val message = dataBody ?: remoteMessage.notification?.body

                        sendNotificationInbox(
                            messageId ?: "",
                            title ?: "",
                            message ?: "",
                            messageType,
                            badge ?: 0
                        )
                    }

                    get("action") == "paySuccess" -> {
                        val intentBroadcast = Intent(BROADCAST_PAY_UPDATE)
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)

                        val messageId = get("messageId")
                        val messageType = get("messageType")
                        val badge = get("badge")?.toInt()
                        val title = dataTitle ?: remoteMessage.notification?.title
                        val message = dataBody ?: remoteMessage.notification?.body

                        sendNotificationInbox(
                            messageId ?: "",
                            title ?: "",
                            message ?: "",
                            messageType ?: "",
                            badge ?: 0
                        )
                    }

                    get("action") == "chat" -> {
                        val badge = get("badge")?.toInt()
                        val title = dataTitle ?: remoteMessage.notification?.title
                        val message = dataBody ?: remoteMessage.notification?.body

                        sendNotificationInbox(
                            "",
                            title ?: "",
                            message ?: "",
                            "chat",
                            badge ?: 0,
                            true
                        )
                    }

                    else -> {
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("debug_dmm new fcm token: $token")
        preferenceStorage.pushToken = token

        if (preferenceStorage.authToken != null) {
            GlobalScope.launch {
                Timber.d("debug_dmm register fcm token: $token")
                mInteractor.registerPushToken(token)
                preferenceStorage.pushTokenRegistered = token
            }
        }
    }

    private fun sendNotificationInbox(
        messageId: String,
        title: String,
        message: String,
        messageType: String,
        badge: Int,
        isChat: Boolean = false
    ) {
        Timber.d("debug_dmm __Notification__")
        preferenceStorage.notificationData.addInboxNotification(preferenceStorage)
        val notId = preferenceStorage.notificationData.currentInboxId

        val tone = SoundChooser.getChosenTone(this, RingtoneManager.TYPE_NOTIFICATION, null, preferenceStorage)
        val soundUri = tone.uri
        Timber.d("debug_dmm soundUri: $soundUri")

        val intent = Intent(this, MainActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.putExtra(NOTIFICATION_MESSAGE_ID, messageId)
        intent.putExtra(NOTIFICATION_MESSAGE_TYPE, TypeMessage.getTypeMessage(messageType))
        intent.putExtra(NOTIFICATION_ID, notId)

        val intentBroadcast = Intent(BROADCAST_ACTION_NOTIF)
        intentBroadcast.putExtra(NOTIFICATION_MESSAGE_ID, messageId)
        intentBroadcast.putExtra(NOTIFICATION_ID, notId)
        intentBroadcast.putExtra(NOTIFICATION_BADGE, badge)
        if (isChat) {
            intentBroadcast.putExtra(NOTIFICATION_CHAT, true)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intentBroadcast)

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_INBOX_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setColor(ContextCompat.getColor(context, R.color.colorAccent))
            .setContentTitle(title)
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)  // используется для Андроид версии ниже 8.0
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notId, notificationBuilder.build())
    }

    companion object {
        const val CHANNEL_INBOX_ID = "channel_inbox"
        const val CHANNEL_CALLS_ID = "channel_calls"
        val CALL_VIBRATION_PATTERN = longArrayOf(0, 1000, 1000)
        const val NOTIFICATION_MESSAGE_ID = "messageId"
        const val NOTIFICATION_MESSAGE_TYPE = "messageType"
        const val NOTIFICATION_BADGE = "badge"
        const val NOTIFICATION_CHAT = "chat"
        const val CALL_STUN = "stun"
        const val CALL_STUN_TRANSPORT = "stn_transport"
        const val CALL_TURN_USERNAME = "turn_username"
        const val CALL_TURN_PASSWORD = "turn_password"
    }

    enum class TypeMessage {
        CHAT, INBOX, NO_DEFINE;

        companion object {
            fun getTypeMessage(type: String): TypeMessage {
                return when (type) {
                    "inbox" -> INBOX
                    "chat" -> CHAT
                    else -> NO_DEFINE
                }
            }
        }
    }

    private fun isAppVisible(): Boolean {
        return ProcessLifecycleOwner
            .get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)
    }

    private fun waitForLinServiceAndRun(fcmCallData: PushCallData, listener: listenerGeneric<LinphoneProvider>) {
        val flagNotification = !isAppVisible()
        fcmCallData.flagNotification = flagNotification
        Thread {
            if (!LinphoneService.isReady()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !flagNotification) {
                    startService(
                        Intent().setClass(context, LinphoneService::class.java).also { intent ->
                            if (fcmCallData.stun?.isNotEmpty() == true) {
                                intent.putExtra(CALL_STUN, fcmCallData.stun)
                                intent.putExtra(CALL_STUN_TRANSPORT, fcmCallData.stun_transport ?: "udp")
                                intent.putExtra(CALL_TURN_USERNAME, fcmCallData.extension)
                                intent.putExtra(CALL_TURN_PASSWORD, fcmCallData.pass)
                            }
                        }
                    )
                } else {
                    startForegroundService(
                        Intent().setClass(context, LinphoneService::class.java).also { intent ->
                            if (fcmCallData.stun?.isNotEmpty() == true) {
                                intent.putExtra(CALL_STUN, fcmCallData.stun)
                                intent.putExtra(CALL_STUN_TRANSPORT, fcmCallData.stun_transport ?: "udp")
                                intent.putExtra(CALL_TURN_USERNAME, fcmCallData.extension)
                                intent.putExtra(CALL_TURN_PASSWORD, fcmCallData.pass)
                            }
                        }
                    )
                }
            }
            while (!LinphoneService.isReady()) {
                try {
                    Thread.sleep(30)
                } catch (e: InterruptedException) {
                    throw RuntimeException("waiting thread sleep() has been interrupted")
                }
            }
            mHandler.post { LinphoneService.instance?.provider?.let { listener(it) } }
        }.start()
    }
}
